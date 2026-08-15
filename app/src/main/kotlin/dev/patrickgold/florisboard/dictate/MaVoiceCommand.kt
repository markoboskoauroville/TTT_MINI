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
 * Turns a finished transcription into a button press, when that is plainly what it was.
 *
 * ### The rule, and why it is this narrow
 *
 * A whole transcription reading exactly **press** and **one more word** is a command. Anything else
 * is text. Not a sentence containing "press send" somewhere, not three words, not a phrase with the
 * word buried in it — the entire result, and two words long.
 *
 * That narrowness is the safety. Dictated text is the thing this app exists to produce, and a rule
 * that ate part of a sentence would lose work that cannot be got back by pressing undo, because the
 * words were spoken rather than typed. Two words on their own is something nobody dictates by
 * accident in the middle of writing: it is a thing said to a machine, after a pause, on purpose.
 *
 * ### Nothing is lost when it misses
 *
 * A parsed command is still only an attempt. The caller presses, and if nothing on screen answers
 * to that name the text is written into the field as it always would have been. So the worst case
 * of a wrong guess here is the words appearing, which is exactly what would have happened anyway.
 *
 * ### Croatian too
 *
 * He dictates in both languages and the command has to work in the one he is speaking, or it
 * becomes a feature that only exists in English on a keyboard built for Croatian. `pritisni` is the
 * everyday imperative; `stisni` is what people actually say more often for a button.
 */
object MaVoiceCommand {

    /**
     * The words that mean "press this".
     *
     * Compared in lowercase. Kept as a small list rather than a setting because a setting for this
     * would be a text field whose wrong answer is silent: the command simply stops working and
     * there is nothing on screen to say why.
     */
    private val VERBS = setOf("press", "pritisni", "stisni", "klikni")

    /**
     * Trailing marks the transcriber adds that were never spoken.
     *
     * AssemblyAI punctuates, so "press send" comes back as "Press send." far more often than not.
     * A full stop is not part of the word and must not be part of the search.
     */
    private const val TRIM = " \t\n.,!?;:\"'\u2019\u201C\u201D"

    /**
     * The one word after the verb, or null when this is ordinary text.
     *
     * Returns null for anything that is not exactly two words, so a longer sentence is never
     * touched. The returned word keeps its own spelling and case as spoken; matching it against a
     * button happens later and is case-insensitive there.
     */
    fun targetIn(text: String): String? {
        val cleaned = text.trim().trim(*TRIM.toCharArray())
        if (cleaned.isEmpty()) return null
        val words = cleaned.split(Regex("\\s+"))
        if (words.size != 2) return null
        val verb = words[0].trim(*TRIM.toCharArray()).lowercase()
        if (verb !in VERBS) return null
        val target = words[1].trim(*TRIM.toCharArray())
        return target.ifBlank { null }
    }

    /**
     * What to look for on screen, best guess first.
     *
     * A spoken word is matched against the terms he has already taught, by face and by term, so
     * saying "stop" finds the target labelled `stop` and searches for `Stop responding` — the long
     * name he never has to say. The spoken word itself goes last as a fallback, so a button that
     * was never taught can still be pressed by its own name.
     *
     * Spacers and unticked terms are absent: a spacer has no name, and a term he switched off is a
     * term he asked not to be used.
     */
    fun candidatesFor(spoken: String, targets: List<MaMagicTargets.Target>): List<String> {
        val wanted = spoken.lowercase()
        val taught = targets
            .filter { it.enabled && !it.isSpacer && it.term.isNotBlank() }
            .filter { it.face.lowercase() == wanted || it.term.lowercase() == wanted }
            .map { it.term }
        return (taught + spoken).distinct()
    }
}
