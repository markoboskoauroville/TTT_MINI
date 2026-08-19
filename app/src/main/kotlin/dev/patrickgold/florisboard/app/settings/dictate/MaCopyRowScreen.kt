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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.patrickgold.florisboard.app.FlorisPreferenceStore
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
        val row = remember(raw) { MaRows.parseCopyRow(raw) }

        fun save(next: MaRows.Row) {
            scope.launch { prefs.dictate.maCopyRow.set(MaRows.serializeCopyRow(next)) }
        }

        Text(
            text = "One row of keys, shown only in the transcription view \u2014 the screen with no " +
                "letters, where the clipboard is most of the work. The three feature rows belong " +
                "to the typing keyboard and are edited under Feature row.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(
                checked = row.enabled,
                onCheckedChange = { on -> save(row.copy(enabled = on)) },
            )
            Column(modifier = Modifier.padding(start = 8.dp)) {
                Text("Show the copy row", style = MaterialTheme.typography.bodyMedium)
                Text(
                    text = "Off leaves no gap \u2014 the rows below move up",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Text(
            text = "Untick a key to take it off the row.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        )

        // Every key in the app, ticked when it is on this row.
        //
        // Driven from the catalogue rather than from a list kept here, so a key added anywhere in
        // the app turns up in this screen with nothing to remember.
        val onRow = row.entries.map { it.button }.toSet()
        for (button in MaRows.catalogue()) {
            val present = button in onRow
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(
                    checked = present,
                    onCheckedChange = { on ->
                        val next = if (on) {
                            row.copy(entries = row.entries + MaRows.Entry(button))
                        } else {
                            row.copy(entries = row.entries.filterNot { it.button == button })
                        }
                        save(next)
                    },
                )
                Text(
                    text = copyRowLabel(button),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        TextButton(
            onClick = { save(MaRows.defaultCopyRow()) },
            modifier = Modifier.padding(horizontal = 8.dp),
        ) { Text("Reset to the default row") }

        Spacer(Modifier.height(24.dp))
    }
}
