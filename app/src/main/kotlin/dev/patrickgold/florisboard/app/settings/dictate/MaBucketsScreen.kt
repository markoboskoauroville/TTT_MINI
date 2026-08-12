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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.patrickgold.florisboard.app.FlorisPreferenceStore
import dev.patrickgold.florisboard.lib.compose.FlorisScreen
import dev.patrickgold.jetpref.datastore.model.PreferenceData
import dev.patrickgold.jetpref.datastore.model.collectAsState
import kotlinx.coroutines.launch

/**
 * The copy buckets, and the timing of the paste they perform.
 *
 * ### Why the delays are a setting at all
 *
 * A bucket key does three things to somebody else's text field: select everything, delete it, put
 * the text in. Each is a round trip to another process, and firing the next before the last has
 * landed acts on a selection that does not exist yet. In a fast field that never happens. In a slow
 * one it happens every time, and the symptom is not an error — it is a key that pasted nothing, or a
 * field emptied and not refilled.
 *
 * There is no way to know from inside this app how slow a given field is. It varies by app, by
 * device and by how busy the phone is at that moment. So the numbers are Marko's to set: he is the
 * one who can see which field is failing.
 *
 * ### Why three numbers and not one
 *
 * They are three different waits. The field that needs longer before the paste is usually fine on
 * the other two, and one number for all three would have to be raised to the slowest of them — which
 * makes every bucket paste slower everywhere to fix one app.
 */
@Composable
fun MaBucketsScreen() = FlorisScreen {
    title = "Copy buckets"

    content {
        val prefs by FlorisPreferenceStore

        Text(
            text = "A bucket key selects everything in the field, deletes it, then pastes. These " +
                "are the waits between those three steps.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
        Text(
            text = "If a bucket does nothing in some app, or empties the field without filling it, " +
                "raise the wait before the paste first. That is the step that gets dropped.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp),
        )

        Spacer(Modifier.height(16.dp))

        MaDelayRow(
            step = "1",
            title = "Before select all",
            summary = "After the keyboard lets go of any selection it was holding",
            pref = prefs.dictate.maClipDelaySelect,
        )
        MaDelayRow(
            step = "2",
            title = "Before delete",
            summary = "After the field has been asked to select everything",
            pref = prefs.dictate.maClipDelayDelete,
        )
        MaDelayRow(
            step = "3",
            title = "Before paste",
            summary = "The one that matters most \u2014 raise this first if a bucket misbehaves",
            pref = prefs.dictate.maClipDelayPaste,
        )

        Spacer(Modifier.height(24.dp))
    }
}

/**
 * One step, with its wait and a stepper.
 *
 * A stepper rather than a text field, because this is a number somebody adjusts by trying it: raise
 * it, go and test the app that was failing, come back. Typing a number means opening a keyboard on
 * top of the keyboard's own settings, which on this app is worse than it sounds.
 */
@Composable
private fun MaDelayRow(
    step: String,
    title: String,
    summary: String,
    pref: PreferenceData<Int>,
) {
    val scope = rememberCoroutineScope()
    val value by pref.collectAsState()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = step,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(20.dp),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = summary,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        TextButton(
            // Steps of 100ms, Marko's number. Fine enough to find the edge of what a field needs
            // without turning tuning into a long press.
            onClick = { scope.launch { pref.set((value - 100).coerceAtLeast(0)) } },
            enabled = value > 0,
        ) {
            Text("\u2212", fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
        }
        Text(
            text = "$value",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.width(52.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        TextButton(
            // Two seconds is the ceiling. Past that a bucket press stops feeling like a key and
            // starts feeling broken, and a field that slow needs a different answer than waiting.
            onClick = { scope.launch { pref.set((value + 100).coerceAtMost(2000)) } },
            enabled = value < 2000,
        ) {
            Text("+", fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}
