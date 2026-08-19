/*
 * Copyright (C) 2026 Marko Bosko, Mantra Productions
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package dev.patrickgold.florisboard.ime.text.keyboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.KeyboardCapslock
import androidx.compose.material.icons.filled.OpenWith
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.patrickgold.florisboard.dictate.MaLog
import dev.patrickgold.florisboard.ime.text.key.KeyCode

/**
 * The cursor trackpad: hold the spacebar, then drag anywhere to move the caret.
 *
 * ### Why it replaces what was there
 *
 * A long press on the spacebar opened the language picker, and picking a language is a thing he does
 * rarely and deliberately — the badge and a long press on volume down both already do it. Meanwhile
 * moving a caret through dictated text is something he does constantly, and doing it by tapping at
 * the text is the least accurate gesture on a phone: the finger covers exactly the character being
 * aimed at.
 *
 * ### Why a whole-keyboard pad rather than a strip
 *
 * The pad takes the entire keyboard because the keyboard is the part of the screen his thumb is
 * already on, and because a large target means the drag can be lazy. Nothing under it can be pressed
 * while it is up, which is the point: it is a mode, entered on purpose and left on purpose.
 *
 * ### Movement
 *
 * Horizontal drag moves by character, vertical by line. Distance is accumulated and spent in whole
 * steps, so a slow drag still moves — a threshold that resets on every event would ignore a careful
 * finger entirely, which is precisely the finger this is for.
 *
 * ### Leaving
 *
 * Lifting the finger closes it. There is no confirm and no cancel: the caret has already moved with
 * the drag, so what he sees when he lifts is what he gets, and a mode that needs to be dismissed is
 * a mode that will be left open by accident.
 */
object MaCursorPad {

    /** Whether the pad is up. Read by the keyboard layout, set by the spacebar's long press. */
    var active by mutableStateOf(false)
        private set

    /**
     * Whether a drag extends a selection instead of moving the caret.
     *
     * Cleared whenever the pad opens: selection is a thing he turns on for a job and it should not
     * be waiting, armed, the next time he holds the spacebar for an unrelated reason.
     */
    var selecting by mutableStateOf(false)
        private set

    fun open() {
        selecting = false
        active = true
        // Logged so "it did not react" can be told from "it opened and I could not see it".
        //
        // Every spacebar in the app routes here — the typing keyboard, the numeric layouts, the
        // transcription view and the feature row all reach this one function — so a hold that
        // writes nothing here never called it, and a hold that writes a line and shows nothing is a
        // drawing problem instead. Two very different faults that look identical from the outside.
        MaLog.add("pad", "opened")
    }

    fun close() {
        selecting = false
        active = false
        // Suggestions were suppressed for the whole time the pad was up, so they have to be asked
        // for again. Without this the row stays empty after closing until the next keystroke
        // happens to refresh it — which looks like the pad broke the suggestions on its way out.
        onClosed?.invoke()
    }

    /**
     * Called after the pad closes, so whoever suppressed things while it was up can restore them.
     *
     * A callback rather than this object reaching for the keyboard manager: the pad is drawn by two
     * different layouts and knows nothing about either, and it should stay that way.
     */
    var onClosed: (() -> Unit)? = null

    fun toggleSelecting() {
        selecting = !selecting
    }

    /**
     * How far the finger travels for one step, in pixels.
     *
     * Characters are cheaper to overshoot than lines, so horizontal is the shorter distance. Both
     * are deliberately larger than a fingertip's natural tremor: the pad should feel deliberate
     * rather than skittish, and an overshoot costs a second drag in the other direction.
     */
    private const val STEP_X = 28f
    // Lowered from 56. A line move has to be reachable inside the height of the pad, and half a key
    // was far enough that a natural upward drag ended before the first step was ever spent.
    private const val STEP_Y = 34f

    @Composable
    fun Overlay(
        modifier: Modifier = Modifier,
        onSelectToggle: () -> Unit,
        onKey: (Int) -> Unit,
    ) {
        // Carried across pointer events rather than recomputed, so a slow drag accumulates instead
        // of being rounded away to nothing on every frame.
        var accX = 0f
        var accY = 0f
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color(0xF20E0E10))
                // The pad STAYS OPEN until the corner key closes it.
                //
                // It used to close whenever the finger lifted, which meant one journey per hold: to
                // move, then select, then delete, he had to raise it three times. A trackpad that
                // shuts every time the hand leaves it is not a trackpad, it is a long gesture.
                //
                // That is safe now in a way it was not in build 150, because there is a visible way
                // out: the keyboard key in the corner. The trap was never that it stayed open, it
                // was that nothing on screen said how to leave.
                // Swallow every tap that is not on a corner key.
                //
                // Without this a tap on the middle of the pad reaches whatever is beneath it. The
                // drag handler alone does not do it: a press with no movement is not a drag, so it
                // was passed straight down to the keys, which is how he ended up typing through a
                // window he could not see the letters of.
                //
                // No action on purpose. The middle of the pad is for dragging; a tap there should do
                // nothing at all rather than close it, since the corner key is the way out and a pad
                // that vanished on a stray tap would be the old problem inverted.
                .pointerInput(Unit) {
                    detectTapGestures { /* absorbed */ }
                }
                .pointerInput(Unit) {
                    detectDragGestures { change, drag ->
                        change.consume()
                        accX += drag.x
                        accY += drag.y
                        while (accX >= STEP_X) {
                            onKey(KeyCode.ARROW_RIGHT); accX -= STEP_X
                        }
                        while (accX <= -STEP_X) {
                            onKey(KeyCode.ARROW_LEFT); accX += STEP_X
                        }
                        while (accY >= STEP_Y) {
                            onKey(KeyCode.ARROW_DOWN); accY -= STEP_Y
                        }
                        while (accY <= -STEP_Y) {
                            onKey(KeyCode.ARROW_UP); accY += STEP_Y
                        }
                    }
                },
            contentAlignment = Alignment.TopCenter,
        ) {
            Row(
                modifier = Modifier.padding(top = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.OpenWith,
                    contentDescription = null,
                    tint = Color(0xFFECEAE3),
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text = if (selecting) "Drag to select" else "Drag to move cursor",
                    color = Color(0xFFECEAE3),
                    fontSize = 19.sp,
                )
            }

            // Three corners, and the fourth left empty on purpose.
            //
            // Top right closes, bottom left selects, bottom right deletes. Top left holds nothing:
            // it is where a thumb crosses the pad on its way anywhere, and a key there would be
            // pressed by accident more often than on purpose.
            //
            // Keys rather than gestures, because the pad is already one gesture. A second and third
            // layered on the same finger would make every drag a guess about which was meant.
            PadCorner(
                modifier = Modifier.align(Alignment.TopEnd),
                icon = Icons.Default.Keyboard,
                description = "Close and go back to the keyboard",
                lit = false,
                // Closing clears selection mode as well as the pad. Leaving it set would send the
                // next arrow from any key extending a selection instead of moving, long after the
                // pad was gone and with nothing on screen to explain it.
                onClick = {
                    if (selecting) onSelectToggle()
                    close()
                },
            )
            PadCorner(
                modifier = Modifier.align(Alignment.BottomStart),
                icon = Icons.Default.KeyboardCapslock,
                description = if (selecting) "Stop selecting" else "Select while dragging",
                // Lit while it holds: it changes what the next drag does, and nothing else on
                // screen would say so.
                lit = selecting,
                onClick = { onSelectToggle() },
            )
            PadCorner(
                modifier = Modifier.align(Alignment.BottomEnd),
                icon = Icons.AutoMirrored.Filled.Backspace,
                description = "Delete",
                lit = false,
                onClick = { onKey(KeyCode.DELETE) },
            )
        }
    }

    /**
     * One corner key: a large, plain target with no chrome.
     *
     * Sized well past a fingertip because it is pressed without looking — the eye is on the text
     * while the caret moves, not on the pad. A small key here would mean glancing down, which is
     * the cost this whole feature exists to remove.
     */
    @Composable
    private fun PadCorner(
        modifier: Modifier,
        icon: ImageVector,
        description: String,
        lit: Boolean,
        onClick: () -> Unit,
    ) {
        Box(
            modifier = modifier
                .padding(10.dp)
                .size(74.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(if (lit) Color(0x33E8B15C) else Color(0x1FFFFFFF))
                .clickable { onClick() },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = description,
                tint = if (lit) Color(0xFFE8B15C) else Color(0xFFECEAE3),
                modifier = Modifier.size(30.dp),
            )
        }
    }
}
