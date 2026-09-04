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
 * ### The audio arrives in growing chunks, and behaves as one file
 *
 * It WAS one mp3 for the whole passage, and that section of this comment used to explain why:
 * pausing is a real pause, resuming continues mid-sentence, and splitting per sentence would cost a
 * synthesis per sentence. All still true — but it also meant the wait before the first word was the
 * wait for the last one, and on a full screen that was seconds he sat through every time.
 *
 * So the passage is fetched as chunks that grow — one sentence, two, four, sixteen — and the wait is
 * now one sentence long. See `MaReadChunks`. The cost the old comment was avoiding is avoided too:
 * the chunks grow, so a long passage is a handful of requests rather than one per sentence.
 *
 * Everything above this line still sees ONE file and ONE timeline. `allWords` joins each chunk's
 * timings with the audio before it, and `chunkBaseMs` is that offset. **The rest of the reader was
 * not taught about chunks**, which is the only reason a change this deep was safe to make at all.
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
     * The whole passage's words, on one timeline, however many chunks it was fetched in.
     *
     * Everything that reads the reading — the ticker, skip, previous, the caption, the effects —
     * looks here rather than at `MaSpeechify.lastWords`, which now holds only the most recently
     * FETCHED chunk and would jump backwards mid-passage.
     */
    var allWords: List<MaSpeechify.Word> = emptyList()
        private set

    /** How much audio came before the chunk now playing. The offset that makes one timeline. */
    private var chunkBaseMs: Int = 0

    private var pending: kotlinx.coroutines.Job? = null
    private var pendingFile: File? = null
    private var pendingWords: List<MaSpeechify.Word> = emptyList()

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
        // Heal a state that is lying, before acting on it.
        //
        // THE BUG THIS FIXES. `state` and `player` are two facts that can disagree, and when they do
        // the key goes dead in a way that looks like nothing at all: with `state = SPEAKING` and no
        // player, a press pauses nothing and sets PAUSED, the next press starts nothing and sets
        // SPEAKING, and it flips between them forever. The icon changes, no sound arrives, and there
        // is no way back to IDLE — so the reader is dead until the keyboard is rebuilt.
        //
        // They come apart easily. The completion handler releases the player and hands off to the
        // scroll-and-continue coroutine; if that coroutine is cancelled in between — the keyboard
        // hidden, the process trimmed — the state is left saying SPEAKING with nothing behind it.
        //
        // Rather than hunt every path that could cancel, the state is checked against the thing it
        // claims. **The player is the truth; `state` is a claim about it.** No player means idle,
        // whatever the machine believes, and pressing the key always does something again.
        val live = runCatching { player?.isPlaying }.getOrNull()
        if (state != State.LOADING && player == null) {
            if (state != State.IDLE) MaLog.add("read", "state said $state with no player, reset")
            state = State.IDLE
        } else if (state == State.SPEAKING && live == false) {
            // A player that exists but has stopped: the audio finished and nothing said so.
            MaLog.add("read", "player had stopped while state said SPEAKING")
            state = State.PAUSED
        }

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
        // Watching is decided when the reading starts, from his setting, and lasts until he stops.
        // Not a separate key: the reader key already means "read this", and "read this and keep
        // reading" is the same intention with the screen still moving. One key, one intention.
        watching = prefs.dictate.maReaderWatch.get()
        scope.launch { speak(context, text, onMessage) }
    }

    /**
     * Synthesises one passage and plays it. Shared by the first press and every continuation.
     *
     * Factored out precisely so the scroll-and-continue path cannot drift from the first read:
     * the same voice, the same speed, the same ticker and the same completion handler, or the
     * second screenful would behave subtly unlike the first.
     */
    /**
     * Speaks a passage, starting with one sentence and growing from there.
     *
     * ### The wait is one sentence long now
     *
     * It used to synthesise the whole screenful and then begin, so the wait before the first word
     * was the wait for the last one. Now the first chunk is a single sentence — about a second — and
     * every request after it happens behind audio that is already playing. See `MaReadChunks` for
     * why the chunks grow and why they stop growing.
     *
     * ### One passage, several files
     *
     * Each chunk is its own mp3 with its own word timings, and everything above this — the karaoke
     * ticker, skip, previous, the caption, the effects — was written for ONE file and ONE list.
     * Rather than teach all of them about chunks, the chunks are hidden here: [allWords] is the
     * whole passage with each chunk's timings shifted by the audio before it, and [chunkBaseMs] is
     * how much audio that is. **The rest of the reader still sees one timeline**, which is the only
     * reason this change is small enough to trust.
     */
    private suspend fun speak(context: Context, text: String, onMessage: (String) -> Unit) {
        val prefs by FlorisPreferenceStore

        // THE GUARD LIVES HERE, WHERE EVERY READING HAS TO PASS.
        //
        // It was in `continueBelow` — the path taken after a chunk finishes and the screen is
        // scrolled — and the loop survived that fix. Which says the loop is not on that path, or not
        // only on it: something else is calling `speak` again with a passage already read.
        //
        // Rather than hunt for the caller, the check moves to the one place all of them must come
        // through. **A guard on one route is a guard on one route; a guard at the door is a guard.**
        // If some path is calling this twice, the second call now ends here instead of speaking.
        //
        // Note what this does NOT do: it does not stop him reading the same screen again on purpose.
        // `passagesRead` is cleared by `stop`, and stopping is what the reader key does — so
        // pressing read, stopping, and pressing read again on the same text works exactly as before.
        // Only an unasked-for repeat inside one reading is refused.
        val key = normalisedForCompare(text)
        if (key.isNotBlank() && passagesRead.contains(key)) {
            MaLog.add("read", "refused to read the same passage twice in one reading")
            state = State.IDLE
            onMessage("Already read this \u2014 stopping")
            return
        }
        lastPassage = text
        passagesRead.add(key)
        // The marker he asked for: a filled square at the head of each passage.
        //
        // His idea, and a good one — he asked for it as a fallback in case the loop could not be
        // found, and it is worth having whether or not it is found. **A reading that starts again
        // says so, visibly, without anybody having to diagnose why.** If he sees a second square he
        // knows immediately that he is hearing the same words rather than similar ones, and he can
        // stop it himself instead of listening to work out what happened.
        //
        // In the caption only. It is never spoken and never inserted into his text — Speechify is
        // sent the passage, not the marker.
        passageMark = "\u25A0"
        
        val voice = MaSpeechify.chosenVoice(MaLanguage.active())
        state = State.LOADING

        val sentences = MaReadChunks.sentences(text)
        val plan = MaReadChunks.plan(sentences.size)
        if (plan.isEmpty()) {
            state = State.IDLE
            return
        }
        allWords = emptyList()
        chunkBaseMs = 0

        // The first chunk, and the only one he waits for.
        val firstFile = withContext(Dispatchers.IO) {
            MaSpeechify.synthesize(
                MaReadChunks.textOf(sentences, plan[0]),
                voice,
                File(context.cacheDir, "ma_reader_0.mp3"),
            )
        }
        if (firstFile == null) {
            state = State.IDLE
            onMessage("Could not speak this \u2014 check your Speechify key")
            return
        }
        allWords = MaSpeechify.lastWords
        playChunk(context, firstFile, 0, plan, sentences, voice, onMessage)
    }

    /**
     * Plays chunk [index], and asks for the next one while it plays.
     *
     * The prefetch is launched BEFORE playback starts rather than after, because the point is to
     * spend the current chunk's playing time on the next chunk's synthesis. Started afterwards it
     * would still work and would waste the first few hundred milliseconds of every chunk.
     */
    private fun playChunk(
        context: Context,
        file: File,
        index: Int,
        plan: List<IntRange>,
        sentences: List<String>,
        voice: MaSpeechify.Voice,
        onMessage: (String) -> Unit,
    ) {
        val prefs by FlorisPreferenceStore
        val speed = prefs.dictate.maReaderSpeed.get().coerceIn(5, 25) / 10f

        // Ask for the next chunk now, into a file of its own. Alternating names would be enough for
        // two in flight, but a numbered file per chunk means a slow request can never overwrite the
        // audio that is playing.
        val nextIndex = index + 1
        if (nextIndex < plan.size) {
            pending = scope.launch(Dispatchers.IO) {
                val dest = File(context.cacheDir, "ma_reader_$nextIndex.mp3")
                val f = MaSpeechify.synthesize(MaReadChunks.textOf(sentences, plan[nextIndex]), voice, dest)
                if (f != null) pendingWords = MaSpeechify.lastWords
                pendingFile = f
            }
        } else {
            pending = null
            pendingFile = null
        }

        runCatching {
            stopPlayer()
            player = MediaPlayer().apply {
                setDataSource(file.path)
                setOnCompletionListener {
                    val played = runCatching { duration }.getOrDefault(0)
                    stopPlayer()
                    scope.launch {
                        if (nextIndex >= plan.size) {
                            // The passage is finished. Scroll and carry on, exactly as before.
                            continueBelow(context, onMessage)
                            return@launch
                        }
                        // Wait for the next chunk if it is not back yet. It usually is — that is the
                        // whole design — but a slow network must produce a pause, not a stop.
                        pending?.join()
                        val next = pendingFile
                        if (next == null) {
                            state = State.IDLE
                            onMessage("The reading stopped early \u2014 check your connection")
                            return@launch
                        }
                        // The timeline the rest of the reader sees: this chunk's words, shifted by
                        // all the audio before them.
                        chunkBaseMs += played
                        allWords = allWords + pendingWords.map {
                            it.copy(startMs = it.startMs + chunkBaseMs, endMs = it.endMs + chunkBaseMs)
                        }
                        playChunk(context, next, nextIndex, plan, sentences, voice, onMessage)
                    }
                }
                setOnErrorListener { _, _, _ ->
                    state = State.IDLE
                    stopPlayer()
                    true
                }
                prepare()
                // Speed applies to playback, not to the synthesis.
                //
                // Changing it therefore costs nothing and takes effect on the next press rather
                // than the next request — and the word timings stay valid, because they are
                // positions in the audio and the position is scaled by the same rate.
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
        // WHAT WAS READ BEFORE THE SCROLL, so "did the screen actually move" can be answered.
        //
        // `scrollScreenDown` returns whether the scroll ACTION was accepted, not whether anything
        // moved — and at the bottom of a list it is accepted and nothing moves. That was half the
        // loop.
        val before = DictateAccessibilityService.readableScreenText()
        val moved = DictateAccessibilityService.scrollScreenDown()
        if (!moved) {
            state = State.IDLE
            return
        }
        // The list needs a moment to build the rows it just scrolled into place. Reading instantly
        // would find the old ones, or none at all.
        kotlinx.coroutines.delay(SETTLE_MS)
        val next = DictateAccessibilityService.readableScreenText()

        // THE LOOP AT THE END OF A CHAT, AND WHY IT WAS NOT CAUGHT BY THE OLD CHECK.
        //
        // It compared the new passage against `lastPassage` — the one immediately before — and
        // stopped when they were identical. At the bottom of a chat the screen does not move, so the
        // two SHOULD be identical and it should have stopped.
        //
        // They were not identical. A live screen changes by a character or two between reads: a
        // clock in the status bar, a relative timestamp, a typing indicator, the reading overlay
        // itself. **One character of difference made the equality check false, and it read the same
        // screen again, and again.** He counted a thousand times and that is not hyperbole — nothing
        // in the loop was bounded.
        //
        // Three answers, because one of them alone is the kind of fix that comes back:
        //
        //  1. the screen did not move — compare what was on it before and after the scroll;
        //  2. this passage has been read ALREADY, at any point in this reading, not just last;
        //  3. a hard ceiling on how many screens one press may read.
        //
        // The third is a backstop and is meant never to fire. It is here because the first two are
        // judgements about text from another app, and a judgement can be wrong, while a counter
        // cannot run forever.
        val cleaned = next.trim()
        val screenStuck = normalisedForCompare(before) == normalisedForCompare(next)
        val seenBefore = passagesRead.contains(normalisedForCompare(cleaned))
        if (cleaned.isBlank() || screenStuck || seenBefore || passagesRead.size >= MAX_SCREENS) {
            // WATCH MODE: the end of the text is not the end of the reading.
            //
            // He reads Claude while it is still writing. The old behaviour was right for a finished
            // page and wrong for a live one: it reached the bottom and stopped, and everything
            // written after that was never spoken.
            //
            // Watching, it waits here instead — polling the screen and reading whatever has been
            // ADDED, until he stops it. **Only he ends a watch.** That is the whole ask, and it is
            // why the ceiling below does not apply: MAX_SCREENS bounds a runaway scroll, and
            // watching is not runaway, it is waiting.
            if (watching && !cleaned.isBlank()) {
                MaLog.add("read", "reached the end \u2014 watching for more")
                watchForMore(context, onMessage)
                return
            }
            state = State.IDLE
            MaLog.add(
                "read",
                when {
                    cleaned.isBlank() -> "reached the end — nothing left to read"
                    screenStuck -> "reached the end — the screen did not move"
                    seenBefore -> "reached the end — this screen has been read already"
                    else -> "stopped after $MAX_SCREENS screens"
                },
            )
            return
        }
        passagesRead.add(normalisedForCompare(cleaned))
        speak(context, next, onMessage)
    }

    /**
     * Every passage read since this reading began, so one cannot be read twice.
     *
     * Cleared by [stop] and by the start of a new reading — it is about this reading, not about the
     * app's whole life. Normalised, because the comparison it exists for is defeated by exactly the
     * differences normalising removes.
     */
    /**
     * A filled square shown at the head of a passage, cleared once the reading is under way.
     *
     * Drawn by the caption, spoken by nobody. See `speak`.
     */
    var passageMark by mutableStateOf("")
        private set

    private val passagesRead = mutableSetOf<String>()

    /**
     * Text as it is compared, not as it is read.
     *
     * Digits go, because a clock and a relative timestamp are the commonest thing that changes
     * between two reads of one screen. Whitespace collapses, because a re-laid-out list wraps
     * differently for the same words. What is left is the words, which is what "the same screen"
     * means to him.
     */
    /**
     * THE NEW TAIL OF A GROWING SCREEN.
     *
     * Watch mode's whole problem in one function. He reads a chat while an answer is still being
     * written: the screen does not CHANGE, it GROWS. Compare whole screens and every poll looks like
     * a brand new passage, because the old text is still in it.
     *
     * **So read the difference, not the screen.** If what is on screen now begins with what was
     * there before, the reading is everything after that point. If it does not — he scrolled, or
     * switched conversation — it is a new passage and the whole thing is read.
     *
     * Compared on the NORMALISED forms, for the same reason the loop guard is: a clock in the status
     * bar and a re-wrapped line would otherwise make a growing screen look like a different one. The
     * text returned is cut from the RAW string, because that is what gets spoken and it should
     * arrive with its punctuation intact.
     *
     * Returns null when nothing was added, which is the common case: he is watching a screen that is
     * not moving yet, and the poll must be cheap and silent.
     */
    /**
     * Whether this reading keeps watching after the text runs out.
     *
     * Set when the reading starts and cleared by [stop], so it belongs to one reading rather than
     * being a mode he has to remember. There is no way to be watching and not reading.
     */
    var watching: Boolean = false
        private set

    /**
     * Waits for more text, reads what arrives, and repeats until he stops.
     *
     * ### Why a poll and not a listener
     *
     * The accessibility service can be told about window content changes, and that sounds better
     * until you watch a chat: a streaming answer fires those events several times a second, each
     * carrying a few more characters. Reading on every event would speak half-words. **A poll asks
     * the question at a speed a voice can answer at**, which is the speed that matters here.
     *
     * ### Why it never gives up
     *
     * No timeout, no maximum. He said it: it never stops automatically. A watch that ended after
     * five quiet minutes would end during the one long pause he stepped away for, which is exactly
     * when he was relying on it.
     *
     * The loop exits on `stop()` alone — `watching` goes false and the coroutine's own check ends
     * it. The scope is the reader's, so the keyboard going away ends it too.
     */
    private fun watchForMore(context: Context, onMessage: (String) -> Unit) {
        state = State.PAUSED
        scope.launch {
            var seen = DictateAccessibilityService.readableScreenText()
            while (watching) {
                kotlinx.coroutines.delay(WATCH_POLL_MS)
                if (!watching) return@launch
                val now = DictateAccessibilityService.readableScreenText()
                val tail = newTail(seen, now) ?: continue
                // The loop guard still applies, and here it earns its keep twice over: a screen that
                // re-renders identically must not be read again just because it arrived again.
                if (passagesRead.contains(normalisedForCompare(tail))) {
                    seen = now
                    continue
                }
                seen = now
                speak(context, tail, onMessage)
                return@launch
            }
        }
    }

    /**
     * How often a watching reader looks for new text.
     *
     * Slow enough that a streaming answer accumulates a sentence between polls rather than a word —
     * reading three words at a time would be worse than not reading at all. Fast enough that he does
     * not notice the wait after the writing stops.
     */
    private const val WATCH_POLL_MS = 1_800L

    fun newTail(before: String, now: String): String? {
        val a = normalisedForCompare(before)
        val b = normalisedForCompare(now)
        if (b.isBlank() || a == b) return null
        if (!b.startsWith(a) || a.isBlank()) {
            // Not a continuation. A different screen, so all of it is new.
            return now.trim().ifBlank { null }
        }
        // The tail, measured in normalised words and then taken from the raw text by walking words
        // rather than characters — the raw and the normalised have different lengths, and slicing
        // the raw by a normalised offset is how the first version cut a word in half.
        val added = b.removePrefix(a).trim()
        if (added.isBlank()) return null
        val wordsAdded = added.split(' ').size
        val rawWords = now.trim().split(Regex("\\s+"))
        if (wordsAdded >= rawWords.size) return now.trim()
        return rawWords.takeLast(wordsAdded).joinToString(" ").ifBlank { null }
    }

    private fun normalisedForCompare(text: String): String =
        // Every non-letter becomes a SPACE rather than being dropped.
        //
        // The first version filtered to `isLetter() || it == ' '`, which deleted newlines — so a
        // re-wrapped screen turned "two\nthree" into "twothree" and compared unequal to the same
        // words wrapped differently. That is exactly the failure this function exists to prevent,
        // reintroduced inside the fix for it, and the test caught it before the build.
        text.lowercase().map { if (it.isLetter()) it else ' ' }
            .joinToString("").split(' ').filter { it.isNotBlank() }.joinToString(" ")

    /**
     * The most screens one press of the reader may work through.
     *
     * A backstop, not a feature. Thirty screens is far past any passage he has read in one go and
     * far short of a thousand.
     */
    private const val MAX_SCREENS = 30

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
                    val words = allWords
                    // The position within THIS chunk, plus the audio before it.
                    val here = pos + chunkBaseMs
                    val idx = words.indexOfFirst { here >= it.startMs && here < it.endMs }
                    // The square lives until the third word. Long enough to be seen when a reading
                    // starts again, short enough that it is not decoration on a passage he is
                    // halfway through — a mark that never leaves stops being a signal.
                    if (idx >= 3) passageMark = ""
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
        val words = allWords
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

    /**
     * One sentence back. One step, always.
     *
     * **No replay window and no two-step.** A first version restarted the current sentence when he
     * was already part-way into it, on the reasoning that every media player does that — and he was
     * clear that this is wrong for reading. A skip key skips. If he wants a sentence again he presses
     * back and then forward, which is two deliberate presses and takes less thought than a key whose
     * meaning changes depending on how long he has been listening.
     *
     * **A control that does two different things depending on timing is a control that has to be
     * predicted.** At speed, predictable beats clever.
     */
    fun previousSentence() {
        val words = allWords
        val here = currentIndex
        if (here < 0 || here >= words.size) return
        fun ends(w: String) = w.lastOrNull() in setOf('.', '!', '?')

        // The first word of the sentence being read.
        var start = here
        while (start > 0 && !ends(words[start - 1].text)) start--

        if (start == 0) {
            // Already in the first sentence: go to its beginning rather than nowhere, so the key
            // always does something.
            runCatching { player?.seekTo(words[0].startMs) }
            MaLog.add("read", "back: first sentence, to its start")
            return
        }

        // The first word of the one before it.
        var prev = start - 1
        while (prev > 0 && !ends(words[prev - 1].text)) prev--
        runCatching { player?.seekTo(words[prev].startMs) }
        MaLog.add("read", "back to word $prev")
    }

    /**
     * Changes the speed of the reading that is happening RIGHT NOW.
     *
     * The speed used to be read once, when a passage started playing. Changing it in settings while
     * the voice was talking therefore did nothing until the next passage — he described it as
     * responding "after a few sentences", which is exactly what that is. A control that only takes
     * effect later is a control he cannot judge, because by the time it works he has forgotten what
     * he changed it from.
     *
     * `playbackParams` on a playing MediaPlayer applies at once, so the setting is now what it looks
     * like: a dial, not a preference for next time.
     *
     * The karaoke needs no adjustment. It reads `currentPosition`, which is a position in the media
     * timeline and stays true whatever rate the media is playing at — the same reason the speed was
     * removed from the ticker in the first place.
     */
    fun setSpeedNow(tenths: Int) {
        val speed = tenths.coerceIn(5, 25) / 10f
        val p = player ?: return
        runCatching {
            // Setting params on a paused player starts it on some versions of Android, so the
            // paused state is restored immediately afterwards rather than trusted to survive.
            val wasPlaying = p.isPlaying
            p.playbackParams = p.playbackParams.setSpeed(speed)
            if (!wasPlaying) p.pause()
        }.onFailure { MaLog.add("read", "speed change refused: ${it.javaClass.simpleName}") }
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
        // The record of what has been read belongs to one reading. Kept across a stop, the next
        // reading of the same screen would find it "already read" and refuse to start — the fix
        // becoming the bug, which is how the loop got here in the first place.
        // A stop ends the watch. This is the ONLY thing that does — no timeout, no ceiling. He said
        // it never stops automatically, and a watch that ended after five quiet minutes would end
        // during the one long pause he stepped away for.
        watching = false
        passagesRead.clear()
        passageMark = ""
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
