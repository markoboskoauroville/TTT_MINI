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

import androidx.compose.foundation.layout.width
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.patrickgold.florisboard.app.FlorisPreferenceStore
import dev.patrickgold.florisboard.dictate.MaLanguage
import dev.patrickgold.florisboard.dictate.MaReader
import dev.patrickgold.florisboard.dictate.MaSpeechify
import dev.patrickgold.jetpref.datastore.model.collectAsState
import kotlinx.coroutines.launch

/**
 * The three dials he actually reaches for while listening: voice, speed, colour.
 *
 * ### Why this exists rather than a trip to settings
 *
 * Changing the speed used to mean leaving the keyboard, finding the Reader screen, stepping a
 * number, and coming back — by which time the sentence he was judging it against is long gone. And
 * until this build the change did not even take effect until the next passage, so the round trip
 * ended in nothing happening.
 *
 * **A dial is judged against what it is doing.** So these three live one long press from the reader
 * key, they apply to the audio that is playing, and the same long press closes them.
 *
 * ### Half the screen, not all of it
 *
 * The void takes the whole display because a single word needs no context. This is the opposite: he
 * is adjusting something *while listening*, so the reading has to stay visible above it. Covering it
 * would mean adjusting blind, which is the problem this was built to solve.
 *
 * ### Only three
 *
 * Voice, speed, colour. Everything else about the reader is chosen once and left, and putting it
 * here would make the one thing he wants mid-sentence harder to find. The full Reader screen still
 * holds the rest.
 */
@Composable
fun MaReaderDashboard(onClose: () -> Unit) {
    val prefs by FlorisPreferenceStore
    val scope = rememberCoroutineScope()

    val language = MaLanguage.active()
    val voices = MaSpeechify.voicesFor(language)
    val chosenVoice = MaSpeechify.chosenVoice(language)
    val speed by prefs.dictate.maReaderSpeed.collectAsState()
    val hex by prefs.dictate.maReaderHighlightHex.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF0B0D10))
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = if (language == MaLanguage.EN) "Reading English" else "Reading Croatian",
                color = MaDashDim,
                fontSize = 13.sp,
                modifier = Modifier.weight(1f),
            )
            // Long press on the reader key closes it too. This is here for the press that lands
            // somewhere unexpected, which on a keyboard is most of them.
            Text(
                text = "close",
                color = MaDashAccent,
                fontSize = 13.sp,
                modifier = Modifier
                    .clickable { onClose() }
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            )
        }

        Spacer(Modifier.height(10.dp))

        // SPEED — the reason this screen exists. It moves the audio that is playing.
        Row(verticalAlignment = Alignment.CenterVertically) {
            DashButton("\u2212") {
                val next = (speed - 1).coerceAtLeast(5)
                scope.launch { prefs.dictate.maReaderSpeed.set(next) }
                MaReader.setSpeedNow(next)
            }
            Text(
                text = "%.1f\u00D7".format(speed / 10f),
                color = MaDashInk,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 14.dp),
            )
            DashButton("+") {
                val next = (speed + 1).coerceAtMost(25)
                scope.launch { prefs.dictate.maReaderSpeed.set(next) }
                MaReader.setSpeedNow(next)
            }
            Spacer(Modifier.weight(1f))
            Text(text = "speed", color = MaDashDim, fontSize = 13.sp)
        }

        Spacer(Modifier.height(12.dp))

        // EFFECTS, not voices.
        //
        // The voice is chosen once and then left alone — it is a matter of taste settled in an
        // afternoon. The effect is what he changes for THIS passage: void for something hard,
        // highlight for something he is skimming, karaoke when he wants to see the sentence coming.
        // That is what belongs on a panel opened mid-reading; the voice belongs in settings.
        Text(text = "effect", color = MaDashDim, fontSize = 13.sp)
        Spacer(Modifier.height(6.dp))
        val style by prefs.dictate.maReaderStyle.collectAsState()
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(MaReaderEffects.ALL) { effect ->
                val on = effect.id == style
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (on) MaDashAccent else MaDashChip)
                        .clickable { scope.launch { prefs.dictate.maReaderStyle.set(effect.id) } }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                ) {
                    Text(
                        text = effect.label,
                        color = if (on) Color.Black else MaDashInk,
                        fontSize = 14.sp,
                        fontWeight = if (on) FontWeight.Bold else FontWeight.Normal,
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // SEVEN SWATCHES, dark to white. He asked for swatches and got a slider; this is the
        // correction.
        //
        // Greys only, because a highlight over a white page needs to be brighter than the page, not
        // a different colour. Seven because a row of seven is read at a glance and chosen without
        // aiming — a slider needs a precise finger and gives back a number nobody wanted.
        //
        // **The dark end is not black.** Black on this background is invisible, so the darkest
        // swatch would be a square that appears to do nothing and a highlight that vanishes. It
        // starts at the darkest grey that still reads as a mark.
        // COLLAPSED until asked for.
        //
        // Seven swatches open by default is seven things to look past every time the panel is
        // raised, and he raises it to change the speed far more often than the colour. One swatch
        // showing the current choice; tap it and the row unfolds.
        //
        // The closed state is not a button that says "colours" — it IS the current colour, so the
        // panel still answers "what is the highlight" at a glance without being asked.
        Text(text = "highlight", color = MaDashDim, fontSize = 13.sp)
        Spacer(Modifier.height(6.dp))
        var open by remember { mutableStateOf(false) }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(swatchColour(hex.ifBlank { MaSwatches.GREYS.last() }))
                    .clickable { open = !open },
            )
            if (open) {
                Spacer(Modifier.width(10.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(MaSwatches.GREYS) { swatch ->
                        val on = hex.equals(swatch, ignoreCase = true)
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(swatchColour(swatch))
                                .clickable {
                                    scope.launch { prefs.dictate.maReaderHighlightHex.set(swatch) }
                                    // Folds itself away once chosen. The row exists to answer one
                                    // question, and it has been answered.
                                    open = false
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            if (on) {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        // Dark on the pale swatches, pale on the dark ones, so the
                                        // mark is visible at both ends of a grey ramp.
                                        .background(if (swatch >= "#B0") Color.Black else Color.White),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DashButton(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(MaDashChip)
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Text(text = label, color = MaDashInk, fontSize = 20.sp, fontWeight = FontWeight.Bold)
    }
}

private fun swatchColour(hex: String): Color {
    val h = hex.removePrefix("#")
    val v = h.toLongOrNull(16) ?: return Color.Gray
    return Color(0xFF000000L or v)
}

private val MaDashInk = Color(0xFFF2DDB4)
private val MaDashDim = Color(0xFF8A8A8A)
private val MaDashChip = Color(0xFF1E1E20)
private val MaDashAccent = Color(0xFFE8B15C)
