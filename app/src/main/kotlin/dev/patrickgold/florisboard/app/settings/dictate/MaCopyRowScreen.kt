
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

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.patrickgold.florisboard.app.FlorisPreferenceStore
import dev.patrickgold.florisboard.dictate.MaMacroSlots
import dev.patrickgold.florisboard.dictate.MaRows
import dev.patrickgold.florisboard.lib.compose.FlorisScreen
import dev.patrickgold.jetpref.datastore.model.collectAsState
import kotlinx.coroutines.launch

/**
 * What a button is called in this list.
 *
 * Its own small function rather than reaching for the feature row editor's version, which is
 * private and needs the macro slot table to name a macro. This screen shows a slot number, which is
 * enough here and costs nothing to keep true.
 */
private fun copyRowLabel(button: MaRows.Button): String = when (button) {
    is MaRows.Button.Builtin -> button.key.label
    is MaRows.Button.Clip -> "Copy bucket ${button.slot}"
    is MaRows.Button.Macro -> "Macro ${button.slot}"
}

/**
 * The copy row: one row, edited exactly like a feature row.
 *
 * ### Why it has a screen of its own
 *
 * It is a special case of a feature row and it earns the separation by *where it appears*: only in
 * the transcription view, which has no letter keys and where the clipboard is most of the job. The
 * three feature rows belong to the typing keyboard. Mixing them into one editor would mean four
 * tabs where one of them behaves differently from the other three, which is worse than two screens.
 *
 * ### And why it reuses everything else
 *
 * The same `MaRows.Row`, the same `Button` catalogue, the same entries. **A key added to the app
 * appears here without anybody remembering to add it**, and a key behaves identically wherever it
 * is placed. Only the storage and the surface differ.
 */
@Composable
fun MaCopyRowScreen() = FlorisScreen {
    title = "Copy row"

    content {
        val prefs by FlorisPreferenceStore
        val scope = rememberCoroutineScope()
        val raw by prefs.dictate.maCopyRow.collectAsState()
        var row by remember(raw) { mutableStateOf(MaRows.parseCopyRow(raw)) }
        var picking by remember { mutableStateOf(false) }
        val macroSlots = remember { emptyList<MaMacroSlots.Slot>() }

        fun commit(next: MaRows.Row) {
            row = next
            scope.launch { prefs.dictate.maCopyRow.set(MaRows.serializeCopyRow(next)) }
        }

        Text(
            text = "One row of clipboard keys. Tick where it should appear \u2014 it can be on both.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )

        // Where it appears, as two independent switches rather than one either/or.
        //
        // It is the same row drawn in two places, not two rows to keep in step — so both, one or
        // neither are all sensible answers and none of them needs a second copy of anything.
        val onKeyboard by prefs.dictate.maCopyRowOnKeyboard.collectAsState()
        val onDictate by prefs.dictate.maCopyRowOnDictate.collectAsState()
        SurfaceTick("On the typing keyboard", onKeyboard) {
            scope.launch { prefs.dictate.maCopyRowOnKeyboard.set(it) }
        }
        SurfaceTick("In the transcription view", onDictate) {
            scope.launch { prefs.dictate.maCopyRowOnDictate.set(it) }
        }

        Spacer(Modifier.height(12.dp))

        Text(
            text = "Hold a key and drag to move it. Untick to take it off the row.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        )

        // The very same list the feature row editor draws — icon, tick, position, drag handle.
        //
        // Reused rather than rebuilt, because two implementations of one list is two places for
        // them to drift apart, and he should not have to learn a second way to arrange keys.
        MaReorderableColumn(
            items = row.entries,
            rowHeight = ROW_HEIGHT,
            onMove = { from, to ->
                val entries = row.entries.toMutableList()
                entries.add(to, entries.removeAt(from))
                row = row.copy(entries = entries)
            },
            onSettled = { commit(row) },
        ) { index, entry, lifted ->
            MaRowKeyItem(
                position = index + 1,
                entry = entry,
                macroSlots = macroSlots,
                lifted = lifted,
                accent = MaCopyRowAccent,
                onToggle = {
                    commit(
                        row.copy(
                            entries = row.entries.mapIndexed { j, e ->
                                if (j == index) e.copy(enabled = !e.enabled) else e
                            },
                        ),
                    )
                },
                onEditMacro = {},
                onRemove = {
                    commit(row.copy(entries = row.entries.filterIndexed { j, _ -> j != index }))
                },
            )
        }

        TextButton(
            onClick = { picking = true },
            modifier = Modifier.padding(horizontal = 12.dp),
        ) { Text("Add a key to the copy row") }

        TextButton(
            onClick = { commit(MaRows.defaultCopyRow()) },
            modifier = Modifier.padding(horizontal = 12.dp),
        ) { Text("Reset to the default row") }

        Spacer(Modifier.height(24.dp))

        if (picking) {
            MaKeyPicker(
                macroSlots = macroSlots,
                onDismiss = { picking = false },
                // The picker adds several at once, in the order they were ticked, so a row can be
                // assembled left to right in one visit.
                onAdd = { buttons ->
                    picking = false
                    commit(row.copy(entries = row.entries + buttons.map { MaRows.Entry(it) }))
                },
            )
        }
    }
}

/**
 * The copy row's own colour: a dark amber, dim enough to sit under white text.
 *
 * Not the sand used on the keyboard itself. That colour means "this key is holding something"
 * throughout the app, and reusing it here for "this is the copy row" would give one colour two
 * jobs — the first time a lit key and a copy row appeared on one screen they would say the same
 * thing and mean different ones.
 */
private val MaCopyRowAccent = Color(0xFF3A2A16)

/** One "where does it appear" tick. Two of them, and they are independent. */
@Composable
private fun SurfaceTick(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = checked, onCheckedChange = onChange)
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}
