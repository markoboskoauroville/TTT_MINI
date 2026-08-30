/*
 * Copyright (C) 2026 Marko Bosko, Mantra Productions
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package dev.patrickgold.florisboard.dictate.ui

import androidx.compose.runtime.mutableStateOf

/**
 * THE APP'S OWN MESSAGES, ABOVE THE KEYBOARD INSTEAD OF ON TOP OF IT.
 *
 * ### Why not a Toast
 *
 * He said it plainly: a toast lands over his buttons while he is using them, which is rude. He is
 * right, and the obvious fix does not work — **`Toast.setGravity` has been ignored since Android 11
 * for ordinary text toasts.** Asking the system to put one at the top does nothing at all; the
 * platform decides, and it decides to put it near the bottom, which is where the keyboard is.
 *
 * So the message is not a toast. It is a line the keyboard draws itself, at the top of its own
 * area, above every row. Nothing it covers is a key.
 *
 * ### What that buys beyond position
 *
 * It is inside the keyboard, so it disappears when the keyboard does — a toast can outlive the thing
 * it was talking about and land over another app. It is styled like everything else here rather than
 * like a system dialog. And it can be replaced instantly: a second message overwrites the first
 * rather than queueing behind it, which is what a toast does and why three quick presses used to
 * mean six seconds of stale text.
 *
 * ### The duration
 *
 * Two and a half seconds, and one timer. A toast's SHORT is about two, which is not quite enough to
 * read a sentence at his reading speed, and LONG is three and a half, which is long enough to still
 * be there when he has moved on. The message is never the reason to wait.
 *
 * Cleared by time or by the next message, never by a tap: a message he has to dismiss is a message
 * that has taken a press away from what he was doing.
 */
object MaMessage {

    /** What is showing, or blank. Compose state, because the keyboard is already drawn when it changes. */
    val text = mutableStateOf("")

    /** Bumped on every new message, so the timer restarts rather than the old one clearing the new. */
    val serial = mutableStateOf(0)

    /** How long a message stays. See the note above on why not two, and why not three and a half. */
    const val SHOW_MS = 2_500L

    /**
     * Shows [message], replacing whatever was there.
     *
     * Replacing rather than queueing: the newest message is the one about what he just did, and a
     * queue would show him the answer to a press he has already forgotten making.
     */
    fun show(message: String) {
        if (message.isBlank()) return
        text.value = message
        serial.value = serial.value + 1
    }

    /** Clears it. Called by the timer, and by anything that knows the message is now wrong. */
    fun clear() {
        text.value = ""
    }
}
