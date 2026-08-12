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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import dev.patrickgold.florisboard.dictate.MaVocabulary
import dev.patrickgold.florisboard.dictate.data.history.DictateHistoryStore
import dev.patrickgold.florisboard.ime.dictionary.DictionaryManager
import dev.patrickgold.florisboard.ime.dictionary.UserDictionaryEntry
import dev.patrickgold.florisboard.lib.compose.FlorisScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Teaches the keyboard the words already dictated.
 *
 * The keyboard's suggestions are weakest on exactly the words that matter most to one person: names,
 * places, the vocabulary of their own work. No general dictionary holds Mantreshvar or Kukljica, and
 * no amount of model quality supplies them. The dictation history holds all of them already, in
 * Marko's own words and in the proportions he uses them.
 *
 * Nothing leaves the phone. The history is local, the dictionary is local, and the whole operation
 * is reading one and writing the other.
 *
 * ### Why it asks rather than adding
 *
 * A transcript contains mistakes, and a mistake added to the dictionary is worse than a missing
 * word: the keyboard will then suggest the mistake forever, confidently. So the list is offered with
 * everything unticked and he chooses. That also makes the transcription errors visible, which is
 * worth something on its own.
 */
@Composable
fun MaVocabularyScreen() = FlorisScreen {
    title = "Learn my words"

    content {
        val context = LocalContext.current
        val scope = rememberCoroutineScope()

        var candidates by remember { mutableStateOf<List<MaVocabulary.Candidate>?>(null) }
        val chosen = remember { mutableStateListOf<String>() }
        var added by remember { mutableStateOf(0) }

        LaunchedEffect(Unit) {
            // On IO: this reads every transcript and counts every word in them, which is not work
            // for the thread that draws the screen.
            candidates = withContext(Dispatchers.IO) {
                val texts = DictateHistoryStore.flow(context).first().map { it.text }
                val dao = DictionaryManager.default().florisUserDictionaryDao()
                // Everything already known, so nothing is offered twice. An empty query returns the
                // whole table, which is what is wanted here.
                val known = runCatching { dao?.query("")?.map { it.word }?.toSet() }
                    .getOrNull().orEmpty()
                MaVocabulary.candidates(texts, known)
            }
        }

        val list = candidates
        when {
            list == null -> Text(
                text = "Reading your dictation history\u2026",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(16.dp),
            )

            list.isEmpty() -> Text(
                text = "Nothing new to add. Words have to appear at least twice in your history " +
                    "before they are offered, so this fills up as you dictate.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(16.dp),
            )

            else -> {
                Text(
                    text = "Words from your own dictations that the keyboard does not know yet, " +
                        "most used first. Tick the ones worth keeping.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
                Text(
                    text = "A transcript can hold mistakes, and a mistake added here would be " +
                        "suggested forever. Nothing is ticked for you.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
                Spacer(Modifier.height(12.dp))

                list.forEach { candidate ->
                    val ticked = candidate.word in chosen
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 52.dp)
                            .clickable {
                                if (ticked) chosen.remove(candidate.word) else chosen.add(candidate.word)
                            }
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(checked = ticked, onCheckedChange = null)
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = candidate.word,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            // How often he said it, which is the only evidence available for
                            // whether a word is worth keeping.
                            text = "${candidate.count}\u00d7",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Button(
                        onClick = {
                            val words = chosen.toList()
                            scope.launch {
                                withContext(Dispatchers.IO) {
                                    val dao = DictionaryManager.default().florisUserDictionaryDao()
                                    words.forEach { word ->
                                        // Frequency 128 is the middle of the 1..255 range the user
                                        // dictionary uses. A word he chose deserves to outrank one
                                        // he never asked for, without drowning what he types most.
                                        runCatching {
                                            dao?.insert(
                                                UserDictionaryEntry(
                                                    id = 0,
                                                    word = word,
                                                    freq = 128,
                                                    // Null locale, so the word is suggested in both
                                                    // languages. Marko dictates in two and a name is
                                                    // a name in either.
                                                    locale = null,
                                                    shortcut = null,
                                                ),
                                            )
                                        }
                                    }
                                }
                                added = words.size
                                chosen.clear()
                                // Re-read, so what was just added drops off the list rather than
                                // sitting there inviting a second add.
                                candidates = withContext(Dispatchers.IO) {
                                    val texts = DictateHistoryStore.flow(context).first().map { it.text }
                                    val dao = DictionaryManager.default().florisUserDictionaryDao()
                                    val known = runCatching { dao?.query("")?.map { it.word }?.toSet() }
                                        .getOrNull().orEmpty()
                                    MaVocabulary.candidates(texts, known)
                                }
                            }
                        },
                        enabled = chosen.isNotEmpty(),
                        modifier = Modifier.weight(1f).heightIn(min = 52.dp),
                    ) {
                        Text(if (chosen.isEmpty()) "Add" else "Add ${chosen.size}")
                    }
                }
                if (added > 0) {
                    Text(
                        text = "Added $added.",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}
