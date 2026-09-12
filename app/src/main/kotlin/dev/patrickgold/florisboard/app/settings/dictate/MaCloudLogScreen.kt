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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import dev.patrickgold.florisboard.app.FlorisPreferenceStore
import dev.patrickgold.florisboard.dictate.MaCloudLog
import dev.patrickgold.florisboard.dictate.MaReader
import dev.patrickgold.florisboard.lib.compose.FlorisScreen
import kotlinx.coroutines.launch
import java.io.File

/**
 * THE CLOUD READER: the conversations it kept, and a way to read them back.
 *
 * The reader speaks a chat once and it is gone — scrolled past, or lost when the chat closes. **A
 * conversation he listened to is a conversation he cannot go back to**, and the answers are often
 * the only place a decision was explained.
 *
 * Two halves on one screen, which is right because they are one idea seen from both ends:
 *
 *  - the switch, and the list of what has been captured
 *  - a log, opened, with every sentence tappable
 *
 * ### Tap a sentence to read from there
 *
 * Not "play from the top", which is what every audiobook does and is wrong here: he is looking for
 * one answer inside an hour of conversation. **The thing he wants read is the thing he is looking
 * at**, so the target is the sentence itself and the reading starts there.
 *
 * Reading a log uses `MaReader.speakText`, the same path the screen reader uses — one voice, one
 * speed, one set of effects. A second reading engine for saved text would drift from the live one
 * within a month.
 */
@Composable
fun MaCloudLogScreen() = FlorisScreen {
    title = "Cloud reader"
    previewFieldVisible = false

    content {
        val prefs by FlorisPreferenceStore
        val context = LocalContext.current
        val scope = rememberCoroutineScope()
        val enabled by prefs.dictate.maCloudLogEnabled.collectAsState()

        // The open log, or null for the list. One screen, two states — rather than a second route,
        // because "back" from a log should return to the list and not to settings.
        var open by remember { mutableStateOf<File?>(null) }

        // Recomputed whenever the switch changes, which is also the moment a first log can appear.
        val logs = remember(enabled, open) { MaCloudLog.logsIn(context.filesDir) }

        val current = open
        if (current == null) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Keep a log of what is read", style = MaterialTheme.typography.titleMedium)
                    Text(
                        // What it does and what it costs, in one line. A feature that writes files
                        // without being asked should say so where it is switched on.
                        text = "While the reader is watching, everything it sees is appended to a " +
                            "file here. Off by default.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = enabled,
                    onCheckedChange = { on -> scope.launch { prefs.dictate.maCloudLogEnabled.set(on) } },
                )
            }

            if (logs.isEmpty()) {
                Text(
                    text = if (enabled) {
                        "Nothing captured yet. Read a chat with the reader and it will appear here."
                    } else {
                        "Nothing captured. Switch it on above."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp),
                )
            }

            LazyColumn {
                items(logs) { file ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { open = file }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                    ) {
                        Text(MaCloudLog.titleOf(file.name), style = MaterialTheme.typography.bodyLarge)
                        Text(
                            text = MaCloudLog.dateOf(file.name) + "  ·  " + (file.length() / 1024) + " kB",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        } else {
            val text = remember(current) { runCatching { current.readText() }.getOrDefault("") }
            val sentences = remember(text) { MaCloudLog.sentences(text) }

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = { open = null }) { Text("BACK") }
                Text(
                    text = MaCloudLog.titleOf(current.name),
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = { MaReader.stop() }) { Text("STOP") }
            }

            LazyColumn {
                itemsIndexed(sentences) { i, sentence ->
                    Text(
                        text = sentence,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier
                            .fillMaxWidth()
                            // Tap a sentence, read FROM there to the end. Not just that sentence:
                            // he is looking for an answer, and an answer is a paragraph rather than
                            // a line. Stopping is one press away at the top.
                            .clickable {
                                MaReader.speakText(
                                    context,
                                    sentences.drop(i).joinToString(" "),
                                ) { }
                            }
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                    )
                }
                item { Spacer(Modifier.height(32.dp)) }
            }
        }
    }
}
