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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.patrickgold.florisboard.app.FlorisPreferenceStore
import dev.patrickgold.florisboard.dictate.MaMagicTargets
import dev.patrickgold.florisboard.lib.compose.FlorisScreen
import dev.patrickgold.jetpref.datastore.model.collectAsState
import kotlinx.coroutines.launch

/**
 * What the magic wand looks for, and in what order.
 *
 * Deliberately the same screen as the feature row editor: a numbered list, a drag handle, a tick and
 * a bin. Two editors that behave differently are two things to learn, and this one is reached by
 * holding the wand, which is already a discovery — the screen it opens should not be a second one.
 *
 * The order is the feature rather than decoration. The wand presses the first term it finds, so a
 * screen carrying both `Send` and `Save` presses whichever sits higher here. Dragging is how the
 * user says which they meant.
 */
@Composable
fun MaMagicScreen() = FlorisScreen {
    title = "Magic wand"

    content {
        val prefs by FlorisPreferenceStore
        val scope = rememberCoroutineScope()
        val raw by prefs.dictate.maMagicTargets.collectAsState()

        var targets by remember(raw) {
            mutableStateOf(MaMagicTargets.parse(raw).ifEmpty { MaMagicTargets.defaults() })
        }
        var editing by remember { mutableStateOf<Int?>(null) }
        var adding by remember { mutableStateOf(false) }

        fun commit(next: List<MaMagicTargets.Target>) {
            targets = next
            scope.launch { prefs.dictate.maMagicTargets.set(MaMagicTargets.serialize(next)) }
        }

        Text(
            text = "The wand presses the first of these it finds on screen. Drag to change which " +
                "is tried first.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
        Text(
            text = "A word on its own matches a whole word, so \"send\" never fires on \"resend\". " +
                "Several words match as a phrase, so \"generate image\" still finds " +
                "\"Generate Images 2\" however the number beside it changes.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp),
        )

        Spacer(Modifier.height(12.dp))

        MaReorderableColumn(
            items = targets,
            rowHeight = ROW_HEIGHT,
            onMove = { from, to -> targets = MaMagicTargets.move(targets, from, to) },
            onSettled = { commit(targets) },
        ) { index, target, lifted ->
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
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "${index + 1}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.width(24.dp),
                    )
                    Text(
                        text = target.term,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = if (lifted) FontWeight.SemiBold else FontWeight.Normal,
                        // Dimmed when off, the way an empty bucket is dimmed. Same signal
                        // everywhere: present, and not doing anything at the moment.
                        color = if (target.enabled) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier
                            .weight(1f)
                            .padding(vertical = 12.dp),
                    )
                    Checkbox(
                        checked = target.enabled,
                        onCheckedChange = { on ->
                            commit(
                                targets.mapIndexed { i, t ->
                                    if (i == index) t.copy(enabled = on) else t
                                },
                            )
                        },
                    )
                    IconButton(onClick = { editing = index }) {
                        Icon(
                            Icons.Default.DragHandle,
                            contentDescription = "Edit this term",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                    IconButton(
                        onClick = { commit(targets.filterIndexed { i, _ -> i != index }) },
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Remove this term",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedButton(
                onClick = { adding = true },
                modifier = Modifier.weight(1f),
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("Add a term")
            }
            TextButton(onClick = { commit(MaMagicTargets.defaults()) }) { Text("Reset") }
        }

        Spacer(Modifier.height(24.dp))

        val editIndex = editing
        if (adding || editIndex != null) {
            MaTermEditor(
                initial = editIndex?.let { targets.getOrNull(it)?.term } ?: "",
                onSave = { term ->
                    if (editIndex != null) {
                        commit(
                            targets.mapIndexed { i, t ->
                                if (i == editIndex) t.copy(term = term) else t
                            },
                        )
                    } else {
                        commit(targets + MaMagicTargets.Target(term))
                    }
                    adding = false
                    editing = null
                },
                onDismiss = {
                    adding = false
                    editing = null
                },
            )
        }
    }
}

@Composable
private fun MaTermEditor(
    initial: String,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var text by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial.isBlank()) "Add a term" else "Edit the term") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("What the button says") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(text.trim()) },
                // A blank term would match every control on screen, so the wand would press
                // whatever it happened to reach first. Worse than a term that finds nothing.
                enabled = text.isNotBlank(),
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
