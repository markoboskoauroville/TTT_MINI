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

class MaClicksTest {

    @Test
    fun `a sequence survives the round trip`() {
        val slots = listOf(
            MaClicks.Slot(
                "Open the thing",
                listOf(
                    MaClicks.Step(MaClicks.Kind.TAP, 0.5f, 0.25f, delayAfterMs = 800),
                    MaClicks.Step(MaClicks.Kind.WAIT, 0f, 0f, delayAfterMs = 2000),
                    MaClicks.Step(MaClicks.Kind.SWIPE, 0.9f, 0.8f, 0.1f, 0.8f, 400),
                    MaClicks.Step(MaClicks.Kind.BACK, 0f, 0f),
                ),
            ),
            MaClicks.Slot("Slot 2", emptyList()),
        )
        assertEquals(slots, MaClicks.parse(MaClicks.serialize(slots)))
    }

    @Test
    fun `an emptied slot keeps its place in the numbering`() {
        // Slot three must still be slot three after slot two is cleared, or every key that
        // triggers a slot quietly starts doing something else.
        val slots = listOf(
            MaClicks.Slot("one", listOf(MaClicks.Step(MaClicks.Kind.TAP, 0.1f, 0.1f))),
            MaClicks.Slot("two", emptyList()),
            MaClicks.Slot("three", listOf(MaClicks.Step(MaClicks.Kind.TAP, 0.3f, 0.3f))),
        )
        val back = MaClicks.parse(MaClicks.serialize(slots))
        assertEquals(3, back.size)
        assertTrue(back[1].isEmpty)
        assertEquals("three", back[2].name)
    }

    @Test
    fun `slotsOrEmpty always returns the full count`() {
        assertEquals(MaClicks.SLOT_COUNT, MaClicks.slotsOrEmpty("").size)
        assertEquals(MaClicks.SLOT_COUNT, MaClicks.slotsOrEmpty("nonsense").size)
        val one = MaClicks.serialize(
            listOf(MaClicks.Slot("only", listOf(MaClicks.Step(MaClicks.Kind.TAP, 0f, 0f)))),
        )
        val padded = MaClicks.slotsOrEmpty(one)
        assertEquals(MaClicks.SLOT_COUNT, padded.size)
        assertEquals("only", padded[0].name)
        assertTrue(padded[7].isEmpty)
    }

    @Test
    fun `rubbish parses to empty slots rather than throwing`() {
        assertTrue(MaClicks.parse("not a sequence").all { it.isEmpty })
        assertEquals(emptyList(), MaClicks.parse(""))
    }

    @Test
    fun `a step naming an unknown kind is dropped and the rest survive`() {
        val stored = MaClicks.serialize(
            listOf(
                MaClicks.Slot(
                    "s",
                    listOf(
                        MaClicks.Step(MaClicks.Kind.TAP, 0.1f, 0.1f),
                        MaClicks.Step(MaClicks.Kind.TAP, 0.2f, 0.2f),
                    ),
                ),
            ),
        ).replaceFirst("tap", "pinch")
        val steps = MaClicks.parse(stored)[0].steps
        assertEquals(1, steps.size)
        assertEquals(0.2f, steps[0].x)
    }

    @Test
    fun `positions outside the screen are clamped rather than dropped`() {
        val stored = MaClicks.serialize(
            listOf(MaClicks.Slot("s", listOf(MaClicks.Step(MaClicks.Kind.TAP, 0.5f, 0.5f)))),
        ).replace("0.5", "4.0")
        val step = MaClicks.parse(stored)[0].steps[0]
        assertEquals(1f, step.x)
        assertEquals(1f, step.y)
    }

    @Test
    fun `a negative delay cannot run the playback timer backwards`() {
        val stored = MaClicks.serialize(
            listOf(MaClicks.Slot("s", listOf(MaClicks.Step(MaClicks.Kind.TAP, 0.1f, 0.1f, delayAfterMs = 500)))),
        ).replace("500", "-500")
        assertEquals(0L, MaClicks.parse(stored)[0].steps[0].delayAfterMs)
    }

    @Test
    fun `a slot name containing punctuation comes back intact`() {
        val name = "Kerstin's list, v2 (final)"
        val slots = listOf(MaClicks.Slot(name, listOf(MaClicks.Step(MaClicks.Kind.TAP, 0f, 0f))))
        assertEquals(name, MaClicks.parse(MaClicks.serialize(slots))[0].name)
    }

    @Test
    fun `total delay adds up the steps`() {
        val slot = MaClicks.Slot(
            "s",
            listOf(
                MaClicks.Step(MaClicks.Kind.TAP, 0f, 0f, delayAfterMs = 100),
                MaClicks.Step(MaClicks.Kind.WAIT, 0f, 0f, delayAfterMs = 250),
            ),
        )
        assertEquals(350L, slot.totalDelayMs)
    }

    @Test
    fun `every kind id is distinct`() {
        val ids = MaClicks.Kind.entries.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
    }
}
