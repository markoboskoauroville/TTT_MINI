/*
 * Copyright (C) 2026 Marko Bosko, Mantra Productions
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package dev.patrickgold.florisboard.app.settings.dictate

/**
 * The switchboard's order, arranged by him.
 *
 * ### Why this is stored at all
 *
 * Every list in this app that he reads often is arrangeable: the settings home, the feature rows,
 * the copy row. The switchboard is read more than any of them and was the last one still in an order
 * somebody else chose. **A shipped order is a guess, and a guess repeated every day is worse than a
 * control.**
 *
 * ### Stored by id, never by position
 *
 * A saved list of numbers would silently mean something different the moment a switch is added or
 * removed — the arrangement would survive and quietly point at the wrong rows. Ids are stable, and
 * an id that no longer exists is simply dropped.
 *
 * ### New switches appear
 *
 * Anything missing from the saved order is appended at the end rather than hidden. A switch added
 * next year must be reachable by somebody whose arrangement predates it, and the alternative — an
 * invisible feature — is the failure that stored orders cause everywhere else in this app if the
 * rule is forgotten.
 */
object MaSwitchboardOrder {

    /** Every switch the board can hold. The declaration order is the order it ships in. */
    enum class Entry(val id: String) {
        KEYS("keys"),
        NUMBER_ROW("number_row"),
        FEATURE_ROW("feature_row"),
        SUGGESTIONS("suggestions"),
        // The copy row on the typing keyboard. Its id stays "edit_row" because that is what is
        // already stored in his arrangement, and an id is a name in a file, not a label.
        //
        // COPY_KEYBOARD and COPY_DICTATION are gone: one switched a second, appended copy of
        // this same row, the other switched nothing at all. `parse` drops ids it does not know,
        // so an arrangement written before this loses them and keeps everything else.
        EDIT_ROW("edit_row"),
        // BUCKETS is gone: the C keys on the row are the switch. parse drops the stored id.
        MAGIC_ROW("magic_row"),
        SUBTITLE("subtitle"),
        FULLSCREEN("fullscreen"),
    }

    /**
     * Reads a stored order, and repairs it.
     *
     * Unknown ids are dropped and missing ones appended, so the result always holds every entry
     * exactly once whatever was stored. That is what makes it safe to write this preference from a
     * drag without validating anything.
     */
    fun parse(raw: String): List<Entry> {
        val byId = Entry.entries.associateBy { it.id }
        val kept = raw.split(',')
            .mapNotNull { byId[it.trim()] }
            .distinct()
        return kept + Entry.entries.filter { it !in kept }
    }

    fun serialize(order: List<Entry>): String = order.joinToString(",") { it.id }
}
