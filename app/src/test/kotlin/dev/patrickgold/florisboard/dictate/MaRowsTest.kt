/*
 * Copyright (C) 2026 Marko Bosko, Mantra Productions
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package dev.patrickgold.florisboard.dictate

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MaRowsTest {

    @Test
    fun `a mixed row survives the round trip`() {
        val rows = listOf(
            listOf(
                MaRows.Button.Builtin(MaFeatureKey.MIC),
                MaRows.macro("^C", "{Ctrl+C}"),
                MaRows.Button.Builtin(MaFeatureKey.BACKSPACE),
            ),
            listOf(MaRows.macro("hi", "Dobar dan")),
        )
        assertEquals(rows, MaRows.parse(MaRows.serialize(rows)))
    }

    @Test
    fun `the default rows survive the round trip`() {
        assertEquals(MaRows.defaultRows(), MaRows.parse(MaRows.defaultSerialized()))
    }

    /**
     * The reason the separators are control characters. A macro holding a comma, a brace, a quote or
     * a newline has to come back exactly as written, because it is going to be typed into somebody's
     * document.
     */
    @Test
    fun `macro text with awkward characters is not mangled`() {
        val nasty = "Dear {F5} \"team\",\nregards; {{literal}} 100%"
        val rows = listOf(listOf(MaRows.macro("ltr", nasty)))
        assertEquals(nasty, (MaRows.parse(MaRows.serialize(rows))[0][0] as MaRows.Button.Macro).macro)
    }

    @Test
    fun `a label longer than the limit is truncated rather than dropped`() {
        val button = MaRows.macro("abcdef", "{Enter}")
        assertEquals("abc", button.label)
        assertEquals("{Enter}", button.macro)
    }

    @Test
    fun `an unknown builtin id is dropped and the rest of the row survives`() {
        // "book" was the reader's key and no longer exists. A row stored before it was removed is
        // otherwise still perfectly good and must not be thrown away whole.
        val stored = MaRows.serialize(
            listOf(listOf(MaRows.Button.Builtin(MaFeatureKey.MIC), MaRows.macro("x", "x"))),
        ).replace("mic", "book")
        val parsed = MaRows.parse(stored)
        assertEquals(1, parsed.size)
        assertEquals(1, parsed[0].size)
        assertEquals(MaRows.macro("x", "x"), parsed[0][0])
    }

    @Test
    fun `a blank string parses to nothing rather than throwing`() {
        assertEquals(emptyList(), MaRows.parse(""))
        assertEquals(emptyList(), MaRows.parse("   "))
    }

    @Test
    fun `rubbish parses to nothing rather than throwing`() {
        assertEquals(emptyList(), MaRows.parse("not a serialized row at all"))
    }

    @Test
    fun `there is no cap on the number of rows`() {
        val many = (1..40).map { listOf(MaRows.macro("r$it".take(3), "row $it")) }
        assertEquals(40, MaRows.parse(MaRows.serialize(many)).size)
    }

    @Test
    fun `migration keeps the feature row first and every macro row after it`() {
        val featureOrder = MaFeatureOrder.serialize(
            listOf(MaFeatureKey.MIC, MaFeatureKey.BACKSPACE, MaFeatureKey.ENTER),
        )
        val hidden = MaFeatureOrder.serializeHidden(emptySet())
        val macros = MaMacros.serialize(
            listOf(
                MaMacros.Preset("Editing", listOf(listOf(MaMacros.Macro("all", "{Ctrl+A}")))),
                MaMacros.Preset("Other", listOf(listOf(MaMacros.Macro(",", ", ")))),
            ),
        )
        val rows = MaRows.migrate(featureOrder, hidden, macros)
        assertEquals(3, rows.size)
        assertTrue(rows[0].all { it is MaRows.Button.Builtin })
        assertEquals(MaRows.macro("all", "{Ctrl+A}"), rows[1][0])
        assertEquals(MaRows.macro(",", ", "), rows[2][0])
    }

    @Test
    fun `a hidden feature key does not come across the migration`() {
        val featureOrder = MaFeatureOrder.serialize(listOf(MaFeatureKey.MIC, MaFeatureKey.ALL_PASTE))
        val hidden = MaFeatureOrder.serializeHidden(setOf(MaFeatureKey.ALL_PASTE))
        val rows = MaRows.migrate(featureOrder, hidden, "")
        assertTrue(rows[0].none { it == MaRows.Button.Builtin(MaFeatureKey.ALL_PASTE) })
    }

    @Test
    fun `every palette label fits on a key`() {
        assertEquals(emptyList(), MaCommandPalette.oversizedLabels())
    }

    @Test
    fun `no palette entry is missing its token`() {
        assertNull(MaCommandPalette.ALL.firstOrNull { it.token.isBlank() })
    }
}
