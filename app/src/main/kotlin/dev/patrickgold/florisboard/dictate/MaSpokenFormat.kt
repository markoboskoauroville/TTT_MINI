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

import dev.patrickgold.florisboard.app.FlorisPreferenceStore
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
     * Underscore: every space in the whole dictation becomes an underscore.
     *
     * The odd one out, and deliberately so. Every other command reaches back one word or one
     * sentence; this one takes the lot, because it is for naming a file and a file name is the whole
     * line or nothing. Said at either end — he dictates it first when he knows in advance and last
     * when he decides after.
     */
    private val UNDERSCORE = setOf("underscore", "underscores", "podvlaka", "donjacrta")

    /**
     * The rest of the marks, each wrapping the word before it or ending the sentence.
     *
     * The full stop family above ends a sentence; these SURROUND or ATTACH, which is a different
     * shape and why they are a separate table rather than more entries in the same one.
     */
    private val WRAPPERS = mapOf(
        "quote" to ("\"" to "\""),
        "quotes" to ("\"" to "\""),
        "navodnici" to ("\"" to "\""),
        "singlequote" to ("'" to "'"),
        "squarebracket" to ("[" to "]"),
        "squarebrackets" to ("[" to "]"),
        "uglatazagrada" to ("[" to "]"),
        "curlybracket" to ("{" to "}"),
        "curlybrackets" to ("{" to "}"),
        "vitičastazagrada" to ("{" to "}"),
        "anglebracket" to ("<" to ">"),
        "anglebrackets" to ("<" to ">"),
        "backtick" to ("`" to "`"),
        "backticks" to ("`" to "`"),
        "asterisks" to ("*" to "*"),
        "star" to ("*" to "*"),
    )

    /** Marks that simply follow the text, with no space before them. */
    private val TRAILERS = mapOf(
        "comma" to ",",
        "zarez" to ",",
        "colon" to ":",
        "dvotočka" to ":",
        "dvotocka" to ":",
        "semicolon" to ";",
        "točkazarez" to ";",
        "hash" to "#",
        "hashtag" to "#",
        "ljestve" to "#",
        "at" to "@",
        "atsign" to "@",
        "manki" to "@",
        "percent" to "%",
        "posto" to "%",
        "ampersand" to "&",
        "plus" to "+",
        "minus" to "-",
        "dash" to "-",
        "hyphen" to "-",
        "crtica" to "-",
        "slash" to "/",
        "kosacrta" to "/",
        "backslash" to "\\\\",
        "pipe" to "|",
        "equals" to "=",
        "jednako" to "=",
        "tilde" to "~",
        "caret" to "^",
        "dollar" to "$",
        "euro" to "\u20AC",
        "ellipsis" to "\u2026",
        "dots" to "\u2026",
    )

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
    /** Marks he has switched off, read fresh so a tick takes effect on the next dictation. */
    private fun disabled(): Set<String> {
        val prefs by FlorisPreferenceStore
        return prefs.dictate.maVoiceFormatOff.get()
            .split(',').map { it.trim() }.filter { it.isNotEmpty() }.toSet()
    }

    fun apply(text: String): String {
        val off = disabled()
        var out = text
        // Bounded rather than `while (true)`. Each pass must remove a word, so it cannot spin — but
        // a bound means a future command that forgets to consume its word degrades to doing nothing
        // instead of hanging the keyboard on a dictation.
        repeat(MAX_COMMANDS) {
            val next = applyOnce(out, off) ?: return out
            out = next
        }
        return out
    }

    private const val MAX_COMMANDS = 8

    private fun applyOnce(text: String, off: Set<String>): String? {
        // Work on the LAST LINE only, and put the rest back untouched.
        //
        // Splitting the whole text on whitespace and rejoining it with spaces flattened every
        // newline in the dictation — so a command used on more than one line silently destroyed
        // the line breaks, which is worse than the command not working at all.
        //
        // The last line is also the only line a command can be on, since a command counts only as
        // the final word. So this is not a compromise: it is the correct scope, arrived at late.
        val cut = text.trimEnd().lastIndexOf('\n')
        val head = if (cut >= 0) text.substring(0, cut + 1) else ""
        val tail = if (cut >= 0) text.substring(cut + 1) else text

        val trimmed = tail.trimEnd()
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
        // A mark he has switched off is not a command at all, so the word stays in his sentence.
        if (command in off) return null
        // The words the command acts on: the last line minus the command itself. Named `body` so it
        // cannot be confused with `head`, which is the earlier lines this must not touch.
        val body = words.dropLast(consumed)
        if (body.isEmpty()) return null

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
            // Any punctuation an earlier pass may have added, not only sentence enders. A comma
            // placed by "comma" is exactly as much his as a full stop placed by "dot", and was
            // being dropped by the next command in the stack.
            .takeIf { it.isNotEmpty() && it.all { c -> c in ".!?,;:" } }
            .orEmpty()

        // `head` is the untouched earlier lines; `body` is what the command rewrites.
        return head + when (command) {
            in PARENTHESIS -> wrapLastWord(body, "(", ")") + suffix
            in UPPERCASE -> upperLastSentence(body) + suffix
            // Underscore preserves the suffix too. It transforms rather than ends, so a stop
            // already placed belongs to the text, not to the spaces being replaced.
            in UNDERSCORE -> body.joinToString("_") + suffix
            in WRAPPERS -> {
                val pair = WRAPPERS.getValue(command)
                wrapLastWord(body, pair.first, pair.second) + suffix
            }
            in TRAILERS -> body.joinToString(" ").trimEnd() + TRAILERS.getValue(command) + suffix
            in DOT -> endSentence(body, '.')
            in QUESTION -> endSentence(body, '?')
            in EXCLAMATION -> endSentence(body, '!')
            else -> null
        }
    }

    /** "the word" -> "the (word)". Punctuation the word carried stays outside the bracket. */
    private fun wrapLastWord(words: List<String>, open: String, close: String): String {
        val target = words.last()
        val core = target.trim(*TRIM.toCharArray())
        if (core.isEmpty()) return words.joinToString(" ")
        val tail = target.substring(target.indexOf(core) + core.length)
        return (words.dropLast(1) + "$open$core$close$tail").joinToString(" ")
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
