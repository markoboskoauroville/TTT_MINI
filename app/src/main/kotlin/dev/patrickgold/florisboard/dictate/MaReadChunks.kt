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
 * How a passage is cut up so that reading starts at once.
 *
 * ### The delay, and what is left of it
 *
 * The first cause was the key ring: it restarted at key one on every read, so every dead or
 * throttled key ahead of the good one cost a full network round trip before a word could be spoken.
 * That is fixed — the ring remembers what worked — and it was the larger half of the twenty seconds.
 *
 * What remains is unavoidable and is what this file is for: **a whole screenful of text takes as
 * long to synthesise as a whole screenful of text.** No amount of key-ring cleverness changes that.
 * The only way to start sooner is to ask for less.
 *
 * ### His idea, and it is the right one
 *
 * Send **one sentence**, start playing it, and while it plays send the next **two**; while those
 * play send **four**. The wait before the first word is the wait for one sentence — about a second —
 * and every request after that happens behind audio that is already playing.
 *
 * The growth matters in both directions:
 *
 *  - **Small at the start** because that is the only part he waits through.
 *  - **Bigger later** because each request has a fixed cost of its own — a connection, a round trip,
 *    the model warming up — and forty one-sentence requests would pay that forty times and start
 *    stuttering between them. By the time the chunks are large there is a minute of audio in hand
 *    and the network has all the time it needs.
 *
 * ### Why it stops doubling
 *
 * At [MAX_CHUNK] it stops. Past that the chunk takes longer to synthesise than the previous chunk
 * takes to play, which is the exact moment prefetching stops being ahead and starts being behind —
 * and a gap in the middle of a sentence is worse than a wait at the start, because he cannot tell it
 * from the reader having failed.
 *
 * Pure and separate so the growth can be walked in Python before anything is built.
 */
object MaReadChunks {

    /** The first chunk. One sentence: the whole point is that this is the only wait. */
    const val FIRST_CHUNK = 1

    /**
     * The largest chunk, in sentences.
     *
     * Sixteen is roughly a minute of speech, which is far more than the few seconds a request takes.
     * Doubling past it buys nothing and risks a chunk that outruns its own head start.
     */
    const val MAX_CHUNK = 16

    /**
     * Splits [text] into sentences.
     *
     * A sentence ends at `.`, `!` or `?` followed by space. Deliberately the same rule
     * `MaReader.skipSentence` uses to find the end of a sentence — **two different ideas of where a
     * sentence ends would put the chunk boundaries somewhere the skip could never land**, and the
     * two would drift apart the first time either was improved.
     *
     * Text with no sentence end at all comes back as one sentence, which is correct: a screenful of
     * list items with no full stops is one thing to say.
     */
    fun sentences(text: String): List<String> {
        val out = mutableListOf<String>()
        val sb = StringBuilder()
        var i = 0
        while (i < text.length) {
            val c = text[i]
            sb.append(c)
            val ends = c == '.' || c == '!' || c == '?'
            val nextIsSpace = i + 1 >= text.length || text[i + 1].isWhitespace()
            if (ends && nextIsSpace) {
                val piece = sb.toString().trim()
                if (piece.isNotEmpty()) out.add(piece)
                sb.setLength(0)
            }
            i++
        }
        val tail = sb.toString().trim()
        if (tail.isNotEmpty()) out.add(tail)
        return out
    }

    /**
     * The chunks, as ranges over the sentence list: 1, 2, 4, 8, 16, 16, 16…
     *
     * Ranges rather than copies of the text, so the caller can map a chunk back to which sentences
     * it holds without comparing strings.
     */
    fun plan(sentenceCount: Int): List<IntRange> {
        if (sentenceCount <= 0) return emptyList()
        val out = mutableListOf<IntRange>()
        var start = 0
        var size = FIRST_CHUNK
        while (start < sentenceCount) {
            val end = (start + size - 1).coerceAtMost(sentenceCount - 1)
            out.add(start..end)
            start = end + 1
            size = (size * 2).coerceAtMost(MAX_CHUNK)
        }
        return out
    }

    /** The text of one chunk, joined the way it will be spoken. */
    fun textOf(sentences: List<String>, range: IntRange): String =
        sentences.slice(range).joinToString(" ")
}
