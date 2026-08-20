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
import android.media.AudioManager
import android.view.KeyEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.jvm.Volatile

/**
 * The volume keys. His main shortcut, and the specification is in `VOLUME_KEYS.md`.
 *
 * ### There is no setting
 *
 * There was one, and it is the reason this was dead for five builds. It defaulted on, it sat in a
 * list of thirteen switches he had just been given the ability to drag, and one stray touch turned
 * it off. Nothing said so. He lost days believing the feature had been deleted, and I spent three
 * rounds reading a handler that was correct the whole time.
 *
 * **A control that can silently disable the thing somebody uses most is not a feature, it is a
 * trapdoor.** The switch is gone. These keys are always live while the keyboard is on screen.
 *
 * ### What they do
 *
 * **Volume up, quick tap** — start recording. Tap again — stop, and send to transcription.
 *
 * **Volume down, quick tap while recording** — stop, transcribe, and when the words land, press Send
 * on screen with the accessibility finger. One press for the whole errand.
 *
 * **Volume down, quick tap otherwise** — press Send on screen.
 *
 * **Either, held** — the real volume, repeating, with the system's own bar showing.
 *
 * ### Why the decision happens on RELEASE
 *
 * At the moment of pressing there is no way to know which of the two he meant. A quick tap is a tap;
 * a finger still down after [HOLD_MS] was never a tap at all. Deciding on release is what lets one
 * key do both jobs without either guessing.
 *
 * Two invariants, and they are the feature: **never both** — no press produces an app action and a
 * volume change — and **always one** — no press does nothing at all.
 */
object MaVolumeKeys {

    /**
     * How long a key must be held before it stops being a tap and becomes the volume.
     *
     * Half a second. Long enough that a deliberate tap never crosses it, short enough that a finger
     * resting on a key is not left wondering.
     */
    private const val HOLD_MS = 500L

    /** The gap between volume steps once the hold has taken over. His number. */
    private const val REPEAT_MS = 111L

    /**
     * Its own scope, on Main.
     *
     * The service's lifecycle scope would carry a repeat across a keyboard that has been hidden, and
     * a volume key repeating for a keyboard nobody can see is how an app gets uninstalled.
     */
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private var holdJob: Job? = null

    /** Set once a press has become the volume, so its release does not also fire an app action. */
    @Volatile
    private var didRepeat = false

    private fun isVolume(keyCode: Int) =
        keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN

    /**
     * A volume key going down. Returns true when this module has taken the key.
     *
     * Nothing happens on the way down except starting the clock — see the note on release above.
     */
    fun onDown(context: Context, keyCode: Int, event: KeyEvent?): Boolean {
        if (!isVolume(keyCode)) return false
        MaLog.add("vol", "${name(keyCode)} down")
        // The system repeats a held key on its own clock. Only the first press opens a hold; the
        // repeats are swallowed, because the repeating is done here.
        if (event != null && event.repeatCount > 0) return true
        didRepeat = false
        holdJob?.cancel()
        holdJob = scope.launch {
            delay(HOLD_MS)
            didRepeat = true
            MaLog.add("vol", "held \u2014 passing to the system volume")
            val direction = if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
                AudioManager.ADJUST_RAISE
            } else {
                AudioManager.ADJUST_LOWER
            }
            val am = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            while (isActive) {
                runCatching {
                    am?.adjustStreamVolume(AudioManager.STREAM_MUSIC, direction, AudioManager.FLAG_SHOW_UI)
                }
                delay(REPEAT_MS)
            }
        }
        return true
    }

    /**
     * A volume key coming up. Returns true when this module has taken the key.
     *
     * This is where the work happens. For a quick tap that is a delay of the length of the tap,
     * which is not felt; for a hold it is the difference between changing the volume and starting a
     * recording nobody asked for.
     */
    fun onUp(context: Context, keyCode: Int): Boolean {
        if (!isVolume(keyCode)) return false
        holdJob?.cancel()
        holdJob = null
        if (didRepeat) {
            // The hold already spent this press on the volume. Doing the app action as well would
            // give him both, which is the one outcome nobody wants.
            didRepeat = false
            MaLog.add("vol", "${name(keyCode)} released after a hold \u2014 nothing more")
            return true
        }
        // WHILE IT IS READING, THE SAME KEYS DRIVE THE READING.
        //
        // Nobody dictates into a screen they are listening to. So for as long as there is a reading
        // in progress, a tap means skip rather than record — forward on up, back on down — and the
        // hold still means the volume, because that is the one thing wanted at exactly that moment.
        //
        // The keys are not remapped by a setting; they follow what is on screen. A control that
        // means the obvious thing for the situation in front of him needs no mode and no memory.
        if (MaReader.currentIndex >= 0) {
            when (keyCode) {
                // DOWN goes FORWARD. Not a mistake and not a preference.
                //
                // The text moves down the screen as it is read, so down is where the next sentence
                // is. Every scroll on this phone works that way, and the hand is already trained by
                // every one of them. Mapping up to "next" because up is bigger would be a rule from
                // arithmetic imposed on a rule from movement.
                KeyEvent.KEYCODE_VOLUME_DOWN -> {
                    MaLog.add("vol", "down tap while reading \u2014 next sentence")
                    MaReader.skipSentence()
                }
                KeyEvent.KEYCODE_VOLUME_UP -> {
                    MaLog.add("vol", "up tap while reading \u2014 previous sentence")
                    MaReader.previousSentence()
                }
            }
            return true
        }

        when (keyCode) {
            KeyEvent.KEYCODE_VOLUME_UP -> {
                MaLog.add("vol", "up tap \u2014 record / stop")
                DictateController.onMicClick(context)
            }
            KeyEvent.KEYCODE_VOLUME_DOWN -> {
                val recording = DictateController.state.value is DictateController.UiState.Recording
                if (recording) {
                    // The whole errand in one press: stop, transcribe, and send once the words are
                    // in the field. The send is ARMED here and fired by the transcription path,
                    // because the text has to land first — a send from this line would send an
                    // empty message and nobody would find out until they read the other end.
                    MaLog.add("vol", "down tap while recording \u2014 stop, transcribe, then send")
                    DictateController.sendAfterCommit = true
                    DictateController.onMicClick(context)
                } else {
                    MaLog.add("vol", "down tap \u2014 press Send on screen")
                    MaMagicTargets.pressSend()
                }
            }
        }
        return true
    }

    private fun name(keyCode: Int) =
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) "up" else "down"
}
