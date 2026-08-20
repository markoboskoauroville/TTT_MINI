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

import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.ClosedCaption
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Keyboard
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

        Section("On the keyboard")

        MaSwitchRow(
            title = "The keys",
            summary = "The letters and the bottom row \u2014 off leaves the rows and the microphone",
            icon = Icons.Default.Keyboard,
            pref = prefs.dictate.maZoneKeyboard,
            route = null,
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
        MaSwitchRow(
            title = "Feature row",
            summary = "The rows of keys above the keyboard",
            icon = Icons.Default.DragHandle,
            pref = prefs.dictate.maFeatureRowShown,
            route = Routes.Settings.MaFeatureRow,
        )
        MaSwitchRow(
            title = "Suggestion row",
            summary = "Word predictions above the keys",
            icon = Icons.Default.Spellcheck,
            pref = prefs.suggestion.enabled,
            route = Routes.Settings.MaPredictions,
        )

        Section("The copy row")

        MaSwitchRow(
            title = "Copy row here",
            summary = "Select all, paste, cut and the histories, on the typing keyboard",
            icon = Icons.Default.ContentPaste,
            pref = prefs.dictate.maCopyRowOnKeyboard,
            route = Routes.Settings.MaCopyRow,
        )
        MaSwitchRow(
            title = "Copy row in dictation",
            summary = "The same row, in the transcription view",
            icon = Icons.Default.ContentPasteGo,
            pref = prefs.dictate.maCopyRowOnDictate,
            route = Routes.Settings.MaCopyRow,
        )
        MaSwitchRow(
            title = "Edit row",
            summary = "The copy and paste strip both views share",
            icon = Icons.Default.ContentCut,
            pref = prefs.dictate.maEditRow,
            route = null,
        )
        MaSwitchRow(
            title = "Copy buckets",
            summary = "C1 to C10, and whether they catch what you copy",
            icon = Icons.Default.Inventory2,
            pref = prefs.dictate.maBucketsEnabled,
            route = Routes.Settings.MaBuckets,
        )

        Section("Pressing and speaking")

        MaSwitchRow(
            title = "Magic Finger row",
            summary = "The magic finger and one key for each thing it presses",
            icon = Icons.Default.TouchApp,
            pref = prefs.dictate.maMagicRowShown,
            route = Routes.Settings.MaMagic,
        )
        MaSwitchRow(
            title = "Volume keys",
            summary = "Up records, down sends. Off gives the keys back to the volume",
            icon = Icons.AutoMirrored.Filled.VolumeUp,
            pref = prefs.dictate.maVolumeKeys,
            route = null,
        )
        MaSwitchRow(
            title = "Subtitle row",
            summary = "The words as they are read aloud",
            icon = Icons.Default.ClosedCaption,
            // A string preference behind a switch: "subtitle" or "off", because the third value
            // lives on the spacebar and belongs in the Reader screen where it can be explained.
            // Switching it back on returns it to the subtitle row, which is what he last had.
            stringPref = prefs.dictate.maReaderDisplay,
            onValue = "subtitle",
            offValue = "off",
            route = Routes.Settings.MaReader,
        )
        MaSwitchRow(
            title = "Full screen reading",
            summary = "The whole passage, with the line being read at the top",
            icon = Icons.Default.Fullscreen,
            pref = prefs.dictate.maReaderFullscreen,
            route = Routes.Settings.MaReader,
        )

        Spacer(Modifier.height(24.dp))
    }
}

/**
 * One row backed by a boolean.
 *
 * Two entry points rather than one function with both kinds of preference in it, because reading a
 * preference in Compose has to happen the same way on every pass. A single function branching on
 * which kind it was given would call `collectAsState` a different number of times depending on the
 * argument, which is the sort of thing that works until it does not.
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
    val checked by pref.collectAsState()
    MaSwitchRowBase(title, summary, icon, checked, route) { on ->
        scope.launch { pref.set(on) }
    }
}

/**
 * One row backed by a word.
 *
 * The subtitle row stores `subtitle`, `spacebar` or `off`, because it has three values and only two
 * of them are shown. A switch is still the honest control: he is asking for it or not, and *where*
 * it appears is a choice for the Reader screen where there is room to explain it. Switching it back
 * on returns the value he is most likely to want rather than the one he last had, because the one he
 * last had was `off`.
 */
@Composable
private fun MaSwitchRow(
    title: String,
    summary: String,
    icon: ImageVector,
    stringPref: PreferenceData<String>,
    onValue: String,
    offValue: String,
    route: Any?,
) {
    val scope = rememberCoroutineScope()
    val value by stringPref.collectAsState()
    MaSwitchRowBase(title, summary, icon, value != offValue, route) { on ->
        scope.launch { stringPref.set(if (on) onValue else offValue) }
    }
}

/**
 * The row itself: a name that opens a screen, and a switch that does not.
 *
 * The switch has its own click target rather than the whole row toggling, because the row navigates.
 * A line where tapping anywhere does one thing and tapping a small part does another is only safe
 * when the small part is obviously a control, which a switch is.
 */
@Composable
private fun MaSwitchRowBase(
    title: String,
    summary: String,
    icon: ImageVector,
    checked: Boolean,
    route: Any?,
    onToggle: (Boolean) -> Unit,
) {
    val navController = LocalNavController.current
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
                                onCheckedChange = onToggle,
        )
    }
}

/** A heading, so a remote with thirteen switches on it reads as three groups rather than a list. */
@Composable
private fun Section(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 18.dp, bottom = 4.dp),
    )
}


/**
 * Sand: this takes you somewhere.
 *
 * The same value as the feature row's, deliberately copied rather than shared. The keyboard's colour
 * lives in the keyboard's file and this is the settings app; a constant reaching across that line to
 * save four characters would tie two things together that are only equal by coincidence today.
 */
private val MaSand = Color(0xFFE8B15C)
