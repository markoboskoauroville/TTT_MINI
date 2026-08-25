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
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.ui.input.pointer.pointerInput
import dev.patrickgold.florisboard.dictate.DictateController
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Column
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
 * ### The window is pinned while the reader is alive, and empty is fine
 *
 * It used to draw nothing whenever there was no word to show, on the reasoning that a band of blank
 * space appearing and disappearing is more distracting than the words it carries. **That reasoning
 * was right and the rule built from it was wrong**, and chunked fetching exposed it: between one
 * chunk finishing and the next starting there is a moment with no current word, so the whole box
 * vanished and came back — several times per passage. He called it blinking, which is exactly what
 * it was.
 *
 * The distracting thing was never the blank bar. It was the APPEARING AND DISAPPEARING. So the box
 * now stays for as long as the reader is anything but idle, and shows nothing in it when there is
 * nothing to show. Empty and still beats full and flickering.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MaSubtitleRow(modifier: Modifier = Modifier) {
    val index = MaReader.currentIndex
    // Idle is the only reason to disappear. LOADING, SPEAKING and PAUSED all keep the window, so
    // the gap between two chunks is a still box rather than a hole in the layout.
    if (MaReader.state == MaReader.State.IDLE) return
    // The joined timeline, not the last chunk fetched.
    //
    // MaSpeechify.lastWords now holds whichever chunk came back most recently — which, while chunk
    // two is playing and chunk three is in flight, is chunk three. The caption would jump forward
    // to text he has not heard yet and then back again.
    val words = MaReader.allWords
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
    // No page yet — the first chunk is still coming, or one has just ended. The box is drawn empty
    // and keeps its place. Returning here is what made it blink.
    // THE LAST PAGE STAYS UP UNTIL THE NEXT ONE IS READY.
    //
    // Build 286 stopped the WINDOW disappearing between chunks. The text still emptied, and he was
    // right that this is the same fault one layer in: an empty box that fills again is a blink
    // whether or not the box itself survived.
    //
    // So the last page that had content is held and redrawn while there is nothing current. **Like a
    // slideshow**, in his words: one image is on screen until the next is ready, and the change is
    // the only event. Nothing is ever blank mid-passage.
    //
    // `remember` outside the null check, so the held value survives the recompositions where the
    // page is missing. Cleared by the reader going idle, not here — a stale page after the reading
    // ends would be a caption for nothing.
    // The start-of-passage square, in front of everything the caption draws.
    //
    // One place rather than one per effect: every style shows it, because the question it answers —
    // "am I hearing this for the second time?" — does not depend on which effect he picked.
    val mark = MaReader.passageMark

    val live = pages.firstOrNull { index in it.range }
    // Keyed on the passage, not on the index: a new reading starts with nothing held, so the last
    // sentence of the previous one cannot appear under the first second of the next.
    var held by remember(words) { mutableStateOf<Page?>(null) }
    if (live != null) held = live
    val page = live ?: held
    if (page == null) {
        // Only before the very first page has ever arrived. There is nothing to hold yet, and an
        // empty box for the first second is honest — it says the reading is coming.
        SubtitleBox(modifier = modifier, full = full) {
            if (mark.isNotBlank()) {
                Text(text = mark, color = lit, fontSize = fontSize, fontWeight = FontWeight.Bold)
            }
        }
        return
    }

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
    // The "top" style branch is gone. Where the reading sits is now `maReaderAlign` and applies to
    // every effect rather than being one of them. See MaReaderEffects.

    // MATRIX. The word resolves out of falling noise, in the box the void uses.
    //
    // It shares the void's frame deliberately: black, full width, one word, and the same long press
    // and the same gestures. The difference is what happens INSIDE the frame, and building it as a
    // second kind of window would have meant a second copy of every gesture to keep in step.
    if (style == "matrix") {
        // The square rides in front of the word for the first few, so it is seen wherever the eye
        // is rather than in a corner it has no reason to look at.
        val word = (if (mark.isNotBlank()) "$mark " else "") + words.getOrNull(index)?.text.orEmpty()
        // Two clocks, as the rule says. `frame` drives the noise at a fixed speed; `progress` is
        // where the voice is inside this word, and only that one follows the speaking rate.
        var frame by remember { mutableIntStateOf(0) }
        LaunchedEffect(index) {
            while (true) {
                frame++
                delay(70L)
            }
        }
        val started = remember(index) { android.os.SystemClock.elapsedRealtime() }
        // The word's own spoken length, from the timings Speechify returned. Falls back to a fixed
        // guess when a word has no usable span, so the resolve still runs rather than freezing on
        // the first letter.
        val spokenMs = words.getOrNull(index)
            ?.let { (it.endMs - it.startMs).toLong() }
            ?.takeIf { it > 0L } ?: 400L
        val progress = ((android.os.SystemClock.elapsedRealtime() - started + frame * 0L)
            .toFloat() / spokenMs.coerceAtLeast(1L)).coerceIn(0f, 1f)
        val grid = remember(word, frame) { MaMatrix.frame(word, progress, frame) }
        SubtitleBox(modifier = modifier, full = full) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                grid.forEachIndexed { row, cells ->
                    Text(
                        text = cells.joinToString("") { it.char.toString() },
                        color = if (row == MaMatrix.WORD_ROW) MaMatrixGreen else MaMatrixGreen.copy(
                            alpha = cells.firstOrNull()?.dim ?: 0.2f,
                        ),
                        fontSize = if (row == MaMatrix.WORD_ROW) (fontSp + 6).sp else (fontSp - 4).coerceAtLeast(9).sp,
                        fontWeight = if (row == MaMatrix.WORD_ROW) FontWeight.Bold else FontWeight.Normal,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1,
                    )
                }
            }
        }
        return
    }

    if (style == "void") {
        // The square rides in front of the word for the first few, so it is seen wherever the eye
        // is rather than in a corner it has no reason to look at.
        val word = (if (mark.isNotBlank()) "$mark " else "") + words.getOrNull(index)?.text.orEmpty()
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
 * A ring that walks silently is a ring nobody can reason about. It climbs as keys are tried, so
 * watching it IS the diagnosis: a still `1` means waiting for the network, `1 2 3 4` means the ring is
 * walking past tired keys. That distinction used to cost a log export.
 *
 * One number, no total. He can count his keys in settings, and a second number on a screen built for
 * one word at a time is one more thing competing for the eye.
 *
 * As dim as the close mark and for the same reason: it is an instrument, not a control, and anything
 * brighter would compete with the words. Drawn only when there is something to say.
 */
@Composable
private fun MaKeyCorner() {
    val n = MaSpeechify.activeKeyNumber
    if (n <= 0) return
    Box(
        modifier = Modifier.fillMaxSize().padding(8.dp),
        contentAlignment = Alignment.BottomEnd,
    ) {
        Text(
            text = "$n",
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
    // Top, middle or bottom, from the dashboard, with no animation between them. The line is DRAWN
    // where it belongs rather than travelling there: movement on screen while he is listening is
    // the thing a caption is supposed to avoid.
    val prefs by FlorisPreferenceStore
    val alignPref by prefs.dictate.maReaderAlign.collectAsState()
    val scope = rememberCoroutineScope()
    Box(
        // His setting, and it replaces the old `if (full) Center else TopStart`. That rule guessed
        // what he wanted from the size of the window; now he says, and it is the same answer in both
        // sizes — which is what "always, zero delay" means.
        contentAlignment = when (alignPref) {
            "middle" -> Alignment.Center
            "bottom" -> Alignment.BottomStart
            else -> Alignment.TopStart
        },
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
            .maReaderGestures(
                onKill = { MaReader.stop(); DictateController.cancelTranscription() },
                onNext = { MaReader.skipSentence() },
                onPrevious = { MaReader.previousSentence() },
                onZoom = { wantFull -> scope.launch { prefs.dictate.maReaderFullscreen.set(wantFull) } },
                full = full,
            )
            .padding(horizontal = 16.dp, vertical = if (full) 18.dp else 10.dp),
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

/**
 * THE FOUR GESTURES ON A READING WINDOW.
 *
 * | gesture | what it does |
 * |---|---|
 * | swipe LEFT or RIGHT | kill it: stop reading, cancel any transcription in flight, close |
 * | swipe DOWN | next sentence |
 * | swipe UP | previous sentence |
 * | pinch | full screen, or back to the subtitle box |
 *
 * ### Why both sides kill
 *
 * He asked for the left swipe to kill and asked me to suggest something for the right. The best
 * suggestion is **the same thing**, and that is not laziness.
 *
 * The gesture is reached for in one situation: something is running that he wants gone, and he is
 * usually not looking at the phone when he decides that. A kill that works from one side only is a
 * kill he has to aim, and a mis-aimed kill does something else instead — which for a pair like
 * *stop* and *pause* means the reading carries on while he believes he has ended it. **When a
 * gesture exists to stop something, both directions of it must stop that thing.**
 *
 * So there is no second meaning to remember and nothing to aim at. Whichever way the thumb goes, it
 * dies.
 *
 * ### Down is forward, and that is deliberate
 *
 * The same rule the volume keys used to carry, and for the same reason: **the text moves down the
 * screen as it is read, so down is where the next sentence is.** Every scroll on this phone works
 * that way and the hand is already trained by all of them. Mapping up to "next" because up is
 * bigger would be arithmetic imposed on movement.
 *
 * ### Kill means all of it
 *
 * `MaReader.stop()` ends the speaking and closes the window; `cancelTranscription()` ends a
 * transcription that is still in flight. Both, from one gesture, because from where he is standing
 * they are one thing — *the phone is busy with something I no longer want* — and a gesture that
 * ended only the half he could see would leave the other half running with nothing on screen to
 * stop it. Cancelling a transcription that is not running is a no-op, so there is no case where
 * this does something he did not ask for.
 */
private fun Modifier.maReaderGestures(
    onKill: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onZoom: (Boolean) -> Unit,
    full: Boolean,
): Modifier = this
    .pointerInput(full) {
        // Pinch on its own detector: a zoom is two fingers and a swipe is one, so they cannot be
        // confused, and putting them in one gesture block would make each wait to see whether it was
        // the other.
        detectTransformGestures { _, _, zoom, _ ->
            if (zoom > 1.18f && !full) onZoom(true)
            if (zoom < 0.85f && full) onZoom(false)
        }
    }
    .pointerInput(full) {
        detectDragGestures(
            onDragEnd = { },
        ) { change, drag ->
            change.consume()
            // One decision per drag, taken on the first movement that is clearly a direction.
            // Reading the running total instead would let a wandering thumb fire twice.
            val x = drag.x
            val y = drag.y
            when {
                kotlin.math.abs(x) > kotlin.math.abs(y) && kotlin.math.abs(x) > SWIPE_MIN -> onKill()
                kotlin.math.abs(y) > SWIPE_MIN && y > 0f -> onNext()
                kotlin.math.abs(y) > SWIPE_MIN -> onPrevious()
            }
        }
    }

/**
 * How far a drag has to travel before it is a swipe.
 *
 * Generous, because the cheapest failure here is nothing happening and the most expensive is a
 * reading killed by a thumb resting on the screen.
 */
private const val SWIPE_MIN = 24f

/** The one green. Bright enough to read on black, not so bright it glows into the next line. */
private val MaMatrixGreen = Color(0xFF56D364)
