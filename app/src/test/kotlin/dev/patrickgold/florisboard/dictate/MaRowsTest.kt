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
import kotlin.test.assertTrue

class MaRowsTest {

    @Test
    fun `an arrangement survives the round trip`() {
        val rows = listOf(
            MaRows.Row(
                listOf(
                    MaRows.Entry(MaRows.Button.Builtin(MaFeatureKey.MIC)),
                    MaRows.Entry(MaRows.Button.Clip(3), enabled = false),
                    MaRows.Entry(MaRows.Button.Macro(7)),
                ),
            ),
            MaRows.Row(listOf(MaRows.Entry(MaRows.Button.Clip(1))), enabled = false),
            MaRows.Row(emptyList(), enabled = false),
        )
        assertEquals(rows, MaRows.parse(MaRows.serialize(rows)))
    }

    @Test
    fun `the defaults survive the round trip`() {
        assertEquals(MaRows.defaultRows(), MaRows.parse(MaRows.defaultSerialized()))
    }

    @Test
    fun `parse always returns three rows`() {
        assertEquals(3, MaRows.parse("").size)
        assertEquals(3, MaRows.parse("rubbish").size)
        assertEquals(3, MaRows.parse(MaRows.defaultSerialized()).size)
    }

    // Switching a row off must not leave a gap. Row two drawn where row one was is the whole
    // behaviour Marko described.
    @Test
    fun `rows collapse upward when one is switched off`() {
        val rows = listOf(
            MaRows.Row(listOf(MaRows.Entry(MaRows.Button.Builtin(MaFeatureKey.MIC))), enabled = false),
            MaRows.Row(listOf(MaRows.Entry(MaRows.Button.Clip(1))), enabled = true),
            MaRows.Row(listOf(MaRows.Entry(MaRows.Button.Macro(1))), enabled = true),
        )
        // Reversed: row 1 sits nearest the keys, so it is drawn last and appears last in this list.
        val visible = MaRows.visibleRows(rows)
        assertEquals(2, visible.size)
        assertEquals(MaRows.Button.Macro(1), visible[0][0])
        assertEquals(MaRows.Button.Clip(1), visible[1][0])
    }

    @Test
    fun `the order among surviving rows never changes`() {
        val rows = listOf(
            MaRows.Row(listOf(MaRows.Entry(MaRows.Button.Clip(1)))),
            MaRows.Row(listOf(MaRows.Entry(MaRows.Button.Clip(2))), enabled = false),
            MaRows.Row(listOf(MaRows.Entry(MaRows.Button.Clip(3)))),
        )
        // Row 3 is furthest from the keys and therefore first in the drawing order; row 1 is last.
        // What must not change is their order relative to each other.
        val visible = MaRows.visibleRows(rows)
        assertEquals(listOf(MaRows.Button.Clip(3)), visible[0])
        assertEquals(listOf(MaRows.Button.Clip(1)), visible[1])
    }

    @Test
    fun `an unticked button is not drawn but is not lost either`() {
        val rows = listOf(
            MaRows.Row(
                listOf(
                    MaRows.Entry(MaRows.Button.Builtin(MaFeatureKey.MIC), enabled = false),
                    MaRows.Entry(MaRows.Button.Builtin(MaFeatureKey.ENTER)),
                ),
            ),
            MaRows.Row(emptyList(), enabled = false),
            MaRows.Row(emptyList(), enabled = false),
        )
        assertEquals(1, MaRows.visibleRows(rows)[0].size)
        // Still in the stored arrangement, so ticking it again puts it back where it was.
        assertEquals(2, MaRows.parse(MaRows.serialize(rows))[0].entries.size)
    }

    // The one guarantee. Everything off, in every row, must still leave a route back to the screen
    // that would put the keys back — otherwise the only repair is uninstalling the app.
    @Test
    fun `with everything switched off the settings key survives alone`() {
        val rows = (1..3).map { MaRows.Row(emptyList(), enabled = false) }
        val visible = MaRows.visibleRows(rows)
        assertEquals(1, visible.size)
        assertEquals(listOf(MaRows.Button.Builtin(MaFeatureKey.SETTINGS)), visible[0])
    }

    @Test
    fun `a row that is on but has nothing ticked is not drawn as an empty strip`() {
        val rows = listOf(
            MaRows.Row(listOf(MaRows.Entry(MaRows.Button.Clip(1), enabled = false)), enabled = true),
            MaRows.Row(listOf(MaRows.Entry(MaRows.Button.Clip(2))), enabled = true),
            MaRows.Row(emptyList(), enabled = false),
        )
        assertEquals(1, MaRows.visibleRows(rows).size)
    }

    @Test
    fun `a stored key that no longer exists is dropped and the rest of the row survives`() {
        val stored = MaRows.serialize(
            listOf(
                MaRows.Row(
                    listOf(
                        MaRows.Entry(MaRows.Button.Builtin(MaFeatureKey.MIC)),
                        MaRows.Entry(MaRows.Button.Clip(2)),
                    ),
                ),
            ),
        ).replace("mic", "book")
        val row = MaRows.parse(stored)[0]
        assertEquals(1, row.entries.size)
        assertEquals(MaRows.Button.Clip(2), row.entries[0].button)
    }

    @Test
    fun `a clip or macro slot outside its range is dropped rather than clamped`() {
        // Clamping would silently point the button at a different slot, which is worse than the
        // button not being there: it would paste the wrong thing rather than nothing.
        val stored = MaRows.serialize(
            listOf(MaRows.Row(listOf(MaRows.Entry(MaRows.Button.Clip(2))))),
        ).replace("c\u001D2", "c\u001D99")
        assertTrue(MaRows.parse(stored)[0].entries.isEmpty())
    }

    @Test
    fun `there are ten clipboard slots and ten macro slots`() {
        assertEquals(10, MaRows.CLIP_SLOTS)
        assertEquals(10, MaRows.MACRO_SLOTS)
        val cat = MaRows.catalogue()
        assertEquals(10, cat.count { it is MaRows.Button.Clip })
        assertEquals(10, cat.count { it is MaRows.Button.Macro })
    }

    @Test
    fun `the catalogue offers every app key plus the twenty slots`() {
        val cat = MaRows.catalogue()
        assertEquals(MaFeatureKey.entries.size, cat.count { it is MaRows.Button.Builtin })
        assertEquals(MaFeatureKey.entries.size + 20, cat.size)
    }

    @Test
    fun `moving a button within a row keeps every other row untouched`() {
        val rows = MaRows.defaultRows()
        val moved = MaRows.move(rows, 0, 0, 3)
        assertEquals(rows[1], moved[1])
        assertEquals(rows[2], moved[2])
        assertEquals(rows[0].entries.size, moved[0].entries.size)
        assertEquals(rows[0].entries[0], moved[0].entries[3])
    }

    @Test
    fun `an out of range move is ignored rather than throwing`() {
        val rows = MaRows.defaultRows()
        assertEquals(rows, MaRows.move(rows, 0, 0, 99))
        assertEquals(rows, MaRows.move(rows, 9, 0, 1))
    }

    @Test
    fun `macro slots round trip and keep awkward macro text intact`() {
        val nasty = "Dear {F5} \"team\",\nregards; {{literal}} 100%"
        val slots = MaMacroSlots.empty().toMutableList()
        slots[3] = MaMacroSlots.slot("ltr", nasty)
        val back = MaMacroSlots.parse(MaMacroSlots.serialize(slots))
        assertEquals(10, back.size)
        assertEquals(nasty, back[3].macro)
        assertEquals("ltr", back[3].label)
    }

    @Test
    fun `macro slots are always ten however short the stored value is`() {
        assertEquals(10, MaMacroSlots.parse("").size)
        assertEquals("M10", MaMacroSlots.parse("").last().label)
    }

    @Test
    fun `a macro label longer than the limit is truncated rather than dropped`() {
        // Twenty-four, not three: a macro key is named rather than numbered now, so "UPPER" fits
        // where "UPP" had to be remembered.
        val s = MaMacroSlots.slot("a".repeat(30), "{Enter}")
        assertEquals(24, s.label.length)
        assertEquals("{Enter}", s.macro)
    }
}
