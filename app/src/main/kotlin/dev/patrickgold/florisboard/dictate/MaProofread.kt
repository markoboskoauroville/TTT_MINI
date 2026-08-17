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
import dev.patrickgold.florisboard.dictate.data.prompts.PromptModel

/**
 * Ctrl+P: proofreading, the way a word processor does it.
 *
 * ### What it is for
 *
 * Marko dictates, and dictation arrives without the small mechanical things writing needs —
 * punctuation in the right places, a capital after a full stop, an agreement the mouth skipped. This
 * fixes those and stops. It is a proofreader, not an editor: it must never improve his writing,
 * because the writing is his and the mistakes it is asked to remove are not the interesting ones.
 *
 * ### Why the instruction is this insistent
 *
 * A model asked to "correct" text will rewrite it — shorten sentences, formalise them, drop the
 * repetition he meant. Coming back to a paragraph that reads better and no longer sounds like him is
 * worse than a missing comma, and worse still because it is hard to see. So the instruction spends
 * most of its words saying what not to touch, and says it in the imperative rather than as
 * preference, which is what these models actually obey.
 *
 * ### Why it returns only the text
 *
 * The result goes straight into the field through `commitText`, replacing the selection. Anything
 * conversational — "Here is the corrected version:" — would be typed into his message as though he
 * had written it. The instruction forbids it explicitly; a note about preamble is the difference
 * between a feature and a mess to clean up.
 *
 * ### Language
 *
 * Nothing here names a language. The text says what language it is in, and this app is used in two;
 * an instruction that mentioned English would quietly translate his Croatian, which is the largest
 * possible version of the mistake this prompt exists to avoid.
 */
object MaProofread {

    const val NAME = "Proofread"

    /**
     * The instruction, kept as one string so it can be read as the reader will receive it.
     *
     * The input text is appended after this by the rewording path, separated by a blank line.
     */
    const val INSTRUCTION =
        "Proofread the text below. Fix spelling, punctuation, capitalisation and grammatical " +
            "agreement, and nothing else.\n\n" +
            "Rules:\n" +
            "- Keep every word the author chose. Do not rephrase, shorten, expand, reorder or " +
            "improve the writing in any way.\n" +
            "- Keep the author's tone, register and personal style exactly, including informal or " +
            "unusual phrasing, which is deliberate.\n" +
            "- Keep the original language. Never translate.\n" +
            "- Keep line breaks, paragraph breaks, lists and any formatting exactly as they are.\n" +
            "- Do not add or remove content, and do not add explanations, comments or notes.\n" +
            "- If the text is already correct, return it completely unchanged.\n\n" +
            "Return only the corrected text. No preamble, no quotation marks around it, no " +
            "commentary."

    /**
     * The prompt as the rewording path expects it.
     *
     * A throwaway [PromptModel] rather than a row in the prompt library: this one is bound to a key,
     * not chosen from a list, and a library entry would be one he could rename, edit or delete out
     * from under a shortcut that then did nothing. `ID_INSTANT_PROMPT` is the id already used for
     * prompts that are not persisted.
     *
     * `requiresSelection` is true, which in `applyPrompt` means: use the selection if there is one,
     * otherwise select the whole field and work on that. Both are exactly right here — correct what
     * I marked, or correct all of it — and neither needed new code.
     */
    /** What he has written in settings, or empty. */
    fun custom(): String {
        val prefs by FlorisPreferenceStore
        return prefs.dictate.maProofreadPrompt.get()
    }

    fun prompt(): PromptModel = PromptModel(
        id = PromptModel.ID_INSTANT_PROMPT,
        pos = 0,
        name = NAME,
        // His wording if he has written one, the shipped instruction otherwise.
        //
        // Read at press time rather than captured once, so editing the text in settings changes the
        // very next press with nothing to restart.
        prompt = custom().ifBlank { INSTRUCTION },
        requiresSelection = true,
        autoApply = false,
    )
}
