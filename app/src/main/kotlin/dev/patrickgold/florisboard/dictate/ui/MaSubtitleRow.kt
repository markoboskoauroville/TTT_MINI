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

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.patrickgold.florisboard.dictate.MaReader
import dev.patrickgold.florisboard.dictate.MaSpeechify

/**
 * The subtitle row: the sentence being read, with the word being said lit up.
 *
 * ### Why a row of its own rather than the spacebar
 *
 * The spacebar version borrows a key that only exists while the letter keyboard is shown, so it
 * disappears exactly when he is reading a screen with the keyboard folded away — which is most of
 * the time. This row belongs to the reader, not to the keyboard, so it is there whenever the reader
 * is.
 *
 * ### One sentence, not the whole passage
 *
 * A whole screen of text on one line is unreadable, and following a moving highlight through it is
 * worse than not having it. A sentence is the unit the ear already works in: it arrives, it is
 * read, the next one replaces it.
 *
 * ### Nothing when nothing is being read
 *
 * Between sentences and while idle it draws nothing at all — not an empty bar. A band of blank
 * space that appears and disappears is more distracting than the words it was meant to carry.
 */
@Composable
fun MaSubtitleRow(modifier: Modifier = Modifier) {
    val word = MaReader.currentWord
    if (word.isEmpty()) return

    val words = MaSpeechify.lastWords
    val index = words.indexOfFirst { it.text == word }
    if (index < 0) return
    val sentence = sentenceContaining(words.map { it.text }, index)
    if (sentence.first.isEmpty()) return

    // One block of wrapped text, not a scrolling line.
    //
    // The first version scrolled horizontally to keep the current word in view, and the effect was
    // that every word shifted the whole line — the text appeared to vibrate, and the highlight ran
    // off the edge on a long sentence. Wrapping instead means nothing moves except the colour.
    //
    // Built as one AnnotatedString rather than a Row of Texts, because a Row cannot wrap: it would
    // push the sentence sideways again, which is the same bug wearing different clothes.
    val text = buildAnnotatedString {
        sentence.first.forEachIndexed { i, w ->
            if (i > 0) append(" ")
            if (i == sentence.second) {
                withStyle(SpanStyle(color = MaSubtitleLit, fontWeight = FontWeight.Bold)) {
                    append(w)
                }
            } else {
                withStyle(SpanStyle(color = MaSubtitleDim)) { append(w) }
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(MaSubtitleBackground)
            // The height of the box he types into, so the two read as one interface. Fixed rather
            // than wrapping to fit, or the keyboard would change height between a short sentence
            // and a long one — with his thumb already moving towards a key.
            .height(SUBTITLE_HEIGHT)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        contentAlignment = Alignment.TopStart,
    ) {
        Text(
            text = text,
            fontSize = 17.sp,
            lineHeight = 23.sp,
            // Three lines hold a long sentence. Past that it is cut rather than scrolled, since a
            // sentence long enough to overflow is one where the highlight matters least.
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Matching the composer he reads beside, so the two read as one interface. */
private val SUBTITLE_HEIGHT = 96.dp

/**
 * The run of words around [index] that forms a sentence, and where [index] sits inside it.
 *
 * Sentences are derived rather than given: Speechify returns a passage as one flat word list
 * whatever its mark types say, so a sentence is the run between one word ending in `.`, `!` or `?`
 * and the next.
 */
private fun sentenceContaining(words: List<String>, index: Int): Pair<List<String>, Int> {
    if (index !in words.indices) return emptyList<String>() to -1
    fun ends(w: String) = w.lastOrNull() in setOf('.', '!', '?')
    var start = index
    while (start > 0 && !ends(words[start - 1])) start--
    var end = index
    while (end < words.lastIndex && !ends(words[end])) end++
    return words.subList(start, end + 1) to (index - start)
}

private val MaSubtitleLit = Color(0xFFE8B15C)
private val MaSubtitleDim = Color(0xFF8A8A8A)

/** The composer's own dark fill, so the box belongs to the screen rather than sitting on it. */
private val MaSubtitleBackground = Color(0xFF1E1E20)
