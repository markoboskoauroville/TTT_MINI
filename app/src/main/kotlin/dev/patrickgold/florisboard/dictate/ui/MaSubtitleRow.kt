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

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
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
    // A chosen colour wins; the yellow-or-white pair is what it falls back to.
    val litHex by prefs.dictate.maReaderHighlightHex.collectAsState()
    val lit = parseHex(litHex)
        ?: if (litColour == "white") MaSubtitleWhite else MaSubtitleYellow
    // One size, both views. Full screen shows MORE, not BIGGER — the two were tied together and he
    // could not have the whole page without also having large type he had not asked for.
    val fontSp by prefs.dictate.maReaderFontSize.collectAsState()
    val fontSize = fontSp.coerceIn(10, 40).sp
    val lineHeight = (fontSp.coerceIn(10, 40) * 1.35f).sp
    // The void draws its own box, so it needs its own scope to toggle full screen.
    val scope = rememberCoroutineScope()

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
    // In the small box a page is three lines; full screen it is ONE line, because the full-screen
    // reader scrolls line by line and a "page" there is the unit that jumps to the top. Scaled by
    // the font, since a bigger type fits fewer letters across the same width.
    val perPage = if (full) (LINE_CHARS * 17 / fontSp).coerceIn(14, 90) else PAGE_CHARS
    val pages = remember(words, perPage) { paginate(words.map { it.text }, perPage) }
    val page = remember(pages, index) { pages.firstOrNull { index in it.range } } ?: return

    // THE BLACK VOID.
    //
    // Full screen, true black, and one word at a time as large as it will go. Nothing else is drawn
    // — no page, no neighbours, no box edge, no background lighter than the void itself.
    //
    // This is the style built for the way he actually reads. He is dyslexic; a page of text is work
    // and a single word is not. With nothing else on the screen there is nothing to track back to,
    // nothing to skip ahead to, and no line to lose — the word arrives, is read, and is replaced.
    //
    // Long press collapses it back to the small subtitle box, the same gesture that opened it.
    if (style == "void") {
        val word = words.getOrNull(index)?.text.orEmpty()
        Box(
            modifier = modifier
                .fillMaxWidth()
                .then(if (full) Modifier.fillMaxHeight() else Modifier.height(SUBTITLE_HEIGHT))
                // True black rather than the box's near-black. The void is the point: anything
                // lighter draws an edge, and an edge is a second thing on screen.
                .background(Color.Black)
                .combinedClickable(
                    onClick = { MaReader.skipSentence() },
                    onLongClick = { scope.launch { prefs.dictate.maReaderFullscreen.set(!full) } },
                )
                .padding(horizontal = 12.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = word,
                color = lit,
                // As large as the void will hold. Sized from his own setting so the whole app still
                // answers to one number, but multiplied hard, because a single word on a black
                // screen can take room that a page never could.
                fontSize = (if (full) fontSp * 5 else fontSp * 2).coerceIn(24, 120).sp,
                lineHeight = (if (full) fontSp * 5.4f else fontSp * 2.2f).coerceIn(28f, 130f).sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (full) {
                MaCloseCorner { scope.launch { prefs.dictate.maReaderFullscreen.set(false) } }
            }
            MaKeyCorner()
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

    if (full) {
        // FULL SCREEN — the whole passage, with the line being read at the top.
        //
        // This is MA Reader's behaviour and it is the reason full screen is worth having at all. The
        // text does not page: it is all there, in short lines, and the line holding the spoken word
        // is scrolled to the TOP of the box. So his eye rests in one place while the sentences come
        // up to meet it, and everything below that line is what is coming — which he can glance at
        // without losing the place, because the place does not move.
        //
        // `scrollToItem` and not `animateScrollToItem`: he cannot watch animated scrolling, and a
        // line that slides into position is exactly the thing that turns his eyes inside out.
        val listState = rememberLazyListState()
        val lineOf = remember(pages) {
            IntArray(words.size).also { arr ->
                pages.forEachIndexed { li, pg -> for (w in pg.range) if (w in arr.indices) arr[w] = li }
            }
        }
        val line = lineOf.getOrElse(index) { 0 }
        LaunchedEffect(line) { runCatching { listState.scrollToItem(line) } }

        SubtitleBox(modifier, full) {
            LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
                itemsIndexed(pages) { li, pg ->
                    Text(
                        text = lineText(pg, index, style, lit, litBold, litUnderline),
                        fontSize = fontSize,
                        lineHeight = lineHeight,
                        // Dimmed once it has been read, so the top of the box is always the live
                        // line and what sits above it is visibly behind.
                        color = if (li < line) MaSubtitleShadow else Color.Unspecified,
                    )
                }
            }
        }
        return
    }

    SubtitleBox(modifier, full) {
        Text(
            text = text,
            fontSize = fontSize,
            lineHeight = lineHeight,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** One line of the full-screen reader, styled by whichever caption style is chosen. */
private fun lineText(
    page: Page,
    index: Int,
    style: String,
    lit: Color,
    litBold: Boolean,
    litUnderline: Boolean,
) = buildAnnotatedString {
    page.words.forEachIndexed { i, w ->
        if (i > 0) append(" ")
        val here = page.range.first + i
        val spoken = here < index
        val current = here == index
        val span = when (style) {
            "typewriter" -> when {
                current -> SpanStyle(color = lit)
                spoken -> SpanStyle(color = MaSubtitleWhite)
                else -> SpanStyle(color = Color.Transparent)
            }
            "karaoke" -> when {
                current -> SpanStyle(color = MaSubtitleWhite)
                spoken -> SpanStyle(color = MaSubtitleYellow)
                else -> SpanStyle(color = MaSubtitleDim)
            }
            "spotlight" -> if (current) SpanStyle(color = lit) else SpanStyle(color = MaSubtitleShadow)
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
/**
 * The way out of full screen, in the corner, barely there.
 *
 * Long press closes it too, and that is the gesture he chose — but a gesture is invisible, and a
 * full-screen view with no visible way out is a trap the first time somebody forgets it. A dark grey
 * X costs nothing to ignore and everything to not have.
 *
 * Deliberately near-invisible: an escape hatch, not a control. Anything brighter would compete with
 * the word being read, which is the only thing on that screen worth looking at.
 */
/**
 * Which Speechify key is speaking, in the bottom corner, for diagnosis.
 *
 * A ring that walks silently is a ring nobody can reason about. When a reading is slow to start, this
 * says at once whether it is on key 3 of 21 because two are tired, or on key 1 and simply waiting for
 * the network — a distinction that used to cost a log export.
 *
 * As dim as the close mark and for the same reason: it is an instrument, not a control, and anything
 * brighter would compete with the words. Drawn only when there is something to say.
 */
@Composable
private fun MaKeyCorner() {
    val n = MaSpeechify.activeKeyNumber
    val total = MaSpeechify.keyCount
    if (n <= 0 || total <= 0) return
    Box(
        modifier = Modifier.fillMaxSize().padding(8.dp),
        contentAlignment = Alignment.BottomEnd,
    ) {
        Text(
            text = "$n/$total",
            color = MaSubtitleShadow,
            fontSize = 11.sp,
        )
    }
}

@Composable
private fun MaCloseCorner(onClose: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize().padding(6.dp),
        contentAlignment = Alignment.TopEnd,
    ) {
        Text(
            text = "\u2715",
            color = MaSubtitleShadow,
            fontSize = 22.sp,
            // A touch target larger than the glyph: a small mark in a corner is hard to hit, and
            // this is what somebody reaches for when they want out.
            modifier = Modifier.clickable { onClose() }.padding(horizontal = 14.dp, vertical = 10.dp),
        )
    }
}

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
            // Full screen takes whatever height the caller gave it — the keyboard's own — rather
            // than a number of its own that would be wrong on the next phone.
            .then(if (full) Modifier.fillMaxHeight() else Modifier.height(SUBTITLE_HEIGHT))
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
        // Every reading view carries it, because the question it answers — which key is speaking —
        // is asked when something is wrong, and being wrong is not a state that picks a view first.
        MaKeyCorner()
        if (full) {
            MaCloseCorner { scope.launch { prefs.dictate.maReaderFullscreen.set(false) } }
        }
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
 * Roughly what one line holds at 17sp on his phone.
 *
 * One line and not a page, because full screen scrolls line by line: this is the unit that jumps to
 * the top, and a three-line unit would jump three lines at a time and lose the steadiness that makes
 * it readable.
 */
private const val LINE_CHARS = 34

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

/**
 * `#RRGGBB` to a colour, or null if it is not one.
 *
 * Null rather than a default, so a stored value that has been corrupted falls back to his real
 * setting instead of silently becoming black on black.
 */
private fun parseHex(hex: String): Color? {
    val h = hex.trim().removePrefix("#")
    if (h.length != 6) return null
    val v = h.toLongOrNull(16) ?: return null
    return Color(0xFF000000L or v)
}
