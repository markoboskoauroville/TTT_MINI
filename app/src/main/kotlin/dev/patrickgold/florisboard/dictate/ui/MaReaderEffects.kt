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

    data class Effect(val id: String, val label: String)

    /** Ordered as he uses them: the plain one first, the extreme one last. */
    val ALL: List<Effect> = listOf(
        Effect("highlight", "Highlight"),
        Effect("typewriter", "Typewriter"),
        Effect("karaoke", "Karaoke"),
        Effect("spotlight", "Spotlight"),
        Effect("void", "Void"),
        // Last, because it is the furthest from a subtitle: it does not mark a word in a page, it
        // resolves one out of noise.
        //
        // Anagram was here and is gone. It spelled the word across a tracing of the key grid, which
        // read as a clever thing happening to the keyboard rather than as a word being read — the
        // eye followed the letters travelling instead of the word arriving. Matrix keeps the one
        // part that worked, which is that the display is not a strip of text.
        Effect("matrix", "Matrix"),
    )
}
