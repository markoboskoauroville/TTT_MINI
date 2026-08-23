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

/**
 * The reading effects, named in one place.
 *
 * The dashboard and the Reader screen both offer them, and a second list would be a second place for
 * a new effect to be forgotten. Ids match `maReaderStyle` exactly — they are what is stored.
 */
object MaReaderEffects {

    /**
     * [short] is what the chip shows: two or three characters, so all of them fit one row without
     * scrolling and no chip is wider than a key on the feature row underneath.
     *
     * [label] is kept for anywhere with room to spell it out. **The short form is an abbreviation of
     * the name, never a different name** — "Kar" is Karaoke shortened, and somebody reading the chip
     * and somebody reading the settings are looking at the same word.
     */
    data class Effect(val id: String, val label: String, val short: String)

    /** Ordered as he uses them: the plain one first, the extreme one last. */
    val ALL: List<Effect> = listOf(
        Effect("highlight", "Highlight", "Hi"),
        Effect("typewriter", "Typewriter", "Ty"),
        Effect("karaoke", "Karaoke", "Kar"),
        Effect("spotlight", "Spotlight", "Sp"),
        Effect("void", "Void", "Vo"),
        // "Top line" is gone, and this is a promotion rather than a deletion.
        //
        // It was one effect that pinned the reading to the top edge. Where the line sits turns out
        // to be a question worth asking of EVERY effect, not a seventh effect — he asked for top,
        // middle and bottom next to the highlight swatch, and that is the right shape: an effect
        // decides what the marking looks like, alignment decides where it sits. Two questions, two
        // controls, and every answer to one now works with every answer to the other.
        // Last, because it is the furthest from a subtitle: it does not mark a word in a page, it
        // resolves one out of noise.
        //
        // Anagram was here and is gone. It spelled the word across a tracing of the key grid, which
        // read as a clever thing happening to the keyboard rather than as a word being read — the
        // eye followed the letters travelling instead of the word arriving. Matrix keeps the one
        // part that worked, which is that the display is not a strip of text.
        Effect("matrix", "Matrix", "Mx"),
    )
}
