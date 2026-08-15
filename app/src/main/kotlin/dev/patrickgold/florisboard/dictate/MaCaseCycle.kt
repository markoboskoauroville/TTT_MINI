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
 * Aa: the four cases, in a ring.
 *
 * lower → UPPER → Sentence case → Title Case → lower.
 *
 * ### Why the next case is read from the text, not from a counter
 *
 * A remembered position would be wrong the moment anything else changed the text — a new dictation,
 * a different field, an edit by hand — and it would be wrong invisibly, so the key would appear to
 * skip a step for no reason anybody could see.
 *
 * Reading the text instead means the key is always honest: whatever is in front of him, one press
 * moves it one step along the ring. Text that matches none of the four — a name in the middle of a
 * sentence, say — starts the ring rather than being treated as a mistake.
 *
 * ### Why the transforms are not written here
 *
 * [MaCaseTransform] already has all four, locale-aware, with the awkward parts solved: title case
 * splits on spaces so `don't` does not become `Don'T`, and sentence case lowers the rest so
 * SHOUTED TEXT genuinely calms down instead of becoming Shouted text with the shout intact. Writing
 * a second set would mean two behaviours for one word.
 */
object MaCaseCycle {

    /** The ring, in the order the key walks it. */
    private val ORDER = listOf(
        MaCaseTransform.LOWER,
        MaCaseTransform.UPPER,
        MaCaseTransform.SENTENCE,
        MaCaseTransform.TITLE,
    )

    /**
     * The text one press further round the ring, or null when there is nothing to change.
     *
     * Null rather than the same string when the text has no letters at all, so the caller can leave
     * the field completely untouched: rewriting a field with identical content still moves the
     * cursor and still costs an undo step.
     */
    fun next(text: String): String? {
        if (text.isEmpty() || text.none { it.isLetter() }) return null
        // Which of the four it already is. The first match wins, and the order of the checks is the
        // order of the ring, so a string that is legitimately two of them at once — a single
        // lowercase word is both lower and, arguably, nothing else — advances predictably.
        val current = ORDER.indexOfFirst { name -> MaCaseTransform.apply(name, text) == text }
        // Unrecognised text starts the ring rather than being refused.
        val nextIndex = if (current < 0) 0 else (current + 1) % ORDER.size
        val out = MaCaseTransform.apply(ORDER[nextIndex], text) ?: return null
        // One more step if the transform changed nothing, so a press always visibly does something.
        // Two cases can produce identical text — a single capitalised word is both Sentence and
        // Title — and stopping there would look like a dead key.
        if (out == text) {
            val after = (nextIndex + 1) % ORDER.size
            return MaCaseTransform.apply(ORDER[after], text)?.takeIf { it != text }
        }
        return out
    }
}
