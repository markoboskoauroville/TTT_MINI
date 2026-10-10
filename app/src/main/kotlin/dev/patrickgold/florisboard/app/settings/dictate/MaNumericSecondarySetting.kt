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

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import dev.patrickgold.florisboard.app.FlorisPreferenceStore
import dev.patrickgold.florisboard.dictate.MaNumericSecondary
import dev.patrickgold.jetpref.datastore.model.collectAsState
import kotlinx.coroutines.launch

/**
 * Editor for what each digit on the number row types when it is held.
 *
 * Ten fields, one per key, laid out in the order the keys are printed so the screen reads as the row
 * it edits. Every edit is persisted immediately: there is no save button, because a settings screen
 * that can be left in an unsaved state is a settings screen that loses work.
 *
 * A field may be emptied, which leaves that key with nothing behind it rather than falling back to
 * some default. Choosing nothing is a choice.
 */
@Composable
fun MaNumericSecondarySetting() {
    // What a slot may hold is decided in MaNumericSecondary.sanitize and stated here, where he is typing.
    // A field that silently drops what he types without saying why is a field he retries.
    val prefs by FlorisPreferenceStore
    val scope = rememberCoroutineScope()
    val stored by prefs.dictate.maNumericSecondary.collectAsState()

    // Seeded from the preference and re-seeded whenever it changes underneath, so a reset elsewhere
    // is reflected here rather than being overwritten by a stale field.
    var slots by remember(stored) { mutableStateOf(MaNumericSecondary.parse(stored)) }

    fun write(index: Int, value: String) {
        val next = slots.toMutableList()
        next[index] = MaNumericSecondary.sanitize(value)
        slots = next
        scope.launch { prefs.dictate.maNumericSecondary.set(MaNumericSecondary.serialize(next)) }
    }

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(
            text = "Number row long press",
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = "Anything you like \u2014 a symbol, a word, a whole sentence. Holding the digit " +
                "types the whole box; the key shows its first character. Hold a digit here for " +
                "keys you cannot type, like Enter, Tab and the arrows. Leave a field empty " +
                "for a key with nothing behind it.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 12.dp),
        )

        MaNumericSecondary.LABELS.forEachIndexed { index, digit ->
            MaSlotRow(
                digit = digit,
                value = slots.getOrElse(index) { "" },
                onValueChange = { write(index, it) },
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            TextButton(
                onClick = {
                    slots = MaNumericSecondary.parse(MaNumericSecondary.DEFAULT)
                    scope.launch { prefs.dictate.maNumericSecondary.set(MaNumericSecondary.DEFAULT) }
                },
            ) {
                Text(text = "Underscore on all ten")
            }
        }
    }
}

/**
 * One digit and its slot.
 *
 * HOLD THE DIGIT for the keys that cannot be typed into a text field: Enter, Tab, the arrows and the
 * rest of [MaNumericSecondary.SPECIAL_KEYS]. Picking one inserts its brace token at the cursor, so
 * `Regards{Enter}Marko` is built by typing, holding, picking, typing. The menu sits on the digit
 * because the digit is the key it edits, and a long press is the gesture the keyboard already uses
 * for "the other things this key does".
 *
 * Holds a [TextFieldValue] of its own so the cursor survives each keystroke being written to the
 * preference and read back; it re-seeds only when the stored text changes from somewhere else, a
 * reset for instance.
 */
@Composable
private fun MaSlotRow(digit: String, value: String, onValueChange: (String) -> Unit) {
    var field by remember { mutableStateOf(TextFieldValue(value, TextRange(value.length))) }
    if (field.text != value) {
        field = TextFieldValue(value, TextRange(value.length))
    }
    var menuOpen by remember { mutableStateOf(false) }

    fun insert(token: String) {
        val start = field.selection.min.coerceIn(0, field.text.length)
        val end = field.selection.max.coerceIn(0, field.text.length)
        val next = field.text.substring(0, start) + token + field.text.substring(end)
        val cleaned = MaNumericSecondary.sanitize(next)
        field = TextFieldValue(cleaned, TextRange((start + token.length).coerceAtMost(cleaned.length)))
        onValueChange(cleaned)
    }

    Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // The digit drawn as the key it is, so the eye matches a row on screen to a row under the
        // thumb without reading anything. Tap or hold both open the special keys: a tap that did
        // nothing on a thing drawn like a key would only teach that it is not one.
        Box {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .combinedClickable(
                        onClick = { menuOpen = true },
                        onLongClick = { menuOpen = true },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = digit,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                MaNumericSecondary.SPECIAL_KEYS.forEach { key ->
                    DropdownMenuItem(
                        text = { Text("${key.glyph}   ${key.name}") },
                        onClick = {
                            insert(key.token)
                            menuOpen = false
                        },
                    )
                }
            }
        }
        Box(modifier = Modifier.width(12.dp))
        OutlinedTextField(
            value = field,
            onValueChange = { next ->
                val cleaned = MaNumericSecondary.sanitize(next.text)
                field = if (cleaned == next.text) next else TextFieldValue(cleaned, TextRange(cleaned.length))
                onValueChange(cleaned)
            },
            // Several lines, because a snippet can be a paragraph and he has to see what he wrote to
            // trust what the key will type.
            singleLine = false,
            maxLines = 5,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
