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
 * What the ten macro buttons do: M1 to M10, each a label and a macro.
 *
 * Held apart from the rows on purpose. A row stores a reference to a slot, not its contents, so the
 * same macro can sit in two rows without being written twice, editing it in one place changes both,
 * and moving a button around cannot lose what was attached to it.
 *
 * The slots are fixed at ten and always exist, empty or not. M4 is M4 whatever else has been edited,
 * which is what lets somebody learn where his own macros live.
 */
object MaMacroSlots {

    private const val SLOT_SEP = '\u001E'
    private const val FIELD_SEP = '\u001D'

    /**
     * Three characters on the face of a key.
     *
     * The constraint is the key, not the label: these are thumb-sized keys sitting a dozen to a row,
     * and a label that does not fit is either drawn too small to read — which on this phone means
     * not at all — or clipped into something that says nothing.
     */
    /**
     * How much of a label a key will show.
     *
     * Was three, which was right when a macro key was a numbered slot and wrong now that it is a
     * thing he names. "UPPER" and "Title" say what they do; "UPP" and "Tit" have to be remembered.
     * The key grows to fit its text and the row scrolls, so a long name costs width rather than
     * legibility — and anything truly long is his own choice to make once and live with.
     */
    const val MAX_LABEL = 24

    /**
     * @param label what the key shows, up to [MAX_LABEL] characters
     * @param macro what it does, in [MaMacroSyntax]'s form: plain text types itself, braces are keys
     */
    data class Slot(val label: String, val macro: String) {
        val isEmpty: Boolean get() = macro.isBlank()
    }

    /**
     * The ten slots at rest: labelled M1 to M10, doing nothing yet.
     *
     * Labelled rather than blank so an unconfigured button still says which slot it is. A row of ten
     * blank keys gives no way to tell which one to go and edit.
     */
    fun empty(): List<Slot> = (1..MaRows.MACRO_SLOTS).map { Slot("M$it", "") }

    /** Truncates rather than rejecting: the macro is the valuable half, the label is only its face. */
    fun slot(label: String, macro: String): Slot = Slot(label.take(MAX_LABEL), macro)

    fun serialize(slots: List<Slot>): String =
        slots.joinToString(SLOT_SEP.toString()) { "${it.label}$FIELD_SEP${it.macro}" }

    /**
     * Parses the stored string, always returning exactly ten slots.
     *
     * Padded and truncated to length because the buttons are numbered and index into this list. A
     * short read is a missing slot, and a missing slot indexed by a button is a crash rather than a
     * key that does nothing.
     *
     * The macro is taken as the whole remainder after the first separator, so it may itself contain
     * that character without being cut short — which is what lets a macro hold anything at all.
     */
    fun parse(raw: String): List<Slot> {
        val parsed = if (raw.isBlank()) emptyList() else {
            raw.split(SLOT_SEP).map { chunk ->
                val idx = chunk.indexOf(FIELD_SEP)
                if (idx < 0) {
                    Slot(chunk.take(MAX_LABEL), "")
                } else {
                    Slot(chunk.substring(0, idx).take(MAX_LABEL), chunk.substring(idx + 1))
                }
            }
        }
        return (0 until MaRows.MACRO_SLOTS).map { i ->
            parsed.getOrNull(i) ?: Slot("M${i + 1}", "")
        }
    }

    fun defaultSerialized(): String = serialize(empty())

    /** The slot behind a button, 1-based, or an empty placeholder when the store is short. */
    fun at(slots: List<Slot>, slot: Int): Slot =
        slots.getOrNull(slot - 1) ?: Slot("M$slot", "")
}
