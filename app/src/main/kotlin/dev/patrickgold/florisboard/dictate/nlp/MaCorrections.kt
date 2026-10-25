/*
 * Copyright (C) 2026 Marko Boško, Mantra Productions
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package dev.patrickgold.florisboard.dictate.nlp

import kotlin.collections.HashMap
import kotlin.jvm.Synchronized
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/**
 * The keyboard learns from his corrections (Marko, 22.10.2026: "if I type something and correct it,
 * that means this application needs to understand that it was wrong typed and I'm correcting. It
 * needs to understand that and write correction in the prediction algorithm").
 *
 * ### Where it comes from
 *
 * He asked for the prediction of the most popular open-source keyboard. Measured on GitHub on
 * 22.10.2026: FlorisBoard is the most starred (8.7k, and this app is built on it, but its prediction is
 * the weak part — the reason MaNgram exists), AnySoftKeyboard the most forked (951), HeliBoard the
 * strongest learner (6.4k). HeliBoard is GPL-3.0 and cannot be copied into this Apache-2.0 app; its
 * learning is Google's AOSP LatinIME's (Apache-2.0), and the two ideas taken from there are:
 *
 *  - **unlearning what was corrected**: LatinIME removes a word from the user history when the user
 *    reverts it, so a mistake typed once is not offered back for ever;
 *  - **the forgetting curve** (LatinIME's ForgettingCurveUtils): a word's weight falls with the time
 *    since it was last written, so what he writes now outranks what he wrote months ago. Here as a
 *    half-life of [MaRecency.HALF_LIFE_DAYS] days with a floor, so a rare word of his never vanishes —
 *    those are the words this model exists for.
 *
 * ### How a correction is seen: [MaCorrectionWatcher]
 *
 * Not from keystrokes — taps, the delete key, a word deleted at once and dictation all reach the field
 * differently — but from the text before the cursor, which every path changes. A word is finished
 * (followed by a space or a mark); the text is cut back into that word; a word is finished again in
 * the same place, preceded by the same text. If the two words differ and are close (one or two letters
 * apart, by Damerau–Levenshtein, so `teh` → `the` counts as one), the first was a typo of the second.
 * A word merely continued (`the` → `there`) or replaced by a different word (`cat` → `dog`) is not a
 * correction and teaches nothing.
 *
 * ### What it does with one: [MaCorrectionStore]
 *
 * The pair is counted, the typo is taken out of the counts, and the next time he types the typo the
 * correction is the first suggestion. Kept per language, like the n-gram models.
 *
 * Pure: strings in, pairs out. Run with kotlinc against scripts/test_corrections.kts' cases.
 */
class MaCorrectionWatcher {

    data class Correction(val typo: String, val fixed: String)

    /** The last finished word, the text that came before it, and whether he is cutting back into it. */
    private var word: String? = null
    private var before: String = ""
    private var editing = false

    /** Feed it the text before the cursor after every change; returns a correction when one is seen. */
    fun observe(textBefore: String): Correction? {
        val last = word
        if (last != null && editing) {
            val finished = finishedWord(textBefore)
            if (finished != null && sameBefore(finished.second, before)) {
                val fixed = finished.first
                val result = if (isCorrection(last, fixed)) Correction(last.lowercase(), fixed.lowercase()) else null
                remember(fixed, finished.second)
                return result
            }
            // Still in that place, retyping: wait. Anywhere else (cut back past it, moved away): forget.
            val (_, rest) = trailingWord(textBefore)
            if (!sameBefore(rest, before)) { word = null; editing = false }
            return null
        }
        if (last != null) {
            // Cut back into the finished word: its space gone, perhaps some of its letters.
            val (typed, rest) = trailingWord(textBefore)
            if (sameBefore(rest, before) && last.startsWith(typed)) {
                editing = true
                return null
            }
        }
        finishedWord(textBefore)?.let { remember(it.first, it.second) }
        return null
    }

    private fun remember(w: String, textBeforeWord: String) {
        word = w
        before = textBeforeWord.takeLast(CONTEXT)
        editing = false
    }

    /** The text before a word matches the remembered text before the finished word. */
    private fun sameBefore(textBeforeWord: String, remembered: String): Boolean =
        if (remembered.length < CONTEXT) textBeforeWord == remembered else textBeforeWord.endsWith(remembered)

    companion object {
        /** How much of the text before a word is compared; enough to know it is the same place. */
        const val CONTEXT = 24

        fun isWordChar(c: Char): Boolean = c.isLetterOrDigit() || c == '\'' || c == '’' || c == '-'

        /** The letters at the very end (possibly none) and the text before them. */
        fun trailingWord(text: String): Pair<String, String> {
            var start = text.length
            while (start > 0 && isWordChar(text[start - 1])) start--
            return text.substring(start) to text.substring(0, start)
        }

        /** The word just finished — a word followed by one or more marks — and the text before it. */
        fun finishedWord(text: String): Pair<String, String>? {
            if (text.isEmpty() || isWordChar(text.last())) return null
            var end = text.length
            while (end > 0 && !isWordChar(text[end - 1])) end--
            if (end == 0) return null
            // Only the space or mark just typed: two words back is not "just finished".
            if (text.length - end > 2) return null
            var start = end
            while (start > 0 && isWordChar(text[start - 1])) start--
            return text.substring(start, end) to text.substring(0, start)
        }

        /** Close enough to be a typo of it, and not merely the same word continued or another word. */
        fun isCorrection(typo: String, fixed: String): Boolean {
            val a = typo.lowercase()
            val b = fixed.lowercase()
            if (a == b || a.isEmpty() || b.isEmpty()) return false
            if (b.startsWith(a) || a.startsWith(b)) return false
            if (a.length < 2 || b.length < 2) return false
            val allowed = when {
                max(a.length, b.length) <= 4 -> 1
                max(a.length, b.length) <= 8 -> 2
                else -> 3
            }
            return damerau(a, b) <= allowed
        }

        /** Damerau–Levenshtein (optimal string alignment): a swap of two neighbours is one edit. */
        fun damerau(a: String, b: String): Int {
            val d = Array(a.length + 1) { IntArray(b.length + 1) }
            for (i in 0..a.length) d[i][0] = i
            for (j in 0..b.length) d[0][j] = j
            for (i in 1..a.length) for (j in 1..b.length) {
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                d[i][j] = min(min(d[i - 1][j] + 1, d[i][j - 1] + 1), d[i - 1][j - 1] + cost)
                if (i > 1 && j > 1 && a[i - 1] == b[j - 2] && a[i - 2] == b[j - 1]) d[i][j] = min(d[i][j], d[i - 2][j - 2] + 1)
            }
            return d[a.length][b.length]
        }
    }
}

/** His corrections: typo → what he meant → how often. Stored as `typo\tfixed\tcount` lines. */
class MaCorrectionStore {
    private val pairs = HashMap<String, HashMap<String, Int>>()
    var isDirty = false
        private set

    @Synchronized fun record(typo: String, fixed: String) {
        val row = pairs.getOrPut(typo.lowercase()) { HashMap() }
        row[fixed.lowercase()] = (row[fixed.lowercase()] ?: 0) + 1
        isDirty = true
    }

    /** What he meant by [typed], if he has corrected it before; the most frequent correction wins. */
    @Synchronized fun fixFor(typed: String): String? =
        pairs[typed.lowercase()]?.maxByOrNull { it.value }?.key

    /** Was [word] ever corrected away? Such a word is not offered as a suggestion. */
    @Synchronized fun isTypo(word: String): Boolean = pairs.containsKey(word.lowercase())

    @Synchronized fun size(): Int = pairs.size

    @Synchronized fun clear() { pairs.clear(); isDirty = true }

    @Synchronized fun serialize(): String {
        isDirty = false
        return pairs.entries.sortedBy { it.key }.flatMap { (typo, row) ->
            row.entries.sortedBy { it.key }.map { (fixed, n) -> "$typo\t$fixed\t$n" }
        }.joinToString("\n")
    }

    @Synchronized fun parse(text: String) {
        pairs.clear()
        text.split('\n').forEach { line ->
            val p = line.split('\t')
            if (p.size != 3 || p[0].isBlank() || p[1].isBlank()) return@forEach
            val n = p[2].toIntOrNull() ?: return@forEach
            pairs.getOrPut(p[0]) { HashMap() }[p[1]] = n
        }
        isDirty = false
    }

    /** [fixed] with the case he is typing in: `Teh` → `The`. */
    companion object {
        fun matchCase(typed: String, fixed: String): String = when {
            typed.length > 1 && typed.all { !it.isLetter() || it.isUpperCase() } -> fixed.uppercase()
            typed.firstOrNull()?.isUpperCase() == true -> fixed.replaceFirstChar { it.uppercaseChar() }
            else -> fixed
        }
    }
}

/**
 * When each word was last written, in days, for the forgetting curve: a word's count is weighed by
 * 0.5^(days since / [HALF_LIFE_DAYS]), never below [FLOOR]. Stored as `word\tday` lines.
 */
class MaRecency {
    private val lastDay = HashMap<String, Int>()
    var isDirty = false
        private set

    @Synchronized fun touch(word: String, today: Int) {
        lastDay[word.lowercase()] = today
        isDirty = true
    }

    @Synchronized fun weight(word: String, today: Int): Double {
        val day = lastDay[word.lowercase()] ?: return FLOOR
        return max(FLOOR, 0.5.pow((today - day).coerceAtLeast(0) / HALF_LIFE_DAYS))
    }

    @Synchronized fun clear() { lastDay.clear(); isDirty = true }

    @Synchronized fun serialize(): String {
        isDirty = false
        return lastDay.entries.joinToString("\n") { "${it.key}\t${it.value}" }
    }

    @Synchronized fun parse(text: String) {
        lastDay.clear()
        text.split('\n').forEach { line ->
            val p = line.split('\t')
            if (p.size == 2) p[1].toIntOrNull()?.let { lastDay[p[0]] = it }
        }
        isDirty = false
    }

    companion object {
        const val HALF_LIFE_DAYS = 30.0
        const val FLOOR = 0.15

        fun today(nowMillis: Long): Int = (nowMillis / 86_400_000L).toInt()
    }
}
