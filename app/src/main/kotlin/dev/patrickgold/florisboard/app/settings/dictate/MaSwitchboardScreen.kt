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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentPasteGo
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Numbers
import androidx.compose.material.icons.filled.Spellcheck
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import dev.patrickgold.florisboard.app.FlorisPreferenceStore
import dev.patrickgold.florisboard.app.LocalNavController
import dev.patrickgold.florisboard.app.Routes
import dev.patrickgold.florisboard.lib.compose.FlorisScreen
import dev.patrickgold.jetpref.datastore.model.PreferenceData
import dev.patrickgold.jetpref.datastore.model.collectAsState
import kotlinx.coroutines.launch

/**
 * Every row the keyboard can show, in one list, with its switch.
 *
 * ### Why this exists
 *
 * The switches were correct and scattered. Turning off the buckets meant Paste timing, the magic
 * row meant Magic button, the suggestions meant Word predictions — three screens to answer one
 * question, which is "what is on my keyboard right now". Nobody holds that map in their head, and
 * Marko was opening screens to find out what he had switched on.
 *
 * The rows are the app's main feature, so the app should be able to show them all at once.
 *
 * ### Two targets per line, on purpose
 *
 * The switch turns the row on and off. The name opens that row's own settings. They are different
 * questions — *whether* I want it, and *how* I want it — and answering the first should not cost a
 * screen, while answering the second needs one.
 *
 * This screen deliberately owns no settings of its own. Every switch here is the same preference
 * the detailed screen writes, so the two can never disagree; a copy would be a second source of
 * truth and the first thing to drift.
 */
@Composable
fun MaSwitchboardScreen() = FlorisScreen {
    title = "Switchboard"

    content {
        val prefs by FlorisPreferenceStore

        Text(
            text = "Everything the keyboard can show. Tap a switch to turn it on or off, or tap a " +
                "name to open its settings.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )

        Spacer(Modifier.height(8.dp))

        MaSwitchRow(
            title = "Feature row",
            summary = "The rows of keys above the keyboard",
            icon = Icons.Default.DragHandle,
            pref = prefs.dictate.maFeatureRowShown,
            route = Routes.Settings.MaFeatureRow,
        )
        MaSwitchRow(
            title = "Magic Finger row",
            summary = "The magic finger and one key for each thing it presses",
            icon = Icons.Default.TouchApp,
            pref = prefs.dictate.maMagicRowShown,
            route = Routes.Settings.MaMagic,
        )
        MaSwitchRow(
            title = "Copy buckets",
            summary = "C1 to C10, and whether they catch what you copy",
            icon = Icons.Default.ContentPasteGo,
            pref = prefs.dictate.maBucketsEnabled,
            route = Routes.Settings.MaBuckets,
        )
        MaSwitchRow(
            title = "Suggestion row",
            summary = "Word predictions above the keys",
            icon = Icons.Default.Spellcheck,
            pref = prefs.suggestion.enabled,
            route = Routes.Settings.MaPredictions,
        )
        MaSwitchRow(
            title = "Number row",
            summary = "Digits along the top of the letters",
            icon = Icons.Default.Numbers,
            pref = prefs.dictate.maExtraRow,
            // No screen of its own: it is one switch and nothing else, so sending him to a page
            // holding that same switch would be a journey to arrive where he already was.
            route = null,
        )

        Spacer(Modifier.height(24.dp))
    }
}

/**
 * One row: a name that opens a screen, and a switch that does not.
 *
 * The switch is given its own click target rather than the whole row toggling, because the row
 * navigates. A line where tapping anywhere does one thing and tapping a small part does another is
 * only safe when the small part is obviously a control, which a switch is.
 */
@Composable
private fun MaSwitchRow(
    title: String,
    summary: String,
    icon: ImageVector,
    pref: PreferenceData<Boolean>,
    route: Any?,
) {
    val scope = rememberCoroutineScope()
    val navController = LocalNavController.current
    val checked by pref.collectAsState()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .then(
                if (route != null) {
                    Modifier.clickable { navController.navigate(route) }
                } else {
                    Modifier
                },
            )
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // The mark the row controls, so the list reads like the keyboard rather than like names.
        // Grey rather than sand: the icon says which row, the colour says where a tap goes, and an
        // icon in sand on a line with nowhere to go would say the wrong one of those.
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp),
        )
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            // Sand means this takes you somewhere — the rule the wand's gear established. He built
            // this screen and still could not tell which half of a line was tappable, because a
            // name that navigates looked exactly like a name that does not. The Number row has no
            // screen, so its name stays ordinary: the colour has to be a promise or it is noise.
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (route != null) MaSand else MaterialTheme.colorScheme.onSurface,
            )
            // Summaries stay grey. They describe, they do not lead anywhere, and colouring them
            // would say they did.
            Text(
                text = summary,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = { on -> scope.launch { pref.set(on) } },
        )
    }
}

/**
 * Sand: this takes you somewhere.
 *
 * The same value as the feature row's, deliberately copied rather than shared. The keyboard's colour
 * lives in the keyboard's file and this is the settings app; a constant reaching across that line to
 * save four characters would tie two things together that are only equal by coincidence today.
 */
private val MaSand = Color(0xFFE8B15C)
