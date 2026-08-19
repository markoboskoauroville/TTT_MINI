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
 * The zone keys — `n`, `k`, `c` — each drawn as a letter inside a keyboard outline.
 *
 * ### Why the outline is back
 *
 * It was taken away and the keys became a bare green letter with a lit ring around the whole key.
 * Marko called that a downgrade and he is right: every other key in the row carries a drawn shape
 * with a recognisable silhouette, and three loose letters sitting among them read as something
 * unfinished that was patched in later rather than as part of the same set. A row is a set or it is
 * a pile, and one key that does not obey the set's own rule is what the eye lands on first.
 *
 * So the frame returns. The outline says *keyboard*, the letter inside says *which part of it* —
 * n for the number row, k for the keyboard itself, c for the copy row.
 *
 * ### What changed from the first version of this frame
 *
 * **The spacebar line is gone.** It cost the bottom quarter of the body and bought a detail nobody
 * needs told twice: at this size the rounded outline already reads as a keyboard on its own, and the
 * bar was mostly there to prove it. Removing it hands that quarter to the letter.
 *
 * **The frame and the letter are both as large as the key will hold.** The body now spans the full
 * width of the glyph box rather than sitting inside a margin, and the letter is drawn at roughly
 * seven tenths of the box against the four tenths it had — nearly twice the height, which is what
 * makes it legible at a glance instead of a mark you have to stop and read.
 *
 * ### Why drawn rather than an icon from the set
 *
 * The material keyboard icon is a keyboard with keys drawn on it. At this size those keys turn to
 * noise, and there is nowhere to put a letter without covering them. This outline is deliberately
 * empty for exactly that reason: the letter is the content, the frame is what tells you what kind of
 * thing the letter is.
 *
 * ### State
 *
 * The colour is passed in and nothing is drawn around the key. Green means this zone is open, and
 * that is the row's business to know, not this glyph's — the same rule every other key in the app
 * follows, where colour carries state and shape carries identity.
 */
@Composable
fun MaZoneGlyph(
    /**
     * The letter inside the outline: `n`, `k` or `c`.
     *
     * Letters rather than 1, 2, 3. A digit says only which position a key holds in a list nobody can
     * see; these say what the key toggles. Lowercase, because `n` and `c` have no ascender and sit
     * comfortably clear of the frame's walls where a capital would crowd them.
     */
    letter: String,
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
                // Full width, and a body a little wider than tall, which is the proportion that
                // reads as a keyboard rather than as a plain box. Centred vertically, so the letter
                // centred in the Box is also centred in the frame with no correction needed.
                val bodyTop = h * 0.12f
                val bodyHeight = h * 0.76f
                drawRoundRect(
                    color = tint,
                    topLeft = Offset(0f, bodyTop),
                    size = Size(w, bodyHeight),
                    cornerRadius = CornerRadius(w * 0.16f, w * 0.16f),
                    style = Stroke(width = w * 0.07f),
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = letter,
            color = tint,
            // Sized against the glyph rather than fixed, so the proportions hold whether it is drawn
            // small in the settings preview or large on the row. Seven tenths is as far as it goes
            // before `k`, the one letter here with an ascender, starts touching the frame.
            fontSize = (size.value * 0.70f).sp,
            fontWeight = FontWeight.SemiBold,
            // A hair down. A line box reserves descender space that none of n, k or c uses, so
            // centring the box leaves the ink reading slightly high.
            modifier = Modifier.offset(y = size * 0.02f),
        )
    }
}
