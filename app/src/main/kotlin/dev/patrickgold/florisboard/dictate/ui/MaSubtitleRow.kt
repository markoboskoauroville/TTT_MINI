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
import androidx.compose.runtime.remember
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
    val index = MaReader.currentIndex
    if (index < 0) return
    val words = MaSpeechify.lastWords
    if (index >= words.size) return

    // Pages of a size that FITS, not sentences.
    //
    // Sentences were the wrong unit. One long sentence overflows a three-line box, so the text
    // jumped as the highlight moved through the part that could not be shown — and a very short
    // sentence left the box nearly empty. Neither had anything to do with how much room there is.
    //
    // A page is a run of words that fits, and it changes only when the spoken word leaves it. So
    // the text is still for the length of a whole page and then replaces itself once, which is the
    // slideshow he asked for rather than a crawl.
    val pages = remember(words) { paginate(words.map { it.text }, PAGE_CHARS) }
    val page = remember(pages, index) { pages.firstOrNull { index in it.range } } ?: return

    val text = buildAnnotatedString {
        page.words.forEachIndexed { i, w ->
            if (i > 0) append(" ")
            // Compared by POSITION, never by text. The same word appears many times in a passage
            // and matching on its letters lands on the first one, which is what made the highlight
            // jump to the top of the screen on every "the".
            if (page.range.first + i == index) {
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
            .height(SUBTITLE_HEIGHT)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        contentAlignment = Alignment.TopStart,
    ) {
        Text(
            text = text,
            fontSize = 17.sp,
            lineHeight = 23.sp,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** One screenful of subtitle: the words, and where they sit in the whole passage. */
private data class Page(val words: List<String>, val range: IntRange)

/**
 * Splits a passage into pages that fit the box.
 *
 * By characters rather than by words, because what fills three lines is a number of letters, not a
 * number of words — "a" and "responsibility" occupy very different amounts of it.
 *
 * A word is never split across pages: a page takes whole words until the next one would not fit.
 * Computed once per passage and remembered, so scrolling the highlight costs nothing.
 */
private fun paginate(words: List<String>, perPage: Int): List<Page> {
    if (words.isEmpty()) return emptyList()
    val pages = mutableListOf<Page>()
    var start = 0
    var length = 0
    for (i in words.indices) {
        val add = words[i].length + if (length == 0) 0 else 1
        if (length + add > perPage && i > start) {
            pages.add(Page(words.subList(start, i), start until i))
            start = i
            length = words[i].length
        } else {
            length += add
        }
    }
    if (start < words.size) pages.add(Page(words.subList(start, words.size), start until words.size))
    return pages
}

/**
 * Roughly what three lines hold at 17sp on his phone.
 *
 * Deliberately a little under, so a page never overflows into the ellipsis — a page that is cut off
 * is a page whose last words are never highlighted, which is worse than one that is not quite full.
 */
private const val PAGE_CHARS = 105

/**
 * Matching the composer he reads beside, so the two read as one interface.
 *
 * Fixed rather than wrapping to fit: a box that grew with the text would change the keyboard's
 * height between one page and the next, with his thumb already moving towards a key.
 */
private val SUBTITLE_HEIGHT = 96.dp

private val MaSubtitleLit = Color(0xFFE8B15C)
private val MaSubtitleDim = Color(0xFF8A8A8A)

/** The composer's own dark fill, so the box belongs to the screen rather than sitting on it. */
private val MaSubtitleBackground = Color(0xFF1E1E20)
