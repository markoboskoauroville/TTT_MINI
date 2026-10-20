// The JNI bridge of the style model on the phone (MaPhoneStyle, §191).
//
// One model, one context, one sequence, one request at a time: it runs inside MaLlmService in its
// own process (":llm"), so a crash or the system taking the memory back costs a style pass, never
// the keyboard.
//
// The tokens of the last prompt stay in the cache. The next prompt starts with the same rules, so
// only the part after the longest shared prefix is computed again: the rules are read once per
// loaded model instead of once per message, which is most of the waiting on a phone.

#include <android/log.h>
#include <jni.h>

#include <algorithm>
#include <atomic>
#include <chrono>
#include <string>
#include <vector>

#include "llama.h"

#define TAG "MaLlm"

static llama_model *g_model = nullptr;
static llama_context *g_ctx = nullptr;
static std::string g_path;
static std::vector<llama_token> g_cached;  // what is in the cache now, position by position
static std::atomic<bool> g_cancel{false};
static bool g_backend = false;

// prompt tokens, reused of them, generated, prefill ms, generation ms
static jlong g_stats[5] = {0, 0, 0, 0, 0};

static void log_cb(ggml_log_level level, const char *text, void *) {
    if (level >= GGML_LOG_LEVEL_WARN) {
        __android_log_print(level >= GGML_LOG_LEVEL_ERROR ? ANDROID_LOG_ERROR : ANDROID_LOG_WARN, TAG, "%s", text);
    }
}

static void free_all() {
    if (g_ctx) llama_free(g_ctx);
    if (g_model) llama_model_free(g_model);
    g_ctx = nullptr;
    g_model = nullptr;
    g_path.clear();
    g_cached.clear();
}

static long long now_ms() {
    using namespace std::chrono;
    return duration_cast<milliseconds>(steady_clock::now().time_since_epoch()).count();
}

static jstring jstr(JNIEnv *env, const std::string &s) { return env->NewStringUTF(s.c_str()); }

/** Loads [path] unless it is loaded already. Returns null, or what went wrong. */
extern "C" JNIEXPORT jstring JNICALL
Java_dev_patrickgold_florisboard_dictate_llm_MaLlmNative_load(
        JNIEnv *env, jobject, jstring jpath, jint n_ctx, jint n_threads) {
    const char *p = env->GetStringUTFChars(jpath, nullptr);
    std::string path(p);
    env->ReleaseStringUTFChars(jpath, p);
    if (g_model && g_ctx && g_path == path) return nullptr;
    free_all();
    if (!g_backend) {
        llama_log_set(log_cb, nullptr);
        llama_backend_init();
        g_backend = true;
    }
    llama_model_params mp = llama_model_default_params();
    mp.n_gpu_layers = 0;
    g_model = llama_model_load_from_file(path.c_str(), mp);
    if (!g_model) return jstr(env, "the model file could not be read");
    llama_context_params cp = llama_context_default_params();
    cp.n_ctx = (uint32_t) n_ctx;
    cp.n_batch = 512;
    cp.n_ubatch = 512;
    cp.n_threads = n_threads;
    cp.n_threads_batch = n_threads;
    g_ctx = llama_init_from_model(g_model, cp);
    if (!g_ctx) {
        free_all();
        return jstr(env, "not enough memory for the model");
    }
    g_path = path;
    return nullptr;
}

extern "C" JNIEXPORT void JNICALL
Java_dev_patrickgold_florisboard_dictate_llm_MaLlmNative_unload(JNIEnv *, jobject) {
    free_all();
}

extern "C" JNIEXPORT void JNICALL
Java_dev_patrickgold_florisboard_dictate_llm_MaLlmNative_cancel(JNIEnv *, jobject) {
    g_cancel = true;
}

extern "C" JNIEXPORT jlongArray JNICALL
Java_dev_patrickgold_florisboard_dictate_llm_MaLlmNative_lastStats(JNIEnv *env, jobject) {
    jlongArray out = env->NewLongArray(5);
    env->SetLongArrayRegion(out, 0, 5, g_stats);
    return out;
}

static bool decode(llama_batch &b) {
    if (llama_decode(g_ctx, b) != 0) {
        // The cache no longer matches what we think is in it. Start clean next time.
        llama_memory_clear(llama_get_memory(g_ctx), true);
        g_cached.clear();
        return false;
    }
    return true;
}

/**
 * Continues [prompt] (already in the model's chat format, UTF-8) greedily for at most [max_tokens].
 * Returns the UTF-8 bytes of the answer, or null with the reason in [err] (a one-element array).
 */
extern "C" JNIEXPORT jbyteArray JNICALL
Java_dev_patrickgold_florisboard_dictate_llm_MaLlmNative_generate(
        JNIEnv *env, jobject, jbyteArray jprompt, jint max_tokens, jobjectArray err) {
    auto fail = [&](const char *why) -> jbyteArray {
        env->SetObjectArrayElement(err, 0, env->NewStringUTF(why));
        return nullptr;
    };
    if (!g_model || !g_ctx) return fail("no model loaded");
    g_cancel = false;

    const jsize n = env->GetArrayLength(jprompt);
    std::string prompt((size_t) n, '\0');
    env->GetByteArrayRegion(jprompt, 0, n, reinterpret_cast<jbyte *>(&prompt[0]));

    const llama_vocab *vocab = llama_model_get_vocab(g_model);
    int cap = (int) prompt.size() + 16;
    std::vector<llama_token> toks((size_t) cap);
    int nt = llama_tokenize(vocab, prompt.c_str(), (int32_t) prompt.size(), toks.data(), cap, true, true);
    if (nt < 0) {
        cap = -nt;
        toks.resize((size_t) cap);
        nt = llama_tokenize(vocab, prompt.c_str(), (int32_t) prompt.size(), toks.data(), cap, true, true);
    }
    if (nt <= 0) return fail("the text could not be tokenized");
    toks.resize((size_t) nt);

    const int n_ctx = (int) llama_n_ctx(g_ctx);
    if (nt + 8 >= n_ctx) return fail("the text is too long for the model on the phone");
    const int budget = std::min((int) max_tokens, n_ctx - nt - 1);

    // The shared prefix stays; at least the last prompt token is computed again, for its logits.
    size_t keep = 0;
    while (keep < g_cached.size() && keep < toks.size() && g_cached[keep] == toks[keep]) keep++;
    if (keep >= toks.size()) keep = toks.size() - 1;
    llama_memory_t mem = llama_get_memory(g_ctx);
    if (!llama_memory_seq_rm(mem, 0, (llama_pos) keep, -1)) {
        llama_memory_clear(mem, true);
        keep = 0;
    }
    g_cached.assign(toks.begin(), toks.begin() + (long) keep);

    const long long t0 = now_ms();
    llama_batch b = llama_batch_init(512, 0, 1);
    for (size_t i = keep; i < toks.size();) {
        b.n_tokens = 0;
        for (; i < toks.size() && b.n_tokens < 512; i++) {
            const int k = b.n_tokens++;
            b.token[k] = toks[i];
            b.pos[k] = (llama_pos) i;
            b.n_seq_id[k] = 1;
            b.seq_id[k][0] = 0;
            b.logits[k] = (i == toks.size() - 1) ? 1 : 0;
        }
        if (g_cancel) { llama_batch_free(b); return fail("cancelled"); }
        if (!decode(b)) { llama_batch_free(b); return fail("the model could not read the text"); }
        g_cached.insert(g_cached.end(), toks.begin() + (long) (i - b.n_tokens), toks.begin() + (long) i);
    }
    const long long t1 = now_ms();

    llama_sampler *smpl = llama_sampler_chain_init(llama_sampler_chain_default_params());
    llama_sampler_chain_add(smpl, llama_sampler_init_greedy());  // the same message, the same result
    std::string out;
    int generated = 0;
    char piece[256];
    bool ok = true;
    while (generated < budget) {
        if (g_cancel) { ok = false; break; }
        const llama_token t = llama_sampler_sample(smpl, g_ctx, -1);
        if (llama_vocab_is_eog(vocab, t)) break;
        const int len = llama_token_to_piece(vocab, t, piece, sizeof(piece), 0, false);
        if (len > 0) out.append(piece, (size_t) len);
        generated++;
        b.n_tokens = 1;
        b.token[0] = t;
        b.pos[0] = (llama_pos) g_cached.size();
        b.n_seq_id[0] = 1;
        b.seq_id[0][0] = 0;
        b.logits[0] = 1;
        if (!decode(b)) { ok = false; break; }
        g_cached.push_back(t);
    }
    const long long t2 = now_ms();
    llama_sampler_free(smpl);
    llama_batch_free(b);

    g_stats[0] = nt;
    g_stats[1] = (jlong) keep;
    g_stats[2] = generated;
    g_stats[3] = t1 - t0;
    g_stats[4] = t2 - t1;
    if (!ok) return fail(g_cancel ? "cancelled" : "the model stopped in the middle");

    jbyteArray res = env->NewByteArray((jsize) out.size());
    env->SetByteArrayRegion(res, 0, (jsize) out.size(), reinterpret_cast<const jbyte *>(out.data()));
    return res;
}
