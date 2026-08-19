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
 * Deciding which of his two languages a recording is in, before it is sent to be transcribed.
 *
 * ### Why this exists
 *
 * He speaks Croatian and English and switches mid-thought, and the language has to be settled
 * BEFORE the transcription request is built: AssemblyAI is told which language to expect, and told
 * wrongly it returns confident nonsense. Setting it by hand means remembering to, every time, and
 * the times he forgets are exactly the times he is in a hurry.
 *
 * ### Why Groq and not AssemblyAI
 *
 * Groq's Whisper answers in about a second and reports the language it heard as part of the
 * transcription. It is a probe, not the transcription he keeps — that still goes to AssemblyAI,
 * which is better at both his languages. Two sends, and the first one is cheap.
 *
 * ### Is it English? If not, it is Croatian.
 *
 * His rule, and it is the right one. Whisper answers with a language NAME, measured: "English" for
 * English speech, "Croatian" for Croatian. But Croatian is routinely heard as Slovenian, Serbian,
 * Bosnian, Czech or Polish — they are neighbours and it is a small language in the training data.
 * English is almost never mistaken for anything.
 *
 * So the question asked is not "which language is this" but **"is this English"**, and everything
 * else lands on Croatian. That also fails in the safe direction: Croatian must never take the sync
 * path, and a wrong answer here sends it to the slow one, which is merely slower.
 */
object MaLanguageProbe {

    /**
     * How much audio the probe listens to.
     *
     * Thirty seconds, his number, and a good one for two reasons: Whisper decides the language from
     * the opening moments anyway, so more audio buys nothing, and a probe that grew with the
     * recording would make the longest dictations wait longest for the thing that has to happen
     * before they are sent.
     */
    const val PROBE_SECONDS = 30

    /**
     * Turns whatever Whisper reported into one of his two languages.
     *
     * Pure, so it can be tested without a network. Everything that is not recognisably English
     * becomes Croatian, including an empty or missing answer: with nothing to go on, the safe
     * direction is the one that cannot reach the sync path.
     */
    private val HR_DIACRITICS = setOf('č', 'ć', 'ž', 'š', 'đ')

    /** Croatian words so common that a sentence without any of them is unusual. */
    private val HR_WORDS = setOf(
        "je", "se", "na", "za", "koje", "koji", "što", "sto", "ali", "ovo", "kao", "sam", "nije",
        "su", "da", "ne", "li", "od", "do", "po", "pa", "te", "ili", "kad", "kada", "gdje", "gde",
        "tako", "samo", "već", "vec", "onda", "jer", "bez", "pod", "nad", "pred", "kroz", "prema",
        "jedan", "jedna", "jedno", "ovaj", "ova", "taj", "ta", "to", "ti", "mi", "vi", "oni",
        "nas", "vas", "njih", "biti", "ima", "imam", "imaš", "može", "moze", "mora", "treba",
        "hoću", "hocu", "ću", "cu", "bi", "bio", "bila", "bilo", "sve", "svi", "ništa", "nista",
        "nešto", "nesto", "dobro", "hvala", "molim", "evo", "eto", "ovdje", "ovde",
    )

    private val EN_WORDS = setOf(
        "the", "and", "is", "to", "of", "that", "with", "this", "for", "are", "was", "have",
        "has", "you", "your", "it", "in", "on", "at", "be", "not", "but", "they", "we", "he",
        "she", "from", "as", "by", "an", "or", "if", "can", "will", "would", "there", "their",
        "what", "when", "which", "who", "all", "been", "one", "do", "does", "did", "just",
        "about", "into", "than", "then", "them", "these", "those", "some", "more", "other",
        "only", "also", "because", "after", "before", "over", "very", "much", "make", "made",
    )

    /**
     * Croatian inflects and English does not, so word ENDINGS are a signal that needs no
     * vocabulary. It is what catches an ordinary sentence built entirely of words nobody listed.
     */
    private val HR_ENDINGS = listOf(
        "ati", "iti", "jeti", "ovati", "ncija", "ost", "ući", "uci", "ijem", "ima", "ama",
        "ovi", "ega", "oga", "omu", "emu", "ih", "og", "im", "ju", "lo", "la", "li", "ti",
    )

    private val EN_ENDINGS = listOf("ing", "tion", "ness", "ment", "ously", "able", "ible")

    /**
     * Reads the language off the TRANSCRIPT, which is far better evidence than the acoustic guess.
     *
     * ### Why this beats asking Whisper
     *
     * Whisper's `language` field is decided from the opening seconds of audio, before it has heard
     * much, and Croatian is a small language sitting beside four larger ones it is routinely
     * confused with. But the probe already returns the TEXT, and text is decisive in a way sound is
     * not: no English sentence contains `č`, and no Croatian one is built out of "the" and "of".
     *
     * ### Three independent signals
     *
     * Diacritics, counted triple — one `ž` is nearly proof on its own. Function words, which are
     * the commonest words in either language and therefore present in almost any sentence. And
     * **endings**, which is the one that matters most: it needs no vocabulary at all, so a sentence
     * about camera rigs or tax forms — words no list will ever contain — still scores correctly.
     *
     * Returns null when there is genuinely nothing to go on, which the caller treats as "no
     * opinion" rather than as a guess.
     */
    fun scoreText(text: String?): String? {
        if (text.isNullOrBlank()) return null
        val low = text.lowercase(Locale.ROOT)
        val words = low.split(Regex("\\s+")).map { it.trim(*" .,!?;:\"'()[]\u2026\u2014-".toCharArray()) }
        var hr = words.count { it in HR_WORDS }
        var en = words.count { it in EN_WORDS }
        hr += low.count { it in HR_DIACRITICS } * 3
        hr += words.count { w -> w.length > 4 && HR_ENDINGS.any { w.endsWith(it) } }
        en += words.count { w -> w.length > 4 && EN_ENDINGS.any { w.endsWith(it) } }
        if (hr == 0 && en == 0) return null
        return when {
            hr >= en * 2 -> MaLanguage.HR
            en >= hr * 2 -> MaLanguage.EN
            hr > en -> MaLanguage.HR
            en > hr -> MaLanguage.EN
            else -> null
        }
    }

    /**
     * The verdict, from the transcript first and the acoustic label only as a fallback.
     *
     * The text wins whenever it has an opinion, because it is evidence about what was actually
     * said rather than about how it sounded in the first two seconds. The label is what remains
     * when the transcript is too short to score — "Okay", "Hvala" — and in that case it is right
     * about as often as anything could be.
     */
    fun decide(reportedLanguage: String?, transcript: String?): String {
        scoreText(transcript)?.let { return it }
        return clampToTwo(reportedLanguage)
    }

    fun clampToTwo(reported: String?): String {
        val name = reported?.trim()?.lowercase(Locale.ROOT).orEmpty()
        if (name.isEmpty()) return MaLanguage.HR
        // Whisper answers with a name in this API, but a code costs nothing to accept as well, and
        // an API that changes its mind about which it returns should not silently break this.
        val isEnglish = name == "english" || name == "en" || name.startsWith("english")
        return if (isEnglish) MaLanguage.EN else MaLanguage.HR
    }
}
