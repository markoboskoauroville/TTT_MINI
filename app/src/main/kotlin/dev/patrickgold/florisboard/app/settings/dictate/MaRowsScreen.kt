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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardReturn
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.KeyboardTab
import androidx.compose.material.icons.filled.KeyboardCapslock
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ToggleOn
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import dev.patrickgold.florisboard.app.FlorisPreferenceStore
import dev.patrickgold.florisboard.dictate.MaCommandPalette
import dev.patrickgold.florisboard.dictate.MaFeatureKey
import dev.patrickgold.florisboard.dictate.MaMacroSlots
import dev.patrickgold.florisboard.dictate.MaRows
import dev.patrickgold.florisboard.dictate.ui.MaZoneGlyph
import dev.patrickgold.florisboard.lib.compose.FlorisScreen
import dev.patrickgold.jetpref.datastore.ui.SwitchPreference
import dev.patrickgold.jetpref.datastore.model.collectAsState
import kotlinx.coroutines.launch

/**
 * The feature row editor: three rows, three tabs, one row at a time.
 *
 * This is the app's main feature and the screen it is arranged from, so it is deliberately the
 * plainest thing in the settings. One tab is one row. Everything visible on the tab belongs to that
 * row and nothing else, which is what stops the screen becoming the crowded single list it was
 * before: with three rows of keys shown at once there was no way to see any of them.
 *
 * ### Same list, same gestures, as the rest of the app
 *
 * A numbered row, the key's own glyph, its name, a tick, and a handle to drag it by. That pattern is
 * already on the settings order screen and was on the feature row screen before this one, and
 * matching it was the whole point of the rewrite: an invented second style made the app look like
 * two apps. Dragging is [MaReorderableColumn], the same component the rest of the app drags with.
 *
 * ### Nothing is protected
 *
 * Every key can be unticked, backspace and enter included. The floor lives in the model rather than
 * here: switch everything off in all three rows and the keyboard draws the settings key alone, so
 * there is always a route back to this screen. A rule enforced by hiding tick boxes would be a rule
 * the user has to discover by failing.
 *
 * ### Unticked, not deleted
 *
 * A key switched off stays in the list, greyed. It keeps its position, so switching it back on puts
 * it where it was rather than at the end — a list whose entries move about cannot be learned by
 * position, and this one is navigated by position.
 */
@Composable
fun MaRowsScreen() = FlorisScreen {
    title = "Feature row"

    content {
        val prefs by FlorisPreferenceStore
        val scope = rememberCoroutineScope()
        val rowsRaw by prefs.dictate.maRows.collectAsState()
        val macroRaw by prefs.dictate.maMacroSlots.collectAsState()

        var rows by remember(rowsRaw) {
            mutableStateOf(
                if (rowsRaw.isBlank()) MaRows.defaultRows() else MaRows.parse(rowsRaw),
            )
        }
        val macroSlots = remember(macroRaw) { MaMacroSlots.parse(macroRaw) }

        fun commit(next: List<MaRows.Row>) {
            rows = next
            scope.launch { prefs.dictate.maRows.set(MaRows.serialize(next)) }
        }

        var tab by remember { mutableStateOf(0) }
        var adding by remember { mutableStateOf(false) }
        var editingMacro by remember { mutableStateOf<Int?>(null) }

        // The only way to hide the feature row, now that a long press no longer does it.

        //

        // It has to live here, above the tabs, because it governs all three rows rather than the one

        // being edited. And it has to exist at all: the gesture that used to hide the row was the

        // only thing that could, so removing it without putting this here would have made the row

        // permanent — which is the opposite of what was asked for.

        SwitchPreference(

            prefs.dictate.maFeatureRowShown,

            title = "Show the feature row",

            summary = "The rows of keys above the keyboard. Turn this off to hide all of them.",

        )

        // How wide a spacer is. Here rather than on the keyboard, because a spacer is invisible
        // there and a control you cannot see is a control you cannot press.
        val spacerTenths by prefs.dictate.maSpacerTenths.collectAsState()
        val spacerScope = rememberCoroutineScope()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Spacer width", style = MaterialTheme.typography.bodyLarge)
                Text(
                    text = "Ten is one key wide. Add a spacer to a row to push the keys after it " +
                        "across \u2014 useful for reaching send with the thumb you actually use.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            TextButton(
                onClick = { spacerScope.launch { prefs.dictate.maSpacerTenths.set((spacerTenths - 5).coerceAtLeast(5)) } },
                enabled = spacerTenths > 5,
            ) { Text("\u2212", fontSize = 20.sp) }
            Text(
                text = "$spacerTenths",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.width(44.dp),
                textAlign = TextAlign.Center,
            )
            TextButton(
                // Forty tenths is four keys, which is already most of a row. Past that a spacer is
                // not arranging the row, it is emptying it.
                onClick = { spacerScope.launch { prefs.dictate.maSpacerTenths.set((spacerTenths + 5).coerceAtMost(40)) } },
                enabled = spacerTenths < 40,
            ) { Text("+", fontSize = 20.sp) }
        }


        TabRow(selectedTabIndex = tab) {
            (0 until MaRows.ROW_COUNT).forEach { i ->
                Tab(
                    selected = tab == i,
                    onClick = { tab = i },
                    text = {
                        Text(
                            text = "Row ${i + 1}",
                            // A row that is switched off says so on its own tab, so the state is
                            // visible before it is opened. Without it, an empty-looking row and a
                            // switched-off row read identically from here.
                            color = if (rows.getOrNull(i)?.enabled == true) {
                                Color.Unspecified
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        )
                    },
                )
            }
        }

        val row = rows.getOrNull(tab) ?: MaRows.Row(emptyList(), enabled = false)

        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Show this row",
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(
                    // The behaviour worth stating, because it is the one that surprises: rows do not
                    // hold their place. Switching row one off moves row two up into it.
                    text = "Rows that are off leave no gap. The ones below move up.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(
                checked = row.enabled,
                onCheckedChange = { on ->
                    commit(rows.mapIndexed { i, r -> if (i == tab) r.copy(enabled = on) else r })
                },
            )
        }

        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
        Spacer(Modifier.height(8.dp))

        if (row.entries.isEmpty()) {
            Text(
                text = "No keys in this row yet. Add one below.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 24.dp),
            )
        } else {
            Text(
                text = "Hold a key and drag to move it. Untick to take it off the keyboard.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )

            MaReorderableColumn(
                items = row.entries,
                rowHeight = ROW_HEIGHT,
                onMove = { from, to -> rows = MaRows.move(rows, tab, from, to) },
                onSettled = { commit(rows) },
            ) { index, entry, lifted ->
                MaRowKeyItem(
                    position = index + 1,
                    entry = entry,
                    macroSlots = macroSlots,
                    lifted = lifted,
                    onToggle = {
                        commit(
                            rows.mapIndexed { i, r ->
                                if (i != tab) r else r.copy(
                                    entries = r.entries.mapIndexed { j, e ->
                                        if (j == index) e.copy(enabled = !e.enabled) else e
                                    },
                                )
                            },
                        )
                    },
                    onEditMacro = { slot -> editingMacro = slot },
                    onRemove = {
                        commit(
                            rows.mapIndexed { i, r ->
                                if (i != tab) r
                                else r.copy(entries = r.entries.filterIndexed { j, _ -> j != index })
                            },
                        )
                    },
                )
            }
        }

        Spacer(Modifier.height(8.dp))
        OutlinedButton(
            onClick = { adding = true },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .heightIn(min = 52.dp),
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Add a key to row ${tab + 1}")
        }

        Spacer(Modifier.height(8.dp))
        TextButton(
            onClick = { commit(MaRows.defaultRows()) },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Reset all three rows") }

        Spacer(Modifier.height(24.dp))

        if (adding) {
            MaKeyPicker(
                macroSlots = macroSlots,
                onAdd = { buttons ->
                    commit(
                        rows.mapIndexed { i, r ->
                            if (i != tab) r
                            else r.copy(entries = r.entries + buttons.map { MaRows.Entry(it) })
                        },
                    )
                    adding = false
                },
                onDismiss = { adding = false },
            )
        }

        editingMacro?.let { slot ->
            MaMacroEditor(
                slot = slot,
                current = MaMacroSlots.at(macroSlots, slot),
                onSave = { label, macro ->
                    val next = macroSlots.toMutableList()
                    next[slot - 1] = MaMacroSlots.slot(label, macro)
                    scope.launch { prefs.dictate.maMacroSlots.set(MaMacroSlots.serialize(next)) }
                    editingMacro = null
                },
                onDismiss = { editingMacro = null },
            )
        }
    }
}

/** What a button is called in the editor's list. */
private fun MaRows.Button.title(macroSlots: List<MaMacroSlots.Slot>): String = when (this) {
    is MaRows.Button.Builtin -> key.label
    // "C1, newest copy" was left over from when these were a window onto the history. C1 has not
    // meant the newest copy since the buckets landed, and a label describing behaviour the app no
    // longer has is worse than no label: it teaches the wrong model of the only feature that matters.
    is MaRows.Button.Clip -> "Copy bucket $slot"
    is MaRows.Button.Macro -> {
        val s = MaMacroSlots.at(macroSlots, slot)
        if (s.isEmpty) "M$slot, empty" else "M$slot, ${s.label}"
    }
}

/**
 * One key in the list: number, glyph, name, tick, handle.
 *
 * Macro rows carry an extra tap target to open their editor, because a macro button is the only
 * kind with anything behind it to edit. The remove button is here rather than as a swipe: a swipe
 * on a row inside a list that also drags is the gesture collision this screen cannot afford.
 */
@Composable
private fun MaRowKeyItem(
    position: Int,
    entry: MaRows.Entry,
    macroSlots: List<MaMacroSlots.Slot>,
    lifted: Boolean,
    onToggle: () -> Unit,
    onEditMacro: (Int) -> Unit,
    onRemove: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .shadow(if (lifted) 8.dp else 0.dp, RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        color = if (lifted) {
            MaterialTheme.colorScheme.surfaceVariant
        } else {
            MaterialTheme.colorScheme.surface
        },
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start,
        ) {
            Text(
                text = position.toString(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(20.dp),
            )
            MaButtonGlyph(entry.button, macroSlots)
            Spacer(Modifier.width(12.dp))
            Text(
                text = entry.button.title(macroSlots),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (lifted) FontWeight.SemiBold else FontWeight.Normal,
                // Greyed rather than removed, so the list keeps its shape whatever is switched off.
                color = if (entry.enabled) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier.weight(1f),
            )
            if (entry.button is MaRows.Button.Macro) {
                TextButton(onClick = { onEditMacro(entry.button.slot) }) { Text("Edit") }
            }
            Checkbox(checked = entry.enabled, onCheckedChange = { onToggle() })
            IconButton(onClick = onRemove) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Remove from this row",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
            }
            Icon(
                imageVector = Icons.Default.DragHandle,
                contentDescription = "Hold and drag to move",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp).padding(end = 4.dp),
            )
        }
    }
}

/**
 * The same glyph the key carries on the keyboard, so the list can be read against it.
 *
 * AP, AC, the zone numerals and the clipboard numbers are letters on the row rather than pictures,
 * so they are letters here. Giving them an icon that appears nowhere on the keyboard would make this
 * list something to translate rather than something to recognise.
 */
@Composable
private fun MaButtonGlyph(button: MaRows.Button, macroSlots: List<MaMacroSlots.Slot>) {
    val tint = MaterialTheme.colorScheme.onSurface
    val size = Modifier.size(24.dp)

    @Composable
    fun letters(text: String, color: Color = tint) {
        Box(modifier = size, contentAlignment = Alignment.Center) {
            Text(text = text, color = color, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
        }
    }

    when (button) {
        is MaRows.Button.Clip -> letters("C${button.slot}")
        is MaRows.Button.Macro -> letters(MaMacroSlots.at(macroSlots, button.slot).label)
        is MaRows.Button.Builtin -> when (button.key) {
            MaFeatureKey.ALL_PASTE -> letters("AP")
            MaFeatureKey.ALL_CLEAR -> letters("AC")
            MaFeatureKey.CLIP_CLEAR ->
                Icon(Icons.Default.Delete, contentDescription = null, tint = tint, modifier = size)
            MaFeatureKey.LANGUAGE -> letters("HR")
            MaFeatureKey.SCROLL -> letters("S")
            // Visible here although invisible on the keyboard: he has to be able to find it in the
            // list to move or remove it, and an empty row entry would be unreachable.
            MaFeatureKey.SPACER -> letters("\u2194")
            MaFeatureKey.SWITCHBOARD ->
                Icon(Icons.Default.ToggleOn, contentDescription = null, tint = tint, modifier = size)
            MaFeatureKey.APP_SWITCH ->
                Icon(Icons.Default.SwapHoriz, contentDescription = null, tint = tint, modifier = size)
            MaFeatureKey.NEXT_FIELD ->
                Icon(Icons.Default.KeyboardTab, contentDescription = null, tint = tint, modifier = size)
            MaFeatureKey.SHIFT ->
                Icon(Icons.Default.KeyboardCapslock, contentDescription = null, tint = tint, modifier = size)
            MaFeatureKey.CHANGE_CASE -> letters("Aa")
            MaFeatureKey.AUTO_BUCKET -> letters("A1")
            MaFeatureKey.READER ->
                Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null, tint = tint, modifier = size)
            MaFeatureKey.PIN ->
                Icon(Icons.Default.PushPin, contentDescription = null, tint = tint, modifier = size)
            MaFeatureKey.HISTORY ->
                Icon(Icons.Default.History, contentDescription = null, tint = tint, modifier = size)
            MaFeatureKey.SPACE -> letters("\u23B5")
            MaFeatureKey.SELECT_ALL ->
                Icon(Icons.Default.SelectAll, contentDescription = null, tint = tint, modifier = size)
            MaFeatureKey.BACKSPACE ->
                Icon(Icons.Default.Backspace, contentDescription = null, tint = tint, modifier = size)
            MaFeatureKey.MIC ->
                Icon(Icons.Default.Mic, contentDescription = null, tint = tint, modifier = size)
            MaFeatureKey.SETTINGS ->
                Icon(Icons.Default.Settings, contentDescription = null, tint = tint, modifier = size)
            MaFeatureKey.ENTER ->
                // AutoMirrored, not Default: Icons.Default.KeyboardReturn is the deprecated spelling
                // and a build failure. ComputingEvaluator in this repo already uses it this way,
                // which is the check that settled it rather than a guess.
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardReturn,
                    contentDescription = null,
                    tint = tint,
                    modifier = size,
                )
            // Green, the colour these three wear on the keyboard when their zone is showing. Colour
            // means state everywhere in this app, so it appears here only for the keys that carry it.
            MaFeatureKey.ZONE_1 -> MaZoneGlyph(1, Color(0xFF6FA85A), size = 24.dp)
            MaFeatureKey.ZONE_2 -> MaZoneGlyph(2, Color(0xFF6FA85A), size = 24.dp)
            MaFeatureKey.ZONE_3 -> MaZoneGlyph(3, Color(0xFF6FA85A), size = 24.dp)
        }
    }
}

/**
 * Picking keys to add: the whole screen, three columns, ticks, and as many as you like at once.
 *
 * ### What was wrong with the version this replaces
 *
 * It was a dialog holding one narrow column of thirty-odd entries, and it could not be scrolled at
 * all: `verticalScroll()` was applied before `heightIn(max = 440.dp)`, so the content was measured
 * at the height of its own viewport and there was never anything to scroll to. Everything past the
 * first eight keys was unreachable. Modifier order is not cosmetic — the constraint has to come
 * first, then the scroll — and the failure looks exactly like a frozen screen rather than a bug.
 *
 * ### Why the whole screen
 *
 * There is no reason to add keys one at a time through a dialog occupying a third of a phone. Three
 * columns fit every key on one screen with no scrolling at all, ticks let a whole row be assembled
 * in one pass, and one Add applies the lot. Adding ten clipboard keys was ten open-pick-reopen
 * cycles; now it is ten taps and a button.
 */
@Composable
private fun MaKeyPicker(
    macroSlots: List<MaMacroSlots.Slot>,
    onAdd: (List<MaRows.Button>) -> Unit,
    onDismiss: () -> Unit,
) {
    // Chosen order is kept rather than catalogue order: keys arrive on the row in the order they
    // were ticked, so a row can be assembled by tapping left to right.
    val chosen = remember { mutableStateListOf<MaRows.Button>() }

    Dialog(
        onDismissRequest = onDismiss,
        // The default dialog width is a fraction of the screen. This one is the screen.
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Add keys",
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                }
                Text(
                    text = "Tick as many as you like, then add them all at once.",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        // weight first, then the scroll. The bug this screen replaces was these two
                        // the other way round.
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 8.dp),
                ) {
                    // Grouped by what a key is for rather than by which kind it is in the model.
                    // The clear key is a Builtin and belongs beside the buckets it empties: a
                    // heading is a promise about what is underneath it, and somebody looking for
                    // the way to empty the buckets looks under the buckets.
                    val sections = MaRows.catalogue().groupBy { button ->
                        when {
                            button is MaRows.Button.Clip -> "Copy buckets, C1 to C10"
                            button is MaRows.Button.Builtin &&
                                button.key == MaFeatureKey.CLIP_CLEAR -> "Copy buckets, C1 to C10"
                            button is MaRows.Button.Macro -> "Your macros, M1 to M10"
                            else -> "Keys"
                        }
                    }
                    sections.forEach { (heading, buttons) ->
                        Text(
                            text = heading,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 8.dp, top = 16.dp, bottom = 4.dp),
                        )
                        // Three per row, laid out by hand rather than with a lazy grid: this is a
                        // fixed, short list inside something that already scrolls, and nesting a
                        // lazy grid in a scrolling column is the arrangement that throws.
                        buttons.chunked(3).forEach { triple ->
                            Row(modifier = Modifier.fillMaxWidth()) {
                                triple.forEach { button ->
                                    val ticked = button in chosen
                                    Row(
                                        modifier = Modifier
                                            .weight(1f)
                                            .heightIn(min = 52.dp)
                                            .clickable {
                                                if (ticked) chosen.remove(button) else chosen.add(button)
                                            }
                                            .padding(end = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Checkbox(checked = ticked, onCheckedChange = null)
                                        // The key's own glyph, the same one the row draws and the
                                        // same one the list beside it draws. Picking a key from a
                                        // column of words means matching a name to something seen
                                        // on the keyboard; picking it from its own face means
                                        // recognising it. That is the difference between reading
                                        // the list and scanning it, which on this screen is the
                                        // whole point.
                                        MaButtonGlyph(button, macroSlots)
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            text = button.title(macroSlots),
                                            style = MaterialTheme.typography.bodyMedium,
                                            maxLines = 2,
                                            modifier = Modifier.weight(1f),
                                        )
                                    }
                                }
                                // Keeps the last, short row aligned in three columns instead of
                                // letting one or two entries stretch across the width.
                                repeat(3 - triple.size) { Spacer(Modifier.weight(1f)) }
                            }
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                }

                HorizontalDivider()
                Button(
                    onClick = { onAdd(chosen.toList()) },
                    enabled = chosen.isNotEmpty(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .heightIn(min = 52.dp),
                ) {
                    Text(
                        if (chosen.isEmpty()) "Add" else "Add ${chosen.size}",
                    )
                }
            }
        }
    }
}

/**
 * Writes what one macro button does.
 *
 * The palette is the reason this is usable. MaMacroSyntax accepts a generous range of names, which
 * is right for reading a macro and useless for writing one, and `{Ctrl+Shift+Z}` cannot be dictated
 * — spoken into a field it arrives as prose. So the commands are picked from a grouped list, and
 * picking one fills the label in too when it is still blank, so the common case is one tap.
 */
@Composable
private fun MaMacroEditor(
    slot: Int,
    current: MaMacroSlots.Slot,
    onSave: (String, String) -> Unit,
    onDismiss: () -> Unit,
) {
    var label by remember { mutableStateOf(current.label) }
    // A TextFieldValue rather than a String, so the cursor can be placed after an insertion.
    //
    // With a plain String, Compose resets the selection to the start on every value change from
    // outside the field. Picking a command therefore dropped the caret in front of everything, and
    // the next command landed before the previous one — commands came out backwards, which is
    // exactly what Marko saw.
    var macro by remember {
        mutableStateOf(TextFieldValue(current.macro, TextRange(current.macro.length)))
    }
    var paletteOpen by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Macro M$slot") },
        text = {
            Column(
                modifier = Modifier
                    // Constraint first, then the scroll. The other way round measures the content
                    // at the height of its own viewport, so there is never anything to scroll to
                    // and the screen simply does not move under the finger. That is the bug that
                    // made the key picker unusable, and it was here too.
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                OutlinedTextField(
                    value = label,
                    // Truncated as it is typed rather than refused at the end, so the limit is
                    // discovered by the field simply stopping instead of by a complaint afterwards.
                    onValueChange = { label = it.take(MaMacroSlots.MAX_LABEL) },
                    label = { Text("What the key says (${MaMacroSlots.MAX_LABEL} characters)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = macro,
                    onValueChange = { macro = it },
                    label = { Text("What it does") },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 96.dp),
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Plain text types itself. Anything in braces is a real key press.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { paletteOpen = true },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                ) { Text("Pick a command") }
                DropdownMenu(expanded = paletteOpen, onDismissRequest = { paletteOpen = false }) {
                    MaCommandPalette.GROUPS.forEach { group ->
                        Text(
                            text = group.name,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                        )
                        group.entries.forEach { e ->
                            DropdownMenuItem(
                                text = { Text("${e.label}   ${e.description}") },
                                onClick = {
                                    // Appended rather than replacing: a macro is usually a short
                                    // sequence rather than a single key.
                                    // One command per line, and the caret left after it.
                                    //
                                    // A newline because a macro of several commands is a list of
                                    // steps and reads like one; running ignores the whitespace, so
                                    // the layout is free. The caret goes to the end so the next
                                    // pick lands after this one rather than in front of it.
                                    val existing = macro.text
                                    val joined = when {
                                        existing.isBlank() -> e.token
                                        existing.endsWith("\n") -> existing + e.token
                                        else -> existing + "\n" + e.token
                                    }
                                    macro = TextFieldValue(joined, TextRange(joined.length))
                                    if (label.isBlank() || label == "M$slot") {
                                        label = e.label.take(MaMacroSlots.MAX_LABEL)
                                    }
                                    paletteOpen = false
                                },
                            )
                        }
                        HorizontalDivider()
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                // A blank label is filled back in with the slot's own name rather than left empty,
                // so an unconfigured key still says which slot it is instead of showing nothing.
                onClick = { onSave(label.ifBlank { "M$slot" }, macro.text) },
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
