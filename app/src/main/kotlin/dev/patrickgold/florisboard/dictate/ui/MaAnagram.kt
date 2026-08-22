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

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.patrickgold.florisboard.app.FlorisPreferenceStore
import dev.patrickgold.florisboard.dictate.MaLanguage
import dev.patrickgold.florisboard.dictate.MaReader
import dev.patrickgold.florisboard.dictate.MaSpeechify

/**
 * ANAGRAM: the keyboard spells the word being read.
 *
 * The other five effects mark the word in a strip of text. This one leaves the strip alone and uses
 * the KEYBOARD as the display: the keys that are not needed fade away, and the letters that are
 * needed leave their home positions and travel to the middle of the keyboard, where they land in
 * order and spell the word out loud alongside the voice.
 *
 * ### What is actually moving
 *
 * Not the keys. The keyboard's key grid is measured and laid out by the layout engine every frame,
 * and a key cannot be re-parented mid-frame without fighting it for control of its own position.
 *
 * So this draws an OVERLAY across the key area, with the letters positioned from the same grid the
 * keys use, and a scrim underneath that dims the real keyboard. The letter that flies is a drawn
 * copy leaving from exactly where the real one sits. **He sees the keyboard come alive; what is
 * really happening is that a faithful tracing of it does.**
 *
 * Saying that plainly matters, because the difference shows up in one place: while the effect runs,
 * the keys underneath are not pressable. That is correct here — he is listening, not typing — but it
 * is a consequence, not a design.
 *
 * ### Everything below is pure
 *
 * Where each letter starts, where it lands, and which keys fade: strings and numbers in, positions
 * out, no Compose and no Android. `scripts/test_anagram.py` walks it before anything is built.
 */
object MaAnagram {

    /**
     * The letter rows, as they sit on his keyboard.
     *
     * QWERTZ for Croatian, QWERTY for English — the same pair the language badge already switches
     * the subtype between, so the tracing matches the keys underneath it in both. They differ in one
     * swap, Y and Z, and getting it backwards would send a letter across the whole keyboard to land
     * on the wrong key.
     */
    val QWERTY: List<String> = listOf("qwertyuiop", "asdfghjkl", "zxcvbnm")
    val QWERTZ: List<String> = listOf("qwertzuiop", "asdfghjkl", "yxcvbnm")

    fun rowsFor(language: String): List<String> =
        if (language.lowercase().startsWith("hr")) QWERTZ else QWERTY

    /** A place on the key grid, in fractions of the keyboard's width and height. */
    data class Spot(val x: Float, val y: Float)

    /**
     * Where a letter's key sits, as a fraction of the whole key area.
     *
     * Rows are centred on their own row height, and each row is centred horizontally, because the
     * middle and bottom rows hold fewer keys than the top one and a left-aligned tracing would drift
     * further from the real keys with every row down.
     *
     * Null for anything not on the letter rows — a digit, a comma, a space, an accented letter with
     * no key of its own. Those are handled by [plan], which lands them in place without a flight.
     */
    fun homeOf(letter: Char, rows: List<String>): Spot? {
        val c = letter.lowercaseChar()
        for ((r, row) in rows.withIndex()) {
            val col = row.indexOf(c)
            if (col < 0) continue
            val rowCount = rows.size
            // Centred within the row: (col + 0.5) of this row's keys, offset by the row's own
            // indent so a 9-key row sits under the middle of a 10-key one.
            val x = (col + 0.5f) / row.length
            val y = (r + 0.5f) / rowCount
            return Spot(x, y)
        }
        return null
    }

    /** Where the nth letter of a word lands, spread across the middle of the keyboard. */
    fun landingOf(position: Int, wordLength: Int): Spot {
        if (wordLength <= 0) return Spot(0.5f, 0.5f)
        // Kept inside the middle 90% so a long word does not touch the edges, and always on the
        // middle row's line, which is where the eye already is.
        val span = 0.9f
        val start = (1f - span) / 2f
        val x = if (wordLength == 1) 0.5f else start + span * position / (wordLength - 1)
        return Spot(x, 0.5f)
    }

    /** One letter's journey: what it is, where it starts, where it lands, and when it leaves. */
    data class Flight(
        val letter: Char,
        val from: Spot?,
        val to: Spot,
        /** 0f..1f — a stagger, so the word assembles left to right rather than arriving at once. */
        val delayFraction: Float,
    )

    /**
     * The whole word as flights.
     *
     * A letter with no key of its own has a null [Flight.from]: it fades in where it lands rather
     * than flying from nowhere. Croatian is full of them — č, ć, š, ž, đ — and a word containing one
     * must still spell out completely, so they arrive rather than being dropped.
     */
    fun plan(word: String, rows: List<String>): List<Flight> {
        val letters = word.trim()
        if (letters.isEmpty()) return emptyList()
        return letters.mapIndexed { i, ch ->
            Flight(
                letter = ch,
                from = homeOf(ch, rows),
                to = landingOf(i, letters.length),
                delayFraction = if (letters.length == 1) 0f else i.toFloat() / letters.length,
            )
        }
    }

    /**
     * The keys that stay visible: the ones this word needs, and nothing else.
     *
     * He asked for the unneeded buttons to be deleted. They are dimmed rather than removed — a key
     * grid that changed its contents would change its measured width and the whole keyboard would
     * jump between words, which is the opposite of the stillness the effect is for.
     */
    fun keysInUse(word: String, rows: List<String>): Set<Char> =
        word.lowercase().filter { ch -> rows.any { ch in it } }.toSet()

    /** How long one word's assembly takes, from the first letter leaving to the last landing. */
    const val FLIGHT_MS = 260

    /** The stagger between letters, as a share of a word's total time. */
    const val STAGGER_MS = 45
}

/**
 * The overlay itself, drawn across the key area while the anagram effect is reading.
 *
 * Sits in a Box with the keyboard, above it, so the keys are traced rather than replaced. Draws
 * nothing at all when nothing is being read, which is why it can live in the layout unconditionally
 * instead of being switched in and out and changing the keyboard's height mid-reading.
 */
@Composable
fun MaAnagramOverlay(modifier: Modifier = Modifier) {
    val prefs by FlorisPreferenceStore
    val style by prefs.dictate.maReaderStyle.collectAsState()
    val index = MaReader.currentIndex
    if (style != "anagram" || index < 0) return
    val word = MaSpeechify.lastWords.getOrNull(index)?.text?.trim().orEmpty()
    if (word.isEmpty()) return

    val rows = remember(MaLanguage.active()) { MaAnagram.rowsFor(MaLanguage.active()) }
    val flights = remember(word, rows) { MaAnagram.plan(word, rows) }

    // One clock per word, restarted when the word changes. `animateFloatAsState` per letter would
    // be a dozen animations to keep in step; one progress value is the same picture and cannot
    // drift between letters.
    val progress = remember(word) { Animatable(0f) }
    LaunchedEffect(word) {
        progress.snapTo(0f)
        progress.animateTo(
            1f,
            animationSpec = tween(
                durationMillis = MaAnagram.FLIGHT_MS + MaAnagram.STAGGER_MS * flights.size,
                easing = FastOutSlowInEasing,
            ),
        )
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            // The scrim, which is the "deleted" keys. Nearly opaque rather than opaque: a trace of
            // the keyboard underneath is what makes it read as the keyboard coming alive rather
            // than as a black panel with letters on it.
            .background(Color.Black.copy(alpha = 0.88f)),
    ) {
        val w = maxWidth
        val h = maxHeight
        flights.forEachIndexed { i, flight ->
            // Each letter's own share of the single clock: it waits its turn, then flies.
            val startAt = (MaAnagram.STAGGER_MS * i).toFloat() /
                (MaAnagram.FLIGHT_MS + MaAnagram.STAGGER_MS * flights.size)
            val span = MaAnagram.FLIGHT_MS.toFloat() /
                (MaAnagram.FLIGHT_MS + MaAnagram.STAGGER_MS * flights.size)
            val t = ((progress.value - startAt) / span).coerceIn(0f, 1f)
            val from = flight.from
            val x = if (from == null) flight.to.x else from.x + (flight.to.x - from.x) * t
            val y = if (from == null) flight.to.y else from.y + (flight.to.y - from.y) * t
            Text(
                text = flight.letter.toString(),
                color = MaAnagramSand.copy(alpha = t.coerceAtLeast(0.15f)),
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.offset(
                    x = w * x - 8.dp,
                    y = h * y - 14.dp,
                ),
            )
        }
    }
}

/**
 * The sand the keys are lettered in.
 *
 * The same value the feature row and the switchboard use, declared here rather than reached for:
 * both of those are `private`, and making one of them public so a third file could borrow it would
 * put the colour's home somewhere arbitrary. Three copies of one hex is a smell; the cure is a theme
 * file, which is a bigger change than this effect deserves.
 */
private val MaAnagramSand = androidx.compose.ui.graphics.Color(0xFFE8B15C)
