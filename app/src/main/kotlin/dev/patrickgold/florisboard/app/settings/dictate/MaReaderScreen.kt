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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.patrickgold.florisboard.app.FlorisPreferenceStore
import dev.patrickgold.florisboard.dictate.MaSpeechify
import dev.patrickgold.florisboard.lib.compose.FlorisScreen
import dev.patrickgold.jetpref.datastore.model.PreferenceData
import dev.patrickgold.jetpref.datastore.model.collectAsState
import kotlinx.coroutines.launch

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

        Text(
            text = "The speaker key reads what is on screen. Press again to pause, again to " +
                "continue. Which voice speaks depends on the language badge \u2014 the same one " +
                "that sets the dictation language.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )

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

@Composable
private fun VoiceGroup(
    heading: String,
    note: String?,
    voices: List<MaSpeechify.Voice>,
    pref: PreferenceData<String>,
) {
    val scope = rememberCoroutineScope()
    val chosen by pref.collectAsState()
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
                .selectable(selected = selected, onClick = { scope.launch { pref.set(voice.id) } })
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RadioButton(selected = selected, onClick = { scope.launch { pref.set(voice.id) } })
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
