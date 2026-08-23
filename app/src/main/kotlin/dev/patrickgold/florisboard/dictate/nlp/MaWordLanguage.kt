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
 * WHICH LANGUAGE IS THIS WORD, AND IS IT WORTH ASKING.
 *
 * The badge says which language he is writing in, and for a sentence that is usually right. For a
 * single word it is not enough: he writes an English word inside a Croatian sentence and back again,
 * and a word filed under the badge alone lands in the wrong model and is offered in the wrong
 * language forever after.
 *
 * ### Four answers, and only the last one costs anything
 *
 * | Evidence | Answer | Cost |
 * |---|---|---|
 * | already in one model | that model | nothing |
 * | already in BOTH models | both, unchanged | nothing |
 * | has č ć ž š đ | Croatian | nothing |
 * | none of the above | ask | one word in a batched request, once ever |
 *
 * **His rule, and it is the right one: a word already in a model is already in the right place.** It
 * was filed once, correctly, and asking again would spend money to be told what is already on disk.
 * So the cost of this decays to nothing — every word he uses is asked about at most once in the life
 * of the phone, and the words he uses most are asked about first and never again.
 *
 * ### Why the diacritic rule sits above the model
 *
 * A word containing č, ć, ž, š or đ is Croatian and no model is needed to say so. It is free
 * certainty, it covers a large share of what he writes, and it shrinks the queue that costs money.
 *
 * The reverse does NOT hold: a word without them is not therefore English. Most Croatian words have
 * no diacritics at all. That asymmetry is why there is no matching "plain letters means English"
 * rule — it would be right often and wrong constantly, which is the worst kind of rule.
 *
 * ### Both is a real answer
 *
 * "Radio", "auto", "student", "film", "problem" are ordinary words in both languages. A word already
 * in both models is left in both. Forcing a choice would delete it from one language to satisfy a
 * schema, and he would lose a suggestion he had earned.
 *
 * Pure: strings in, an answer out. No network here — this decides WHETHER to ask, and the asking
 * lives in MaNgram where the queue and the key ring already are.
 */
object MaWordLanguage {

    const val EN = "en"
    const val HR = "hr"

    /** Letters that exist in Croatian and not in English. Free, certain evidence. */
    private const val CROATIAN_LETTERS = "čćžšđ"

    sealed interface Answer {
        /** Already known, and already in the right place. Learn it there. */
        data class Known(val languages: Set<String>) : Answer

        /** Certain without asking anybody. */
        data class Certain(val language: String) : Answer

        /** Nobody knows yet. Queue it, learn it under [fallback] meanwhile so nothing is lost. */
        data class Ask(val word: String, val fallback: String) : Answer
    }

    /** Lowercased, stripped of anything that is not a letter. The form both models are keyed on. */
    fun normalise(word: String): String =
        word.lowercase().filter { it.isLetter() || it == '\'' }.trim('\'')

    /**
     * What to do with [word], given what each model already knows and what the badge says.
     *
     * [knownEn] and [knownHr] are asked of the models, not of a cache: a word learned a second ago
     * must count as known, or the first few words of every session are asked about twice.
     */
    fun decide(word: String, badge: String, knownEn: Boolean, knownHr: Boolean): Answer {
        val w = normalise(word)
        if (w.isBlank()) return Answer.Known(emptySet())

        // Known beats everything, including the diacritic rule. If a word is already filed, the
        // filing is the answer — that is his rule and it is what makes this nearly free.
        val known = buildSet {
            if (knownEn) add(EN)
            if (knownHr) add(HR)
        }
        if (known.isNotEmpty()) return Answer.Known(known)

        if (w.any { it in CROATIAN_LETTERS }) return Answer.Certain(HR)

        // Unknown and unmarked. It goes in under the badge so nothing is ever lost, and it goes on
        // the queue so the badge's guess can be corrected later by something that actually knows.
        return Answer.Ask(w, fallback = badge)
    }

    /**
     * The question, for a batch.
     *
     * One request for many words, because a request per word would be a request per keystroke-ish
     * and the whole point of the known-word rule is to keep this rare. Answers come back as
     * `word=en` lines, which is the smallest thing that cannot be misread — a model asked for prose
     * gives prose.
     */
    fun prompt(words: List<String>): String = buildString {
        append("Classify each word as English or Croatian.\n")
        append("Answer one line per word, exactly: word=en or word=hr\n")
        append("If a word is ordinary in BOTH languages, answer word=both.\n")
        append("No other text.\n\n")
        words.forEach { append(it).append('\n') }
    }

    /**
     * The answers, as a map of word to languages.
     *
     * Anything unparseable is dropped rather than guessed at. A word that gets no usable answer stays
     * where the badge put it and stays on no queue — one unanswered classification is not worth a
     * retry loop, and it will be asked again the next time the word is genuinely new, which it will
     * not be.
     */
    fun readAnswer(reply: String, asked: Set<String>): Map<String, Set<String>> {
        val out = mutableMapOf<String, Set<String>>()
        for (line in reply.lineSequence()) {
            val parts = line.trim().lowercase().split('=')
            if (parts.size != 2) continue
            val word = normalise(parts[0])
            if (word !in asked) continue
            val langs = when (parts[1].trim().trim('.', '"', '\'')) {
                EN -> setOf(EN)
                HR -> setOf(HR)
                "both" -> setOf(EN, HR)
                else -> continue
            }
            out[word] = langs
        }
        return out
    }
}
