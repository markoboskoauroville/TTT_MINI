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

import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import dev.patrickgold.florisboard.app.FlorisPreferenceStore
import dev.patrickgold.jetpref.datastore.model.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
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
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MaSubtitleRow(modifier: Modifier = Modifier) {
    val index = MaReader.currentIndex
    if (index < 0) return
    val words = MaSpeechify.lastWords
    if (index >= words.size) return

    // How the spoken word is marked, read as Compose state so a change in settings shows on the very
    // next word rather than the next reading.
    val prefs by FlorisPreferenceStore
    val style by prefs.dictate.maReaderStyle.collectAsState()
    val full by prefs.dictate.maReaderFullscreen.collectAsState()
    val litColour by prefs.dictate.maReaderHighlightColor.collectAsState()
    val litBold by prefs.dictate.maReaderHighlightBold.collectAsState()
    val litUnderline by prefs.dictate.maReaderHighlightUnderline.collectAsState()
    val lit = if (litColour == "white") MaSubtitleWhite else MaSubtitleYellow

    // Pages of a size that FITS, not sentences.
    //
    // Sentences were the wrong unit. One long sentence overflows a three-line box, so the text
    // jumped as the highlight moved through the part that could not be shown — and a very short
    // sentence left the box nearly empty. Neither had anything to do with how much room there is.
    //
    // A page is a run of words that fits, and it changes only when the spoken word leaves it. So
    // the text is still for the length of a whole page and then replaces itself once, which is the
    // slideshow he asked for rather than a crawl.
    // Paged for the box it is actually in. A fullscreen box paged for three lines would show a
    // paragraph floating in an empty screen and turn a page every few seconds for no reason.
    val perPage = if (full) FULLSCREEN_CHARS else PAGE_CHARS
    val pages = remember(words, perPage) { paginate(words.map { it.text }, perPage) }
    val page = remember(pages, index) { pages.firstOrNull { index in it.range } } ?: return

    // ONE WORD is its own layout: the word alone, as large as the box will hold.
    //
    // The style Instagram and TikTok captions made ordinary — one word at a time, centred, big
    // enough to read at arm's length. It is the only one of the five that does not show its
    // neighbours, which is exactly why it is worth having: there is nothing to read ahead to.
    if (style == "oneword") {
        val word = words.getOrNull(index)?.text.orEmpty()
        SubtitleBox(modifier, full) {
            Text(
                text = word,
                color = lit,
                fontSize = if (full) 64.sp else 34.sp,
                lineHeight = if (full) 70.sp else 40.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        return
    }

    val text = buildAnnotatedString {
        page.words.forEachIndexed { i, w ->
            if (i > 0) append(" ")
            val here = page.range.first + i
            // Compared by POSITION, never by text. The same word appears many times in a passage
            // and matching on its letters lands on the first one, which is what made the highlight
            // jump to the top of the screen on every "the".
            val spoken = here < index
            val current = here == index

            // Every style leaves the LAYOUT alone. Colour, alpha and decoration change nothing
            // about where a word sits; only weight would, and only the styles that ask for it use
            // it. That is the whole reason these five and not five others.
            val span = when (style) {
                // TYPEWRITER — the page arrives as it is spoken. What has not been said yet is
                // fully transparent rather than absent, so it still occupies its space and the
                // words that have arrived never move to make room.
                "typewriter" -> when {
                    current -> SpanStyle(color = lit)
                    spoken -> SpanStyle(color = MaSubtitleWhite)
                    else -> SpanStyle(color = Color.Transparent)
                }
                // KARAOKE — the line fills up behind the voice. Said words keep the accent, the
                // word being said is white, and what is coming is dim. A bar in a song, in text.
                "karaoke" -> when {
                    // White and NOT bold. Bold would widen the word, and the line would re-flow as
                    // the fill passed through it — in a style whose whole effect is a steady front
                    // edge moving along a still line, that is the one thing that ruins it. Three
                    // colours are enough: said, saying, coming.
                    current -> SpanStyle(color = MaSubtitleWhite)
                    spoken -> SpanStyle(color = MaSubtitleYellow)
                    else -> SpanStyle(color = MaSubtitleDim)
                }
                // SPOTLIGHT — everything is turned down and one word is left lit. For reading
                // something difficult, where the point is to stop the eye running ahead.
                "spotlight" -> when {
                    current -> SpanStyle(color = lit)
                    else -> SpanStyle(color = MaSubtitleShadow)
                }
                // HIGHLIGHT — the plain one, and the default. One word marked, the page readable.
                else -> if (current) {
                    SpanStyle(
                        color = lit,
                        fontWeight = if (litBold) FontWeight.Bold else null,
                        textDecoration = if (litUnderline) TextDecoration.Underline else null,
                    )
                } else {
                    SpanStyle(color = MaSubtitleWhite)
                }
            }
            withStyle(span) { append(w) }
        }
    }

    SubtitleBox(modifier, full) {
        Text(
            text = text,
            fontSize = if (full) 26.sp else 17.sp,
            lineHeight = if (full) 36.sp else 23.sp,
            maxLines = if (full) 9 else 3,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * The box every style is drawn in, so the five cannot drift apart.
 *
 * ### Fullscreen, and the rule taken from MA Reader
 *
 * Full screen there is done by hiding **every** child of the view except the text, so that anything
 * added later is hidden by default and has to argue its way back on. The version before it named
 * the things to hide, which is a list, and lists go stale — which is exactly how the player bar and
 * the voice strip survived into full screen.
 *
 * Here the same idea costs one number: the box grows to cover the keys, and nothing else is asked
 * to move or hide. Whatever is added to the keyboard later is behind it automatically.
 *
 * ### Two gestures, and neither is a button
 *
 * Tap skips the sentence. **Long press toggles full screen.** No control is drawn for either,
 * because a caption with buttons on it is no longer a caption — and both gestures are on the one
 * element that is already under the thumb while reading.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SubtitleBox(
    modifier: Modifier,
    full: Boolean,
    content: @Composable () -> Unit,
) {
    val prefs by FlorisPreferenceStore
    val scope = rememberCoroutineScope()
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(MaSubtitleBackground)
            .height(if (full) FULLSCREEN_HEIGHT else SUBTITLE_HEIGHT)
            .combinedClickable(
                // The commonest thing he wants while listening is "not this bit". Reaching for the
                // speaker key pauses; reaching here moves on.
                onClick = { MaReader.skipSentence() },
                onLongClick = {
                    scope.launch { prefs.dictate.maReaderFullscreen.set(!full) }
                },
            )
            .padding(horizontal = 16.dp, vertical = if (full) 18.dp else 10.dp),
        contentAlignment = if (full) Alignment.Center else Alignment.TopStart,
    ) {
        content()
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
 * Roughly what nine lines hold at 26sp.
 *
 * Deliberately a little under, like its small sibling: a page cut off by the ellipsis is a page
 * whose last words are never highlighted, which is worse than one that is not quite full.
 */
private const val FULLSCREEN_CHARS = 460

/** Tall enough to cover the keys, so the passage is the only thing on screen. */
private val FULLSCREEN_HEIGHT = 330.dp

/**
 * Matching the composer he reads beside, so the two read as one interface.
 *
 * Fixed rather than wrapping to fit: a box that grew with the text would change the keyboard's
 * height between one page and the next, with his thumb already moving towards a key.
 */
private val SUBTITLE_HEIGHT = 96.dp

/** The sand this app uses everywhere for a lit thing. */
private val MaSubtitleYellow = Color(0xFFE8B15C)

/** Plain white, for the page and for the second highlight colour. */
private val MaSubtitleWhite = Color(0xFFFFFFFF)

/** What is coming, in karaoke: present and clearly not yet said. */
private val MaSubtitleDim = Color(0xFF8A8A8A)

/**
 * Spotlight's everything-else. Very dark, but not invisible.
 *
 * Left readable on purpose: the point of spotlight is to stop the eye running ahead, not to hide
 * the sentence. Turning it to the background colour would make it a one-word display, and there is
 * already one of those.
 */
private val MaSubtitleShadow = Color(0xFF3A3A3C)

/** The composer's own dark fill, so the box belongs to the screen rather than sitting on it. */
private val MaSubtitleBackground = Color(0xFF1E1E20)
