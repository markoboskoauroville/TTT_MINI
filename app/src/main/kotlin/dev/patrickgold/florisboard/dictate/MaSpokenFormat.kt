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

import java.util.Locale

/**
 * Words that format the text instead of joining it.
 *
 * Say "the word parenthesis" and you get "the (word)". Say "hello dot" and you get "hello."
 *
 * ### Only at the end, and only one word
 *
 * A command is recognised **only as the last word of the dictation**, and it is removed. That is the
 * same rule the voice commands use (§26), for the same reason: a rule that fired anywhere would eat
 * the word "dot" out of the middle of a sentence about a dot, and dictated words that get eaten
 * cannot be undone back to, because they were spoken rather than typed.
 *
 * Several can be stacked — "hello world uppercase dot" applies uppercase, then the full stop — since
 * after removing one the next is now the last word. That falls out of the rule rather than being a
 * feature bolted on.
 *
 * ### Both languages
 *
 * He dictates in Croatian and English and switches mid-thought, so both are recognised at once. A
 * command in the wrong language is still a command; there is no state to get wrong.
 *
 * ### What each does
 *
 * - **parenthesis / zagrada** wraps the word before it in round brackets.
 * - **uppercase / velika slova** raises the whole preceding **sentence**. He said "everything, what
 *   I just said", which is more than one word — so it takes back to the last sentence ending.
 * - **dot / točka**, **question mark / upitnik**, **exclamation mark / uskličnik** end the sentence,
 *   replacing whatever punctuation the transcriber already guessed at.
 */
object MaSpokenFormat {

    /** Trailing marks the transcriber adds. Stripped before a word is read as a command. */
    private const val TRIM = " \t\n.,!?;:\"'\u2019\u201C\u201D"

    private val PARENTHESIS = setOf("parenthesis", "parentheses", "bracket", "brackets", "zagrada", "zagrade")
    private val UPPERCASE = setOf("uppercase", "caps", "capitals")
    private val DOT = setOf("dot", "period", "fullstop", "točka", "tocka")
    private val QUESTION = setOf("questionmark", "upitnik")
    private val EXCLAMATION = setOf("exclamationmark", "uskličnik", "usklicnik")

    /**
     * Two-word commands, joined before matching.
     *
     * "question mark" is two words spoken and one command meant. They are folded into the single
     * tokens above so the whole thing stays one rule rather than two with different shapes.
     */
    private val PAIRS = mapOf(
        "question mark" to "questionmark",
        "exclamation mark" to "exclamationmark",
        "full stop" to "fullstop",
        "velika slova" to "uppercase",
        "veliko slovo" to "uppercase",
    )

    /**
     * Applies every trailing command, innermost last, and returns the text to write.
     *
     * Never throws and never returns null: text with no command in it comes back untouched, which is
     * the overwhelmingly common case and must cost nothing.
     */
    fun apply(text: String): String {
        var out = text
        // Bounded rather than `while (true)`. Each pass must remove a word, so it cannot spin — but
        // a bound means a future command that forgets to consume its word degrades to doing nothing
        // instead of hanging the keyboard on a dictation.
        repeat(MAX_COMMANDS) {
            val next = applyOnce(out) ?: return out
            out = next
        }
        return out
    }

    private const val MAX_COMMANDS = 8

    private fun applyOnce(text: String): String? {
        val trimmed = text.trimEnd()
        if (trimmed.isEmpty()) return null
        val words = trimmed.split(Regex("\\s+"))
        if (words.size < 2) return null

        // The last word, and the last two joined, both cleaned of punctuation the transcriber added.
        val last = words.last().trim(*TRIM.toCharArray()).lowercase(Locale.ROOT)
        val lastTwo = (words[words.size - 2].trim(*TRIM.toCharArray()) + " " + last)
            .lowercase(Locale.ROOT)

        // A two-word command is checked first: "question mark" must not be read as the word "mark".
        val pairHit = PAIRS[lastTwo]
        val command = pairHit ?: last
        val consumed = if (pairHit != null) 2 else 1
        val head = words.dropLast(consumed)
        if (head.isEmpty()) return null

        // Punctuation stuck to the command word, which may be a full stop THIS class put there on an
        // earlier pass.
        //
        // "hello world uppercase dot" is processed from the end: the dot ends the sentence, leaving
        // "hello world uppercase.", and then uppercase runs — and without this the stop it had just
        // added was dropped along with the word it was attached to. Stacking silently lost the
        // punctuation it had been asked for.
        //
        // Transforming commands keep it, because they say nothing about how the sentence ends.
        // Ending commands discard it, because deciding how the sentence ends is the whole of what
        // they do, and re-appending would give "Hello world..".
        val rawLast = words.last()
        val suffix = rawLast.dropWhile { it !in TRIM || it == ' ' }
            .takeIf { it.isNotEmpty() && it.all { c -> c in ".!?" } }
            .orEmpty()

        return when (command) {
            in PARENTHESIS -> wrapLastWord(head) + suffix
            in UPPERCASE -> upperLastSentence(head) + suffix
            in DOT -> endSentence(head, '.')
            in QUESTION -> endSentence(head, '?')
            in EXCLAMATION -> endSentence(head, '!')
            else -> null
        }
    }

    /** "the word" -> "the (word)". Punctuation the word carried stays outside the bracket. */
    private fun wrapLastWord(words: List<String>): String {
        val target = words.last()
        val core = target.trim(*TRIM.toCharArray())
        if (core.isEmpty()) return words.joinToString(" ")
        val tail = target.substring(target.indexOf(core) + core.length)
        return (words.dropLast(1) + "($core)$tail").joinToString(" ")
    }

    /**
     * Raises the last sentence, not the last word.
     *
     * Back to the previous sentence ending, or the start of the text when there is none. The
     * punctuation ending the previous sentence is left alone — it belongs to that sentence.
     */
    private fun upperLastSentence(words: List<String>): String {
        val joined = words.joinToString(" ")
        val cut = joined.indexOfLast { it == '.' || it == '!' || it == '?' || it == '\n' }
        val head = if (cut >= 0) joined.substring(0, cut + 1) else ""
        val tail = if (cut >= 0) joined.substring(cut + 1) else joined
        return head + tail.uppercase(Locale.ROOT)
    }

    /** Ends the sentence with [mark], replacing whatever the transcriber guessed. */
    private fun endSentence(words: List<String>, mark: Char): String {
        val joined = words.joinToString(" ").trimEnd()
        val stripped = joined.trimEnd('.', '!', '?', ',', ';', ':', ' ')
        return if (stripped.isEmpty()) joined else stripped + mark
    }
}
