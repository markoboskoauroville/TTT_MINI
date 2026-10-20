/*
 * Copyright (C) 2026 Marko Bosko, Mantra Productions
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package dev.patrickgold.florisboard.dictate.llm

import kotlin.text.Charsets

import android.app.Service
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Message
import android.os.Messenger
import android.os.RemoteException
import android.util.Log
import java.util.concurrent.Executors

/**
 * The model on the phone, in its own process (":llm" in the manifest).
 *
 * The keyboard binds to it while it needs the model and unbinds when it has been idle
 * ([MaLocalLlm]), so the gigabytes are held only while he is dictating. If Android takes the memory
 * back, or llama.cpp crashes, this process dies and the keyboard's process does not.
 *
 * One request at a time on one worker thread. A cancel arrives on the main thread and only sets a
 * flag the worker reads between tokens, so it is heard while a request is running.
 */
class MaLlmService : Service() {

    companion object {
        const val MSG_GENERATE = 1
        const val MSG_CANCEL = 2
        const val MSG_RESULT = 3

        const val KEY_PATH = "path"
        const val KEY_PROMPT = "prompt"
        const val KEY_MAX_TOKENS = "max_tokens"
        const val KEY_N_CTX = "n_ctx"
        const val KEY_TEXT = "text"
        const val KEY_ERROR = "error"
        const val KEY_STATS = "stats"

        private const val TAG = "MaLlmService"
    }

    private val worker = Executors.newSingleThreadExecutor()

    private val messenger = Messenger(
        Handler(Looper.getMainLooper()) { msg ->
            when (msg.what) {
                MSG_CANCEL -> runCatching { MaLlmNative.cancel() }
                MSG_GENERATE -> {
                    val data = Bundle(msg.data)
                    val replyTo = msg.replyTo
                    worker.execute { generate(data, replyTo) }
                }
            }
            true
        },
    )

    private fun generate(data: Bundle, replyTo: Messenger?) {
        val out = Bundle()
        try {
            val path = data.getString(KEY_PATH).orEmpty()
            // Threads: four. The phone has two fast cores and six slow ones; more threads than four
            // wait on the slow ones and are slower overall, fewer leave the prompt reading slow.
            val loadError = MaLlmNative.load(path, data.getInt(KEY_N_CTX, 3072), 4)
            if (loadError != null) {
                out.putString(KEY_ERROR, loadError)
            } else {
                val err = arrayOfNulls<String>(1)
                val bytes = MaLlmNative.generate(
                    data.getString(KEY_PROMPT).orEmpty().toByteArray(Charsets.UTF_8),
                    data.getInt(KEY_MAX_TOKENS, 256),
                    err,
                )
                if (bytes == null) out.putString(KEY_ERROR, err[0] ?: "no answer")
                else out.putString(KEY_TEXT, String(bytes, Charsets.UTF_8))
                out.putLongArray(KEY_STATS, MaLlmNative.lastStats())
            }
        } catch (t: Throwable) {
            Log.e(TAG, "generate failed", t)
            out.putString(KEY_ERROR, t.message ?: t.javaClass.simpleName)
        }
        try {
            replyTo?.send(Message.obtain(null, MSG_RESULT).apply { this.data = out })
        } catch (_: RemoteException) {
            // The keyboard went away while we worked. Nothing to tell.
        }
    }

    override fun onBind(intent: Intent?): IBinder = messenger.binder

    override fun onDestroy() {
        runCatching { MaLlmNative.cancel() }
        worker.execute { runCatching { MaLlmNative.unload() } }
        worker.shutdown()
        super.onDestroy()
    }
}
