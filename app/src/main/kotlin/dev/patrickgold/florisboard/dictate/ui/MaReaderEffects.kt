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
        Effect("oneword", "One word"),
        Effect("void", "Void"),
    )
}
