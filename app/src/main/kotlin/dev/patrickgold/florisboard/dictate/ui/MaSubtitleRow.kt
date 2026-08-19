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

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
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
    // Recomposes as the word changes, because `currentWord` is the Compose state the ticker writes.
    // The position is not read here: the word is what changed, and asking the player again would
    // race the ticker for no gain.
    val word = MaReader.currentWord
    if (word.isEmpty()) return

    val words = MaSpeechify.lastWords
    val index = words.indexOfFirst { it.text == word }
    if (index < 0) return
    val sentence = sentenceContaining(words.map { it.text }, index)
    if (sentence.first.isEmpty()) return

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        sentence.first.forEachIndexed { i, w ->
            val isCurrent = i == sentence.second
            Text(
                text = w,
                fontSize = 17.sp,
                // Sand and bold for the word being said, dim for the rest. Two states only: a
                // gradient of "recently said" would make the eye chase the fade instead of resting
                // on the word.
                color = if (isCurrent) MaSubtitleLit else MaSubtitleDim,
                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                modifier = Modifier.padding(end = 6.dp),
            )
        }
    }
}

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
