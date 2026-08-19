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
 * Turning a screen full of nodes into something worth listening to.
 *
 * ### The hard part is not reading, it is refusing
 *
 * A screen carries dozens of strings and almost none of them are prose: button names, tab labels,
 * timestamps, "Claude is AI and can make mistakes", the word "Send". Read them all aloud and the
 * result is unlistenable, and the paragraph he actually wanted is buried three minutes in.
 *
 * So this file is mostly rules about what NOT to say. Each of them is one line here and the
 * difference between a feature and a noise machine.
 *
 * ### Reading order comes from the screen, not the tree
 *
 * The accessibility tree is a layout hierarchy: nesting order, not reading order. It is usually
 * top-to-bottom because that is how layouts are built, and it is wrong exactly when a screen has
 * columns or an overlay. Sorting by the top edge, then the left, turns it back into the order a
 * person's eye takes.
 */
object MaScreenText {

    /**
     * Shorter than this and it is a label, not a sentence.
     *
     * Twelve characters. "Send", "New chat", "3 steps", "Opus 5" and every timestamp fall under it;
     * a real sentence almost never does. It is a blunt rule and it is the single most effective one
     * — nearly all the noise on a screen is short.
     */
    private const val MIN_LENGTH = 12

    /**
     * Chrome that is long enough to pass the length test but is still not his text.
     *
     * Matched case-insensitively as a whole-string prefix, so a paragraph that happens to mention
     * one of these is unaffected. Kept short on purpose: a long list here becomes a second thing to
     * maintain, and the length rule already does most of the work.
     */
    private val CHROME = listOf(
        "claude is ai and can make mistakes",
        "please double-check responses",
        "gemini is ai and can make mistakes",
        "reply to claude",
        "message claude",
        "ask anything",
        "start speech input",
    )

    /** One piece of text on screen, with where it sits. */
    data class Line(val text: String, val top: Int, val left: Int)

    /**
     * Whether a string is worth speaking.
     *
     * Pure, so the rule can be tested without a screen — which matters, because this is the rule
     * that decides whether the feature is pleasant or intolerable.
     */
    fun isWorthReading(text: String?, clickable: Boolean = false): Boolean {
        val t = text?.trim().orEmpty()
        // A pressable thing is a control, whatever its length.
        //
        // The length rule catches almost all chrome and cannot catch "Copy message" or "More
        // options" — twelve characters of pure interface that read exactly like prose to a counter.
        // But nobody writes a paragraph inside a button, so the question "can this be pressed"
        // separates them perfectly and needs no list to maintain.
        //
        // Checked before the length, because it is the cheaper test and the more certain one.
        if (clickable) return false
        if (t.length < MIN_LENGTH) return false
        // Nothing but digits, punctuation and separators: a timestamp, a counter, a file size.
        if (t.none { it.isLetter() }) return false
        val lower = t.lowercase()
        if (CHROME.any { lower.startsWith(it) }) return false
        return true
    }

    /**
     * Orders what was collected the way the eye reads it, and joins it into one passage.
     *
     * Sorted by top edge then left, because the tree's own order is nesting rather than reading.
     * Duplicates are dropped: a string often appears twice, once on a container and once on the
     * text node inside it, and hearing every sentence twice is the fastest way to make a reader
     * useless.
     *
     * Joined with a full stop and a space where a line does not already end in punctuation, so the
     * voice pauses between paragraphs instead of running them together into one breath.
     */
    fun assemble(lines: List<Line>): String {
        val seen = HashSet<String>()
        return lines
            .sortedWith(compareBy({ it.top }, { it.left }))
            .map { it.text.trim() }
            .filter { seen.add(it) }
            .joinToString(" ") { line ->
                if (line.lastOrNull() in setOf('.', '!', '?', ':', ';', ',')) line else "$line."
            }
            .trim()
    }

    /**
     * How much is sent to be spoken in one press.
     *
     * Speechify bills by the character and a screen is normally a few hundred. The cap is here for
     * the screen that is not normal — a wall of text in a reader app — where one press would
     * otherwise become a long synthesis he did not ask for and cannot easily stop.
     */
    const val MAX_CHARS = 4000
}
