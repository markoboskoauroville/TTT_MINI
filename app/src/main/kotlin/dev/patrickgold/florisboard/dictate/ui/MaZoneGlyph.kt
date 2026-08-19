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
import androidx.compose.ui.graphics.Color
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
    /**
     * The letter: `n`, `k` or `c` — number row, keyboard, copy row.
     *
     * The keyboard outline and its little spacebar line are gone. At the size a key actually is,
     * that outline left room for a letter too small to read at a glance, which defeated the whole
     * reason for using letters instead of digits. **The key's own border carries the "this is on"
     * state now**, so the glyph has the whole face to itself and the letter can be as big as it
     * needs to be.
     */
    letter: String,
    tint: Color,
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 24.dp,
) {
    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = letter,
            color = tint,
            // Nearly the whole glyph. Sized against it rather than fixed, so the same letter keeps
            // its proportions drawn small on the row and larger in the editor.
            fontSize = (size.value * 0.86f).sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}
