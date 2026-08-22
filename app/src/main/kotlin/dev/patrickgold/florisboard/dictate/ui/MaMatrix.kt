/*
 * Copyright (C) 2026 Marko Bosko, Mantra Productions
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package dev.patrickgold.florisboard.dictate.ui

import kotlin.random.Random

/**
 * MATRIX: one word, resolving out of falling noise.
 *
 * The word being spoken stands in the middle of the box. Behind it, columns of characters fall and
 * scramble; each letter of the word settles out of that noise a moment before the voice reaches it,
 * so the word assembles itself just ahead of being heard.
 *
 * ### Why this and not the anagram
 *
 * The anagram spelled the word across a tracing of the key grid, and it read as *a clever thing
 * happening to the keyboard* rather than as a word being read: the eye followed the letters
 * travelling and lost the word. **A reading effect has one job — to put the word where the eye
 * already is.** Matrix keeps the part of anagram that worked, which is that the display is not a
 * strip of text, and drops the part that competed with the reading.
 *
 * ### The rule, and why it is here rather than in the composable
 *
 * Everything below is pure: given a word, a random seed and how far through the word the voice has
 * got, it says what each cell shows. No Compose, no Android, no clock of its own — the caller passes
 * the progress it already has. So the whole effect can be walked in Python before anything is built,
 * which for an animation is the only way to test it at all: the alternative is watching it and
 * hoping.
 */
object MaMatrix {

    /**
     * The alphabet the noise is drawn from.
     *
     * Latin letters and digits, not the film's mirrored katakana. Two reasons, and the second is the
     * real one: the katakana would need a font that ships with the app, and — he is dyslexic, and a
     * screen of characters he cannot read even in principle is noise in a sense nobody wanted. These
     * are letters that could plausibly be the word, which is what makes the resolve legible.
     */
    private const val GLYPHS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"

    /** How many rows of falling noise stand behind the word. */
    const val ROWS = 7

    /** The row the word itself occupies: the middle one, where the eye already is. */
    const val WORD_ROW = ROWS / 2

    /**
     * One cell of the display.
     *
     * [settled] is the difference between the word and the noise: a settled cell holds a letter of
     * the word and is drawn bright, an unsettled one holds a random glyph and is drawn dim.
     */
    data class Cell(val char: Char, val settled: Boolean, val dim: Float)

    /**
     * How many letters of [word] have settled at [progress], where progress runs 0f to 1f across the
     * word's spoken length.
     *
     * Deliberately one letter AHEAD of the voice, rounded up. A letter settling exactly as it is
     * spoken arrives too late to be read — the eye needs it a moment early, and the effect is
     * supposed to feel like the word forming for him rather than a caption catching up.
     */
    fun settledCount(word: String, progress: Float): Int {
        if (word.isEmpty()) return 0
        val p = progress.coerceIn(0f, 1f)
        val ahead = (word.length * p) + 1f
        return ahead.toInt().coerceIn(0, word.length)
    }

    /**
     * The word row: settled letters, and noise where the word has not arrived yet.
     *
     * The unsettled cells are drawn from the same alphabet and change every frame, so the tail of
     * the word churns until it lands. The seed makes that churn reproducible, which is what allows
     * a test to assert anything about it at all.
     */
    fun wordRow(word: String, progress: Float, seed: Int): List<Cell> {
        val settled = settledCount(word, progress)
        val rnd = Random(seed)
        return word.mapIndexed { i, c ->
            if (i < settled) {
                Cell(c, settled = true, dim = 1f)
            } else {
                Cell(GLYPHS[rnd.nextInt(GLYPHS.length)], settled = false, dim = 0.55f)
            }
        }
    }

    /**
     * A background row of falling noise, [width] cells wide.
     *
     * [distance] is how far this row is from the word row; the further away, the dimmer, so the eye
     * is pulled to the middle rather than to the edges. **The noise must never compete with the
     * word** — that was the anagram's mistake in a different costume.
     */
    fun noiseRow(width: Int, distance: Int, seed: Int): List<Cell> {
        val rnd = Random(seed)
        val dim = (0.34f - distance * 0.07f).coerceAtLeast(0.06f)
        return (0 until width).map {
            // A gap now and then, so the columns fall in broken streaks rather than a solid block.
            if (rnd.nextInt(100) < 22) {
                Cell(' ', settled = false, dim = 0f)
            } else {
                Cell(GLYPHS[rnd.nextInt(GLYPHS.length)], settled = false, dim = dim)
            }
        }
    }

    /**
     * The whole display for one frame: [ROWS] rows, the word in the middle.
     *
     * [frame] advances the noise; [progress] advances the word. Two separate clocks on purpose — the
     * noise falls at its own speed and must not speed up or slow down with the speaking rate, or the
     * background becomes a second thing to read.
     */
    fun frame(word: String, progress: Float, frame: Int): List<List<Cell>> {
        val width = word.length.coerceAtLeast(1)
        return (0 until ROWS).map { row ->
            if (row == WORD_ROW) {
                wordRow(word, progress, seed = frame)
            } else {
                noiseRow(width, distance = kotlin.math.abs(row - WORD_ROW), seed = frame * 31 + row)
            }
        }
    }
}
