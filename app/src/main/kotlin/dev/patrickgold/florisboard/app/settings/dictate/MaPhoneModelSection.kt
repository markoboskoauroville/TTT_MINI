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
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
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
import dev.patrickgold.florisboard.dictate.llm.MaSharedModels
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
    val sharedTick by MaSharedModels.tick.collectAsState()
    // All files access is granted in the system's own screen; read again whenever he comes back.
    val lifecycleOwner = LocalLifecycleOwner.current
    var access by remember { mutableStateOf(MaSharedModels.hasAccess(context)) }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val now = MaSharedModels.hasAccess(context)
                if (now && !access) MaSharedModels.verifyInBackground(context)
                access = now
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

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
    // THE SHARED FOLDER. One copy of each model in Download/models/, under Hugging Face's names, for
    // the app, aria2 and every other tool (MaSharedModels). Asked once, here, and said why.
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = "Shared folder: Download/models", style = MaterialTheme.typography.titleSmall)
            Text(
                text = if (access) {
                    "Allowed. Models are downloaded there, and a model already there at the right " +
                        "size is used without downloading it again."
                } else {
                    "Not allowed yet. To use a model that aria2 or another app already put in " +
                        "Download/models, and to download into that folder, this app needs All files " +
                        "access. Until then models go to the app's own folder."
                },
                style = MaterialTheme.typography.bodySmall,
                color = if (access) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (!access) {
            TextButton(onClick = { openAllFilesAccess(context) }) { Text("Allow") }
        }
    }

    for (m in MaPhoneModels.all) {
        val installed = remember(tick, deleted, downloads, sharedTick, access) { MaPhoneModels.isInstalled(context, m) }
        val sharedState = remember(tick, deleted, downloads, sharedTick, access) { MaSharedModels.state(context, m) }
        val sharedReason = remember(tick, deleted, downloads, sharedTick, access) { MaSharedModels.reason(context, m) }
        val kept = remember(tick, deleted, downloads, sharedTick, access) { MaSharedModels.partialPercent(context, m) }
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
                    sharedState == MaSharedModels.State.CHECKING ->
                        "In Download/models, in use; checking its checksum in the background"
                    sharedState == MaSharedModels.State.READY -> "Downloaded, in Download/models"
                    installed -> "Downloaded"
                    // A stopped download is kept, and Download carries on from it (phone test, build 398).
                    kept != null -> "Stopped at $kept %. Download continues from there."
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
                // A copy in the shared folder that cannot be used, and why, in red.
                if (sharedReason != null && !downloading) {
                    Text(
                        text = sharedReason,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
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
                    val free = (MaSharedModels.downloadTarget(context, m.id)?.parentFile ?: context.filesDir).usableSpace
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
                val shared = MaSharedModels.sharedFile(m)
                val sharedNote = if (access && shared.isFile) {
                    "\n\nThis deletes the SHARED file ${shared.name} in Download/models, which other " +
                        "apps such as aria2 may also be using."
                } else {
                    ""
                }
                Text(
                    "${gb(m.sizeBytes)} GB come back. A prompt that uses it keeps it as its model and " +
                        "says it is not downloaded until you download it again." + sharedNote,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    LocalModelManager.delete(context, m.id)
                    if (access) MaSharedModels.delete(context, m)
                    deleted++
                    confirmDelete = null
                }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = null }) { Text("Cancel") } },
        )
    }
}

// Always a decimal point, as the model notes are written ("2.4 GB"); the phone's Croatian locale would
// print "2,4" and the same size would read two ways on one screen (phone test, build 398).
private fun gb(bytes: Long): String = "%.1f".format(java.util.Locale.ROOT, bytes / 1_000_000_000.0)

private fun totalRamGb(context: Context): Int {
    val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager ?: return 0
    val info = ActivityManager.MemoryInfo()
    am.getMemoryInfo(info)
    return Math.round(info.totalMem / 1_000_000_000.0).toInt()
}

/** The system screen where All files access is switched on for this app (Android 11 and later). */
private fun openAllFilesAccess(context: Context) {
    val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, Uri.parse("package:${context.packageName}"))
    } else {
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))
    }.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { context.startActivity(intent) }
}
