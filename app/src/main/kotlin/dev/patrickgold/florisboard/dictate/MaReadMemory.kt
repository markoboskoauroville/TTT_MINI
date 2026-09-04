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
 * WHAT HAS ALREADY BEEN SPOKEN, AND WHAT IS NOT WORTH SPEAKING.
 *
 * ### Why the passage-level memory was not enough
 *
 * The reader already refused to read the same PASSAGE twice. He scrolled up while watching, and it
 * read old text anyway — because a scroll gives a different WINDOW onto the same words. Half a screen
 * he has heard plus half a screen he has not is a passage nobody has seen before, and the old guard
 * waves it through.
 *
 * **So the unit of memory has to be the unit of speech.** A sentence is what gets spoken, so a
 * sentence is what gets remembered. Everything he has heard is kept, and any sentence in that set is
 * dropped before synthesis — which means scrolling back over read text is silent, and a new paragraph
 * halfway up the screen is read on its own.
 *
 * ### The cost, which is the point
 *
 * Remembering sentences rather than passages is a bigger set and a slower check. Both are trivial at
 * the scale of a chat — a long session is thousands of sentences, and a hash lookup does not care —
 * and the alternative is the thing he called annoying.
 *
 * ### Held in memory, not in a file
 *
 * He asked for a temporary file. This is a set that lives as long as the reading and is cleared by
 * `stop`, which is the same lifetime a temporary file would have been given and one fewer thing to
 * clean up. **If a reading should ever survive the keyboard being killed, that is when this becomes a
 * file** — and it would be a real change, because it would also need to survive being wrong.
 */
object MaReadMemory {

    /** Every sentence spoken in this reading, normalised. */
    private val spoken = mutableSetOf<String>()

    /**
     * Compared with letters only, for the reason the loop guard uses: a re-rendered line wraps
     * differently and a timestamp ticks, and neither makes a sentence new.
     */
    fun normalise(text: String): String =
        text.lowercase().map { if (it.isLetter()) it else ' ' }
            .joinToString("").split(' ').filter { it.isNotBlank() }.joinToString(" ")

    /**
     * PHRASES THAT MEAN "MORE IS COMING", not text to read.
     *
     * He photographed them: *Thought process*, *Still working on it…*, *Identifying each image in the
     * sequence*. They are the app telling him it is busy — and reading them aloud is noise, while
     * treating them as the end of the text is worse, because they are the strongest possible signal
     * that it is not.
     *
     * Matched on a normalised CONTAINS rather than equality, because they arrive with a chevron, a
     * spinner glyph or a count beside them and none of that is stable.
     *
     * Deliberately short. **A list like this is a liability**: every entry is a phrase this app
     * refuses to read, and a careless addition silences something he wanted. These four are the ones
     * he showed me.
     */
    private val WAITING_CUES = listOf(
        "thought process",
        "still working on it",
        "identifying each image",
        "running command",
    )

    /** True if [line] is one of the waiting cues rather than something to read. */
    fun isWaitingCue(line: String): Boolean {
        val n = normalise(line)
        if (n.isBlank()) return false
        return WAITING_CUES.any { n.contains(it) }
    }

    /**
     * The sentences of [text] that have not been spoken and are not cues, in order.
     *
     * Returns an empty list when there is nothing new, which the caller must treat as "say nothing"
     * rather than "read it all" — the difference between silence and repeating the screen.
     */
    fun unheard(text: String, sentences: List<String>): List<String> {
        return sentences.filter { s ->
            val n = normalise(s)
            n.isNotBlank() && !isWaitingCue(s) && n !in spoken
        }
    }

    /** Records [sentences] as spoken. Called when synthesis succeeds, not when it is requested. */
    fun remember(sentences: List<String>) {
        sentences.forEach { s ->
            val n = normalise(s)
            if (n.isNotBlank()) spoken.add(n)
        }
    }

    /** True if the text is nothing but cues — a screen that is only telling him to wait. */
    fun onlyCues(sentences: List<String>): Boolean =
        sentences.isNotEmpty() && sentences.all { isWaitingCue(it) || normalise(it).isBlank() }

    /** How many sentences are remembered. For the log, and for a test that asserts it grows. */
    fun size(): Int = spoken.size

    /** Forgotten when the reading stops, the same lifetime the passage memory has. */
    fun clear() = spoken.clear()
}
