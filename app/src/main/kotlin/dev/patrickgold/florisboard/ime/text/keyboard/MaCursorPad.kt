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
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.OpenWith
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

    fun open() {
        active = true
    }

    fun close() {
        active = false
    }

    /**
     * How far the finger travels for one step, in pixels.
     *
     * Characters are cheaper to overshoot than lines, so horizontal is the shorter distance. Both
     * are deliberately larger than a fingertip's natural tremor: the pad should feel deliberate
     * rather than skittish, and an overshoot costs a second drag in the other direction.
     */
    private const val STEP_X = 28f
    private const val STEP_Y = 56f

    @Composable
    fun Overlay(
        modifier: Modifier = Modifier,
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
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragEnd = { close() },
                        onDragCancel = { close() },
                    ) { change, drag ->
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
                    text = "Drag finger to move cursor",
                    color = Color(0xFFECEAE3),
                    fontSize = 19.sp,
                )
            }
        }
    }
}
