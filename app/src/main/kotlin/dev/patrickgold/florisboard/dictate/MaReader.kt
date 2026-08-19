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
     * The passage last spoken, used to notice the bottom of a page.
     *
     * A list that will not scroll further often reports a SUCCESSFUL scroll and does not move, so
     * "did it scroll" is not enough to know when to stop. Identical text twice running is the
     * reliable signal, and it is what keeps the reader from scrolling forever at the end.
     */
    private var lastPassage: String = ""

    /**
     * How long a list is given to build the rows it has just scrolled into place.
     *
     * Cut from 450 ms to 120 ms. The long wait was sized for a smooth scroll to finish travelling;
     * a jump has nothing to travel, and the only thing left to wait for is the rows being attached,
     * which is one or two frames.
     *
     * Not zero. At zero the read happens in the same frame as the jump and finds the old rows —
     * which would not look like a fast reader, it would look like a reader that skips a screen.
     */
    private const val SETTLE_MS = 120L

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
     * WHICH word is being spoken, by position in the list. -1 when none.
     *
     * The index rather than the text, because the text is not an identity. A passage says "the"
     * twenty times, and anything matching by text lands on the first of them — so the highlight
     * jumped back to the top of the screen on every common word. That was the whole of the
     * "highlight is off" problem, and no amount of tightening the timing would have touched it.
     */
    var currentIndex by mutableStateOf(-1)
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
        scope.launch { speak(context, text, onMessage) }
    }

    /**
     * Synthesises one passage and plays it. Shared by the first press and every continuation.
     *
     * Factored out precisely so the scroll-and-continue path cannot drift from the first read:
     * the same voice, the same speed, the same ticker and the same completion handler, or the
     * second screenful would behave subtly unlike the first.
     */
    private suspend fun speak(context: Context, text: String, onMessage: (String) -> Unit) {
        val prefs by FlorisPreferenceStore
        lastPassage = text
        val voice = MaSpeechify.chosenVoice(MaLanguage.active())
        state = State.LOADING
        run {
            val dest = File(context.cacheDir, "ma_reader.mp3")
            val file = withContext(Dispatchers.IO) { MaSpeechify.synthesize(text, voice, dest) }
            if (file == null) {
                state = State.IDLE
                onMessage("Could not speak this \u2014 check your Speechify key")
                return
            }
            // Read once, outside the player, because both the playback rate and the karaoke
            // ticker need the same number and they must not be able to disagree.
            val speed = prefs.dictate.maReaderSpeed.get().coerceIn(5, 25) / 10f
            runCatching {
                stopPlayer()
                player = MediaPlayer().apply {
                    setDataSource(file.path)
                    setOnCompletionListener {
                        // A screenful is finished. Scroll and carry on rather than stopping at the
                        // edge, which is what he asked for and what makes it a reader rather than a
                        // sampler.
                        stopPlayer()
                        scope.launch { continueBelow(context, onMessage) }
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
                startTicker()
            }.onFailure {
                state = State.IDLE
                onMessage("Could not play the audio")
            }
        }
    }

    /**
     * Scrolls down and reads the next screenful, or stops at the bottom.
     *
     * ### Knowing when to stop
     *
     * Two independent conditions, because either alone fails. **Nothing scrolled** catches a screen
     * that cannot move at all; **the text is the same as last time** catches the far commoner case
     * of a list that reports a successful scroll and does not actually move, which is what turns a
     * reader into the maniac scrolling forever at the bottom.
     *
     * Comparing the text rather than counting attempts is deliberate: a fixed number of tries would
     * either stop early on a long page or spin on a short one.
     */
    private suspend fun continueBelow(context: Context, onMessage: (String) -> Unit) {
        val moved = DictateAccessibilityService.scrollScreenDown()
        if (!moved) {
            state = State.IDLE
            return
        }
        // The list needs a moment to build the rows it just scrolled into place. Reading instantly
        // would find the old ones, or none at all.
        kotlinx.coroutines.delay(SETTLE_MS)
        val next = DictateAccessibilityService.readableScreenText()
        if (next.isBlank() || next == lastPassage) {
            state = State.IDLE
            MaLog.add("read", "reached the end")
            return
        }
        speak(context, next, onMessage)
    }

    /**
     * Follows the playhead and publishes the word being spoken.
     *
     * The speed is deliberately NOT applied here, and the parameter is gone so it cannot creep
     * back. `MediaPlayer.currentPosition` reports a position in the media's own timeline, which
     * already advances at the playback rate — the timings are in that same timeline, so the two
     * line up with no arithmetic at all. Scaling made the highlight run ahead by exactly the speed
     * factor.
     */
    private fun startTicker() {
        ticker?.cancel()
        ticker = scope.launch {
            while (state == State.SPEAKING || state == State.PAUSED) {
                if (state == State.SPEAKING) {
                    // No speed scaling. `currentPosition` is a position in the MEDIA timeline, not
                    // wall-clock time — at 1.5x it is already advancing at 1.5x — so multiplying by
                    // the speed counted it twice and the highlight ran ahead by exactly that
                    // factor. It was only right at 1.0, which is why it looked like drift rather
                    // than a plain error.
                    val pos = runCatching { player?.currentPosition ?: 0 }.getOrDefault(0)
                    val words = MaSpeechify.lastWords
                    val idx = words.indexOfFirst { pos >= it.startMs && pos < it.endMs }
                    currentIndex = idx
                    currentWord = if (idx >= 0) words[idx].text else ""
                }
                kotlinx.coroutines.delay(TICK_MS)
            }
            currentWord = ""
            currentIndex = -1
        }
    }

    /**
     * Jumps the playhead past the end of the sentence being spoken.
     *
     * ### Seeking, not re-synthesising
     *
     * The audio for the whole passage is one file with word timings, so skipping is a seek — free,
     * instant, and it costs nothing at Speechify. Re-asking for audio from a later point would be
     * a second charge for words already bought.
     *
     * ### The sentence is derived, as everywhere else
     *
     * Speechify returns a passage as one flat word list whatever its mark types claim (§84), so the
     * end of a sentence is the next word ending in `.`, `!` or `?`. Finding it here rather than
     * asking the subtitle keeps the two from disagreeing about where a sentence stops.
     *
     * At the last sentence there is nothing to skip to, so it stops — which is what "skip the rest"
     * means when the rest is all of it.
     */
    fun skipSentence() {
        val words = MaSpeechify.lastWords
        val here = currentIndex
        if (here < 0 || here >= words.size) return
        fun ends(w: String) = w.lastOrNull() in setOf('.', '!', '?')
        var end = here
        while (end < words.lastIndex && !ends(words[end].text)) end++
        val next = words.getOrNull(end + 1)
        if (next == null) {
            MaLog.add("read", "skip: already in the last sentence")
            return
        }
        runCatching { player?.seekTo(next.startMs) }
        MaLog.add("read", "skipped to word ${end + 1}")
    }

    /** Unconditional. Used when he leaves, and by a press during loading. */
    fun stop() {
        stopPlayer()
        // Cleared here and NOT in stopPlayer, which runs between screenfuls: clearing it there
        // would erase the very comparison that detects the bottom of a page.
        //
        // Without this, pressing play again on the same screen would find the text identical to
        // last time and stop immediately, which reads as a reader that has died.
        lastPassage = ""
        state = State.IDLE
    }

    private fun stopPlayer() {
        ticker?.cancel()
        ticker = null
        currentWord = ""
        currentIndex = -1
        runCatching { player?.stop() }
        runCatching { player?.release() }
        player = null
    }
}
