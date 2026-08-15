/*
 * Copyright (C) 2026 Marko Bosko, Mantra Productions
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package dev.patrickgold.florisboard.dictate.nlp

import android.content.Context

/**
 * Fifty thousand Croatian words, shipped, so the keyboard does not start knowing none.
 *
 * ### The gap this fills
 *
 * [MaNgram] learns from what Marko writes, and it is very good at that — it knows his place names,
 * his project names, the words of his trade. But it can only ever suggest a word it has already
 * seen him write, which means a fresh install suggests nothing and Croatian stays thin for months,
 * because every word has to be typed in full before it can ever be offered.
 *
 * English never had this problem: a proper English dictionary came with FlorisBoard upstream.
 * Croatian had nothing. So this is not a smarter predictor, it is the missing half of the one that
 * already works — a floor under it, in the language that had no floor.
 *
 * ### Why a word list and not a model
 *
 * The neural alternative is 81 MB, English-only, and heavy enough to be killed inside an input
 * method. This is 545 KB, Croatian, and costs a binary search. For the thing actually missing, the
 * word list is not the cheap approximation of the model; it is the correct answer, and the model
 * would not have solved it at all.
 *
 * ### Where the words come from
 *
 * A frequency list built from OpenSubtitles, filtered to the Croatian alphabet, with subtitle noise
 * removed (runs of three identical letters, and words that are one letter repeated). Fifty thousand
 * covers roughly 90% of running text; the next fifty thousand would add about three points and
 * double the file, which is the definition of the wrong trade.
 *
 * Source: hermitdave/FrequencyWords, from the OpenSubtitles corpus, CC BY-SA 4.0. See
 * `assets/dictate/hr_words_LICENSE.txt`.
 *
 * ### Memory, because this is an input method
 *
 * Android kills input methods that use what an app may use freely, and being killed reads as the
 * keyboard vanishing mid-sentence. So the file is held as **one string plus one offset array**
 * rather than fifty thousand String objects, which would cost several megabytes in object headers
 * alone. Lookup compares characters in place and allocates only the words it actually returns.
 *
 * It loads on first use and only when Croatian is the active language, so an English-only session
 * never pays for it at all.
 */
object MaBaseDictionary {

    private const val ASSET = "dictate/hr_words.txt"

    /**
     * The most matches examined for one prefix.
     *
     * A one-letter prefix can match a few thousand lines. Scanning those is a character comparison
     * each and costs well under a millisecond, but the cap exists so that a pathological input
     * cannot turn a keystroke into a scan of the whole file.
     */
    private const val MAX_SCAN = 4000

    @Volatile
    private var blob: String? = null

    /** Start offset of every line in [blob]. Its size is the word count. */
    @Volatile
    private var starts: IntArray? = null

    @Volatile
    private var failed = false

    /**
     * Loads the asset once. Returns false when there is nothing to search.
     *
     * A failure here is not an error worth showing: the keyboard keeps working with the personal
     * model alone, which is exactly how it behaved before this existed.
     */
    private fun ensureLoaded(context: Context): Boolean {
        if (failed) return false
        blob?.let { return true }
        synchronized(this) {
            blob?.let { return true }
            if (failed) return false
            return try {
                val text = context.assets.open(ASSET).bufferedReader().use { it.readText() }
                val offsets = ArrayList<Int>(52_000)
                var i = 0
                while (i < text.length) {
                    offsets.add(i)
                    val nl = text.indexOf('\n', i)
                    if (nl < 0) break
                    i = nl + 1
                }
                starts = offsets.toIntArray()
                blob = text
                true
            } catch (t: Throwable) {
                failed = true
                false
            }
        }
    }

    /** Compares the word on the line beginning at [from] against [prefix], as far as the prefix goes. */
    private fun compareWord(text: String, from: Int, prefix: String): Int {
        var i = from
        var j = 0
        while (j < prefix.length) {
            val c = if (i < text.length) text[i] else '\u0000'
            // The word ends at its separator, so a shorter word sorts before a longer prefix.
            if (c == ' ' || c == '\n' || c == '\u0000') return -1
            val d = c.compareTo(prefix[j])
            if (d != 0) return d
            i++
            j++
        }
        return 0
    }

    /**
     * Croatian words beginning with [prefix], best first, excluding anything in [exclude].
     *
     * Returns at most [limit]. Case is folded to lower, which is what the list holds; the caller
     * decides how a suggestion is capitalised, exactly as it does for the personal model.
     */
    fun suggest(
        context: Context,
        prefix: String,
        limit: Int,
        exclude: Set<String>,
    ): List<MaNgramSuggestion> {
        if (limit <= 0 || prefix.isBlank()) return emptyList()
        if (!ensureLoaded(context)) return emptyList()
        val text = blob ?: return emptyList()
        val offsets = starts ?: return emptyList()
        val needle = prefix.lowercase()

        // First line whose word is not less than the prefix.
        var lo = 0
        var hi = offsets.size
        while (lo < hi) {
            val mid = (lo + hi) ushr 1
            if (compareWord(text, offsets[mid], needle) < 0) lo = mid + 1 else hi = mid
        }

        // Walk forward while the prefix still matches, keeping the best few by score. A short
        // insertion into a list of three beats sorting a few thousand matches we would throw away.
        val bestWords = ArrayList<String>(limit)
        val bestScores = ArrayList<Int>(limit)
        var scanned = 0
        var idx = lo
        while (idx < offsets.size && scanned < MAX_SCAN) {
            val start = offsets[idx]
            if (compareWord(text, start, needle) != 0) break
            scanned++
            idx++
            val space = text.indexOf(' ', start)
            if (space < 0) continue
            val word = text.substring(start, space)
            if (word == needle || word in exclude) continue
            var end = text.indexOf('\n', space + 1)
            if (end < 0) end = text.length
            val score = text.substring(space + 1, end).trim().toIntOrNull() ?: continue
            var at = bestScores.size
            while (at > 0 && bestScores[at - 1] < score) at--
            if (at < limit) {
                bestWords.add(at, word)
                bestScores.add(at, score)
                if (bestWords.size > limit) {
                    bestWords.removeAt(bestWords.size - 1)
                    bestScores.removeAt(bestScores.size - 1)
                }
            }
        }

        return bestWords.mapIndexed { i, w ->
            MaNgramSuggestion(word = w, count = bestScores[i], tier = TIER_BASE)
        }
    }

    /**
     * Below every personal tier, deliberately.
     *
     * A word he has actually written outranks a word the language merely contains, always. The
     * shipped list is a floor, not a competitor: it should be invisible on any phrase he has typed
     * before and should only speak where his own model has nothing to say.
     */
    const val TIER_BASE = 0
}
