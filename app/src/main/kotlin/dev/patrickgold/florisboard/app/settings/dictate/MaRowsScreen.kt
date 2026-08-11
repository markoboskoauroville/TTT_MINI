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

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.patrickgold.florisboard.app.FlorisPreferenceStore
import dev.patrickgold.florisboard.dictate.MaCommandPalette
import dev.patrickgold.florisboard.dictate.MaFeatureKey
import dev.patrickgold.florisboard.dictate.MaRows
import dev.patrickgold.florisboard.lib.compose.FlorisScreen
import dev.patrickgold.jetpref.datastore.model.collectAsState
import kotlinx.coroutines.launch

/**
 * The one editor for every key above the keyboard.
 *
 * This replaced two screens that each did half the job: a feature row editor that could reorder
 * built-in keys and not add any, and a macro editor that could add custom keys and not place a
 * built-in one among them.
 *
 * ### Arrows rather than dragging
 *
 * The screen it replaces reordered by holding a key and dragging it. That is the wrong interaction
 * here and it was worth losing. Marko reads this screen with low vision and works largely by voice:
 * a drag needs the target held under a fingertip and tracked while it moves, which is precisely the
 * kind of fine, sighted, sustained gesture that costs him the most. Arrow buttons are large fixed
 * targets that can be hit repeatedly without tracking anything.
 *
 * There is also a plainer reason. A row of keys wraps and scrolls sideways, and dragging within
 * something that scrolls sideways inside something that scrolls vertically is the interaction that
 * fights back hardest on a phone.
 */
@Composable
fun MaRowsScreen() = FlorisScreen {
    title = "Keys and rows"

    content {
        val prefs by FlorisPreferenceStore
        val scope = rememberCoroutineScope()
        val rowsRaw by prefs.dictate.maRows.collectAsState()

        // Held locally and written on every change. Editing a row is a deliberate act with a pause
        // after it, not a drag producing a value per frame, so there is nothing here worth batching
        // and a write straight through means the keyboard behind the settings updates as he goes.
        val rows: List<List<MaRows.Button>> = remember(rowsRaw) {
            MaRows.parse(rowsRaw).ifEmpty { MaRows.defaultRows() }
        }

        fun commit(next: List<List<MaRows.Button>>) {
            // An empty list would be read back as "not migrated yet" by the keyboard and silently
            // replaced with the defaults, so a row set emptied down to nothing has to be stored as
            // one empty row rather than as no rows at all.
            val safe = next.filter { it.isNotEmpty() }.ifEmpty { MaRows.defaultRows() }
            scope.launch { prefs.dictate.maRows.set(MaRows.serialize(safe)) }
        }

        var editing by remember { mutableStateOf<Pair<Int, Int>?>(null) }
        var addingToRow by remember { mutableStateOf<Int?>(null) }

        Text(
            text = "Every key above the keyboard lives here. A key is either one of the app's own " +
                "or one you write yourself, and both can sit in the same row in any order.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
        Text(
            text = "Add as many rows as you like. Backspace and enter cannot be removed from every " +
                "row at once: with the keyboard folded away there would be no way to delete a " +
                "character or end a line.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp),
        )

        Spacer(Modifier.height(12.dp))

        rows.forEachIndexed { rowIndex, row ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 4.dp, top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Row ${rowIndex + 1}",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(
                        onClick = {
                            if (rowIndex > 0) {
                                commit(rows.toMutableList().apply { add(rowIndex - 1, removeAt(rowIndex)) })
                            }
                        },
                        enabled = rowIndex > 0,
                    ) { Icon(Icons.Default.ArrowUpward, contentDescription = "Move row up") }
                    IconButton(
                        onClick = {
                            if (rowIndex < rows.lastIndex) {
                                commit(rows.toMutableList().apply { add(rowIndex + 1, removeAt(rowIndex)) })
                            }
                        },
                        enabled = rowIndex < rows.lastIndex,
                    ) { Icon(Icons.Default.ArrowDownward, contentDescription = "Move row down") }
                    IconButton(
                        onClick = { commit(rows.filterIndexed { i, _ -> i != rowIndex }) },
                    ) { Icon(Icons.Default.Delete, contentDescription = "Delete row ${rowIndex + 1}") }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    row.forEachIndexed { buttonIndex, button ->
                        OutlinedButton(
                            onClick = { editing = rowIndex to buttonIndex },
                            modifier = Modifier.heightIn(min = 48.dp),
                        ) {
                            Text(
                                text = button.face(),
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp,
                            )
                        }
                    }
                    OutlinedButton(
                        onClick = { addingToRow = rowIndex },
                        modifier = Modifier.heightIn(min = 48.dp),
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("Key")
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedButton(
                onClick = { commit(rows + listOf(listOf(MaRows.macro("new", "")))) },
                modifier = Modifier.weight(1f),
            ) { Text("Add a row") }
            TextButton(
                onClick = { commit(MaRows.defaultRows()) },
            ) { Text("Reset") }
        }

        Spacer(Modifier.height(24.dp))

        editing?.let { (rowIndex, buttonIndex) ->
            val current = rows.getOrNull(rowIndex)?.getOrNull(buttonIndex)
            if (current == null) {
                editing = null
            } else {
                MaButtonEditor(
                    initial = current,
                    canMoveLeft = buttonIndex > 0,
                    canMoveRight = buttonIndex < rows[rowIndex].lastIndex,
                    onMove = { direction ->
                        val target = buttonIndex + direction
                        commit(
                            rows.mapIndexed { i, r ->
                                if (i != rowIndex) r
                                else r.toMutableList().apply { add(target, removeAt(buttonIndex)) }
                            },
                        )
                        editing = null
                    },
                    onDelete = {
                        commit(
                            rows.mapIndexed { i, r ->
                                if (i != rowIndex) r else r.filterIndexed { j, _ -> j != buttonIndex }
                            },
                        )
                        editing = null
                    },
                    onSave = { updated ->
                        commit(
                            rows.mapIndexed { i, r ->
                                if (i != rowIndex) r
                                else r.mapIndexed { j, b -> if (j == buttonIndex) updated else b }
                            },
                        )
                        editing = null
                    },
                    onDismiss = { editing = null },
                )
            }
        }

        addingToRow?.let { rowIndex ->
            MaButtonEditor(
                initial = MaRows.macro("", ""),
                canMoveLeft = false,
                canMoveRight = false,
                onMove = {},
                onDelete = { addingToRow = null },
                onSave = { added ->
                    commit(rows.mapIndexed { i, r -> if (i == rowIndex) r + added else r })
                    addingToRow = null
                },
                onDismiss = { addingToRow = null },
            )
        }
    }
}

/** What the key shows on its face, for the editor's own preview of it. */
private fun MaRows.Button.face(): String = when (this) {
    is MaRows.Button.Builtin -> key.label
    is MaRows.Button.Macro -> label.ifBlank { "\u00b7\u00b7\u00b7" }
}

/**
 * Edits one key: which kind it is, what it says, and what it does.
 *
 * The palette is the point of the whole dialog. [dev.patrickgold.florisboard.dictate.MaMacroSyntax]
 * accepts a generous range of names, which is right for reading a macro and useless for writing one,
 * and `{Ctrl+Shift+Z}` cannot be dictated — spoken into a text field it arrives as prose. So the
 * commands are picked from a list instead of typed, and picking one fills in the label as well, so
 * the ordinary case is a single choice and no typing at all.
 */
@Composable
private fun MaButtonEditor(
    initial: MaRows.Button,
    canMoveLeft: Boolean,
    canMoveRight: Boolean,
    onMove: (Int) -> Unit,
    onDelete: () -> Unit,
    onSave: (MaRows.Button) -> Unit,
    onDismiss: () -> Unit,
) {
    var isBuiltin by remember { mutableStateOf(initial is MaRows.Button.Builtin) }
    var builtinKey by remember {
        mutableStateOf((initial as? MaRows.Button.Builtin)?.key ?: MaFeatureKey.MIC)
    }
    var label by remember { mutableStateOf((initial as? MaRows.Button.Macro)?.label ?: "") }
    var macro by remember { mutableStateOf((initial as? MaRows.Button.Macro)?.macro ?: "") }
    var builtinMenuOpen by remember { mutableStateOf(false) }
    var paletteOpen by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isBuiltin) "A key of the app's own" else "A key you write") },
        text = {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .heightIn(max = 420.dp),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { isBuiltin = true },
                        enabled = !isBuiltin,
                        modifier = Modifier.weight(1f),
                    ) { Text("App key") }
                    OutlinedButton(
                        onClick = { isBuiltin = false },
                        enabled = isBuiltin,
                        modifier = Modifier.weight(1f),
                    ) { Text("Macro") }
                }

                Spacer(Modifier.height(12.dp))

                if (isBuiltin) {
                    OutlinedButton(
                        onClick = { builtinMenuOpen = true },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                    ) { Text(builtinKey.label) }
                    DropdownMenu(
                        expanded = builtinMenuOpen,
                        onDismissRequest = { builtinMenuOpen = false },
                    ) {
                        MaFeatureKey.entries.forEach { key ->
                            DropdownMenuItem(
                                text = { Text(key.label) },
                                onClick = {
                                    builtinKey = key
                                    builtinMenuOpen = false
                                },
                            )
                        }
                    }
                } else {
                    OutlinedTextField(
                        value = label,
                        // Truncated as it is typed rather than rejected at the end, so the limit is
                        // discovered by the field simply stopping rather than by a complaint after
                        // the work is done.
                        onValueChange = { label = it.take(MaRows.MAX_LABEL) },
                        label = { Text("What the key says (${MaRows.MAX_LABEL} characters)") },
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
                    DropdownMenu(
                        expanded = paletteOpen,
                        onDismissRequest = { paletteOpen = false },
                    ) {
                        MaCommandPalette.GROUPS.forEach { group ->
                            Text(
                                text = group.name,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                            )
                            group.entries.forEach { entry ->
                                DropdownMenuItem(
                                    text = { Text("${entry.label}   ${entry.description}") },
                                    onClick = {
                                        // Appended rather than replacing, so several commands can be
                                        // built into one key: a macro is often a short sequence.
                                        macro += entry.token
                                        if (label.isBlank()) label = entry.label.take(MaRows.MAX_LABEL)
                                        paletteOpen = false
                                    },
                                )
                            }
                            HorizontalDivider()
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(Modifier.height(8.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { onMove(-1) },
                        enabled = canMoveLeft,
                        modifier = Modifier.weight(1f),
                    ) { Text("\u2190 Earlier") }
                    OutlinedButton(
                        onClick = { onMove(1) },
                        enabled = canMoveRight,
                        modifier = Modifier.weight(1f),
                    ) { Text("Later \u2192") }
                }
                TextButton(
                    onClick = onDelete,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Remove this key") }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(
                        if (isBuiltin) MaRows.Button.Builtin(builtinKey)
                        else MaRows.macro(label, macro),
                    )
                },
                // A macro key with nothing to do is a key that looks like it works and does not.
                // A blank label is allowed: the key shows dots and can be named later.
                enabled = isBuiltin || macro.isNotBlank(),
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
