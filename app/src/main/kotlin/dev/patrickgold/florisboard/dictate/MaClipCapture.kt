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
 * So the slots are buckets. A copy goes into the lowest empty bucket and stays in it. Pasting from
 * a bucket pours it out and leaves it empty, and the next copy fills the lowest empty bucket again —
 * which may well be the one just emptied. When every bucket is full, capturing stops until one is
 * poured out or the row is cleared.
 *
 * The consequence worth stating: a bucket never changes contents while it is holding something. C4
 * is whatever went into C4, until C4 is used. Nothing slides.
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

    /** Ten buckets, matching the ten keys. Beyond the last key a copy would be unreachable. */
    const val CAPACITY = MaRows.CLIP_SLOTS

    /** Ten buckets, all empty. */
    val Empty: List<String?> = List(CAPACITY) { null }

    /**
     * Pours [text] into the lowest empty bucket, or returns the buckets unchanged.
     *
     * Lowest rather than next-after-the-last: the buckets are numbered and pressed from memory, so
     * filling the first free one keeps the used range contiguous and makes C1 the thing to reach for
     * after a clear, every time. Filling after the highest used bucket would scatter the contents
     * into gaps nobody can see on a row of numbered keys.
     *
     * Unchanged in three cases. Full: capturing stops rather than pushing anything out, because a
     * row that rewrites itself is exactly what this replaced. Blank: whitespace would burn a bucket.
     * Already held: people re-copy the same text constantly to be sure, and that must not fill the
     * row with one repeated value.
     */
    fun capture(slots: List<String?>, text: String): List<String?> {
        if (text.isBlank()) return slots
        if (slots.any { it == text }) return slots
        val free = slots.indexOfFirst { it == null }
        if (free < 0) return slots
        return slots.toMutableList().also { it[free] = text }
    }

    /** What C[slot] pastes, 1-based, or null when that bucket is empty. */
    fun at(slots: List<String?>, slot: Int): String? =
        if (slot < 1 || slot > CAPACITY) null else slots.getOrNull(slot - 1)

    /**
     * Empties one bucket, after its contents have been pasted.
     *
     * Marko's rule and the reason the whole thing is called a bucket: text is carried from one place
     * to another, and once it has been poured out the bucket is free rather than still holding a
     * copy of what was moved. It also means the row empties as it is used, so the keys still
     * showing a number are exactly the ones still holding something.
     */
    fun pour(slots: List<String?>, slot: Int): List<String?> {
        if (slot < 1 || slot > CAPACITY) return slots
        if (slots.getOrNull(slot - 1) == null) return slots
        return slots.toMutableList().also { it[slot - 1] = null }
    }

    /** Which bucket holds [text], or null. Used to label the history panel. */
    fun slotFor(slots: List<String?>, text: String): Int? =
        slots.indexOfFirst { it != null && it == text }.takeIf { it >= 0 }?.plus(1)

    fun isFull(slots: List<String?>): Boolean = slots.all { it != null }

    /** How many buckets are holding something, for the trash key's dimmed state. */
    fun filledCount(slots: List<String?>): Int = slots.count { it != null }

    /**
     * Serialised with a control character, so captured text may contain commas, quotes, braces and
     * newlines and come back exactly as it was. This text is going to be pasted into somebody's
     * document; it has to survive the round trip character for character.
     *
     * Every bucket is written, empty ones included, because position is the whole meaning here. A
     * format that dropped empties would shuffle C7 into C3 the moment an earlier bucket was poured
     * out, which is the sliding this design exists to prevent.
     */
    fun serialize(slots: List<String?>): String =
        (0 until CAPACITY).joinToString(SEP.toString()) { slots.getOrNull(it) ?: "" }

    fun parse(raw: String): List<String?> {
        if (raw.isBlank()) return Empty
        val parts = raw.split(SEP)
        return (0 until CAPACITY).map { parts.getOrNull(it)?.takeIf { p -> p.isNotEmpty() } }
    }

    /**
     * What a C key says out loud.
     *
     * The key itself shows only its number: ten text previews across one row would be a few
     * characters wide each and unreadable. Spoken, it is the only way to answer "what is in C4"
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
