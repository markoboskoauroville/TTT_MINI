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

import android.content.Context
import android.media.MediaPlayer
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import dev.patrickgold.florisboard.app.FlorisPreferenceStore
import dev.patrickgold.florisboard.dictate.MaVoiceFormatCatalog
import dev.patrickgold.florisboard.lib.compose.FlorisScreen
import dev.patrickgold.jetpref.datastore.model.collectAsState
import kotlinx.coroutines.launch

/**
 * Voice formatting: which spoken marks are live, and how to say them.
 *
 * ### Why every row can be switched off
 *
 * A command is a word taken out of his sentence. The commoner the word, the more often that is
 * wrong — "at" and "minus" and "plus" turn up in ordinary speech constantly, and somebody who never
 * dictates an email address pays for that command every time he says "meet me at six". Ticking is
 * how he decides which trade is worth it, mark by mark.
 *
 * ### Why there is a speaker on every row
 *
 * His English carries an accent the transcriber sometimes mishears, and reading "say: ampersand"
 * does not tell him which sounds the recogniser is listening for. Each row plays the word in
 * Received Pronunciation so he can copy it. 31 recordings, 76 KB in total.
 */
@Composable
fun MaVoiceFormatScreen() = FlorisScreen {
    title = "Voice formatting"

    content {
        val prefs by FlorisPreferenceStore
        val context = LocalContext.current
        val scope = rememberCoroutineScope()
        val disabledRaw by prefs.dictate.maVoiceFormatOff.collectAsState()
        val off = remember(disabledRaw) {
            disabledRaw.split(',').map { it.trim() }.filter { it.isNotEmpty() }.toSet()
        }

        Text(
            text = "Say one of these as the LAST word of a dictation and it formats the text " +
                "instead of being typed. Said anywhere else it is just a word, so a sentence " +
                "about a dot still gets its dot.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
        Text(
            text = "Tap the speaker to hear how to say it.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp),
        )

        Spacer(Modifier.height(8.dp))

        for (group in MaVoiceFormatCatalog.GROUPS) {
            Text(
                text = group,
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
            for (mark in MaVoiceFormatCatalog.ALL.filter { it.group == group }) {
                val enabled = mark.id !in off
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(
                        checked = enabled,
                        onCheckedChange = { on ->
                            // Stored as the ones switched OFF, not the ones on.
                            //
                            // A new mark added in a later version is then live by default, instead
                            // of being invisible to anybody whose saved list predates it. The empty
                            // string means everything works, which is also the right first run.
                            val next = if (on) off - mark.id else off + mark.id
                            scope.launch {
                                prefs.dictate.maVoiceFormatOff.set(next.sorted().joinToString(","))
                            }
                        },
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "say: ${mark.say}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (enabled) {
                                MaterialTheme.colorScheme.onSurface
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        )
                        Text(
                            text = mark.result,
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(onClick = { playSample(context, mark.id) }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = "Hear ${mark.say}",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

/**
 * Plays one pronunciation and releases the player when it finishes.
 *
 * A fresh player per tap rather than one kept alive: these are two-second files, and a player held
 * across a screen that may be left at any moment is a hold on the audio output that nothing would
 * ever let go of. Released on completion and on error, so a missing or corrupt file costs nothing.
 */
private fun playSample(context: Context, id: String) {
    runCatching {
        val afd = context.assets.openFd(MaVoiceFormatCatalog.assetPath(id))
        val player = MediaPlayer()
        player.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
        afd.close()
        player.setOnCompletionListener { it.release() }
        player.setOnErrorListener { mp, _, _ -> mp.release(); true }
        player.prepare()
        player.start()
    }
}
