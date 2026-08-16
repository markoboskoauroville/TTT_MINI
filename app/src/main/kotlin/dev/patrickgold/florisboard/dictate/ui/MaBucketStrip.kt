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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.patrickgold.florisboard.app.FlorisPreferenceStore
import dev.patrickgold.florisboard.dictate.MaClipCapture
import dev.patrickgold.florisboard.FlorisImeService
import dev.patrickgold.florisboard.dictate.MaRows
import dev.patrickgold.jetpref.datastore.model.collectAsState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * The strip along the top: what is in each copy bucket, numbered, side by side.
 *
 * ### What it replaced and why
 *
 * This slot used to show the system's clipboard suggestions — a paste icon, a link icon, and a
 * truncated line of text. Those icons said what *kind* of thing an entry was, which is the least
 * useful fact about it: the user already knows they copied a link. What they cannot know, looking at
 * a row of keys labelled C1 to C4, is which bucket holds which text.
 *
 * So the strip answers exactly that. Each entry is the bucket's number and the beginning of its
 * text, in bucket order, scrollable sideways with a finger. It is the legend for the row of numbered
 * keys below, and nothing else.
 *
 * ### Only buckets that exist
 *
 * Built from [MaRows.visibleClipSlots], the same set the capture and the keys use. A bucket that is
 * not on the keyboard is not listed, because it cannot be filled and could not be pasted from if it
 * were. One source of truth for how many buckets there are, read in three places.
 *
 * ### Tapping
 *
 * Tapping an entry does what its key does: paste-replace, then empty that bucket. Not a shortcut
 * around the keys but the same action from a place where the text is readable, which is what makes
 * it useful when several buckets hold similar-looking things.
 */
@Composable
fun MaBucketStrip(modifier: Modifier = Modifier) {
    val prefs by FlorisPreferenceStore
    val scope = rememberCoroutineScope()
    val rowsRaw by prefs.dictate.maRows.collectAsState()
    val capturedRaw by prefs.dictate.maClipCaptured.collectAsState()

    val storedRows = remember(rowsRaw) {
        if (rowsRaw.isBlank()) MaRows.defaultRows() else MaRows.parse(rowsRaw)
    }
    val visible = remember(storedRows) { MaRows.visibleClipSlots(storedRows) }
    val slots = remember(capturedRaw) { MaClipCapture.parse(capturedRaw) }

    val filled = visible.sorted().mapNotNull { n ->
        MaClipCapture.at(slots, n)?.let { n to it }
    }
    // Nothing held yet, so nothing to legend. Drawn empty it would be a bar of blank space above the
    // keyboard for as long as the buckets stay empty, which is most of the time.
    if (filled.isEmpty()) return

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        filled.forEachIndexed { index, (slot, text) ->
            if (index > 0) {
                // The divider Marko asked to keep. It is what makes a row of text read as separate
                // entries rather than as one long sentence running off the edge.
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(1.dp)
                        .padding(vertical = 8.dp)
                        .background(MaStripDivider),
                )
            }
            Row(
                modifier = Modifier
                    // The strip does its own paste rather than calling back to whoever placed it.
                    // It is dropped into the Smartbar, which is shared by two layouts, and a
                    // callback would have to be threaded through both with a coroutine scope each.
                    // Self-contained, it can be placed anywhere with one line and no plumbing.
                    .clickable {
                        scope.launch {
                            val connection = FlorisImeService.currentInputConnection()
                            if (connection != null) {
                                // The same sequence the C key runs, and the same delays, so the two
                                // routes cannot behave differently: select all, delete, paste.
                                connection.performContextMenuAction(android.R.id.selectAll)
                                delay(MA_BUCKET_STEP_MS)
                                FlorisImeService.currentInputConnection()?.commitText("", 1)
                                delay(MA_BUCKET_STEP_MS)
                                FlorisImeService.currentInputConnection()?.commitText(text, 1)
                                // Poured out only after the text has landed, exactly as the key does
                                // it: a failed paste leaves the bucket holding its contents.
                                prefs.dictate.maClipCaptured.set(
                                    MaClipCapture.serialize(MaClipCapture.pour(slots, slot)),
                                )
                            }
                        }
                    }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = slot.toString(),
                    // The number carries the whole link to the key below, so it is the one thing
                    // here that must never be mistaken for part of the text.
                    //
                    // Always sand, never red. Red used to mean "every bucket is full", and full is
                    // not a fault — it is the normal end of filling them. Red on a keyboard reads as
                    // something being wrong, so it sent him looking for a problem that did not
                    // exist, every time he used the feature as intended. Empty the buckets with the
                    // bin key and they fill again; the strip does not need to shout about it.
                    color = MaStripNumber,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    // One line, cut with an ellipsis. The beginning of a copied line is what
                    // identifies it; a strip that wrapped would push the keyboard down to show text
                    // nobody is reading in full anyway.
                    text = text.replace('\n', ' ').trim(),
                    color = MaStripText,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.width(160.dp),
                )
            }
        }
    }
}

/** The sand the app uses for a label that names something. */
private val MaStripNumber = Color(0xFFE8B15C)

private val MaStripText = Color(0xFFECEAE3)
private val MaStripDivider = Color(0x33FFFFFF)

/** The gap between paste steps, matching the C keys exactly. See MaFeatureRow. */
private const val MA_BUCKET_STEP_MS = 200L

/**
 * Whether any visible bucket is holding something, and therefore whether the strip has anything to
 * draw.
 *
 * Exists so the caller can decide *which* composable takes the slot rather than composing both and
 * letting one draw nothing. AnimatedVisibility lays its content out in a Box, so two composables in
 * that slot are two layers on top of each other, not one after the other — which is exactly what
 * put the bucket legend and the word suggestions in the same strip, overlapping.
 *
 * Deliberately asks the same three questions the strip itself asks, in the same order, so the two
 * cannot disagree about whether there is anything to show.
 */
@Composable
fun maBucketStripHasContent(): Boolean {
    val prefs by FlorisPreferenceStore
    val rowsRaw by prefs.dictate.maRows.collectAsState()
    val capturedRaw by prefs.dictate.maClipCaptured.collectAsState()
    val storedRows = remember(rowsRaw) {
        if (rowsRaw.isBlank()) MaRows.defaultRows() else MaRows.parse(rowsRaw)
    }
    val visible = remember(storedRows) { MaRows.visibleClipSlots(storedRows) }
    val slots = remember(capturedRaw) { MaClipCapture.parse(capturedRaw) }
    return visible.any { MaClipCapture.at(slots, it) != null }
}
