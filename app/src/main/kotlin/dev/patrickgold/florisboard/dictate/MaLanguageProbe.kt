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
    fun clampToTwo(reported: String?): String {
        val name = reported?.trim()?.lowercase(Locale.ROOT).orEmpty()
        if (name.isEmpty()) return MaLanguage.HR
        // Whisper answers with a name in this API, but a code costs nothing to accept as well, and
        // an API that changes its mind about which it returns should not silently break this.
        val isEnglish = name == "english" || name == "en" || name.startsWith("english")
        return if (isEnglish) MaLanguage.EN else MaLanguage.HR
    }
}
