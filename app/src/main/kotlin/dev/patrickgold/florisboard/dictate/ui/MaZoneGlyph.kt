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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * The zone keys: a keyboard, with the number of the row it shows.
 *
 * ### Why not just the numeral
 *
 * A bare green 1, 2 and 3 sat on the row among AP, AC and the clipboard numbers, and Marko could
 * not tell them apart — there were digits everywhere and nothing said which of them opened a row of
 * the keyboard. A numeral is a value; these are switches.
 *
 * So the numeral is drawn inside a keyboard: a rounded outline with a wide bar along the bottom for
 * the spacebar, which is the shape everyone recognises as a keyboard at any size. The number sits
 * on it, so the key reads as "the keyboard's row one" rather than as the digit one.
 *
 * ### Why drawn rather than an icon from the set
 *
 * The material keyboard icon is a keyboard with keys drawn on it. At 22dp those keys become noise,
 * and there is nowhere to put a number without covering them. This outline is deliberately almost
 * empty for exactly that reason: the number is the content, and the keyboard is the frame telling
 * you what kind of thing the number is.
 *
 * The colour is passed in rather than chosen here, because green means "this zone is open" and that
 * is the row's business to know, not this glyph's.
 */
@Composable
fun MaZoneGlyph(
    number: Int,
    tint: Color,
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 24.dp,
) {
    Box(
        modifier = modifier
            .size(size)
            .drawBehind {
                val w = this.size.width
                val h = this.size.height
                // The body: a little wider than tall, the way a keyboard is.
                val bodyTop = h * 0.16f
                val bodyHeight = h * 0.68f
                val stroke = Stroke(width = w * 0.075f)
                drawRoundRect(
                    color = tint,
                    topLeft = Offset(0f, bodyTop),
                    size = Size(w, bodyHeight),
                    cornerRadius = CornerRadius(w * 0.14f, w * 0.14f),
                    style = stroke,
                )
                // The spacebar, low and wide. One mark rather than a grid of keys: at this size a
                // grid turns to mush, and this single bar is what makes the outline read as a
                // keyboard rather than as a plain box.
                val barInset = w * 0.26f
                val barY = bodyTop + bodyHeight * 0.74f
                drawLine(
                    color = tint,
                    start = Offset(barInset, barY),
                    end = Offset(w - barInset, barY),
                    strokeWidth = w * 0.075f,
                )
            },
        // Centred on the body rather than on the whole glyph, and nudged up by the height of the
        // spacebar so the number sits in the empty part instead of on the bar.
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = number.toString(),
            color = tint,
            // Sized against the glyph rather than fixed, so the number keeps its proportions when
            // the same glyph is drawn small on the row and larger in the editor.
            fontSize = (size.value * 0.40f).sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.offset(y = -(size * 0.06f)),
        )
    }
}
