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
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.SpaceBar
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
import androidx.compose.runtime.LaunchedEffect
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
import dev.patrickgold.jetpref.datastore.ui.SwitchPreference
import dev.patrickgold.florisboard.lib.compose.FlorisScreen
import dev.patrickgold.jetpref.datastore.model.collectAsState
import kotlinx.coroutines.launch

/**
 * What the magic button presses, and in what order it tries.
 *
 * Deliberately the same screen as the feature row editor: a numbered list, a drag handle, a tick and
 * a bin. Two editors that behave differently are two things to learn, and this one is reached by
 * holding the magic button, which is already a discovery — the screen it opens should not be a second one.
 *
 * The order is the feature rather than decoration. The magic button presses the first term it finds, so a
 * screen carrying both `Send` and `Save` presses whichever sits higher here. Dragging is how the
 * user says which they meant.
 */
@Composable
fun MaMagicScreen() = FlorisScreen {
    title = "Magic finger"

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

        // Bring an existing list up to the current crop of built-ins, once.
        //
        // Here rather than at app start because this is the screen where the change is visible. A
        // list that quietly grew two entries while he was somewhere else is a list that looks like
        // it changed itself; arriving on the screen that shows the terms means the new ones are
        // simply there, in front of him, the first time he looks.
        //
        // LaunchedEffect keyed to Unit so it runs on entry and not on every recomposition, and the
        // version is written whether or not anything was added — the flag records that the offer
        // was made, not that it was accepted.
        val defaultsVersion by prefs.dictate.maMagicDefaultsVersion.collectAsState()
        LaunchedEffect(Unit) {
            if (defaultsVersion < MaMagicTargets.DEFAULTS_VERSION) {
                val merged = MaMagicTargets.mergeNewDefaults(
                    MaMagicTargets.parse(prefs.dictate.maMagicTargets.get()),
                    defaultsVersion,
                )
                prefs.dictate.maMagicDefaultsVersion.set(MaMagicTargets.DEFAULTS_VERSION)
                if (merged.isNotEmpty()) {
                    targets = merged
                    prefs.dictate.maMagicTargets.set(MaMagicTargets.serialize(merged))
                }
            }
        }

        // The switch that puts the row on the keyboard, above the list it draws.

        //

        // A switch rather than a key in the row editor: this row's contents are these terms, so it

        // belongs to this screen. And on means on — it cannot drift out of an arrangement by

        // accident, which is what happened to the settings key.

        SwitchPreference(

            prefs.dictate.maMagicRowShown,

            title = "Show the magic finger row",

            summary = "A row of its own on the keyboard: the magic finger, then one key for each term below. " +

                "Turn it off here to take it away.",

        )

        Spacer(Modifier.height(8.dp))

        // How it works, in his words rather than Android's. Somebody who knows what the finger is
        // doing can tell a term that was typed wrong from a button that is not really a button, and
        // that is the difference between fixing it and giving up on it.
        Text(
            text = "How it works: the finger reads the screen the way a screen reader does — every " +
                "button on it announces a name — finds the name you gave it below, and presses it. " +
                "It is the same accessibility service the floating button uses, so if it stops " +
                "working, that service has been switched off.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )

        Spacer(Modifier.height(8.dp))

        SwitchPreference(

            prefs.dictate.maVoiceCommands,

            title = "Voice commands",

            summary = "Say \"press send\" on its own and the finger presses send instead of typing " +
                "the words. Two words only — press, and the name of the button. A longer sentence " +
                "is always text, and if nothing on screen answers to that name the words are " +
                "typed as usual, so nothing is lost. Works in Croatian too: pritisni, stisni, klikni.",

        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = "The magic finger presses the first of these it finds on screen. Drag to change which " +
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
                    if (target.isSpacer) {
                        // A spacer has nothing to search for, nothing to name and nothing to
                        // switch off, so it is drawn with none of those controls rather than with
                        // controls that do nothing. It keeps the grip and the bin, because moving
                        // it and removing it are the only two things it is for.
                        Text(
                            text = "Room",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = if (lifted) FontWeight.SemiBold else FontWeight.Normal,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 12.dp),
                        )
                        Text(
                            text = "a gap on the row, its width set in Feature row",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 8.dp),
                        )
                    } else {
                    Text(
                        // The face, so the row reads like the key it draws.
                        text = target.face,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = if (lifted) FontWeight.SemiBold else FontWeight.Normal,
                        // Dimmed when off, the way an empty bucket is dimmed. Same signal
                        // everywhere: present, and not doing anything at the moment.
                        color = if (target.enabled) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.padding(vertical = 12.dp),
                    )
                    // Which app the term belongs to, because the same word can now appear twice.
                    // "Send" for Claude and "Send" for Gemini are two rows that would otherwise be
                    // indistinguishable, and deleting the wrong one is silent.
                    Text(
                        // The term when a label is hiding it, so the row still says what it
                        // searches for. A key labelled STOP that silently looks for something else
                        // is a row nobody can check.
                        text = buildString {
                            if (target.label.isNotBlank()) append(target.term).append("  \u00b7  ")
                            append(target.appPackage?.substringAfterLast('.') ?: "any app")
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 8.dp),
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
                    // A pencil, because this opens the editor.
                    //
                    // It was drawing a drag handle, which is why Marko could not find it: the row
                    // is dragged to reorder, so a grip on it says "hold me and move", and he read
                    // it as exactly that. An icon that describes a different gesture from the one it
                    // performs is worse than no icon — he was looking straight at the control and
                    // could not see it.
                    IconButton(onClick = { editing = index }) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Edit this term",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(22.dp),
                        )
                    }
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
                    // The grip, last, exactly where the feature row editor puts it.
                    //
                    // This list has been draggable all along and never showed one: the icon that
                    // looked like a grip was the edit button. So the header said "drag to change
                    // which is tried first" while the only thing resembling a handle opened a
                    // dialog. Now the pencil edits and the grip is a grip, in the same order and
                    // the same position as the other editor, because somebody who has learned one
                    // screen should not have to learn the second.
                    Icon(
                        imageVector = Icons.Default.DragHandle,
                        contentDescription = "Hold and drag to move",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 4.dp).size(22.dp),
                    )
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
            // Out here rather than at the foot of the term dialog, which is where it was first
            // going to go. A spacer has no name to type, so putting it inside the naming dialog
            // would mean opening a form to add the one thing with nothing to fill in. It arrives
            // at the bottom of the list and is dragged to where the gap is wanted, exactly like a
            // term.
            OutlinedButton(
                onClick = { commit(targets + MaMagicTargets.spacer()) },
                modifier = Modifier.weight(1f),
            ) {
                Icon(Icons.Default.SpaceBar, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("Add a spacer")
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TextButton(onClick = { commit(MaMagicTargets.defaults()) }) { Text("Reset") }
        }

        Spacer(Modifier.height(24.dp))

        val editIndex = editing
        if (adding || editIndex != null) {
            MaTermEditor(
                initial = editIndex?.let { targets.getOrNull(it)?.term } ?: "",
                initialLabel = editIndex?.let { targets.getOrNull(it)?.label } ?: "",
                onSave = { term, label ->
                    if (editIndex != null) {
                        commit(
                            targets.mapIndexed { i, t ->
                                if (i == editIndex) t.copy(term = term, label = label) else t
                            },
                        )
                    } else {
                        commit(targets + MaMagicTargets.Target(term, label = label))
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
    initialLabel: String,
    onSave: (String, String) -> Unit,
    onDismiss: () -> Unit,
) {
    var text by remember { mutableStateOf(initial) }
    var label by remember { mutableStateOf(initialLabel) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial.isBlank()) "Add a term" else "Edit the term") },
        text = {
            Column {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    // Named for what it is rather than for what it does. This is the text on the
                    // real button, copied exactly; the key's own wording is the field below.
                    label = { Text("The button on screen, word for word") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text("What the key says (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Leave this empty and the key shows the term itself. Give it something " +
                        "short when the term is long \u2014 \"Stop responding\" is the right thing to " +
                        "search for and too wide to write on a key.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(text.trim(), label.trim()) },
                // A blank term would match every control on screen, so the magic button would press
                // whatever it happened to reach first. Worse than a term that finds nothing.
                // A blank label is fine: the key falls back to the term.
                enabled = text.isNotBlank(),
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
