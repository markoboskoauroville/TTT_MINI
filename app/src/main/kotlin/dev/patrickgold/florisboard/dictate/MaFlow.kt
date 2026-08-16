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

import dev.patrickgold.florisboard.dictate.data.prompts.PromptModel

/**
 * Ctrl+F: make it flow, without making it somebody else's.
 *
 * ### The other half of Ctrl+P
 *
 * [MaProofread] is forbidden from touching a single word he chose — it fixes what is *wrong* and
 * nothing more. This one is allowed to rewrite, and that difference is the entire reason both
 * exist rather than one key with a compromise between them. When he wants the commas fixed he
 * wants only the commas fixed; when he wants the paragraph to read well he is asking for the words
 * to move.
 *
 * ### What speech leaves behind
 *
 * Dictation repeats. The same thought arrives three times in three shapes, because that is how
 * speaking works — the mouth finds the sentence while it is saying it, and the false starts stay in
 * the transcript. It also strands clauses, loses the thread mid-sentence and picks it up again a
 * line later. None of that is a mistake in the thinking, so a proofreader will not touch it and the
 * result reads as rambling written by someone careless, which is the opposite of true.
 *
 * So the instruction is aimed squarely at redundancy and order: say each thing once, put the things
 * in the sequence that makes them follow.
 *
 * ### The register, in his words
 *
 * Half friendly, half professional. Not the flattened corporate voice a model reaches for when told
 * to "improve" something — that voice would strip out exactly what makes a message from him
 * recognisable as from him. The instruction names the trap directly, because a model told only to
 * make text professional will produce something nobody would ever say aloud.
 *
 * ### The line it must not cross
 *
 * It may cut, join and reorder. It may not add. A model given permission to rewrite will invent a
 * politeness he did not offer, a commitment he did not make, or a fact he did not state, and a
 * message that goes out carrying an invented promise is worse than one that rambles.
 */
object MaFlow {

    /** Shown beside the spinning dust while it runs, so the two keys are told apart mid-flight. */
    const val NAME = "Better flow"

    const val INSTRUCTION =
        "Rewrite the text below so it flows well, keeping the author's own voice.\n\n" +
            "Do this:\n" +
            "- Remove redundancy. Dictated speech says the same thing two or three times in " +
            "different words; keep the best one and cut the rest.\n" +
            "- Fix the order so the ideas follow each other, joining or splitting sentences where " +
            "that helps.\n" +
            "- Remove false starts, filler and stranded half-sentences left over from speaking.\n" +
            "- Fix spelling, punctuation and grammar along the way.\n\n" +
            "Keep this:\n" +
            "- The author's voice and register: warm and direct, half friendly and half " +
            "professional. Do NOT make it sound corporate, formal or generic.\n" +
            "- The original language. Never translate.\n" +
            "- Every fact, name, number, question and commitment exactly as given.\n" +
            "- Roughly the original length or shorter. Never pad.\n\n" +
            "Never add information, opinions, greetings, sign-offs, politeness or promises that " +
            "are not already there.\n\n" +
            "Return only the rewritten text. No preamble, no quotation marks around it, no " +
            "commentary."

    /**
     * The prompt as the rewording path expects it.
     *
     * A throwaway [PromptModel] rather than a library row, for the same reason as the proofreader:
     * this one is bound to a key, and a library entry could be renamed or deleted out from under a
     * shortcut that would then do nothing.
     *
     * `requiresSelection` is true, which in `applyPrompt` means the selection if there is one and
     * the whole field otherwise. That matters more here than for Ctrl+P — this key rewrites, so
     * being able to hand it one paragraph rather than the whole message is the difference between a
     * tool and a gamble.
     */
    fun prompt(): PromptModel = PromptModel(
        id = PromptModel.ID_INSTANT_PROMPT,
        pos = 0,
        name = NAME,
        prompt = INSTRUCTION,
        requiresSelection = true,
        autoApply = false,
    )
}
