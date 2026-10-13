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

/**
 * CROATIAN SPELL CHECK, AFTER EVERY CROATIAN DICTATION. MANDATORY, IN HIS WORD.
 *
 * AssemblyAI transcribes Croatian on Universal-2, and it invents words: a sound that is nearly a
 * Croatian word comes back as a word that does not exist, or as a real word that was not said. The
 * text then goes into his message looking finished. So every Croatian transcript passes through
 * Claude once more before it is typed, with one job: put back the word that was spoken.
 *
 * ### Narrower than Ctrl+P on purpose
 *
 * Proofreading fixes punctuation and agreement. This fixes MISHEARING — the word that cannot be
 * there — plus Croatian spelling (č/ć, ije/je, dž/đ) and punctuation. It must not rephrase: a spell
 * check that improves style would change every dictation, silently, under his name.
 *
 * ### The guard
 *
 * A reply far longer or shorter than the transcript is not a corrected transcript; it is a model
 * talking. [accept] keeps the original then, the same rule §51 applies to Ctrl+P.
 *
 * The instruction is editable (Settings → Prompts), as a named set like Ctrl+P and Ctrl+F, and so
 * is the model it runs on.
 */
object MaSpellcheck {

    const val NAME = "Croatian spell check"

    const val INSTRUCTION =
        "This is a Croatian speech-to-text transcript. The recogniser sometimes writes a word that " +
            "does not exist in Croatian, or a similar-sounding wrong word. Correct only those: replace " +
            "each non-existent or clearly misheard word with the Croatian word that was most likely " +
            "spoken, judging by sound and by the sentence around it.\n\n" +
            "Also correct Croatian spelling (č and ć, dž and đ, ije and je), capital letters and " +
            "punctuation.\n\n" +
            "Rules:\n" +
            "- Do not rephrase, reorder, shorten or add anything. Keep every word that is already " +
            "a correct Croatian word, even if it is informal.\n" +
            "- Keep English words, names and brand names that were spoken as such.\n" +
            "- If you are not sure what was said, keep the original word.\n" +
            "- Never translate.\n\n" +
            "Return only the corrected text. No preamble, no quotation marks, no comments."

    /** The wording in force: his ticked instance, or the shipped one. */
    fun instruction(): String {
        val prefs by FlorisPreferenceStore
        return MaPrompts.activeText(prefs.dictate.maSpellcheckPrompt.get()).ifBlank { INSTRUCTION }
    }

    /** The model for this job, or null for the rewording account's own. */
    fun model(): String? {
        val prefs by FlorisPreferenceStore
        return prefs.dictate.maSpellcheckModel.get().trim().ifBlank { null }
    }

    fun enabled(): Boolean {
        val prefs by FlorisPreferenceStore
        return prefs.dictate.maSpellcheckEnabled.get()
    }

    /**
     * Whether [corrected] may replace [original]. Blank, or more than half again as long, or less
     * than half as long, is refused: a spell check changes words, not the size of the text.
     */
    fun accept(original: String, corrected: String): Boolean {
        val c = corrected.trim()
        if (c.isEmpty()) return false
        val o = original.trim().length.coerceAtLeast(1)
        val ratio = c.length.toDouble() / o
        return ratio in 0.5..1.5
    }
}
