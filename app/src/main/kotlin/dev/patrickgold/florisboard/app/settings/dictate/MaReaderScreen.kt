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
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import dev.patrickgold.florisboard.app.FlorisPreferenceStore
import dev.patrickgold.florisboard.dictate.MaSpeechify
import dev.patrickgold.florisboard.lib.compose.FlorisScreen
import dev.patrickgold.jetpref.datastore.model.PreferenceData
import dev.patrickgold.jetpref.datastore.model.collectAsState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Reader settings: which voice speaks which language.
 *
 * ### Two lists, because there is no Croatian voice
 *
 * Speechify has none — checked across the whole catalogue, 985 voices, no `hr-HR` on any model. So
 * Croatian is read by a foreign voice and the only question is which one hurts least. The list is
 * ordered the way Marko ranked them by ear, Ukrainian first.
 *
 * The English list is separate because it has no such compromise: those voices are reading their own
 * language and the choice is only taste.
 *
 * ### The language pill decides which list is used
 *
 * Not a setting here. The same badge that chooses the transcription language chooses the reading
 * voice, so English text is read by the English voice and Croatian by the Croatian one, with nothing
 * further to keep in step. **Ukrainian simply stands where Croatian would.**
 */
@Composable
fun MaReaderScreen() = FlorisScreen {
    title = "Reader"

    content {
        val prefs by FlorisPreferenceStore
        // Declared here rather than beside the display list, because moving the speed block above
        // that list left three uses of `scope` sitting above its declaration.
        val scope = rememberCoroutineScope()

        Text(
            text = "The speaker key reads what is on screen. Press again to pause, again to " +
                "continue. Which voice speaks depends on the language badge \u2014 the same one " +
                "that sets the dictation language.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )

        // Speed, in tenths, applied to playback rather than to the synthesis — so changing it costs
        // nothing and takes effect on the next press instead of the next request.
        val speed by prefs.dictate.maReaderSpeed.collectAsState()
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "Speed", style = MaterialTheme.typography.bodyMedium)
                Text(
                    text = "How fast the voice reads",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            TextButton(onClick = {
                scope.launch { prefs.dictate.maReaderSpeed.set((speed - 1).coerceAtLeast(5)) }
            }) { Text("\u2212", style = MaterialTheme.typography.titleMedium) }
            Text(
                text = "%.1f\u00D7".format(speed / 10f),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(horizontal = 6.dp),
            )
            TextButton(onClick = {
                scope.launch { prefs.dictate.maReaderSpeed.set((speed + 1).coerceAtMost(25)) }
            }) { Text("+", style = MaterialTheme.typography.titleMedium) }
        }

        // Hearing a voice is the whole basis for choosing one, so this is on by default.
        val preview by prefs.dictate.maReaderPreviewVoices.collectAsState()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .selectable(selected = preview, onClick = {
                    scope.launch { prefs.dictate.maReaderPreviewVoices.set(!preview) }
                })
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(
                checked = preview,
                onCheckedChange = { on -> scope.launch { prefs.dictate.maReaderPreviewVoices.set(on) } },
            )
            Column(modifier = Modifier.padding(start = 8.dp)) {
                Text("Speak a sample when I pick a voice", style = MaterialTheme.typography.bodyMedium)
                Text(
                    text = "The voice says its own name \u2014 costs a few characters each time",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Text(
            text = "While reading",
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
        val display by prefs.dictate.maReaderDisplay.collectAsState()
        for ((value, label, detail) in listOf(
            Triple("subtitle", "Subtitle row", "A row of its own showing the sentence, current word lit"),
            Triple("spacebar", "On the spacebar", "One word at a time, only while the keys are shown"),
            Triple("off", "Nothing", "Just the voice"),
        )) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectable(
                        selected = display == value,
                        onClick = { scope.launch { prefs.dictate.maReaderDisplay.set(value) } },
                    )
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(
                    selected = display == value,
                    onClick = { scope.launch { prefs.dictate.maReaderDisplay.set(value) } },
                )
                Column(modifier = Modifier.padding(start = 8.dp)) {
                    Text(text = label, style = MaterialTheme.typography.bodyMedium)
                    Text(
                        text = detail,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        VoiceGroup(
            heading = "Croatian",
            note = "Speechify has no Croatian voice \u2014 none exists on any model. These read " +
                "Croatian with a foreign accent, best first.",
            voices = MaSpeechify.CROATIAN_VOICES,
            pref = prefs.dictate.maReaderVoiceHr,
        )

        VoiceGroup(
            heading = "English",
            note = null,
            voices = MaSpeechify.ENGLISH_VOICES,
            pref = prefs.dictate.maReaderVoiceEn,
        )

        Spacer(Modifier.height(24.dp))
    }
}

/** Plays one file and releases the player when it finishes. */
private fun playPreview(path: String) {
    runCatching {
        val player = android.media.MediaPlayer()
        player.setDataSource(path)
        player.setOnCompletionListener { it.release() }
        player.setOnErrorListener { mp, _, _ -> mp.release(); true }
        player.prepare()
        player.start()
    }
}

@Composable
private fun VoiceGroup(
    heading: String,
    note: String?,
    voices: List<MaSpeechify.Voice>,
    pref: PreferenceData<String>,
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val prefs by FlorisPreferenceStore
    val previewOn by prefs.dictate.maReaderPreviewVoices.collectAsState()
    val chosen by pref.collectAsState()

    fun pick(voice: MaSpeechify.Voice) {
        scope.launch {
            pref.set(voice.id)
            if (previewOn) {
                // On IO: this is a network call, and on Main it would freeze the settings screen
                // for the length of the synthesis.
                val file = withContext(Dispatchers.IO) { MaSpeechify.previewFile(context, voice) }
                if (file != null) playPreview(file.path)
            }
        }
    }
    Text(
        text = heading,
        style = MaterialTheme.typography.titleSmall,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
    )
    if (note != null) {
        Text(
            text = note,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        Spacer(Modifier.height(4.dp))
    }
    for (voice in voices) {
        val selected = voice.id == chosen
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .selectable(selected = selected, onClick = { pick(voice) })
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RadioButton(selected = selected, onClick = { pick(voice) })
            Column(modifier = Modifier.padding(start = 8.dp)) {
                Text(text = voice.label, style = MaterialTheme.typography.bodyMedium)
                Text(
                    text = voice.detail,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
