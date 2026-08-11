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
 * What C1 to C10 hold: copies caught in the order they were made, and then held still.
 *
 * ### Why this exists instead of reading the clipboard history
 *
 * The keys used to be a window onto the history, sorted newest first. That meant every key changed
 * meaning on every copy: what was on C1 slid to C2, then C3, and a key pressed from memory pasted
 * whatever had since moved under it. For a row navigated by number and pressed without looking, that
 * is the worst possible behaviour.
 *
 * So the slots fill instead. The first copy after a reset takes C1 and stays there. The second takes
 * C2. When all ten are full, capturing stops and nothing moves again until the row is cleared. C4 is
 * the fourth thing copied for as long as it takes to use it, which is the whole point.
 *
 * ### Why the text is stored, not a reference to the history entry
 *
 * A history row can be trimmed, deleted or unpinned out from under a slot, and a slot pointing at a
 * vanished row is a key that silently stops working. The text is what gets pasted, so the text is
 * what is kept.
 *
 * The cost is that only text is captured. An image copied while slots are free is skipped rather
 * than taking one, because a C key that pastes nothing would occupy a slot that a usable copy could
 * have had. Images are still in the history panel, reachable by a long press on any C key.
 */
object MaClipCapture {

    private const val SEP = '\u001E'

    /** Ten, matching the ten keys. Filling beyond the last key would capture what nothing can paste. */
    const val CAPACITY = MaRows.CLIP_SLOTS

    /**
     * Adds [text] as the next free slot, or returns the list unchanged.
     *
     * Unchanged in three cases, each deliberate. Full: capturing stops rather than pushing the
     * oldest out, because a row that rewrites itself is the behaviour this replaced. Blank: a copy
     * of whitespace would burn a slot. Already held: copying the same thing twice is something
     * people do constantly — re-copying to be sure — and it must not consume two slots or, worse,
     * fill the row with one repeated value.
     */
    fun capture(slots: List<String>, text: String): List<String> = when {
        slots.size >= CAPACITY -> slots
        text.isBlank() -> slots
        slots.any { it == text } -> slots
        else -> slots + text
    }

    /** What C[slot] pastes, 1-based, or null when that slot has not been filled yet. */
    fun at(slots: List<String>, slot: Int): String? =
        if (slot < 1 || slot > CAPACITY) null else slots.getOrNull(slot - 1)

    /** Which key holds [text], or null. Used to label the history panel. */
    fun slotFor(slots: List<String>, text: String): Int? =
        slots.indexOfFirst { it == text }.takeIf { it >= 0 }?.plus(1)

    val Empty: List<String> = emptyList()

    fun isFull(slots: List<String>): Boolean = slots.size >= CAPACITY

    /**
     * Serialised with a control character, so a captured copy may contain commas, quotes, braces and
     * newlines and come back exactly as it was. This text is going to be pasted into somebody's
     * document; it has to survive the round trip character for character.
     */
    fun serialize(slots: List<String>): String = slots.joinToString(SEP.toString())

    fun parse(raw: String): List<String> =
        if (raw.isBlank()) emptyList() else raw.split(SEP).filter { it.isNotBlank() }.take(CAPACITY)

    /**
     * What a C key says out loud.
     *
     * The key itself shows only its number: ten text previews across one row would be a few
     * characters wide each and unreadable. Spoken, it is the only way to answer "what is on C4"
     * without pasting it somewhere to find out, which matters more here than it usually would.
     */
    fun describeSlot(text: String?, slot: Int): String = when {
        text == null -> "C$slot, empty"
        else -> {
            val short = text.replace('\n', ' ').trim().take(60)
            if (text.length > 60) "C$slot, $short\u2026" else "C$slot, $short"
        }
    }
}
