/*
 * Copyright (C) 2026 Marko Bosko, Mantra Productions
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package dev.patrickgold.florisboard.app.settings.dictate

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.patrickgold.florisboard.app.FlorisPreferenceStore
import dev.patrickgold.florisboard.app.settings.dictate.MaButtonGlyph
import dev.patrickgold.florisboard.dictate.MaKeyColors
import dev.patrickgold.florisboard.dictate.MaMacroSlots
import dev.patrickgold.florisboard.dictate.MaRows
import dev.patrickgold.florisboard.dictate.ui.maFeatureToken
import dev.patrickgold.florisboard.dictate.MaNumericSecondary
import dev.patrickgold.florisboard.ime.text.key.KeyCode
import dev.patrickgold.florisboard.ime.text.keyboard.TextKeyboard
import dev.patrickgold.florisboard.keyboardManager
import dev.patrickgold.florisboard.lib.compose.FlorisScreen
import kotlinx.coroutines.launch
import dev.patrickgold.jetpref.datastore.model.collectAsState as collectPrefAsState

/**
 * Every key on one grid, each in its own colour — the Logitech G815 idea.
 *
 * ### How it is used
 *
 * Pick a colour, then tap keys: each tap paints. Tapping a key already that colour clears it, and
 * the eraser clears whatever it touches. That is how the G815 software works and it is the quick
 * way for a whole row — choose red once, tap ten digits — instead of a dialog per key.
 *
 * ### The grid is the real keyboard
 *
 * The letter rows are read from the layout the keyboard last drew, so the grid matches what is on
 * his screen — his layout, his bottom row, his special keys with their own glyphs (settings show a
 * key's icon, HANDOFF's rule). Before the keyboard has been opened in this session there is no
 * computed layout to read, so a plain letter grid stands in and says so.
 *
 * The number row is on top, by position, with each digit's macro printed under it — the same
 * third line the keyboard now prints — so it is clear which digit is being painted.
 */
@Composable
fun MaKeyColorsScreen() = FlorisScreen {
    title = "Key colours"

    content {
        val prefs by FlorisPreferenceStore
        val context = LocalContext.current
        val keyboardManager by context.keyboardManager()
        val scope = rememberCoroutineScope()

        val raw by prefs.dictate.maKeyColors.collectPrefAsState()
        val colors = remember(raw) { MaKeyColors.parse(raw) }
        val secondaryRaw by prefs.dictate.maNumericSecondary.collectPrefAsState()
        val secondaries = remember(secondaryRaw) { MaNumericSecondary.parse(secondaryRaw) }
        val rowsRaw by prefs.dictate.maRows.collectPrefAsState()
        val macroRaw by prefs.dictate.maMacroSlots.collectPrefAsState()
        val macroSlots = remember(macroRaw) { MaMacroSlots.parse(macroRaw) }
        val evaluator by keyboardManager.lastCharactersEvaluator.collectAsState()
        val keyboard = evaluator.keyboard as? TextKeyboard

        // The brush: a palette colour, or null for the eraser.
        var brush by remember { mutableStateOf<Int?>(MaKeyColors.PALETTE.first().argb) }

        fun paint(id: String) {
            val current = colors[id]
            val next = if (brush == null || current == brush) null else brush
            scope.launch { prefs.dictate.maKeyColors.set(MaKeyColors.serialize(MaKeyColors.with(colors, id, next))) }
        }

        Text(
            text = "Choose a colour, then tap keys to paint them. Tap a key again to clear it. " +
                "Keys you leave alone keep the theme's colour.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )

        // ---------------------------------------------------------------- the palette
        Column(modifier = Modifier.padding(horizontal = 12.dp)) {
            MaKeyColors.PALETTE.chunked(8).forEach { line ->
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    line.forEach { swatch ->
                        val chosen = brush == swatch.argb
                        Box(
                            modifier = Modifier
                                .padding(vertical = 3.dp)
                                .size(34.dp)
                                .background(Color(swatch.argb), CircleShape)
                                .border(
                                    width = if (chosen) 3.dp else 1.dp,
                                    color = if (chosen) MaterialTheme.colorScheme.primary else Color.Black.copy(alpha = 0.25f),
                                    shape = CircleShape,
                                )
                                .clickable { brush = swatch.argb },
                        )
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = { brush = null }) {
                    Text(
                        text = "Eraser",
                        fontWeight = if (brush == null) FontWeight.Bold else FontWeight.Normal,
                    )
                }
                TextButton(onClick = { scope.launch { prefs.dictate.maKeyColors.set("") } }) {
                    Text("Clear all")
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        // ---------------------------------------------------------------- the grid
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp),
        ) {
            Text(
                text = "Number row",
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(start = 6.dp, top = 4.dp, bottom = 2.dp),
            )
            Row(modifier = Modifier.fillMaxWidth()) {
                MaNumericSecondary.LABELS.forEachIndexed { index, digit ->
                    val id = MaKeyColors.idForNumberRow(index)
                    GridKey(
                        label = digit,
                        icon = null,
                        under = secondaries.getOrNull(index)?.let { MaNumericSecondary.preview(it) ?: MaNumericSecondary.hint(it) },
                        paint = colors[id],
                        modifier = Modifier.weight(1f),
                        onTap = { paint(id) },
                    )
                }
            }

            // The feature rows, as they are arranged — each key by what it is, so a key moved to
            // another row keeps its colour. State colours (recording, lit, a full bucket) are drawn
            // over the paint on the keyboard and still mean what they meant.
            val rows = remember(rowsRaw) {
                MaRows.visibleRows(if (rowsRaw.isBlank()) MaRows.defaultRows() else MaRows.parse(rowsRaw))
            }
            rows.forEachIndexed { rowIndex, buttons ->
                Text(
                    text = "Feature row ${rowIndex + 1}",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(start = 6.dp, top = 10.dp, bottom = 2.dp),
                )
                // Wrapped at eight, so a long row stays tappable on a phone.
                buttons.chunked(8).forEach { line ->
                    Row(modifier = Modifier.fillMaxWidth()) {
                        line.forEach { button ->
                            val id = MaKeyColors.idForFeature(maFeatureToken(button))
                            FeatureGridKey(
                                button = button,
                                macroSlots = macroSlots,
                                paint = colors[id],
                                modifier = Modifier.weight(1f),
                                onTap = { paint(id) },
                            )
                        }
                        repeat(8 - line.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }

            Text(
                text = "Keyboard",
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(start = 6.dp, top = 10.dp, bottom = 2.dp),
            )
            if (keyboard != null && keyboard.arrangement.isNotEmpty()) {
                keyboard.arrangement.forEach { row ->
                    // Widths as the keyboard draws them, so a spacebar is wide here too.
                    val narrowest = row.map { it.visibleBounds.width }.filter { it > 0f }.minOrNull()
                    Row(modifier = Modifier.fillMaxWidth()) {
                        row.forEach { key ->
                            val code = key.computedData.code
                            val id = MaKeyColors.idForCode(code)
                            val weight = if (narrowest != null && key.visibleBounds.width > 0f) {
                                key.visibleBounds.width / narrowest
                            } else {
                                1f
                            }
                            GridKey(
                                label = when {
                                    code == KeyCode.SPACE -> "space"
                                    else -> key.label
                                },
                                icon = key.foregroundImageVector,
                                under = null,
                                paint = colors[id],
                                modifier = Modifier.weight(weight),
                                onTap = { paint(id) },
                            )
                        }
                    }
                }
            } else {
                Text(
                    text = "Open the keyboard once and come back to see every key of your layout. " +
                        "Until then, the letters:",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                )
                listOf("qwertyuiop", "asdfghjkl", "zxcvbnm").forEach { letters ->
                    Row(modifier = Modifier.fillMaxWidth()) {
                        letters.forEach { ch ->
                            val id = MaKeyColors.idForCode(ch.code)
                            GridKey(
                                label = ch.toString(),
                                icon = null,
                                under = null,
                                paint = colors[id],
                                modifier = Modifier.weight(1f),
                                onTap = { paint(id) },
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

/** One key on the grid: its face, its colour if it has one, and the macro under a digit. */
@Composable
private fun GridKey(
    label: String?,
    icon: ImageVector?,
    under: String?,
    paint: Int?,
    modifier: Modifier,
    onTap: () -> Unit,
) {
    val background = paint?.let { Color(it) } ?: MaterialTheme.colorScheme.surfaceVariant
    val foreground = when {
        paint == null -> MaterialTheme.colorScheme.onSurfaceVariant
        MaKeyColors.wantsDarkText(paint) -> Color(0xFF111111)
        else -> Color.White
    }
    Box(
        modifier = modifier
            .padding(2.dp)
            .height(if (under != null) 52.dp else 44.dp)
            .background(background, RoundedCornerShape(6.dp))
            .clickable(onClick = onTap),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            when {
                icon != null -> Icon(icon, contentDescription = label, tint = foreground, modifier = Modifier.size(18.dp))
                else -> Text(
                    text = label.orEmpty(),
                    color = foreground,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (under != null) {
                Text(
                    text = under,
                    color = foreground.copy(alpha = 0.8f),
                    fontSize = 8.sp,
                    lineHeight = 9.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 2.dp),
                )
            }
        }
    }
}

/** A feature-row key on the grid, wearing the same glyph the keyboard draws. */
@Composable
private fun FeatureGridKey(
    button: MaRows.Button,
    macroSlots: List<MaMacroSlots.Slot>,
    paint: Int?,
    modifier: Modifier,
    onTap: () -> Unit,
) {
    val background = paint?.let { Color(it) } ?: MaterialTheme.colorScheme.surfaceVariant
    val foreground = when {
        paint == null -> MaterialTheme.colorScheme.onSurfaceVariant
        MaKeyColors.wantsDarkText(paint) -> Color(0xFF111111)
        else -> Color.White
    }
    Box(
        modifier = modifier
            .padding(2.dp)
            .height(44.dp)
            .background(background, RoundedCornerShape(6.dp))
            .clickable(onClick = onTap),
        contentAlignment = Alignment.Center,
    ) {
        MaButtonGlyph(button, macroSlots, tintOverride = foreground)
    }
}
