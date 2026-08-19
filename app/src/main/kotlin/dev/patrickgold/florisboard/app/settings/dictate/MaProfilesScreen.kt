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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
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
import androidx.compose.ui.unit.dp
import dev.patrickgold.florisboard.app.FlorisPreferenceStore
import dev.patrickgold.florisboard.dictate.MaSettingsVault
import dev.patrickgold.florisboard.lib.compose.FlorisScreen
import dev.patrickgold.jetpref.datastore.model.collectAsState
import kotlinx.coroutines.launch

/**
 * Profiles: whole configurations, saved by name and swapped in one tap.
 *
 * ### Avid's model, and the part of it that earns its place here
 *
 * An Avid editor keeps a user profile per kind of work, because the layout and key mapping that
 * suit cutting rushes are wrong for finishing. He has the same split — the keyboard he wants
 * dictating at four in the morning is not the one he wants at the studio.
 *
 * Three of Avid's behaviours are here: **a checkmark that means active and moves when another is
 * chosen**, **duplicate**, because nobody edits a working configuration directly — they copy it,
 * rename the copy and edit that — and **save under a name that already exists** to update it.
 *
 * ### What is deliberately not here yet
 *
 * The collapsible tree and the master-detail pane. Those reorganise every settings screen in the
 * app, and profiles are worth having on their own before anything is rearranged around them.
 */
@Composable
fun MaProfilesScreen() = FlorisScreen {
    title = "Profiles"

    content {
        val prefs by FlorisPreferenceStore
        val scope = rememberCoroutineScope()
        val active by prefs.dictate.maActiveProfile.collectAsState()
        // Bumped after any change, so the list is re-read from the folder rather than from a copy
        // this screen keeps. The folder is the truth; a cached list drifts the moment a file is
        // written by anything else.
        var revision by remember { mutableStateOf(0) }
        val profiles = remember(revision) { MaSettingsVault.profiles() }
        var naming by remember { mutableStateOf<String?>(null) }
        var duplicating by remember { mutableStateOf<MaSettingsVault.Profile?>(null) }

        Text(
            text = "A profile is every setting in the app, saved under a name. Tap one to switch " +
                "to it. Saving under a name that already exists updates it.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )

        if (profiles.isEmpty()) {
            Text(
                text = "No profiles yet.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }

        for (profile in profiles) {
            val isActive = profile.name == active
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectable(
                        selected = isActive,
                        onClick = {
                            scope.launch {
                                if (MaSettingsVault.applyProfile(profile)) {
                                    prefs.dictate.maActiveProfile.set(profile.name)
                                }
                            }
                        },
                    )
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // The checkmark is a radio, because exactly one profile is in force and choosing
                // another must visibly take it from the last — Avid's rule, and the reason it reads
                // as a state rather than as a list of files.
                RadioButton(selected = isActive, onClick = null)
                Column(modifier = Modifier.weight(1f).padding(start = 8.dp)) {
                    Text(text = profile.name, style = MaterialTheme.typography.bodyMedium)
                    if (isActive) {
                        Text(
                            text = "in use",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
                IconButton(onClick = { duplicating = profile }) {
                    Icon(
                        Icons.Default.ContentCopy,
                        contentDescription = "Duplicate ${profile.name}",
                        modifier = Modifier.size(20.dp),
                    )
                }
                IconButton(onClick = {
                    MaSettingsVault.deleteProfile(profile)
                    revision++
                }) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete ${profile.name}",
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        TextButton(
            onClick = { naming = "" },
            modifier = Modifier.padding(horizontal = 12.dp),
        ) { Text("Save everything as a new profile") }

        Spacer(Modifier.height(24.dp))

        naming?.let { current ->
            NameDialog(
                title = "Save as a profile",
                initial = current,
                onDismiss = { naming = null },
                onConfirm = { name ->
                    naming = null
                    scope.launch {
                        if (MaSettingsVault.saveProfile(name)) {
                            prefs.dictate.maActiveProfile.set(name.trim())
                            revision++
                        }
                    }
                },
            )
        }

        duplicating?.let { source ->
            NameDialog(
                title = "Duplicate ${source.name}",
                initial = source.name + " copy",
                onDismiss = { duplicating = null },
                onConfirm = { name ->
                    duplicating = null
                    scope.launch {
                        MaSettingsVault.duplicateProfile(source, name)
                        revision++
                    }
                },
            )
        }
    }
}

@Composable
private fun NameDialog(
    title: String,
    initial: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var text by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(text) },
                enabled = text.isNotBlank(),
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
