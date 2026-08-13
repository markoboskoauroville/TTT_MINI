/*
 * Copyright (C) 2026 Marko Bosko, Mantra Productions
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package dev.patrickgold.florisboard.dictate.nlp

import android.content.Context
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference

/**
 * A small neural next-word model, behind a contract that cannot slow the keyboard down.
 *
 * ### The rule everything else follows from
 *
 * **[suggest] never computes anything.** It returns whatever the last background pass produced, or
 * nothing, and it returns immediately. Inference happens off the typing path, and its result is
 * collected on the next keystroke.
 *
 * That sounds like a limitation and is the opposite. A keystroke has roughly ten milliseconds before
 * a person feels it; a 20M-parameter int8 model on a Dimensity 7200 needs five to fifteen, and the
 * upper end of that is a keyboard that stutters. But a person typing produces keystrokes hundreds of
 * milliseconds apart, so a prediction started at keystroke N is ready long before keystroke N+1 — it
 * is simply one letter behind, which nobody can see. Waiting for it would be visible; being a letter
 * late is not.
 *
 * The n-gram answers instantly and always. This adds to that answer and can never delay it.
 *
 * ### Memory, which is the real constraint
 *
 * A keyboard is an input method, and Android holds input methods to a far tighter budget than apps.
 * Exceed it and the keyboard is killed mid-sentence — which the user experiences as the keyboard
 * vanishing while they type, with no explanation. That is why the model unloads when idle and why
 * this file cares more about releasing the session than about loading it quickly.
 *
 * ### Absent model
 *
 * With no model file, every method here is a no-op that costs one null check. Nothing is loaded,
 * nothing is scheduled, and the suggestion row behaves exactly as it does today. This is the state
 * the app ships in, and it must be a state that works rather than a state that waits.
 */
object MaNeuralPredictor {

    /** Where a model lives once downloaded. One file, so its presence is the whole check. */
    private const val MODEL_NAME = "predict.int8.onnx"

    /**
     * How long a single pass may take before its result is thrown away.
     *
     * Not a timeout on the typing path — nothing there waits. This guards against a pass that is so
     * slow it would still be running when the next one is due, which on a busy phone turns a queue
     * of predictions into a queue of dropped ones. A pass that misses this is discarded and counted;
     * enough of them and the model is unloaded and left alone, because a model that cannot keep up
     * on this device is not going to start.
     */
    private const val BUDGET_MS = 60L

    /** Consecutive over-budget passes before the model is given up on for this session. */
    private const val STRIKES = 5

    /** Idle time after which the session is released. See the memory note above. */
    private const val IDLE_UNLOAD_MS = 90_000L

    private val session = AtomicReference<Any?>(null)
    private val busy = AtomicBoolean(false)
    private val lastUsed = AtomicLong(0L)
    private val strikes = AtomicLong(0L)
    private val disabled = AtomicBoolean(false)
    private val latest = AtomicReference<List<String>>(emptyList())
    private val latestFor = AtomicReference<String>("")

    /** True when a model has been downloaded. Cheap: one file existence check, cached by the caller. */
    fun isAvailable(context: Context): Boolean = modelFile(context).isFile

    private fun modelFile(context: Context) = File(context.filesDir, MODEL_NAME)

    /**
     * The last completed prediction, if it was for this context. Returns immediately, always.
     *
     * [contextKey] is what the prediction was made for — normally the few words before the cursor.
     * A result computed for different text is discarded rather than shown: a suggestion belonging to
     * a sentence the user has moved on from is worse than no suggestion, because it looks like the
     * keyboard has misunderstood rather than like it has nothing to say.
     */
    fun suggest(contextKey: String): List<String> =
        if (latestFor.get() == contextKey) latest.get() else emptyList()

    /**
     * Asks for a prediction to be computed in the background. Returns at once.
     *
     * Drops the request when one is already running rather than queueing it. A queue here would
     * mean the model working on text from three keystrokes ago while the user has typed a word — the
     * work would complete and be useless, having cost the battery anyway. The newest request is
     * always the only one worth doing.
     */
    fun requestAsync(context: Context, contextKey: String, run: (() -> Unit) -> Unit) {
        if (disabled.get() || contextKey.isBlank()) return
        if (!isAvailable(context)) return
        if (!busy.compareAndSet(false, true)) return
        run {
            try {
                val started = System.currentTimeMillis()
                val words = infer(context, contextKey)
                val took = System.currentTimeMillis() - started
                if (took > BUDGET_MS) {
                    // Too slow to be useful. Count it, and stop entirely once it is clearly the
                    // device rather than one unlucky pass.
                    if (strikes.incrementAndGet() >= STRIKES) {
                        disabled.set(true)
                        unload()
                    }
                } else {
                    strikes.set(0)
                    latest.set(words)
                    latestFor.set(contextKey)
                }
                lastUsed.set(System.currentTimeMillis())
            } catch (t: Throwable) {
                // A model that throws is a model that is wrong for this build — a shape mismatch, a
                // truncated download, an operator this runtime does not have. It is switched off for
                // the session rather than retried, because the same input will fail the same way and
                // a keyboard must not spend a user's battery rediscovering that on every keystroke.
                disabled.set(true)
                unload()
            } finally {
                busy.set(false)
            }
        }
    }

    /**
     * Releases the session if it has been idle long enough. Safe to call often; does nothing when
     * there is nothing loaded.
     *
     * Called from the keyboard's own lifecycle rather than from a timer, because a timer would keep
     * a process alive to free memory, which is the opposite of the point.
     */
    fun trimIfIdle() {
        val last = lastUsed.get()
        if (last != 0L && System.currentTimeMillis() - last > IDLE_UNLOAD_MS) unload()
    }

    /** Drops the session and everything it holds. */
    fun unload() {
        val s = session.getAndSet(null) ?: return
        runCatching { (s as? AutoCloseable)?.close() }
        latest.set(emptyList())
        latestFor.set("")
    }

    /**
     * The forward pass.
     *
     * Deliberately unimplemented, and this is the honest state rather than an oversight: no model
     * has been chosen, so its input names, tensor shape and tokenizer are unknown, and writing an
     * inference body against a guessed signature would be code that has never run pretending to be
     * code that works.
     *
     * What is real is everything around it. The contract above — never blocking, dropping stale
     * results, unloading when idle, giving up on a device that cannot keep up — is the part that is
     * difficult to get right and the part that a model cannot be safely added without.
     *
     * To fill this in: put the ONNX file at [MODEL_NAME] in filesDir, create one OrtSession from
     * OrtEnvironment.getEnvironment() and cache it in [session], tokenize [contextKey], run, and
     * return the top few tokens. ONNX Runtime and its native libraries are already in the APK —
     * fetched by tools/fetch-sherpa-onnx.sh for the speech engine — so nothing new needs adding.
     */
    private fun infer(context: Context, contextKey: String): List<String> = emptyList()
}
