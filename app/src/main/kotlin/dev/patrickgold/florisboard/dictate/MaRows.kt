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

/**
 * The feature rows: three of them, each a preset, each switchable on its own.
 *
 * This is the app's main feature and its model is deliberately flat. Everything that can sit on a
 * row is a button of equal standing — the app's own keys, the ten clipboard slots C1 to C10, and the
 * ten macro slots M1 to M10. Nothing nests inside anything else and nothing is protected.
 *
 * ### What replaced what, and why
 *
 * An earlier design made the clipboard a child row of the feature row that expanded and collapsed.
 * That is gone. Clipboard slots are now simply buttons, so they can be put anywhere, mixed with
 * anything, in any of the three rows, and a row of five clipboard keys beside a backspace is as
 * valid an arrangement as any other. The nesting bought nothing and cost the freedom to arrange.
 *
 * ### Rows collapse upward
 *
 * Three rows exist always. Switching one off does not leave a gap: the rows that remain move up and
 * take the space, so with only row two enabled it is drawn where row one would have been. The order
 * among the survivors never changes, which is what makes the arrangement predictable — switching row
 * one off must not reshuffle two and three.
 *
 * ### Nothing is protected, except the way back
 *
 * There is no locked key. Every button, including backspace and enter, can be unticked. The single
 * guarantee is that if the user switches off everything, in every row, the settings key appears on
 * its own — because a keyboard with no keys at all and no route to the settings that would restore
 * them is a keyboard that can only be fixed by uninstalling the app. That is a floor, not a lock:
 * the settings key is an ordinary button the rest of the time and can be placed, moved and unticked
 * like any other.
 */
object MaRows {

    private const val ROW_SEP = '\u001E'
    private const val BTN_SEP = '\u001F'
    private const val FIELD_SEP = '\u001D'
    private const val ROW_META_SEP = '\u001C'

    private const val T_BUILTIN = "b"
    private const val T_CLIP = "c"
    private const val T_MACRO = "m"

    /** Three rows, always. They are presets to switch between, not a list to grow. */
    const val ROW_COUNT = 3

    /**
     * The copy row: a fourth row that belongs to the transcription view alone.
     *
     * It is the same machinery as the feature rows — same entries, same catalogue, same editor —
     * and stored separately because it is not one of the three presets. **It appears only in the
     * transcription view**, where there are no letter keys and the clipboard is the whole job, and
     * never on the typing keyboard where the feature rows already live.
     *
     * Its own preference rather than a fourth slot in `maRows`, because `parse` pads and truncates
     * to exactly ROW_COUNT — a fourth row there is silently dropped, and the source would look
     * right while the keyboard never showed it.
     */
    fun defaultCopyRow(): Row = Row(
        listOf(
            MaFeatureKey.SELECT_ALL,
            MaFeatureKey.PASTE,
            MaFeatureKey.CUT,
            MaFeatureKey.CLIP_HISTORY,
            MaFeatureKey.HISTORY,
            MaFeatureKey.ALL_PASTE,
            MaFeatureKey.ALL_CLEAR,
        ).map { Entry(Button.Builtin(it)) },
        enabled = true,
    )

    fun parseCopyRow(raw: String): Row =
        if (raw.isBlank()) defaultCopyRow() else parse(raw).firstOrNull() ?: defaultCopyRow()

    fun serializeCopyRow(row: Row): String = serialize(listOf(row))

    /** C1 to C10: the last ten things copied, newest first. */
    const val CLIP_SLOTS = 10

    /** M1 to M10: the ten buttons the user writes himself. */
    const val MACRO_SLOTS = 10

    /**
     * Anything that can sit on a row. Three kinds, all of equal standing.
     */
    sealed interface Button {

        /** One of the app's own keys: record, backspace, AP, AC, a zone toggle, the gear. */
        data class Builtin(val key: MaFeatureKey) : Button

        /**
         * A clipboard slot, 1 to [CLIP_SLOTS]. C1 is the last thing copied.
         *
         * The slot number is stored, not the text. The clipboard changes constantly and a button
         * that captured its contents when it was placed would paste whatever happened to be copied
         * on the day the row was arranged.
         */
        data class Clip(val slot: Int) : Button

        /**
         * A macro slot, 1 to [MACRO_SLOTS]. What M4 does is stored in [MaMacroSlots], not here.
         *
         * The reference rather than the content: the same macro can then appear in two rows without
         * being written twice, and editing it in one place changes both. It also means moving a
         * button around cannot lose the macro attached to it.
         */
        data class Macro(val slot: Int) : Button
    }

    /**
     * One button on a row, and whether its tick is on.
     *
     * Unticked buttons stay in the list rather than being deleted. A list whose entries disappear
     * when switched off cannot be learned by position, and this is a list navigated by position, by
     * somebody who reads it with difficulty. Switching a button back on has to put it back where it
     * was, not at the end.
     */
    data class Entry(val button: Button, val enabled: Boolean = true)

    /** One of the three rows: its buttons in order, and whether the row itself is switched on. */
    data class Row(val entries: List<Entry>, val enabled: Boolean = true) {
        val visibleButtons: List<Button> get() = entries.filter { it.enabled }.map { it.button }
    }

    /**
     * What the keyboard actually draws.
     *
     * Rows that are off are absent rather than empty, so the ones below move up and the keyboard is
     * genuinely shorter. A row that is on but has nothing ticked is dropped too — an empty strip is
     * wasted height, and the row is still there in the editor to be filled in again.
     *
     * The floor: when that leaves nothing at all, the settings key is drawn alone. Without it the
     * user would be looking at a keyboard with no keys and no way to reach the screen that puts them
     * back.
     */
    fun visibleRows(rows: List<Row>): List<List<Button>> {
        val drawn = rows.filter { it.enabled }
            .map { it.visibleButtons }
            .filter { it.isNotEmpty() }
        // Reversed, so row 1 is the one nearest the keys.
        //
        // The rows are numbered by how close they sit to the typing, not by how far down the editor
        // they appear. Row 1 is the one his thumb reaches without moving, which is why it is the
        // one he fills first, and it has to stay in that place as rows above it come and go. With
        // 1 and 3 on, 1 is at the bottom and 3 above it; with all three, 1, 2, 3 from the bottom up.
        //
        // Drawn last means drawn lowest in a Column, so the list is reversed here rather than at the
        // point of drawing — one place, and it cannot disagree with itself.
        return drawn.reversed().ifEmpty {
            listOf(listOf(Button.Builtin(MaFeatureKey.SETTINGS)))
        }
    }

    /**
     * Which copy buckets are actually on the keyboard, as slot numbers.
     *
     * The buckets a copy may fill are exactly the ones the user can see and press. A bucket that is
     * not on any row cannot be pasted from, so filling it silently swallows a copy: the text is
     * captured, the row looks unchanged, and the only way to reach it would be to go into the editor
     * and add the button afterwards. That is not a bucket, it is a hole.
     *
     * Returned as the set of numbers rather than a count, because the buttons need not be
     * contiguous. Somebody with C1, C2 and C7 on the row has three buckets and they are 1, 2 and 7 —
     * a capacity of three would fill C1, C2 and C3, and C3 is not on the screen.
     */
    fun visibleClipSlots(rows: List<Row>): Set<Int> =
        visibleRows(rows).flatten().filterIsInstance<Button.Clip>().map { it.slot }.toSet()

    /**
     * The starting arrangement: one full row, two empty rows waiting to be built.
     *
     * Rows two and three start switched off and empty rather than pre-filled with a guess. A row
     * arriving full of keys somebody did not choose has to be emptied before it can be used, which
     * is more work than filling an empty one.
     */
    fun defaultRows(): List<Row> = listOf(
        // The row Marko actually settled on, shipped as it stands.
        //
        // It is six keys rather than eleven, and that is the finding rather than a compromise: after
        // weeks of arranging, what he kept was the gear, the keyboard zone, history, the dictation
        // view, AC, and the magic row above it. Everything else he took off. Shipping the row he
        // built beats shipping the row I guessed at, and a new install starts where he finished
        // instead of where I started.
        //
        // Row one is nearest the keys, so this is the row under his thumb.
        Row(
            listOf(
                MaFeatureKey.SETTINGS,
                MaFeatureKey.ZONE_2,
                MaFeatureKey.HISTORY,
                MaFeatureKey.MIC,
                MaFeatureKey.ALL_CLEAR,
            ).map { Entry(Button.Builtin(it)) },
            enabled = true,
        ),
        // Row two: the copy row, in the order he asked for.
        //
        // Select all, paste, cut, clipboard history, dictation history, AP, AC. It is the row he
        // had been arranging by hand in the Smartbar, which could never hold AP and AC because
        // those are not Smartbar actions — the four clipboard keys were moved into the feature row
        // vocabulary precisely so this row could exist in one place and be edited like any other.
        //
        // COPY is deliberately absent: he listed seven keys and copy was not among them. It is one
        // tick away in the catalogue if that was an oversight rather than a decision.
        Row(
            listOf(
                MaFeatureKey.SELECT_ALL,
                MaFeatureKey.PASTE,
                MaFeatureKey.CUT,
                MaFeatureKey.CLIP_HISTORY,
                MaFeatureKey.HISTORY,
                MaFeatureKey.ALL_PASTE,
                MaFeatureKey.ALL_CLEAR,
            ).map { Entry(Button.Builtin(it)) },
            enabled = true,
        ),
        // Three starts empty and off. A row arriving full of keys nobody chose has to be emptied
        // before it can be used, which is more work than filling an empty one.
        //
        // Only one empty row now, because the copy row above took the second slot and ROW_COUNT is
        // three. A fourth Row here would be silently dropped by `parse`, which pads or truncates to
        // exactly three — the list would look right in this file and never reach the keyboard.
        Row(emptyList(), enabled = false),
    )

    /** Every button that can be added, in the order the editor offers them. */
    fun catalogue(): List<Button> =
        // No filtering needed: the clipboard is no longer a set of keys in this enum, it is
        // Button.Clip. The enum now holds only the app's own keys, so every one of them is offered.
        MaFeatureKey.entries.map { Button.Builtin(it) } +
            (1..CLIP_SLOTS).map { Button.Clip(it) } +
            (1..MACRO_SLOTS).map { Button.Macro(it) }

    fun serialize(rows: List<Row>): String =
        rows.joinToString(ROW_SEP.toString()) { row ->
            val flag = if (row.enabled) "1" else "0"
            val body = row.entries.joinToString(BTN_SEP.toString()) { entry ->
                val on = if (entry.enabled) "1" else "0"
                when (val b = entry.button) {
                    is Button.Builtin -> "$T_BUILTIN$FIELD_SEP${b.key.id}$FIELD_SEP$on"
                    is Button.Clip -> "$T_CLIP$FIELD_SEP${b.slot}$FIELD_SEP$on"
                    is Button.Macro -> "$T_MACRO$FIELD_SEP${b.slot}$FIELD_SEP$on"
                }
            }
            "$flag$ROW_META_SEP$body"
        }

    /**
     * Parses the stored string, dropping anything malformed rather than throwing.
     *
     * Read while the keyboard is opening, in front of whatever the user was about to type into. A
     * damaged preference has to cost an arrangement, never a keyboard that refuses to draw: there is
     * no route to the settings app from behind a keyboard that never appears.
     *
     * Always returns exactly [ROW_COUNT] rows, padding with empty ones. The editor has three tabs and
     * indexes into this list; a short read would be a missing tab or a crash.
     */
    fun parse(raw: String): List<Row> {
        val parsed = if (raw.isBlank()) emptyList() else {
            raw.split(ROW_SEP).map { rowText ->
                val metaIdx = rowText.indexOf(ROW_META_SEP)
                val enabled = metaIdx <= 0 || rowText.substring(0, metaIdx) == "1"
                val body = if (metaIdx >= 0) rowText.substring(metaIdx + 1) else rowText
                val entries = if (body.isBlank()) emptyList() else {
                    body.split(BTN_SEP).mapNotNull { parseEntry(it) }
                }
                Row(entries, enabled)
            }
        }
        return (0 until ROW_COUNT).map { i ->
            parsed.getOrNull(i) ?: Row(emptyList(), enabled = false)
        }
    }

    private fun parseEntry(raw: String): Entry? {
        val f = raw.split(FIELD_SEP)
        if (f.size != 3) return null
        val enabled = f[2] == "1"
        val button = when (f[0]) {
            // A key that no longer exists is dropped rather than guessed at. Keys have been removed
            // from this app before — the reader's book key went with the reader — and a row stored
            // before that is otherwise still perfectly good.
            T_BUILTIN -> MaFeatureKey.byId(f[1])?.let { Button.Builtin(it) }
            T_CLIP -> f[1].toIntOrNull()?.takeIf { it in 1..CLIP_SLOTS }?.let { Button.Clip(it) }
            T_MACRO -> f[1].toIntOrNull()?.takeIf { it in 1..MACRO_SLOTS }?.let { Button.Macro(it) }
            else -> null
        } ?: return null
        return Entry(button, enabled)
    }

    fun defaultSerialized(): String = serialize(defaultRows())

    /** Moves a button within its row. Out-of-range indices are ignored rather than throwing. */
    fun move(rows: List<Row>, rowIndex: Int, from: Int, to: Int): List<Row> {
        val row = rows.getOrNull(rowIndex) ?: return rows
        if (from !in row.entries.indices || to !in row.entries.indices) return rows
        val moved = row.entries.toMutableList().apply { add(to, removeAt(from)) }
        return rows.mapIndexed { i, r -> if (i == rowIndex) r.copy(entries = moved) else r }
    }
}
