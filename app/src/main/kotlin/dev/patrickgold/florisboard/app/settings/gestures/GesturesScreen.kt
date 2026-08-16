/*
 * Copyright (C) 2021-2025 The FlorisBoard Contributors
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

package dev.patrickgold.florisboard.app.settings.gestures

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import dev.patrickgold.florisboard.dictate.MaMagicTargets
import dev.patrickgold.jetpref.datastore.model.collectAsState
import kotlinx.coroutines.launch
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.app.FlorisPreferenceStore
import dev.patrickgold.florisboard.app.settings.search.settingsSearchAnchor
import dev.patrickgold.florisboard.app.enumDisplayEntriesOf
import dev.patrickgold.florisboard.ime.text.gestures.SwipeAction
import dev.patrickgold.florisboard.lib.compose.FlorisScreen
import dev.patrickgold.jetpref.datastore.ui.DialogSliderPreference
import dev.patrickgold.jetpref.datastore.ui.ExperimentalJetPrefDatastoreUi
import dev.patrickgold.jetpref.datastore.ui.ListPreference
import dev.patrickgold.jetpref.datastore.ui.PreferenceGroup
import dev.patrickgold.jetpref.datastore.ui.SwitchPreference
import org.florisboard.lib.compose.FlorisInfoCard
import org.florisboard.lib.compose.stringRes

@OptIn(ExperimentalJetPrefDatastoreUi::class)
@Composable
fun GesturesScreen() = FlorisScreen {
    title = stringRes(R.string.settings__gestures__title)

    content {
        // General gestures are gone. Swipe up for shift, down to hide, left and right to change
        // subtype: four whole-keyboard swipes on a keyboard whose words arrive by voice, where every
        // one of them is a way to trigger something by accident mid-sentence. The space bar gestures
        // stay, because those are aimed at one key and do the one thing worth doing there.
        PreferenceGroup(title = "Volume keys") {
            SwitchPreference(
                prefs.dictate.maVolumeKeys,
                modifier = Modifier.settingsSearchAnchor("ma__volume_keys"),
                title = "Volume keys do more on a long press",
                summary = "Hold volume up to start recording, hold it again to stop and send. " +
                    "Hold volume down to press a button on screen. A short press on either is " +
                    "still just the volume, and both are ordinary volume keys the moment the " +
                    "keyboard closes.",
            )
            SwitchPreference(
                prefs.dictate.maGlobalVolumeKeys,
                modifier = Modifier.settingsSearchAnchor("ma__global_volume_keys"),
                title = "Work everywhere, with no keyboard",
                summary = "Hold volume up in any app \u2014 a gallery, a map, a book \u2014 and it " +
                    "records. With no text field to write into, the note is kept in History. " +
                    "Needs the accessibility service, and Android will warn you more strongly " +
                    "when granting it, because seeing every key is also how a keylogger works. " +
                    "This app reads the two volume keys and hands every other key straight back. " +
                    "Short presses stay volume.",
            )

            // Which taught term the long press carries.
            //
            // Offered as a list of the terms he has already taught the finger rather than a text
            // field, because a term typed here that does not match one on the Magic finger screen
            // is a key that silently does nothing, and nothing on this screen would say why.
            // Named explicitly. The JetPref preference composables above reach the store through
            // their own scope; plain Compose code in the same block does not, so it has to ask.
            val store by FlorisPreferenceStore
            val magicRaw by store.dictate.maMagicTargets.collectAsState()
            val terms = remember(magicRaw) {
                MaMagicTargets.parse(magicRaw)
                    .ifEmpty { MaMagicTargets.defaults() }
                    .filter { !it.isSpacer && it.term.isNotBlank() }
                    .map { it.term }
                    .distinct()
            }
            val current by store.dictate.maVolumeDownTerm.collectAsState()
            val scope = rememberCoroutineScope()
            Text(
                text = "Long press on volume down presses:",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp),
            )
            terms.forEach { term ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { scope.launch { store.dictate.maVolumeDownTerm.set(term) } }
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(
                        selected = current == term,
                        onClick = { scope.launch { store.dictate.maVolumeDownTerm.set(term) } },
                    )
                    Text(text = term, style = MaterialTheme.typography.bodyMedium)
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { scope.launch { store.dictate.maVolumeDownTerm.set("") } }
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(
                    selected = current.isBlank(),
                    onClick = { scope.launch { store.dictate.maVolumeDownTerm.set("") } },
                )
                Text(
                    text = "Nothing \u2014 leave it a plain volume key",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }

        PreferenceGroup(title = stringRes(R.string.pref__gestures__space_bar_title)) {
            ListPreference(
                prefs.gestures.spaceBarSwipeUp,
                modifier = Modifier.settingsSearchAnchor("pref__gestures__space_bar_swipe_up__label"),
                title = stringRes(R.string.pref__gestures__space_bar_swipe_up__label),
                entries = enumDisplayEntriesOf(SwipeAction::class, "general"),
            )
            ListPreference(
                prefs.gestures.spaceBarSwipeLeft,
                modifier = Modifier.settingsSearchAnchor("pref__gestures__space_bar_swipe_left__label"),
                title = stringRes(R.string.pref__gestures__space_bar_swipe_left__label),
                entries = enumDisplayEntriesOf(SwipeAction::class, "general"),
            )
            ListPreference(
                prefs.gestures.spaceBarSwipeRight,
                modifier = Modifier.settingsSearchAnchor("pref__gestures__space_bar_swipe_right__label"),
                title = stringRes(R.string.pref__gestures__space_bar_swipe_right__label),
                entries = enumDisplayEntriesOf(SwipeAction::class, "general"),
            )
            ListPreference(
                prefs.gestures.spaceBarLongPress,
                modifier = Modifier.settingsSearchAnchor("pref__gestures__space_bar_long_press__label"),
                title = stringRes(R.string.pref__gestures__space_bar_long_press__label),
                entries = enumDisplayEntriesOf(SwipeAction::class, "general"),
            )
        }

        PreferenceGroup(title = stringRes(R.string.pref__gestures__other_title)) {
            ListPreference(
                prefs.gestures.deleteKeySwipeLeft,
                modifier = Modifier.settingsSearchAnchor("pref__gestures__delete_key_swipe_left__label"),
                title = stringRes(R.string.pref__gestures__delete_key_swipe_left__label),
                entries = enumDisplayEntriesOf(SwipeAction::class, "deleteSwipe"),
            )
            ListPreference(
                prefs.gestures.deleteKeyLongPress,
                modifier = Modifier.settingsSearchAnchor("pref__gestures__delete_key_long_press__label"),
                title = stringRes(R.string.pref__gestures__delete_key_long_press__label),
                entries = enumDisplayEntriesOf(SwipeAction::class, "deleteLongPress"),
            )
            // Talk to Type: the swipe-to-symbol switch, placed directly above the two
            // thresholds that govern how far and how fast a swipe must be, since turning it on
            // and then tuning those two is one continuous task rather than three separate ones.
            // The command and arrow bars switch is gone with the bars themselves. A switch that
            // turns nothing on is worse than a missing one: it teaches that settings do not work.
            SwitchPreference(
                prefs.gestures.maSwipeToSymbol,
                title = "Swipe to symbol",
                summary = "Swipe from a key toward one of its printed symbols to type it, " +
                    "instead of long pressing. Up types the hint, left, right and down type the " +
                    "first three extras. Tune the two thresholds below if a flick reads as a tap.",
            )
            DialogSliderPreference(
                prefs.gestures.swipeVelocityThreshold,
                modifier = Modifier.settingsSearchAnchor("pref__gestures__swipe_velocity_threshold__label"),
                title = stringRes(R.string.pref__gestures__swipe_velocity_threshold__label),
                valueLabel = { stringRes(R.string.unit__display_pixel_per_seconds__symbol, "v" to it) },
                min = 400,
                max = 4000,
                stepIncrement = 100,
            )
            DialogSliderPreference(
                prefs.gestures.swipeDistanceThreshold,
                modifier = Modifier.settingsSearchAnchor("pref__gestures__swipe_distance_threshold__label"),
                title = stringRes(R.string.pref__gestures__swipe_distance_threshold__label),
                valueLabel = { stringRes(R.string.unit__display_pixel__symbol, "v" to it) },
                min = 12,
                max = 72,
                stepIncrement = 1,
            )
        }
    }
}
