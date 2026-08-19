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

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.patrickgold.florisboard.app.FlorisPreferenceStore
import dev.patrickgold.florisboard.dictate.MaFlow
import dev.patrickgold.florisboard.dictate.MaProofread
import dev.patrickgold.florisboard.dictate.MaPrompts
import dev.patrickgold.florisboard.lib.compose.FlorisScreen
import dev.patrickgold.jetpref.datastore.model.PreferenceData
import dev.patrickgold.jetpref.datastore.model.collectAsState
import kotlinx.coroutines.launch

/**
 * Prompts, as categories holding named instances — the Avid model where it earns its keep.
 *
 * ### What this screen is
 *
 * Two categories, Ctrl+P and Ctrl+F. Each holds as many named wordings as he likes and exactly one
 * carries the checkmark, which is the one the key sends. Choosing another visibly takes the mark
 * from the last, because it is a radio and not a tick-box: there is one wording in force and the
 * control should say so rather than leave two looking equally chosen.
 *
 * ### Duplicate is the main verb, not delete
 *
 * Nobody edits a working configuration directly. They copy it, rename the copy and edit that, so the
 * thing that was working is still there when the experiment turns out worse. The Default cannot be
 * edited at all for the same reason — its editor offers exactly one action, which is to duplicate
 * it and open the copy.
 *
 * ### Why not a tree with twist-downs
 *
 * Because two categories are not a tree. Section 96 records the order: instances first, and the
 * folder structure only if enough categories end up holding several of them. Nineteen settings
 * screens behind collapsed triangles, most of them holding one thing, would put every setting one
 * tap further away and call it an improvement.
 */
@Composable
fun MaPromptsScreen() = FlorisScreen {
    title = "Prompts"

    content {
        val prefs by FlorisPreferenceStore

        Text(
            text = "Ctrl+P and Ctrl+F each send an instruction to the rewording model. Keep as " +
                "many wordings as you like and tick the one in force. Default is the instruction " +
                "built into the app \u2014 it cannot be edited, only duplicated.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )

        PromptCategory(
            title = "Ctrl + P \u2014 Proofread",
            pref = prefs.dictate.maProofreadPrompt,
            shipped = MaProofread.INSTRUCTION,
        )

        Spacer(Modifier.height(20.dp))

        PromptCategory(
            title = "Ctrl + F \u2014 Better flow",
            pref = prefs.dictate.maFlowPrompt,
            shipped = MaFlow.INSTRUCTION,
        )

        Spacer(Modifier.height(28.dp))
    }
}

/** One category: its instances, the checkmark, and the three verbs. */
@Composable
private fun PromptCategory(
    title: String,
    pref: PreferenceData<String>,
    shipped: String,
) {
    val scope = rememberCoroutineScope()
    val raw by pref.collectAsState()
    // Parsed from the stored string on every change rather than held in a separate state. The
    // preference is the truth; a copy kept beside it drifts the moment anything else writes it,
    // and a restored profile writes exactly this.
    val set = remember(raw) { MaPrompts.parse(raw) }

    var editing by remember { mutableStateOf<MaPrompts.Instance?>(null) }
    var confirmDelete by remember { mutableStateOf<MaPrompts.Instance?>(null) }

    fun save(next: MaPrompts.Set) {
        scope.launch { pref.set(MaPrompts.serialize(next)) }
    }

    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
    )

    for (inst in set.instances) {
        val isActive = inst.name == set.active.name
        // The wording this row would actually send, so the Default shows the shipped words rather
        // than an empty line. Nothing on screen should read as blank when it is not.
        val shown = inst.text.ifBlank { shipped }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { editing = inst }
                .padding(start = 8.dp, end = 8.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RadioButton(
                selected = isActive,
                onClick = { save(set.copy(activeName = inst.name)) },
            )
            Column(modifier = Modifier.weight(1f).padding(start = 4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = inst.name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal,
                    )
                    if (inst.locked) {
                        Text(
                            text = "  built in",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Text(
                    text = shown,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            IconButton(
                onClick = {
                    val name = MaPrompts.freeName(set.instances, inst.name)
                    val copy = MaPrompts.Instance(name, shown)
                    // The copy is added and opened, but NOT made active. Duplicating is the start of
                    // an experiment, and a key that changed what it does the moment you pressed
                    // copy would be a nasty surprise mid-sentence.
                    save(set.copy(instances = set.instances + copy))
                    editing = copy
                },
                modifier = Modifier.size(40.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = "Duplicate ${inst.name}",
                    modifier = Modifier.size(20.dp),
                )
            }
            if (!inst.locked) {
                IconButton(
                    onClick = { confirmDelete = inst },
                    modifier = Modifier.size(40.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete ${inst.name}",
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
    }

    TextButton(
        onClick = {
            val name = MaPrompts.freeName(set.instances, MaPrompts.DEFAULT_NAME)
            val copy = MaPrompts.Instance(name, shipped)
            save(set.copy(instances = set.instances + copy))
            editing = copy
        },
        modifier = Modifier.padding(start = 8.dp),
    ) { Text("New wording") }

    editing?.let { inst ->
        PromptEditor(
            instance = inst,
            shipped = shipped,
            existing = set.instances,
            onDismiss = { editing = null },
            onDuplicate = {
                val name = MaPrompts.freeName(set.instances, inst.name)
                val copy = MaPrompts.Instance(name, inst.text.ifBlank { shipped })
                save(set.copy(instances = set.instances + copy))
                editing = copy
            },
            onSave = { newName, newText ->
                val renamed = MaPrompts.Instance(newName, newText)
                val instances = set.instances.map { if (it.name == inst.name) renamed else it }
                // The checkmark follows a rename. It is stored by name, so leaving it pointing at
                // the old one would silently drop the category back to Default — the exact failure
                // storing by name was meant to prevent, arriving through the back door.
                val active = if (set.activeName == inst.name) newName else set.activeName
                save(MaPrompts.Set(instances, active))
                editing = null
            },
        )
    }

    confirmDelete?.let { inst ->
        AlertDialog(
            onDismissRequest = { confirmDelete = null },
            title = { Text("Delete ${inst.name}?") },
            text = { Text("The wording goes with it. Default cannot be deleted, so the key always has something to send.") },
            confirmButton = {
                TextButton(onClick = {
                    val instances = set.instances.filterNot { it.name == inst.name }
                    // Deleting the active one hands the checkmark back to Default rather than to
                    // whatever happened to be next in the list, which would be a wording he never
                    // chose taking over a key.
                    val active = if (set.activeName == inst.name) MaPrompts.DEFAULT_NAME else set.activeName
                    save(MaPrompts.Set(instances, active))
                    confirmDelete = null
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = null }) { Text("Cancel") }
            },
        )
    }
}

/**
 * The detail pane, as a dialog.
 *
 * A dialog rather than a second screen because these are paragraphs and the list is short: a
 * master-detail split pays on a desktop with room for both, and on a phone it is one more place to
 * navigate back from. Section 96 keeps the real routing for when the tree exists, if it ever does.
 */
@Composable
private fun PromptEditor(
    instance: MaPrompts.Instance,
    shipped: String,
    existing: List<MaPrompts.Instance>,
    onDismiss: () -> Unit,
    onDuplicate: () -> Unit,
    onSave: (String, String) -> Unit,
) {
    var name by remember(instance.name) { mutableStateOf(instance.name) }
    var text by remember(instance.name) { mutableStateOf(instance.text.ifBlank { shipped }) }

    val clash = !instance.locked &&
        name.trim() != instance.name &&
        existing.any { it.name.equals(name.trim(), ignoreCase = true) }
    val blank = name.trim().isEmpty()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(instance.name) },
        text = {
            Column {
                if (instance.locked) {
                    Text(
                        text = "This is the instruction built into the app. Duplicate it to make " +
                            "a version you can edit \u2014 the original stays as it is.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = shipped,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        singleLine = true,
                        isError = clash || blank,
                        label = { Text(if (clash) "That name is taken" else "Name") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = text,
                        onValueChange = { text = it },
                        minLines = 6,
                        maxLines = 14,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        },
        confirmButton = {
            if (instance.locked) {
                TextButton(onClick = onDuplicate) { Text("Duplicate") }
            } else {
                TextButton(
                    enabled = !clash && !blank,
                    onClick = { onSave(name.trim(), text) },
                ) { Text("Save") }
            }
        },
        dismissButton = {
            Row {
                if (!instance.locked) {
                    TextButton(onClick = onDuplicate) { Text("Duplicate") }
                }
                TextButton(onClick = onDismiss) { Text("Close") }
            }
        },
    )
}
