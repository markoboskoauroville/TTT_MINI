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
 * Finds the words worth teaching the keyboard, out of what has already been dictated.
 *
 * ### Why the history and not a language model
 *
 * The keyboard's suggestions are weak on exactly the words that matter most to one person: names,
 * places, the vocabulary of their own work. No general dictionary will ever hold Mantreshvar,
 * Kukljica, Auroville or Kerstin, and no amount of model quality supplies them.
 *
 * The dictation history already holds all of them, in the user's own words, in the proportions he
 * actually uses. It is on the phone, it is free, nothing leaves the device, and it needs no network
 * call. A prediction service would have to be asked on every keystroke, which is impossible at the
 * speed typing happens and would send everything typed to a server besides.
 *
 * ### What counts as worth suggesting
 *
 * A word has to be repeated, long enough to be worth completing, and not already known. Repetition
 * is the important one: a transcript is full of words said once, and offering those would bury the
 * handful that matter under everything the user has ever mentioned in passing.
 */
object MaVocabulary {

    /**
     * How often a word must appear before it is offered.
     *
     * Two, not one. Once is a mention; twice is vocabulary. It is a low bar deliberately, because
     * the cost of offering a word that is not wanted is one glance and an untick, while the cost of
     * missing one is that the keyboard keeps fighting him over a name he uses daily.
     */
    const val MIN_OCCURRENCES = 2

    /**
     * Shorter than this and completing it saves nothing.
     *
     * Four rather than three: three-letter words are almost all common ones already in the
     * dictionary, and a suggestion that saves one keystroke is not worth a row in a list somebody
     * has to read.
     */
    const val MIN_LENGTH = 4

    /** Longer than this is a run-on, a URL or a transcription mistake rather than a word. */
    const val MAX_LENGTH = 24

    data class Candidate(val word: String, val count: Int)

    /**
     * Words that are common in both languages and would only clutter the list.
     *
     * Deliberately short. This is not a stopword list and should not become one: the dictionary the
     * candidates are checked against already knows ordinary words, so anything this catches is
     * something that slipped past that check. Croatian and English both, because Marko dictates in
     * both and a list that only knew English would offer him every Croatian preposition.
     */
    private val COMMON = setOf(
        "this", "that", "with", "have", "from", "they", "will", "would", "there", "their",
        "what", "when", "which", "been", "were", "them", "then", "than", "some", "just",
        "koji", "koja", "koje", "kako", "kada", "ovo", "ono", "ali", "pa", "jer",
        "onda", "samo", "tako", "jedan", "jedna", "ovaj", "taj", "sve", "sam", "smo",
    )

    /**
     * Splits text into words, keeping letters and the marks Croatian needs.
     *
     * Apostrophes stay inside a word so "don't" survives, and hyphens do too, because a hyphenated
     * name is one word rather than two. Digits end a word: a version number or a time is not
     * vocabulary, and "V5" would otherwise be offered as a name.
     */
    fun tokenise(text: String): List<String> =
        text.split(Regex("[^\\p{L}'\\-]+"))
            .map { it.trim('\'', '-') }
            .filter { it.isNotBlank() }

    /**
     * The words worth offering, commonest first.
     *
     * @param texts everything already dictated
     * @param known words the dictionary already has, lower case
     */
    fun candidates(texts: List<String>, known: Set<String>): List<Candidate> {
        val knownLower = known.map { it.lowercase() }.toSet()
        val counts = HashMap<String, Int>()
        val display = HashMap<String, String>()
        for (text in texts) {
            for (raw in tokenise(text)) {
                val lower = raw.lowercase()
                if (lower.length < MIN_LENGTH || lower.length > MAX_LENGTH) continue
                if (lower in COMMON || lower in knownLower) continue
                counts[lower] = (counts[lower] ?: 0) + 1
                // The form to offer is the one most often written, so a name keeps its capital and
                // an ordinary word does not gain one. Sentences start with a capital, so first-word
                // capitals would otherwise turn every common word into a proper noun.
                if (raw.firstOrNull()?.isUpperCase() == true) {
                    display[lower] = raw
                } else {
                    display.putIfAbsent(lower, raw)
                }
            }
        }
        return counts.asSequence()
            .filter { it.value >= MIN_OCCURRENCES }
            .map { Candidate(display[it.key] ?: it.key, it.value) }
            // Commonest first, and alphabetically within a count so the list does not reshuffle
            // between visits: a list that reorders itself cannot be worked through.
            .sortedWith(compareByDescending<Candidate> { it.count }.thenBy { it.word.lowercase() })
            .toList()
    }
}
