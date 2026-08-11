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

import dev.patrickgold.florisboard.ime.clipboard.ClipboardHistory
import dev.patrickgold.florisboard.ime.clipboard.provider.ClipboardItem

/**
 * The clipboard history, addressed by number, for the CH row.
 *
 * Key 1 is the last thing copied, key 2 the one before it, and so on down the row. That is the whole
 * mental model and it is worth keeping literally true, because a row of numbers invites counting and
 * anything that quietly disturbs the count makes every key wrong at once rather than one of them.
 *
 * ### Why the ordering is recomputed here rather than taken as it comes
 *
 * [ClipboardHistory] sorts pinned entries apart from unpinned ones, which is right for a history
 * panel somebody is reading and wrong for a row somebody is counting along. If a pinned item drifted
 * to the front, key 1 would stop meaning "the last thing I copied" without anything visibly
 * changing. So every entry is ranked by when it was created, newest first, pinned or not.
 */
object MaClipboardSlots {

    /** One key for each finger, which is also as many as fit on a row at a legible size. */
    const val SLOT_COUNT = 9

    /**
     * The number on a clipboard key's face, or 0 for anything that is not one.
     *
     * The mapping lives here beside the slot logic rather than in the renderer, so the enum entry
     * and the number it stands for cannot drift apart in the one place it would matter.
     */
    fun slotOf(key: MaFeatureKey): Int = when (key) {
        MaFeatureKey.CLIP_1 -> 1
        MaFeatureKey.CLIP_2 -> 2
        MaFeatureKey.CLIP_3 -> 3
        MaFeatureKey.CLIP_4 -> 4
        MaFeatureKey.CLIP_5 -> 5
        MaFeatureKey.CLIP_6 -> 6
        MaFeatureKey.CLIP_7 -> 7
        MaFeatureKey.CLIP_8 -> 8
        MaFeatureKey.CLIP_9 -> 9
        else -> 0
    }

    /**
     * The entry behind key [slot], where slot 1 is the most recent, or null when the history does
     * not reach that far back yet.
     */
    fun itemAt(history: ClipboardHistory, slot: Int): ClipboardItem? {
        if (slot < 1 || slot > SLOT_COUNT) return null
        return history.all
            .sortedByDescending { it.creationTimestampMs }
            .getOrNull(slot - 1)
    }

    /**
     * A short preview of what a key holds, for its accessibility label.
     *
     * Spoken rather than shown: the key itself shows only its number, because nine text previews on
     * one row would each be a few characters wide and unreadable. The label is what makes the row
     * usable without sight — it is the only thing that answers "what is on key 4" without pasting it
     * somewhere to find out.
     */
    fun describe(item: ClipboardItem?, slot: Int): String = when {
        item == null -> "Clipboard $slot, empty"
        else -> {
            val text = item.text?.trim().orEmpty()
            if (text.isBlank()) {
                "Clipboard $slot, an image"
            } else {
                val short = text.replace('\n', ' ').take(60)
                if (text.length > 60) "Clipboard $slot, $short\u2026" else "Clipboard $slot, $short"
            }
        }
    }
}
