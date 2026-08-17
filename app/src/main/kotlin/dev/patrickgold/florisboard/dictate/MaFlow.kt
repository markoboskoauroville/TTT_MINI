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
 * Ctrl+F: rewrite it as him speaking.
 *
 * ### The other half of Ctrl+P
 *
 * [MaProofread] may not touch a word he chose — it fixes what is wrong and stops. This one rewrites,
 * and that difference is why there are two keys rather than one compromise. When he wants the commas
 * fixed he wants only the commas fixed; when he presses this he is asking for the words to move.
 *
 * ### The instruction is his, word for word
 *
 * It was written by Marko and is shipped exactly as he gave it, down to the sentence order. That is
 * not deference for its own sake: the output of this key goes out **as his own words, under his own
 * name**, so a prompt smoothed into something more standard-sounding would produce messages that
 * read like anybody. The point of the key is the opposite.
 *
 * If it needs changing, change it to what he says next — do not improve it.
 *
 * ### What still guards it
 *
 * The instruction does not say "return only the text", and it is left that way on purpose. §51
 * catches the failure it would have prevented: a reply far longer than its input is refused and
 * shown rather than typed into his message. A guard on the output beats a line in the prompt,
 * because a model can ignore the line and cannot ignore the guard.
 */
object MaFlow {

    /** Shown beside the spinning dust while it runs, so the two keys are told apart mid-flight. */
    const val NAME = "Better flow"

    /**
     * His prompt, word for word.
     *
     * Written by Marko and shipped exactly as given. It is a voice, and a voice is not something to
     * paraphrase into something more standard-sounding — the whole purpose of the key is that the
     * result is his to send as his own.
     *
     * The rewording path appends the text to work on after a blank line, so nothing here has to
     * announce it. An earlier version ended on "Text to rewrite:" and that line was mine, not his;
     * it is gone with the rest of my edits to his words.
     */
    const val INSTRUCTION =
        "Rewrite the text as me speaking. I will send it myself, as my own words, so never write " +
            "anything I would not say.\n\n" +
            "Warm and direct. Short sentences. No bullet points, no dashes, no headings, no bold. " +
            "It should read like a person talking, not a document. Say the thing, then say why, " +
            "then stop.\n\n" +
            "Give the reason behind an instruction, not just the instruction. People make better " +
            "decisions when they know what something is for. When something is their decision, " +
            "hand it to them properly and mean it. When it is mine, say so.\n\n" +
            "Own mistakes plainly and early, in one sentence, without apologising twice. If " +
            "someone caught the mistake, say they caught it.\n\n" +
            "Never thank people for something they have not done yet. Never write a farewell that " +
            "assumes I am in the room with them. If the message needs an ending, make it useful, " +
            "like where I can be reached, not ceremonial.\n\n" +
            "Warm does not mean soft. If something will not work, say it will not work, then offer " +
            "the version that will.\n\n" +
            "Keep it as short as it can be while still carrying the reason."

    /** What he has written in settings, or empty. */
    fun custom(): String {
        val prefs by FlorisPreferenceStore
        return prefs.dictate.maFlowPrompt.get()
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
