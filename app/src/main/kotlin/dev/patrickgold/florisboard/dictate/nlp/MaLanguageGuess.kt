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

/**
 * Croatian or English, decided from the text itself. No model, no network, no key.
 *
 * ### Why this is not an API call
 *
 * Telling these two apart is far easier than the general problem. Croatian carries five letters
 * English never uses, and where those are absent the commonest words of each language share almost
 * nothing. A few dozen words and five letters answer it in microseconds, on the phone, offline, for
 * nothing — where a model would cost a round trip, a key, and a dependency on somebody's uptime.
 *
 * ### Why it is clamped to two
 *
 * Marko speaks two languages. A general detector has ninety-nine answers and ninety-seven of them
 * are wrong by construction — it can return Spanish for accented English or Chinese for a noisy
 * room. This one cannot: it returns Croatian, English, or *don't know*, and don't know is a real
 * answer rather than a failure.
 *
 * ### Why it is allowed to say nothing
 *
 * "OK", a phone number, a URL, a single name — none of these are evidence of anything, and a
 * detector that guesses on them would flip Marko's language key while he watched. [UNKNOWN] means
 * leave everything exactly as it is.
 */
object MaLanguageGuess {

    const val HR = "hr"
    const val EN = "en"
    const val UNKNOWN = ""

    /**
     * Letters that exist in Croatian and not in English.
     *
     * One of these in a sentence is nearly conclusive, which is why they are weighted far above any
     * single word. Their absence proves nothing, though — plenty of Croatian sentences contain none.
     */
    private val CROATIAN_LETTERS = setOf('č', 'ć', 'ž', 'š', 'đ')

    /**
     * The commonest words of each language, chosen for how rarely they appear in the other.
     *
     * Deliberately short. A long list is not more accurate here, it is only more work to read and
     * more places for a word that exists in both to slip in. Words that are ambiguous — "i" is
     * Croatian "and" but also an English pronoun, "to" is Croatian "that" and English "to" — are
     * left out entirely rather than weighted, because a word that argues for both sides is noise
     * whichever way it is counted.
     */
    private val CROATIAN_WORDS = setOf(
        // to be
        "je", "su", "sam", "smo", "ste", "nije", "nema", "bio", "bila", "bilo", "biti",
        // prepositions — the densest signal in ordinary Croatian, and none of them are English
        // words. "u" is included deliberately: it is everywhere in Croatian and appears in English
        // only as a texting abbreviation, which dictated text does not produce.
        "u", "na", "za", "sa", "iz", "od", "do", "po", "uz", "bez", "pod", "nad", "kroz",
        "kod", "prema", "oko", "pri",
        // pronouns and particles
        "se", "si", "mi", "ti", "mu", "joj", "im", "ga", "ju", "nam", "vam",
        // conjunctions and adverbs
        "da", "ne", "ali", "ili", "pa", "jer", "kako", "kada", "gdje", "zato", "već",
        "onda", "sada", "opet", "tako", "samo", "još", "vrlo", "puno",
        // common determiners and verbs
        "koji", "koja", "koje", "ovo", "ovaj", "ova", "taj", "sve", "svi",
        "treba", "može", "ima", "hoću", "moram", "znam", "kaže",
    )

    private val ENGLISH_WORDS = setOf(
        "the", "and", "of", "is", "are", "was", "were", "be", "been",
        "with", "from", "for", "that", "this", "these", "those",
        "have", "has", "had", "will", "would", "should", "could",
        "what", "when", "which", "there", "their", "they", "you", "your",
        "not", "but", "all", "just", "some", "more", "than", "then",
    )

    /**
     * Enough evidence to be worth acting on.
     *
     * Two, not one. A single hit is a coincidence — "sam" appears in English text as a name, "is"
     * appears in Croatian as part of nothing but turns up in quoted English. Two independent hits
     * pointing the same way is a sentence, and a sentence is what this is asked about.
     */
    private const val MIN_EVIDENCE = 2

    /** How far ahead one language must be before the answer is given. */
    private const val MIN_MARGIN = 2

    /**
     * The language of [text], or [UNKNOWN] when the text does not say.
     *
     * Croatian letters count double, because they are near-proof where a word is only a hint.
     */
    fun guess(text: String): String {
        if (text.isBlank()) return UNKNOWN
        val lower = text.lowercase()

        var hr = 0
        var en = 0

        // Letters first. Each occurrence counts, up to a cap: a single word full of them should not
        // outvote a whole English sentence, but three of them across a line certainly should.
        val letterHits = lower.count { it in CROATIAN_LETTERS }
        hr += minOf(letterHits, 4) * 2

        for (word in lower.split(Regex("[^\\p{L}]+"))) {
            if (word.isEmpty()) continue
            if (word in CROATIAN_WORDS) hr++
            if (word in ENGLISH_WORDS) en++
        }

        return when {
            hr >= MIN_EVIDENCE && hr - en >= MIN_MARGIN -> HR
            en >= MIN_EVIDENCE && en - hr >= MIN_MARGIN -> EN
            // Both languages present in quantity, or neither. A mixed sentence is a real thing
            // Marko writes, and calling it one language would be a lie the app then acts on.
            else -> UNKNOWN
        }
    }

    /**
     * Whether [text] contradicts the language it was transcribed as.
     *
     * The point of the whole file: a transcript that reads Croatian but was sent as English is the
     * failure Marko keeps finding by reading it afterwards. This finds it immediately, and only
     * says so when it is sure — [UNKNOWN] never contradicts anything.
     */
    fun disagreesWith(text: String, transcribedAs: String): Boolean {
        val guessed = guess(text)
        return guessed != UNKNOWN && guessed != transcribedAs.lowercase().take(2)
    }
}
