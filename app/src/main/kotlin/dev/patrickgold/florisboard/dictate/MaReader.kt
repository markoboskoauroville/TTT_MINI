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
import dev.patrickgold.florisboard.app.FlorisPreferenceStore
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
    private var ticker: kotlinx.coroutines.Job? = null

    /**
     * The word being spoken right now, or empty.
     *
     * Read by the spacebar, which shows it while the voice reads — a karaoke line on the widest key
     * on the board. The timings come free with the synthesis, so this costs one comparison every
     * 60 ms and nothing else.
     */
    var currentWord by mutableStateOf("")
        private set

    /**
     * How often the word is looked up.
     *
     * 60 ms is about four times faster than speech, so a short word is never skipped, and slow
     * enough that the cost is invisible. Faster would redraw the spacebar for no gain; slower and
     * quick words like "je" — 213 ms in the measured sample — would flicker past unseen.
     */
    private const val TICK_MS = 60L

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
        val prefs by FlorisPreferenceStore
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
            // Read once, outside the player, because both the playback rate and the karaoke
            // ticker need the same number and they must not be able to disagree.
            val speed = prefs.dictate.maReaderSpeed.get().coerceIn(5, 25) / 10f
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
                    // Speed applies to playback, not to the synthesis.
                    //
                    // Changing it therefore costs nothing and takes effect on the next press
                    // rather than the next request — and the word timings stay valid, because they
                    // are positions in the audio and the position is scaled by the same rate.
                    if (speed != 1.0f) {
                        runCatching { playbackParams = playbackParams.setSpeed(speed) }
                    }
                    start()
                }
                state = State.SPEAKING
                startTicker(speed)
            }.onFailure {
                state = State.IDLE
                onMessage("Could not play the audio")
            }
        }
    }

    /**
     * Follows the playhead and publishes the word being spoken.
     *
     * The position is divided by the speed before the lookup, because the timings describe the
     * audio at normal rate while `currentPosition` advances in real time. At 1.5x, two seconds of
     * listening is three seconds of script — without this the karaoke would drift further behind
     * the voice the longer he listened, which is worse than not having it.
     */
    private fun startTicker(speed: Float) {
        ticker?.cancel()
        ticker = scope.launch {
            while (state == State.SPEAKING || state == State.PAUSED) {
                if (state == State.SPEAKING) {
                    val pos = runCatching { player?.currentPosition ?: 0 }.getOrDefault(0)
                    val scriptPos = (pos * speed).toInt()
                    currentWord = MaSpeechify.wordAt(MaSpeechify.lastWords, scriptPos).orEmpty()
                }
                kotlinx.coroutines.delay(TICK_MS)
            }
            currentWord = ""
        }
    }

    /** Unconditional. Used when he leaves, and by a press during loading. */
    fun stop() {
        stopPlayer()
        state = State.IDLE
    }

    private fun stopPlayer() {
        ticker?.cancel()
        ticker = null
        currentWord = ""
        runCatching { player?.stop() }
        runCatching { player?.release() }
        player = null
    }
}
