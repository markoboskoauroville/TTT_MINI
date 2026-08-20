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

import kotlin.math.roundToInt
import androidx.compose.ui.layout.layout
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectDragGestures
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

        // VOICE — only the ones for the language being read, because the other list is not a choice
        // he can make right now without also changing the language.
        Text(text = "voice", color = MaDashDim, fontSize = 13.sp)
        Spacer(Modifier.height(6.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(voices) { voice ->
                val on = voice.id == chosenVoice.id
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (on) MaDashAccent else MaDashChip)
                        .clickable {
                            scope.launch {
                                if (language == MaLanguage.EN) {
                                    prefs.dictate.maReaderVoiceEn.set(voice.id)
                                } else {
                                    prefs.dictate.maReaderVoiceHr.set(voice.id)
                                }
                            }
                        }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                ) {
                    Text(
                        text = voice.label,
                        color = if (on) Color.Black else MaDashInk,
                        fontSize = 14.sp,
                        fontWeight = if (on) FontWeight.Bold else FontWeight.Normal,
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // BRIGHTNESS, not colour.
        //
        // Four coloured chips became one grey-to-white ramp, because he does not want a colourful
        // app and a highlight does not need a hue. What it needs is to be *more* than the page
        // around it, and on a white page that is one axis: how bright.
        //
        // Half grey to white, and no darker. Below about half the word stops out-reading the page
        // and the highlight is doing the opposite of its job — so the dim end of the ramp is a
        // choice, not a limit anybody would want to pass.
        Text(text = "highlight", color = MaDashDim, fontSize = 13.sp)
        Spacer(Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Brush.horizontalGradient(listOf(MaRampFrom, Color.White)))
                .pointerInput(Unit) {
                    fun set(x: Float) {
                        val f = (x / size.width).coerceIn(0f, 1f)
                        scope.launch { prefs.dictate.maReaderHighlightHex.set(rampHex(f)) }
                    }
                    detectTapGestures { set(it.x) }
                }
                .pointerInput(Unit) {
                    detectDragGestures { change, _ ->
                        val f = (change.position.x / size.width).coerceIn(0f, 1f)
                        scope.launch { prefs.dictate.maReaderHighlightHex.set(rampHex(f)) }
                    }
                },
        ) {
            // The marker sits where the current brightness is, so the bar shows the setting rather
            // than only accepting one.
            Box(
                modifier = Modifier
                    .rampMarker(rampFraction(hex))
                    .size(width = 4.dp, height = 44.dp)
                    .background(Color(0xFF0B0D10)),
            )
        }
    }
}

/** The dim end of the ramp: half grey. Darker than this and the highlight stops leading the eye. */
private val MaRampFrom = Color(0xFF808080)

/** A position on the ramp to a grey. 0 is half grey, 1 is white. */
private fun rampHex(fraction: Float): String {
    val v = (128 + (127 * fraction)).toInt().coerceIn(128, 255)
    return "#%02X%02X%02X".format(v, v, v)
}

/** A stored grey back to its position, so the marker can be drawn where he left it. */
private fun rampFraction(hex: String): Float {
    val h = hex.trim().removePrefix("#")
    if (h.length != 6) return 1f
    val v = h.substring(0, 2).toIntOrNull(16) ?: return 1f
    return ((v - 128) / 127f).coerceIn(0f, 1f)
}

/** Places the ramp marker along the bar. */
private fun Modifier.rampMarker(fraction: Float) = this.then(
    Modifier.layout { measurable, constraints ->
        val placeable = measurable.measure(constraints)
        val x = ((constraints.maxWidth - placeable.width) * fraction).roundToInt()
        layout(constraints.maxWidth, placeable.height) { placeable.place(x, 0) }
    },
)

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
