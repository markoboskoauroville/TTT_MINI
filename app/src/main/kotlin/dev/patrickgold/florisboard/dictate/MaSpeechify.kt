/*
 * Copyright (C) 2026 Marko Bosko, Mantra Productions
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package dev.patrickgold.florisboard.dictate

import android.util.Base64
import dev.patrickgold.florisboard.app.FlorisPreferenceStore
import dev.patrickgold.florisboard.dictate.provider.MaKeys
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * Speaking, through Speechify.
 *
 * ### The voices, and why these two
 *
 * **Speechify has no Croatian voice.** Verified against the whole catalogue — 985 voices, every page
 * walked — and there is no `hr-HR` on any model. The only Slavic locales that exist at all are
 * Russian, Polish and Ukrainian.
 *
 * So Croatian is always read by a foreign voice, and the question is only which foreign voice hurts
 * least. Marko listened to five renderings of the same Croatian sentence and chose:
 *
 * - **Lesya**, Ukrainian, for Croatian — Slavic phonetics, so `č ć ž š đ` and the `-lj-` `-nj-`
 *   clusters come out as sounds rather than as spellings.
 * - **Beatrice**, British, for English, and as the second option for Croatian.
 *
 * ### TRAP: the model is not free to choose
 *
 * `lesya` exists ONLY on `simba-multilingual` and `simba-3.0`. Ask for it with `simba-english` and
 * the request fails. And `simba-3.2`, which the documentation recommends, answers **HTTP 400** for
 * any voice outside the eight curated `_32` ids. Both facts are measured, not read.
 *
 * So the model follows the voice, never a global default.
 *
 * ### The key ring never tests speculatively
 *
 * Take the first key not known dead, use it until a real request returns 401 or 403, and only then
 * condemn it and retry the same request with the next. **A dead key costs one wasted request in its
 * whole life, and a healthy ring costs none.** 429 is not death — the key is valid and throttled, so
 * it is rested rather than killed.
 */
object MaSpeechify {

    const val PROVIDER_ID = "speechify"

    private const val BASE = "https://api.sws.speechify.com/v1/audio/speech"
    private const val TIMEOUT_MS = 30_000

    /**
     * One word, and when it is spoken. Times are milliseconds from the start of the audio.
     *
     * Speechify returns these with every synthesis at no extra cost, which is what makes the
     * karaoke word on the spacebar free rather than a second request.
     */
    data class Word(val text: String, val startMs: Int, val endMs: Int)

    /** The words of the last synthesis, in order. Empty when the marks could not be read. */
    @Volatile
    var lastWords: List<Word> = emptyList()
        private set

    /**
     * Pulls the word timings out of a response.
     *
     * The marks arrive as a nested object — a sentence with a `chunks` array of words — rather than
     * the flat list the name suggests. Reading it defensively because a shape change here should
     * cost the karaoke display, never the audio.
     */
    fun parseWords(marks: JSONObject?): List<Word> {
        val chunks = marks?.optJSONArray("chunks") ?: return emptyList()
        return buildList {
            for (i in 0 until chunks.length()) {
                val c = chunks.optJSONObject(i) ?: continue
                if (c.optString("type") != "word") continue
                val value = c.optString("value").trim()
                if (value.isEmpty()) continue
                add(Word(value, c.optInt("start_time", -1), c.optInt("end_time", -1)))
            }
        }.filter { it.startMs >= 0 && it.endMs >= it.startMs }
    }

    /**
     * The word being spoken at [positionMs], or null between words.
     *
     * A plain scan rather than a binary search: a screen is a few hundred words and this runs a few
     * times a second, so the clearer code costs nothing measurable.
     */
    fun wordAt(words: List<Word>, positionMs: Int): String? =
        words.firstOrNull { positionMs >= it.startMs && positionMs < it.endMs }?.text

    /**
     * The sentence being spoken, and which word inside it is current.
     *
     * ### Sentences are derived, not given
     *
     * Measured: a passage of three sentences comes back as ONE mark object of type `sentence` with
     * a flat list of words. There is no per-sentence nesting to read, whatever the type field
     * suggests. So a sentence here is simply the run of words between one ending in `.`, `!` or `?`
     * and the next — which is what the subtitle row needs and costs nothing to compute.
     *
     * Returns the words of the current sentence and the index of the current word within them, or
     * an empty list between sentences.
     */
    fun sentenceAround(words: List<Word>, positionMs: Int): Pair<List<Word>, Int> {
        if (words.isEmpty()) return emptyList<Word>() to -1
        val current = words.indexOfFirst { positionMs >= it.startMs && positionMs < it.endMs }
        if (current < 0) return emptyList<Word>() to -1
        fun ends(w: Word) = w.text.lastOrNull() in setOf('.', '!', '?')
        var start = current
        while (start > 0 && !ends(words[start - 1])) start--
        var end = current
        while (end < words.lastIndex && !ends(words[end])) end++
        return words.subList(start, end + 1) to (current - start)
    }

    /** A voice he can pick, with the model it actually works on. */
    data class Voice(
        val id: String,
        val label: String,
        val detail: String,
        val model: String,
    )

    /**
     * Croatian is read by Ukrainian Lesya by default.
     *
     * Ordered as he ranked them by ear, so the first entry is the shipped default and the settings
     * screen can simply show the list.
     */
    val CROATIAN_VOICES = listOf(
        Voice("lesya", "Lesya", "Ukrainian female \u2014 Slavic sounds", "simba-multilingual"),
        Voice("beatrice_32", "Beatrice", "British female \u2014 warm", "simba-multilingual"),
        Voice("dominika", "Dominika", "Polish female", "simba-multilingual"),
        Voice("daria", "Daria", "Russian female", "simba-multilingual"),
    )

    /** English keeps the English voices, on the model that serves the whole catalogue. */
    val ENGLISH_VOICES = listOf(
        Voice("beatrice_32", "Beatrice", "British female \u2014 warm", "simba-english"),
        Voice("imogen", "Imogen", "British female", "simba-english"),
        Voice("edmund", "Edmund", "British male", "simba-english"),
        Voice("hugh", "Hugh", "British male", "simba-english"),
    )

    /**
     * Speaks a one-line sample in a voice, so he can hear it before choosing.
     *
     * The voice says its OWN name. "Hi, I am Lesya" tells him the accent, the pace and the warmth
     * in four words, and ties the sound to the row he is looking at — which a neutral sentence
     * would not.
     *
     * Written to its own cache file so a preview never overwrites audio the reader is playing.
     */
    fun previewFile(context: android.content.Context, voice: Voice): File? {
        val dest = File(context.cacheDir, "ma_voice_preview.mp3")
        return synthesize("Hi, I am ${voice.label}.", voice, dest)
    }

    fun voicesFor(language: String): List<Voice> =
        if (language == MaLanguage.EN) ENGLISH_VOICES else CROATIAN_VOICES

    /** The voice chosen for [language], falling back to that language's default. */
    fun chosenVoice(language: String): Voice {
        val prefs by FlorisPreferenceStore
        val stored = if (language == MaLanguage.EN) {
            prefs.dictate.maReaderVoiceEn.get()
        } else {
            prefs.dictate.maReaderVoiceHr.get()
        }
        val list = voicesFor(language)
        return list.firstOrNull { it.id == stored } ?: list.first()
    }

    /**
     * Speaks [text] into [dest] as mp3. Null on failure, with the reason in the log.
     *
     * Walks the key ring, and treats each status the way the engine handoff proved correct: 401 and
     * 403 mean the key is dead and the next is tried immediately; anything else means stop and
     * report, because retrying a working key against a real error just spends money twice.
     */
    fun synthesize(text: String, voice: Voice, dest: File): File? {
        val prefs by FlorisPreferenceStore
        val account = prefs.dictate.providerAccounts.get().accounts[PROVIDER_ID] ?: run {
            MaLog.add("read", "no Speechify key configured")
            return null
        }
        val keys = MaKeys.split(account.apiKey).filter { it.isNotBlank() }
        if (keys.isEmpty()) {
            MaLog.add("read", "Speechify account has no keys")
            return null
        }
        for ((index, key) in keys.withIndex()) {
            val result = runCatching { speakOnce(text, voice, key, dest) }.getOrElse { e ->
                MaLog.add("read", "speak failed: ${e.javaClass.simpleName}")
                return null
            }
            when (result) {
                200 -> {
                    MaLog.add("read", "spoke ${text.length} chars as ${voice.label}")
                    return dest
                }
                401, 403 -> {
                    // Dead key. Move to the next and try the same request, so he never sees it.
                    MaLog.add("read", "key ${index + 1} rejected, rolling forward")
                    continue
                }
                429 -> {
                    MaLog.add("read", "key ${index + 1} throttled, rolling forward")
                    continue
                }
                else -> {
                    MaLog.add("read", "speak refused, http $result")
                    return null
                }
            }
        }
        MaLog.add("read", "every Speechify key was refused")
        return null
    }

    /**
     * Checks one key against the voice catalogue. Returns the HTTP status, or -1 for a real network
     * failure.
     *
     * ### Why this exists rather than the shared validator
     *
     * `OpenAiCompatibleClient.validateKey()` asks for `/models`, which every OpenAI-shaped provider
     * has and **Speechify does not**. It answered 404 "The requested resource could not be found",
     * which fell through to the catch-all and was reported as **"no connection"** — sending him to
     * look at his wifi over a key that was perfectly good and an API that was answering.
     *
     * `/v1/voices?limit=1` is the cheapest thing Speechify will answer: one voice, no synthesis, no
     * billing. Measured against the real API — a good key returns 200, a nonsense key returns 401
     * with `{"error":{"code":"unauthorized"}}`.
     *
     * **A test must ask the question the service can answer**, not the question the other services
     * happen to share.
     */
    fun validateKey(key: String): Int = runCatching {
        val conn = (URL("https://api.sws.speechify.com/v1/voices?limit=1").openConnection()
            as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = TIMEOUT_MS
            readTimeout = TIMEOUT_MS
            setRequestProperty("Authorization", "Bearer $key")
        }
        val code = conn.responseCode
        runCatching { conn.disconnect() }
        code
    }.getOrDefault(-1)

    /** One request with one key. Returns the HTTP status; writes [dest] only on 200. */
    private fun speakOnce(text: String, voice: Voice, key: String, dest: File): Int {
        val conn = (URL(BASE).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            connectTimeout = TIMEOUT_MS
            readTimeout = TIMEOUT_MS
            setRequestProperty("Authorization", "Bearer $key")
            setRequestProperty("Content-Type", "application/json")
        }
        val body = JSONObject().apply {
            put("input", text)
            put("voice_id", voice.id)
            put("audio_format", "mp3")
            // The model follows the voice. A global default would break Lesya, who does not exist
            // on simba-english, and simba-3.2 answers 400 for anything outside the curated set.
            put("model", voice.model)
        }
        conn.outputStream.use { it.write(body.toString().toByteArray()) }
        val code = conn.responseCode
        if (code != 200) return code
        val payload = conn.inputStream.bufferedReader().use { it.readText() }
        val json = JSONObject(payload)
        val audio = json.optString("audio_data")
        if (audio.isBlank()) return -1
        // Timings kept alongside the audio, so the karaoke word costs nothing extra.
        lastWords = parseWords(json.optJSONObject("speech_marks"))
        dest.writeBytes(Base64.decode(audio, Base64.DEFAULT))
        return 200
    }
}
