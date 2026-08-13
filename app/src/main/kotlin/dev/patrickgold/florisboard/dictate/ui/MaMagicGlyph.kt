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

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * The magic button: a wand inside a round button.
 *
 * ### Why it is not simply a wand
 *
 * A bare wand already means something else in this app. Rewording — the feature that fixes grammar
 * with a model — draws AutoFixHigh and AutoAwesome, and two features sharing a picture means the
 * picture has stopped saying which one you are looking at. That is a real collision rather than an
 * aesthetic one: one of them sends text to a model and costs money, and the other presses a button
 * on screen.
 *
 * So this is a wand enclosed in a circle, and the circle is the distinction. It says *button*: the
 * thing this feature does is press one. Rewording keeps the loose wand it already had, and nothing
 * had to change on that side.
 *
 * ### Drawn rather than composed from two icons
 *
 * Stacking a wand on a circle from the icon set gives a wand at the size the circle leaves, which at
 * 22dp is a smudge. Drawing both means the wand can be sized against the circle instead of against
 * the key, and the three sparks can be placed where there is room rather than where the original
 * icon happens to put them.
 */
@Composable
fun MaMagicGlyph(
    tint: Color,
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val line = w * 0.075f
        val cap = StrokeCap.Round

        // The button: a full circle, drawn thin so it frames rather than competes.
        drawCircle(
            color = tint,
            radius = (w / 2f) - line / 2f,
            style = Stroke(width = line),
        )

        // The wand, corner to corner across the middle of the circle rather than the whole glyph,
        // so it never touches the rim — a wand that meets the edge reads as a struck-through circle.
        drawLine(
            color = tint,
            start = Offset(w * 0.30f, h * 0.70f),
            end = Offset(w * 0.62f, h * 0.38f),
            strokeWidth = line * 1.35f,
            cap = cap,
        )

        // The tip, thickened, so the wand has a direction. Without it the line is a slash.
        drawLine(
            color = tint,
            start = Offset(w * 0.56f, h * 0.44f),
            end = Offset(w * 0.64f, h * 0.36f),
            strokeWidth = line * 2.1f,
            cap = cap,
        )

        // Three sparks of falling size, in the space above the wand. Three because two reads as an
        // accident and four crowds the rim at this size.
        val sparks = listOf(
            Triple(w * 0.72f, h * 0.26f, line * 1.5f),
            Triple(w * 0.62f, h * 0.20f, line * 1.0f),
            Triple(w * 0.78f, h * 0.40f, line * 0.9f),
        )
        for ((sx, sy, r) in sparks) {
            drawCircle(color = tint, radius = r / 2f, center = Offset(sx, sy))
        }
    }
}
