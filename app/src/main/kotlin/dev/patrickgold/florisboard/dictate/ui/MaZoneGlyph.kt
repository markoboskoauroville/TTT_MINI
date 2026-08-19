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
    letter: String,
    tint: Color,
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 24.dp,
) {
    // Just the letter, as large as the key will hold.
    //
    // The keyboard outline and its spacebar line are gone. They drew a picture of a keyboard on
    // three keys that toggle three different things, so the picture said the same word every time
    // and the only distinguishing mark — the letter — was squeezed into the space left over.
    //
    // Inverted now: the letter is the whole glyph, and whether the key is ON is said by an outline
    // around the KEY itself. One thing per surface, and the thing that varies gets the room.
    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = letter,
            color = tint,
            // Nearly the full height of the box. Sized against the glyph rather than fixed, so it
            // keeps its proportions drawn small on the row and larger in the editor.
            fontSize = (size.value * 0.86f).sp,
            fontWeight = FontWeight.SemiBold,
            // Lowercase letters sit on a baseline with descender space beneath that nothing here
            // uses, so without this nudge the letter reads as hanging above centre.
            modifier = Modifier.offset(y = size * 0.04f),
        )
    }
}
