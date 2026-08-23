/*
 * Copyright (C) 2026 Marko Boško, Mantra Productions
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dev.patrickgold.florisboard.app.settings.dictate

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
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
import dev.patrickgold.florisboard.dictate.nlp.MaWordLanguage
import dev.patrickgold.florisboard.dictate.nlp.MaNgram
import dev.patrickgold.jetpref.datastore.model.collectAsState
import kotlinx.coroutines.launch

/**
 * The switch for personal word prediction, and the way to make it forget.
 *
 * The count of known words is shown because this feature is worthless for its first few thousand
 * words and there is otherwise no way to tell whether it is working or simply still learning. A
 * number that climbs is the difference between "not working" and "not ready".
 */
@Composable
fun MaNgramSetting() {
    val prefs by FlorisPreferenceStore
    val scope = rememberCoroutineScope()
    val enabled by prefs.dictate.maNgramEnabled.collectAsState()
    var forgotten by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                Text(
                    text = "Learn my words",
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = "Suggests from what you have written on this phone, including names and " +
                        "words no dictionary has. Stays on the device and is never sent anywhere. " +
                        "Nothing is learned in incognito mode.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(
                checked = enabled,
                onCheckedChange = { next ->
                    scope.launch { prefs.dictate.maNgramEnabled.set(next) }
                },
            )
        }

        // ONE LINE PER LANGUAGE, because there are two models and a single total would hide the
        // thing worth knowing: whether the Croatian one has anything in it yet.
        //
        // He asked to see the size and to be able to wipe it. Both, and per language, since wiping
        // Croatian to fix Croatian should not cost him a year of English.
        var wiped by remember { mutableStateOf(setOf<String>()) }
        for (language in listOf(MaWordLanguage.EN, MaWordLanguage.HR)) {
            val name = if (language == MaWordLanguage.EN) "English" else "Croatian"
            val known = if (forgotten || language in wiped) 0 else MaNgram.vocabularyOf(language)
            val written = if (forgotten || language in wiped) 0L else MaNgram.totalOf(language)
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "$name: $known words known, from $written written",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = { MaNgram.forget(language); wiped = wiped + language }) {
                    Text(text = "Wipe")
                }
            }
        }

        // The queue, and the button that empties it.
        //
        // A word is asked about only when it is NEW and carries no Croatian letters — a word already
        // in a model is already in the right place, which is what keeps this from being a bill. So
        // this number falls to nothing on its own as he writes, and rises only when he uses a word
        // he has never used before.
        //
        // A button rather than automatic: it costs money and a second, and there is no moment while
        // typing when either is acceptable.
        var pending by remember { mutableStateOf(MaNgram.pendingCount()) }
        var message by remember { mutableStateOf("") }
        Text(
            text = if (pending == 0) {
                "No new words waiting to be sorted."
            } else {
                "$pending new word(s) waiting. They are already learned under the language badge; " +
                    "sorting checks whether the badge was right."
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 12.dp),
        )
        if (message.isNotBlank()) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }

        Row(modifier = Modifier.fillMaxWidth()) {
            TextButton(
                enabled = pending > 0,
                onClick = {
                    scope.launch {
                        MaNgram.classifyPending { message = it }
                        pending = MaNgram.pendingCount()
                    }
                },
            ) {
                Text(text = "Sort new words by language")
            }
            TextButton(
                onClick = {
                    MaNgram.forgetEverything()
                    forgotten = true
                },
            ) {
                Text(text = "Forget everything")
            }
        }
    }
}
