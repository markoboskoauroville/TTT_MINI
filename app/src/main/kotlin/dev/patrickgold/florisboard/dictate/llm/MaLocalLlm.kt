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

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Message
import android.os.Messenger
import dev.patrickgold.florisboard.dictate.MaLog
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

/**
 * The keyboard's side of the model on the phone: binds to [MaLlmService], sends one prompt, waits.
 *
 * Bound on the first request and released after [IDLE_MS] without one, so the model is in memory
 * while he is dictating and gone when he is not. A request cancelled here (the stop key) is
 * cancelled there too, between two tokens.
 */
object MaLocalLlm {

    /** How long the model stays loaded after the last request. Loading it again takes seconds. */
    private const val IDLE_MS = 5 * 60_000L

    data class Answer(
        val text: String,
        val promptTokens: Long,
        val reusedTokens: Long,
        val generatedTokens: Long,
        val prefillMs: Long,
        val generateMs: Long,
    )

    class LocalModelException(message: String) : Exception(message)

    private var app: Context? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val lock = Mutex()
    private var service: Messenger? = null
    private var connection: ServiceConnection? = null
    private var idleJob: Job? = null
    /** The request waiting for its answer, so a dying service can fail it instead of leaving it. */
    private var pending: CompletableDeferred<Bundle>? = null

    fun attach(context: Context) {
        app = context.applicationContext
    }

    /** Continues [prompt] with [m]. Throws [LocalModelException] with a sentence he can read. */
    suspend fun complete(m: MaPhoneModels.PhoneModel, prompt: String, maxTokens: Int): Answer = lock.withLock {
        val context = app ?: throw LocalModelException("the keyboard has not started yet")
        val file = MaPhoneModels.file(context, m)
        if (!MaPhoneModels.isInstalled(context, m)) {
            throw LocalModelException("${m.name} is not downloaded. Settings → Prompts → Models on the phone.")
        }
        idleJob?.cancel()
        try {
            val messenger = withContext(Dispatchers.Main) { bind(context) }
            val reply = CompletableDeferred<Bundle>()
            pending = reply
            val replyTo = Messenger(
                Handler(Looper.getMainLooper()) { msg ->
                    if (msg.what == MaLlmService.MSG_RESULT) reply.complete(Bundle(msg.data))
                    true
                },
            )
            val data = Bundle().apply {
                putString(MaLlmService.KEY_PATH, file.absolutePath)
                putString(MaLlmService.KEY_PROMPT, prompt)
                putInt(MaLlmService.KEY_MAX_TOKENS, maxTokens)
                putInt(MaLlmService.KEY_N_CTX, m.nCtx)
            }
            messenger.send(Message.obtain(null, MaLlmService.MSG_GENERATE).apply {
                this.data = data
                this.replyTo = replyTo
            })
            val out = try {
                reply.await()
            } catch (e: CancellationException) {
                runCatching { messenger.send(Message.obtain(null, MaLlmService.MSG_CANCEL)) }
                throw e
            }
            out.getString(MaLlmService.KEY_ERROR)?.let { throw LocalModelException(it) }
            val s = out.getLongArray(MaLlmService.KEY_STATS) ?: LongArray(5)
            Answer(out.getString(MaLlmService.KEY_TEXT).orEmpty(), s[0], s[1], s[2], s[3], s[4]).also {
                MaLog.add(
                    "phone-llm",
                    "${m.name}: ${it.promptTokens} in (${it.reusedTokens} cached), ${it.generatedTokens} out, " +
                        "read ${it.prefillMs} ms, wrote ${it.generateMs} ms",
                )
            }
        } finally {
            pending = null
            idleJob = scope.launch {
                delay(IDLE_MS)
                unbind()
            }
        }
    }

    private suspend fun bind(context: Context): Messenger {
        service?.let { return it }
        // A connection whose process died is still bound and would come back by itself; one
        // connection at a time, so the old one goes before the new one.
        connection?.let { old -> runCatching { context.unbindService(old) } }
        connection = null
        val ready = CompletableDeferred<Messenger>()
        val conn = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
                val m = Messenger(binder)
                service = m
                ready.complete(m)
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                // The ":llm" process died: out of memory, most likely. The keyboard lives on.
                service = null
                MaLog.add("phone-llm", "the model's process ended")
                pending?.completeExceptionally(LocalModelException("the phone took the model's memory back"))
                ready.completeExceptionally(LocalModelException("the model could not start"))
            }
        }
        connection = conn
        if (!context.bindService(Intent(context, MaLlmService::class.java), conn, Context.BIND_AUTO_CREATE)) {
            connection = null
            throw LocalModelException("the model could not start")
        }
        return withTimeout(30_000) { ready.await() }
    }

    private fun unbind() {
        val conn = connection ?: return
        connection = null
        service = null
        runCatching { app?.unbindService(conn) }
    }
}
