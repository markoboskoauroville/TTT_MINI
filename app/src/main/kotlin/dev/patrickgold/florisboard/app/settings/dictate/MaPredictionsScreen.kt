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

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.patrickgold.florisboard.app.FlorisPreferenceStore
import dev.patrickgold.florisboard.lib.compose.FlorisScreen
import dev.patrickgold.jetpref.datastore.ui.SwitchPreference

/**
 * The suggestion row: whether it is there, and what it follows.
 *
 * ### The flicker, and why a switch fixes it
 *
 * The row appeared and disappeared while Marko typed, which made the whole keyboard jump. That was
 * not a bug in the suggestions — it was the row being drawn only when there was something to show,
 * so an empty moment removed a strip of height and everything below moved up.
 *
 * On means on: the row keeps its height whether or not it has anything in it. A keyboard whose keys
 * move while a finger is travelling towards one is worse than a keyboard with an empty strip.
 *
 * ### What follows what
 *
 * Suggestions follow the HR/ENG key, the same one that decides the transcription language. One
 * choice, everywhere — there is no separate keyboard language to keep in step, and no way for the
 * two to disagree.
 */
@Composable
fun MaPredictionsScreen() = FlorisScreen {
    title = "Word predictions"

    content {
        val prefs by FlorisPreferenceStore

        SwitchPreference(
            prefs.suggestion.enabled,
            title = "Show the suggestion row",
            summary = "On, the row is always there and keeps its height. It was appearing only when " +
                "it had something to say, which moved every key underneath it while you typed.",
        )

        Spacer(Modifier.height(16.dp))

        Text(
            text = "Suggestions follow the HR/ENG key, the same one that sets the transcription " +
                "language. One choice for both, so they cannot disagree.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp),
        )

        Spacer(Modifier.height(12.dp))

        Text(
            text = "What the suggestions know about you is built from what you dictate, on this " +
                "phone, and never leaves it. Add words from your own history under Learn my words.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp),
        )

        Spacer(Modifier.height(24.dp))
    }
}
