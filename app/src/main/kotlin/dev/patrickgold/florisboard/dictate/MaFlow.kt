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
            "Keep it as short as it can be while still carrying the reason.\n\n" +
            // THE VOICE, from MANTRA_MANIFEST/modules/prose-voice.md. The rules above were written
            // before that module existed and said most of this in other words; these are the parts
            // it names that were missing.
            "Short paragraphs of one or two sentences, with white space between them. Almost never " +
            "three.\n\n" +
            "Say the thing, then the honest counterweight to it. The qualification is what makes " +
            "the claim trustworthy.\n\n" +
            "Witness rather than assert: say what was seen, not what is true in general.\n\n" +
            "One image, carried through. Never stack metaphors.\n\n" +
            "The principle before the request, so an ask arrives already justified.\n\n" +
            "Practical facts bare: time, link, place, with no apology and no padding.\n\n" +
            "End by taking pressure off the reader rather than adding it.\n\n" +
            "No exclamation marks. No emoji. No bullet points. No dashes. Written to one person as " +
            "\"you\", never to \"everyone\".\n\n" +
            "Normal sentence capitalisation, a capital after every full stop, correct " +
            "apostrophes.\n\n" +
            "Change the shape only, never the meaning. No new facts, no invented details, no " +
            "softening of something said firmly. Three rambling sentences become the same three " +
            "points in clean prose, not four."

    /**
     * The wording of the instance that carries the checkmark, or empty for the shipped one.
     *
     * The preference holds a whole set of named wordings now rather than a single one, so this is
     * "which of his is in force" rather than "has he written one". [MaPrompts.parse] reads a value
     * written by the old version just as happily, so nothing had to be migrated for this to work.
     */
    fun custom(): String {
        val prefs by FlorisPreferenceStore
        return MaPrompts.activeText(prefs.dictate.maFlowPrompt.get())
    }

    /**
     * The instruction plus the capitalisation rule the chosen style asks for.
     *
     * Appended rather than woven in, so the shipped instruction stays one readable string and the
     * style is a line at the end that can be read on its own. It sits before the language rule that
     * `requestReword` adds, which must stay last of all.
     */
    fun instructionFor(style: String): String = INSTRUCTION + if (style == STYLE_YSHAI) {
        "\n\nWrite it entirely in lower case, including the first word of every sentence and the " +
            "word I. Drop apostrophes from contractions: dont, havent, im. Everything else above " +
            "stays exactly as it is."
    } else {
        ""
    }

    const val STYLE_MARKO = "marko"
    const val STYLE_YSHAI = "yshai"

    fun prompt(): PromptModel = PromptModel(
        id = PromptModel.ID_INSTANT_PROMPT,
        pos = 0,
        name = NAME,
        // His wording if he has written one, the shipped instruction otherwise.
        //
        // Read at press time rather than captured once, so editing the text in settings changes the
        // very next press with nothing to restart.
        // His own wording wins outright, including its capitalisation — if he has written a prompt,
        // the style toggle is not entitled to append rules to it.
        prompt = custom().ifBlank {
            val prefs by FlorisPreferenceStore
            instructionFor(prefs.dictate.maProseStyle.get())
        },
        requiresSelection = true,
        autoApply = false,
    )
}
