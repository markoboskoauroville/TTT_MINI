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

import android.content.Context
import android.media.MediaPlayer
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dev.patrickgold.florisboard.dictate.overlay.DictateAccessibilityService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Reading the screen aloud, and stopping when told.
 *
 * ### Three states, one key
 *
 * Idle, speaking, paused. A short press moves to the next sensible one — speak, pause, resume — and
 * the key's face shows what the NEXT press will do rather than what is happening now, because a
 * control that describes its own state leaves him working out what pressing it would achieve.
 *
 * ### Why the audio is one file
 *
 * Speechify returns the whole passage as a single mp3, so pausing is a real pause in playback and
 * resuming continues mid-sentence exactly where the voice was. Splitting it into sentences would buy
 * finer seeking and cost a synthesis per sentence, which is his money.
 *
 * ### Leaving stops it
 *
 * Nothing here follows him to another app. A voice still describing a screen he has left is the same
 * shape of mistake as a mode with no way out, and the fix is the same one: an unconditional stop
 * that does not depend on him finding a control.
 */
object MaReader {

    enum class State { IDLE, LOADING, SPEAKING, PAUSED }

    var state by mutableStateOf(State.IDLE)
        private set

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var player: MediaPlayer? = null

    /**
     * The one press. Speaks the screen, pauses, or resumes, depending on where it is.
     *
     * [onMessage] carries anything he needs told — no key, nothing readable, a refusal — because
     * this object has no screen of its own and silence after a press is indistinguishable from a
     * broken feature.
     */
    fun toggle(context: Context, onMessage: (String) -> Unit) {
        when (state) {
            State.SPEAKING -> {
                runCatching { player?.pause() }
                state = State.PAUSED
            }
            State.PAUSED -> {
                runCatching { player?.start() }
                state = State.SPEAKING
            }
            // A press while it is still fetching is taken as "stop", not as a second request. The
            // alternative is two syntheses racing for the same speaker.
            State.LOADING -> stop()
            State.IDLE -> start(context, onMessage)
        }
    }

    private fun start(context: Context, onMessage: (String) -> Unit) {
        if (!DictateAccessibilityService.isRunning) {
            onMessage("Turn on the accessibility service to read the screen")
            return
        }
        val text = DictateAccessibilityService.readableScreenText()
        if (text.isBlank()) {
            onMessage("Nothing to read on this screen")
            return
        }
        val voice = MaSpeechify.chosenVoice(MaLanguage.active())
        state = State.LOADING
        scope.launch {
            val dest = File(context.cacheDir, "ma_reader.mp3")
            val file = withContext(Dispatchers.IO) { MaSpeechify.synthesize(text, voice, dest) }
            if (file == null) {
                state = State.IDLE
                onMessage("Could not speak this \u2014 check your Speechify key")
                return@launch
            }
            runCatching {
                stopPlayer()
                player = MediaPlayer().apply {
                    setDataSource(file.path)
                    setOnCompletionListener {
                        state = State.IDLE
                        stopPlayer()
                    }
                    setOnErrorListener { _, _, _ ->
                        state = State.IDLE
                        stopPlayer()
                        true
                    }
                    prepare()
                    start()
                }
                state = State.SPEAKING
            }.onFailure {
                state = State.IDLE
                onMessage("Could not play the audio")
            }
        }
    }

    /** Unconditional. Used when he leaves, and by a press during loading. */
    fun stop() {
        stopPlayer()
        state = State.IDLE
    }

    private fun stopPlayer() {
        runCatching { player?.stop() }
        runCatching { player?.release() }
        player = null
    }
}
