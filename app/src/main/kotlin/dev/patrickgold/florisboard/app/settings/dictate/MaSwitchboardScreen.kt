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

import androidx.compose.ui.draw.alpha
import androidx.compose.runtime.remember
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.ClosedCaption
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

        // One list, in his order, with no headings and no prose.
        //
        // It had three headings and a paragraph of explanation, and he said plainly that he already
        // knows what "on the keyboard" means. He is right: a heading that names a category he can
        // see is a line to scroll past, and a switchboard is read at a glance or not at all.
        //
        // Rearrangeable for the same reason the settings list and the rows are. **The one he uses
        // most goes at the top**, and only he knows which that is — a shipped order is a guess, and
        // a guess repeated every day is worse than a control.
        val scope = rememberCoroutineScope()
        val raw by prefs.dictate.maSwitchboardOrder.collectAsState()
        val items = remember(raw) { MaSwitchboardOrder.parse(raw) }

        MaReorderableColumn(
            items = items,
            rowHeight = SWITCH_ROW_HEIGHT,
            onMove = { from, to ->
                val next = items.toMutableList()
                next.add(to, next.removeAt(from))
                scope.launch { prefs.dictate.maSwitchboardOrder.set(MaSwitchboardOrder.serialize(next)) }
            },
            onSettled = { },
        ) { _, entry, lifted ->
            MaSwitchRowFor(entry = entry, lifted = lifted)
        }

        Spacer(Modifier.height(24.dp))
    }
}

/**
 * One row, chosen by id.
 *
 * A `when` over the entry rather than a list of composables built up front, so a switch that reads a
 * preference only reads it when it is actually drawn.
 */
@Composable
private fun MaSwitchRowFor(entry: MaSwitchboardOrder.Entry, lifted: Boolean) {
    val prefs by FlorisPreferenceStore
    val modifier = if (lifted) Modifier.alpha(0.7f) else Modifier
    when (entry) {
        MaSwitchboardOrder.Entry.KEYS ->
            MaSwitchRow("The keys", Icons.Default.Keyboard, prefs.dictate.maZoneKeyboard, null, modifier)
        MaSwitchboardOrder.Entry.NUMBER_ROW ->
            MaSwitchRow("Number row", Icons.Default.Numbers, prefs.dictate.maExtraRow, null, modifier)
        MaSwitchboardOrder.Entry.FEATURE_ROW ->
            MaSwitchRow("Feature row", Icons.Default.DragHandle, prefs.dictate.maFeatureRowShown, Routes.Settings.MaFeatureRow, modifier)
        MaSwitchboardOrder.Entry.SUGGESTIONS ->
            MaSwitchRow("Suggestion row", Icons.Default.Spellcheck, prefs.suggestion.enabled, Routes.Settings.MaPredictions, modifier)
        // The copy row, and the same switch key 3 presses on the feature row. The glyph is the one
        // that key draws, so the row and the key are recognisable as one thing.
        MaSwitchboardOrder.Entry.EDIT_ROW ->
            MaSwitchRow("Copy row", Icons.Default.ContentPaste, prefs.dictate.maEditRow, Routes.Settings.MaCopyRow, modifier)
        MaSwitchboardOrder.Entry.MAGIC_ROW ->
            MaSwitchRow("Magic Finger row", Icons.Default.TouchApp, prefs.dictate.maMagicRowShown, Routes.Settings.MaMagic, modifier)
        MaSwitchboardOrder.Entry.FULLSCREEN ->
            MaSwitchRow("Full screen reading", Icons.Default.Fullscreen, prefs.dictate.maReaderFullscreen, Routes.Settings.MaReader, modifier)
        MaSwitchboardOrder.Entry.SUBTITLE ->
            MaSwitchRow("Subtitle row", Icons.Default.ClosedCaption, prefs.dictate.maReaderDisplay, "subtitle", "off", Routes.Settings.MaReader, modifier)
    }
}

@Composable
private fun MaSwitchRow(
    title: String,
    icon: ImageVector,
    pref: PreferenceData<Boolean>,
    route: Any?,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val checked by pref.collectAsState()
    MaSwitchRowBase(title, icon, checked, route, modifier) { on ->
        scope.launch { pref.set(on) }
    }
}

@Composable
private fun MaSwitchRow(
    title: String,
    icon: ImageVector,
    stringPref: PreferenceData<String>,
    onValue: String,
    offValue: String,
    route: Any?,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val value by stringPref.collectAsState()
    MaSwitchRowBase(title, icon, value != offValue, route, modifier) { on ->
        scope.launch { stringPref.set(if (on) onValue else offValue) }
    }
}

@Composable
private fun MaSwitchRowBase(
    title: String,
    icon: ImageVector,
    checked: Boolean,
    route: Any?,
    rowModifier: Modifier,
    onToggle: (Boolean) -> Unit,
) {
    val navController = LocalNavController.current
    Row(
        modifier = rowModifier
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

/** One switch row, tall enough for a 24dp icon and a name with room around them. */
private val SWITCH_ROW_HEIGHT = 64.dp
