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

import java.util.Locale

/**
 * HIS CORRECTIONS, LEARNED: WHAT WAS WRITTEN, WHAT HE MADE OF IT, AND THE SAME FIX DONE FOR HIM NEXT TIME.
 *
 * Marko (25.10.2026) asked whether the app tracks his corrections and moves towards them. It did not:
 * the personal model learned every word written, the wrong one included, and nothing knew that
 * "Amina" had become "Emina". This is that memory. Pure: strings in, decisions out, no Android, so
 * `MaCorrectionRulesTest` walks it on the desk.
 *
 * ### Finding a correction
 *
 * A dictation is remembered as it was written ([Pending]). Later — when the next dictation arrives
 * or the keyboard closes — the same stretch of the field is read again and the two are compared word
 * by word ([diff]). A small swap inside otherwise unchanged text (one to three words replaced by one
 * to three words) is a correction. A stretch more than half rewritten is a rewrite and teaches
 * nothing: he changed his mind, not the spelling.
 *
 * ### When a fix is used
 *
 * Once is a coincidence, so a fix seen once is only counted. The [ACTIVE_AT]th time the same fix is
 * made, it becomes a rule: applied to every new dictation in that language, and its right form goes
 * to the speech model's word list. Rules he typed in himself, or loaded from his list ([SEEDS]),
 * are rules at once. Every rule can be switched off or deleted on Settings → My corrections.
 */
object MaCorrectionRules {

    /** The number of times the same fix must be made before it is applied by itself. */
    const val ACTIVE_AT = 2

    /** The most words on either side of a swap that still count as a correction. */
    const val MAX_WORDS = 3

    /** Language of a rule that applies to everything he writes. */
    const val ANY = "*"

    enum class Source { LEARNED, MINE }

    data class Rule(
        val wrong: String,
        val right: String,
        val language: String,
        val count: Int,
        val enabled: Boolean,
        val source: Source,
    ) {
        /** Applied now: switched on, and either his own or made [ACTIVE_AT] times. */
        val isActive: Boolean get() = enabled && (source == Source.MINE || count >= ACTIVE_AT)

        fun key(): String = keyOf(wrong, language)
    }

    fun keyOf(wrong: String, language: String) = language + "\u0000" + wrong.lowercase(Locale.ROOT)

    /**
     * His own list (the "ALWAYS REPLACE" part of his transcription prompt), offered as a starting set
     * by a button on the settings screen, never loaded behind his back. "Cloud" on its own is left
     * out: his own list says it is Claude only when it means the AI, and a replacement cannot tell.
     */
    val SEEDS: List<Pair<String, String>> = listOf(
        "Amina" to "Emina",
        "leaving with" to "living with",
        "Fever" to "Fiverr",
        "Fevr" to "Fiverr",
        "Arvil Community" to "auroville.community",
        "Auroville community" to "auroville.community",
        "pixel video" to "Pixar video",
        "pixel videos" to "Pixar videos",
        "Kiipi" to "Kie",
        "KIA" to "Kie",
        "Keyie" to "Kie",
        "kiae" to "Kie",
        "Kia AI" to "Kie",
        "KIE" to "Kie",
        "YouCloud" to "Claude",
        "You Cloud" to "Claude",
        "cloud session" to "Claude session",
        "Animatix" to "animatic",
        "Animatics" to "animatic",
        "Davinci" to "DaVinci Resolve",
        "Da Vinci" to "DaVinci Resolve",
        "Elivate" to "Elevate Media",
        "Elevate media" to "Elevate Media",
        "Mantra production" to "Mantra Productions",
        "Mantra productions" to "Mantra Productions",
        "Bok Edibe" to "Bok Edib",
    )

    // ---------------------------------------------------------------------------------------------
    // Words
    // ---------------------------------------------------------------------------------------------

    /** A word or a mark, with where it sits in the text it came from. */
    data class Token(val text: String, val start: Int, val end: Int) {
        val isWord: Boolean get() = text.any { it.isLetterOrDigit() }
    }

    private fun inWord(ch: Char) = ch.isLetterOrDigit() || ch == '\'' || ch == '’' || ch == '-' || ch == '_' || ch == '.' || ch == '@'

    /**
     * Words (letters, digits and the marks that live inside them, including the dot of
     * "auroville.community") and single punctuation marks. Spaces separate and are dropped.
     */
    fun tokenize(text: String): List<Token> {
        val out = ArrayList<Token>()
        var i = 0
        while (i < text.length) {
            val ch = text[i]
            when {
                ch.isWhitespace() -> i++
                ch.isLetterOrDigit() -> {
                    var j = i + 1
                    while (j < text.length && inWord(text[j])) j++
                    // A word does not end on its own dot, dash or apostrophe: "Emina." is "Emina" and ".".
                    while (j > i + 1 && !text[j - 1].isLetterOrDigit()) j--
                    out += Token(text.substring(i, j), i, j)
                    i = j
                }
                else -> {
                    out += Token(ch.toString(), i, i + 1)
                    i++
                }
            }
        }
        return out
    }

    // ---------------------------------------------------------------------------------------------
    // Comparing what was written with what is there now
    // ---------------------------------------------------------------------------------------------

    /** One correction found: the words as written, the words he left in their place. */
    data class Swap(val wrong: String, val right: String)

    /**
     * The corrections between [written] (a dictation as it went into the field) and [now] (the same
     * stretch of the field, read later; it may run on past the dictation, into what came after).
     *
     * Both are aligned word by word (longest common subsequence). The gaps between aligned words are
     * the changes. A gap at the very start counts, because [now] begins where the dictation began; a
     * gap at the end does not, because there [now] runs into text that was never the dictation.
     */
    fun diff(written: String, now: String): List<Swap> {
        val a = tokenize(written)
        val b = tokenize(now)
        if (a.isEmpty() || b.isEmpty() || a.size > 400 || b.size > 600) return emptyList()
        // LCS table on case-sensitive text: "emina" to "Emina" is a change worth seeing.
        val n = a.size
        val m = b.size
        val dp = Array(n + 1) { IntArray(m + 1) }
        for (i in n - 1 downTo 0) {
            for (j in m - 1 downTo 0) {
                dp[i][j] = if (a[i].text == b[j].text) dp[i + 1][j + 1] + 1 else maxOf(dp[i + 1][j], dp[i][j + 1])
            }
        }
        val matched = dp[0][0]
        // More than half of the dictation gone: a rewrite, not a correction.
        if (matched * 2 < n) return emptyList()
        val gaps = ArrayList<Pair<List<Token>, List<Token>>>()
        var i = 0
        var j = 0
        var gapA = ArrayList<Token>()
        var gapB = ArrayList<Token>()
        var lastA = -1
        while (i < n && j < m) {
            if (a[i].text == b[j].text) {
                if (gapA.isNotEmpty() || gapB.isNotEmpty()) gaps += gapA to gapB
                gapA = ArrayList()
                gapB = ArrayList()
                lastA = i
                i++
                j++
            } else if (dp[i + 1][j] >= dp[i][j + 1]) {
                gapA += a[i++]
            } else {
                gapB += b[j++]
            }
        }
        // What is left after the last aligned word is the end, which does not count (see above).
        if (lastA < 0) return emptyList()
        val out = ArrayList<Swap>()
        for ((old, new) in gaps) {
            if (old.isEmpty() || new.isEmpty()) continue
            if (old.size > MAX_WORDS || new.size > MAX_WORDS) continue
            if (!old.all { it.isWord } || !new.all { it.isWord }) continue
            val wrong = old.joinToString(" ") { it.text }
            val right = new.joinToString(" ") { it.text }
            if (wrong == right) continue
            if (wrong.equals(right, ignoreCase = true) && !isNameFix(written, old.first(), wrong, right)) continue
            out += Swap(wrong, right)
        }
        return out
    }

    /**
     * A change of case alone is a correction only when it gives a word a capital it did not have, in
     * the middle of a sentence: "emina" to "Emina". At the start of a sentence it is grammar, and a
     * capital taken away is style.
     */
    private fun isNameFix(written: String, first: Token, wrong: String, right: String): Boolean {
        if (!right.first().isUpperCase() || wrong.first().isUpperCase()) return false
        val before = written.substring(0, first.start).trimEnd()
        return before.isNotEmpty() && before.last() !in ".!?\n:"
    }

    // ---------------------------------------------------------------------------------------------
    // Keeping count
    // ---------------------------------------------------------------------------------------------

    /**
     * [rules] with [swap] made once more in [language]. The same fix again adds one; a different fix
     * of the same wrong words replaces the old one and starts again from one, because the newest
     * thing he did is the best guess at what he wants. A rule of his own is never overwritten by a
     * learned one.
     */
    fun record(rules: List<Rule>, swap: Swap, language: String): List<Rule> {
        val key = keyOf(swap.wrong, language)
        val existing = rules.firstOrNull { it.key() == key }
        val updated = when {
            existing == null -> Rule(swap.wrong, swap.right, language, 1, true, Source.LEARNED)
            existing.source == Source.MINE -> return rules
            existing.right == swap.right -> existing.copy(count = existing.count + 1)
            else -> existing.copy(right = swap.right, count = 1)
        }
        // A fix that undoes an active rule ("Emina" back to "Amina") switches that rule off: he
        // has just shown it was wrong for him.
        val reverse = keyOf(swap.right, language)
        return rules.filter { it.key() != key }.map {
            if (it.key() == reverse && it.right.equals(swap.wrong, ignoreCase = true)) it.copy(enabled = false) else it
        } + updated
    }

    /** [rules] with his own list added; rules he already has for the same words are left alone. */
    fun withSeeds(rules: List<Rule>): List<Rule> {
        val have = rules.map { keyOf(it.wrong, it.language) }.toSet()
        val add = SEEDS.filter { keyOf(it.first, ANY) !in have }
            .map { Rule(it.first, it.second, ANY, 0, true, Source.MINE) }
        return rules + add
    }

    // ---------------------------------------------------------------------------------------------
    // Applying
    // ---------------------------------------------------------------------------------------------

    /**
     * [text] with every active rule for [language] (and for every language) applied, whole words only,
     * ignoring case. The longest wrong words first, so "Kia AI" is used before "KIA" can split it.
     *
     * A match already followed by the rest of the right form is left as it is: "DaVinci Resolve"
     * does not become "DaVinci Resolve Resolve". A match at a capital keeps the capital.
     */
    fun apply(text: String, rules: List<Rule>, language: String): String {
        val active = rules.filter { it.isActive && (it.language == language || it.language == ANY) }
            .sortedByDescending { it.wrong.length }
        if (active.isEmpty() || text.isEmpty()) return text
        var out = text
        for (rule in active) {
            val words = rule.wrong.trim().split(Regex("\\s+")).joinToString("\\s+") { Regex.escape(it) }
            val re = Regex("(?<![\\p{L}\\p{N}])$words(?![\\p{L}\\p{N}])", setOf(RegexOption.IGNORE_CASE))
            out = re.replace(out) { m ->
                val rest = out.substring(m.range.first)
                when {
                    rest.startsWith(rule.right, ignoreCase = true) -> m.value
                    m.value.first().isUpperCase() && rule.right.first().isLowerCase() &&
                        !rule.right.contains('.') -> rule.right.replaceFirstChar { it.uppercaseChar() }
                    else -> rule.right
                }
            }
        }
        return out
    }

    /** The right forms of the active rules, for the speech model's word list. */
    fun glossary(rules: List<Rule>, language: String): List<String> =
        rules.filter { it.isActive && (it.language == language || it.language == ANY) }
            .map { it.right }.distinct()

    // ---------------------------------------------------------------------------------------------
    // Storage: one line per rule, tab-separated
    // ---------------------------------------------------------------------------------------------

    fun serialize(rules: List<Rule>): String = rules.joinToString("\n") {
        listOf(it.wrong, it.right, it.language, it.count.toString(), if (it.enabled) "1" else "0", it.source.name)
            .joinToString("\t") { field -> field.replace('\t', ' ').replace('\n', ' ') }
    }

    fun parse(raw: String?): List<Rule> = raw.orEmpty().lines().mapNotNull { line ->
        val f = line.split('\t')
        if (f.size < 6 || f[0].isBlank() || f[1].isBlank()) return@mapNotNull null
        Rule(
            wrong = f[0],
            right = f[1],
            language = f[2].ifBlank { ANY },
            count = f[3].toIntOrNull() ?: 0,
            enabled = f[4] == "1",
            source = runCatching { Source.valueOf(f[5]) }.getOrDefault(Source.LEARNED),
        )
    }
}
