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

import android.app.ActivityManager
import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import dev.patrickgold.florisboard.dictate.llm.MaPhoneModels
import dev.patrickgold.florisboard.dictate.provider.LocalModelDownloads
import dev.patrickgold.florisboard.dictate.provider.LocalModelManager

/**
 * MODELS ON THE PHONE (§191): download, cancel, delete, and what each one is.
 *
 * The same rows as Parakeet's on the Recording screen, through the same downloader, so a download
 * keeps going when he leaves the screen and shows in the notifications. Choosing one for a prompt
 * happens on the prompt itself, beside Haiku, Sonnet and Opus.
 */
@Composable
fun MaPhoneModelSection() {
    val context = LocalContext.current
    val downloads by LocalModelDownloads.state.collectAsState()
    val tick by LocalModelDownloads.installedTick.collectAsState()
    var deleted by remember { mutableIntStateOf(0) }
    var refused by remember { mutableStateOf<String?>(null) }
    var confirmDelete by remember { mutableStateOf<MaPhoneModels.PhoneModel?>(null) }
    val ramGb = remember { totalRamGb(context) }

    Text(
        text = "Models on the phone",
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(horizontal = 16.dp),
    )
    Text(
        text = "The style model of Marko Transcribe on the Mac, running here: no internet, no key, " +
            "nothing leaves the phone. Download it once, on Wi‑Fi. Then choose it as the model " +
            "of any prompt below. This phone has about $ramGb GB of memory.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
    )
    for (m in MaPhoneModels.all) {
        val installed = remember(tick, deleted, downloads) { MaPhoneModels.isInstalled(context, m) }
        val state = downloads[m.id]
        val downloading = state != null && state.error == null
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = m.name, style = MaterialTheme.typography.titleSmall)
                Text(
                    text = m.note,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                val status = when {
                    downloading -> "Downloading, ${state.percent} %"
                    state?.error != null -> "Download failed: ${state.error}"
                    installed -> "Downloaded"
                    m == MaPhoneModels.QWEN3_8B && ramGb < 11 -> "Too big for this phone's memory: it may not start"
                    else -> null
                }
                if (status != null) {
                    Text(
                        text = status,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (state?.error != null) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.primary,
                    )
                }
                if (downloading) {
                    LinearProgressIndicator(
                        progress = { state.percent / 100f },
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp, end = 8.dp),
                    )
                }
            }
            when {
                downloading -> IconButton(onClick = { LocalModelDownloads.cancel(m.id) }) {
                    Icon(Icons.Default.Close, contentDescription = "Cancel the download of ${m.name}")
                }
                installed -> IconButton(onClick = { confirmDelete = m }) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete ${m.name}")
                }
                else -> IconButton(onClick = {
                    LocalModelDownloads.clearError(m.id)
                    // The model and a margin must fit, or the download fails at 90 % an hour later.
                    val free = context.filesDir.usableSpace
                    if (free < m.sizeBytes + 300_000_000L) {
                        refused = "${m.name} needs ${gb(m.sizeBytes)} GB and the phone has ${gb(free)} GB free."
                    } else {
                        LocalModelDownloads.start(context, m.spec)
                    }
                }) {
                    Icon(Icons.Default.Download, contentDescription = "Download ${m.name}")
                }
            }
        }
    }

    refused?.let { why ->
        AlertDialog(
            onDismissRequest = { refused = null },
            title = { Text("Not enough space") },
            text = { Text(why) },
            confirmButton = { TextButton(onClick = { refused = null }) { Text("OK") } },
        )
    }
    confirmDelete?.let { m ->
        AlertDialog(
            onDismissRequest = { confirmDelete = null },
            title = { Text("Delete ${m.name}?") },
            text = {
                Text(
                    "${gb(m.sizeBytes)} GB come back. A prompt that uses it keeps it as its model and " +
                        "says it is not downloaded until you download it again.",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    LocalModelManager.delete(context, m.id)
                    deleted++
                    confirmDelete = null
                }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = null }) { Text("Cancel") } },
        )
    }
}

private fun gb(bytes: Long): String = "%.1f".format(bytes / 1_000_000_000.0)

private fun totalRamGb(context: Context): Int {
    val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager ?: return 0
    val info = ActivityManager.MemoryInfo()
    am.getMemoryInfo(info)
    return Math.round(info.totalMem / 1_000_000_000.0).toInt()
}
