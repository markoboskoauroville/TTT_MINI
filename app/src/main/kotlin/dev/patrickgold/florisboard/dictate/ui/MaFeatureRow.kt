/*
 * Copyright (C) 2026 Marko Bosko, Mantra Productions
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

package dev.patrickgold.florisboard.dictate.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material.icons.outlined.AudioFile
import androidx.compose.material.icons.outlined.AutoFixHigh
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material3.Text
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import dev.patrickgold.jetpref.datastore.model.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import dev.patrickgold.florisboard.clipboardManager
import dev.patrickgold.florisboard.dictate.MaClipboardSlots
import androidx.compose.foundation.layout.size
import kotlinx.coroutines.delay
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.patrickgold.florisboard.dictate.MaRows
import dev.patrickgold.florisboard.dictate.MaMacroSyntax
import androidx.compose.ui.platform.LocalContext
import dev.patrickgold.florisboard.FlorisImeService
import dev.patrickgold.florisboard.app.FlorisPreferenceStore
import dev.patrickgold.florisboard.dictate.MaFeatureOrder
import dev.patrickgold.florisboard.dictate.MaFeatureKey
import dev.patrickgold.florisboard.dictate.DictateController
import dev.patrickgold.florisboard.dictate.MaSettingsResume
import dev.patrickgold.florisboard.dictate.DictateLongformMode
import dev.patrickgold.florisboard.editorInstance
import dev.patrickgold.florisboard.keyboardManager
import dev.patrickgold.florisboard.ime.text.key.KeyCode
import kotlinx.coroutines.launch
import org.florisboard.lib.compose.stringRes
import dev.patrickgold.florisboard.R

/**
 * The feature row: one row, drawn by both views and the last row standing when everything
 * above it is folded away.
 *
 * `AP · select-all · backspace · mic · book · 1 2 3 · enter`, **by default**. The order now lives in a
 * preference and is rearranged by the user in Settings, Mantra, Feature row; what follows is the
 * reasoning behind the default and the shape of the row, not a description of what is on screen.
 *
 * **This order is Marko's and it has been corrected twice.** At build 139 the switches were on the
 * left and the borrowed keys on the right, and he swapped them. At build 146 he moved the microphone
 * in beside backspace, put the book next to it, and sent enter to the far end. Do not rearrange this
 * row on a theory; it is arranged by the person using it.
 *
 * What the current order means, so a future change can tell what it would be breaking:
 *
 * - **The busy end is the end the thumb starts from.** AP, select-all and backspace are used inside
 *   a sentence; the numbered switches are reached a few times a session.
 * - **The two view swaps sit together.** The microphone and the book are the only keys here that
 *   change which view is on screen, and a pair that does one kind of thing is found by feel. They
 *   were at opposite ends of the row before, which is the arrangement that made that hard.
 * - **Enter is bottom right**, where every keyboard ever made has put it. Borrowing a habit somebody
 *   already has beats any argument about grouping.
 * - **The numbers read left to right** as the parts of the keyboard they control.
 * - **AP, select-all, backspace and enter are the keys that must survive a fold.** The first two are
 *   the copy row's most used pair; the last two are the only keys from the keyboard proper with no
 *   substitute anywhere else once zone two is shut.
 *
 * The rule it exists to satisfy is Marko's, and it is a good one: a feature you have to enable in a
 * settings app before you can see it is a feature most people never find. The dictation view is not
 * the full keyboard, so it has the vertical room the typing view does not, and this spends some of
 * that room on reach.
 *
 * Three things borrowed from keyboards that have already been judged by very large numbers of
 * people:
 *
 * - **Gboard put its shortcuts in key-shaped buttons and then deleted its overflow menu**, freeing
 *   the slot it occupied. A shortcut behind a menu is a shortcut to a menu. Every key here acts on
 *   the first tap; none of them opens a list of more keys except the dashboard, which is a list of
 *   switches rather than of actions.
 * - **HeliBoard distributes its toolbar keys evenly** rather than packing them from one edge. Equal
 *   weights below, so the row reads as a row and the thumb learns positions rather than icons.
 * - **Ten keys is exactly what the letter row already is.** The usual objection to ten controls in a
 *   phone-width row is that each falls under the 48dp touch minimum, and at a typical 360dp width
 *   these are about 36dp. But the QWERTY row above is ten keys at the same width, on the same
 *   device, in the same hand, and it has been usable for as long as touch keyboards have existed.
 *   The precedent is not theoretical, it is one row up.
 *
 * Colour is state only, as everywhere else in this app: gold ink on the near-black key, and green
 * on the three keys that are switches so their positions are readable without pressing them.
 */
@Composable
fun MaFeatureRow(modifier: Modifier = Modifier, rowHeight: Dp) {
    val context = LocalContext.current
    val keyboardManager by context.keyboardManager()
    val prefs by FlorisPreferenceStore
    val scope = rememberCoroutineScope()

    // Three zones, and that is the whole keyboard, numbered as they are stacked on screen from the
    // keys outwards: 1 the number row, 2 the keys themselves, 3 the copy and paste row along the
    // top. Each has one switch and each switch has one key here.
    //
    // The numbering is Marko's and it is the arrangement, not a naming: he drew the three dividing
    // lines on the keyboard and these are the three parts they cut it into.
    val zone1 by prefs.dictate.maExtraRow.collectAsState()
    val zone2 by prefs.dictate.maZoneKeyboard.collectAsState()
    val zone3 by prefs.dictate.maEditRow.collectAsState()

    // Green when the zone is showing, dark when it is folded away, so the row is a map of what is
    // above it and can be read without pressing anything.
    //
    // Each key shows its own switch rather than what is actually on screen. Folding the keys away
    // with 2 takes the number row with them, since the number row sits inside that zone, but it
    // must not erase the decision about whether the number row should be there when the keys come
    // back. So 1 can be green while nothing is visible, and that is the truth about the switch.
    val onGreen = Color(0xFF6FA85A)

    // Long press anywhere here folds the row away. The finger is already on the row it wants gone.
    val fold: () -> kotlin.Unit = { scope.launch { prefs.dictate.maFeatureRowShown.set(false) } }

    // Select-all becomes deselect when there is a selection, so the key it draws has to know. Read
    // once here and handed down, exactly as the copy row reads it.
    val editorInstance by context.editorInstance()
    val editorContent by editorInstance.activeContentFlow.collectAsState()
    val hasSelection = editorContent.selection.isSelectionMode

    // Every row the user has, drawn one under another. The height is per row rather than for the
    // whole block, so the stack is as tall as it needs to be and the caller does not have to know
    // how many rows there are — which it cannot, since the number is a preference and changes while
    // the keyboard is open.
    // The clipboard history, for the CH row. Read once here rather than per key: nine keys each
    // collecting the same flow would recompose nine times for every copy anywhere on the phone.
    val clipboardManager by context.clipboardManager()
    val clipHistory by clipboardManager.historyFlow.collectAsState()
    val clipReplace by prefs.dictate.maClipReplace.collectAsState()

    val rowsRaw by prefs.dictate.maRows.collectAsState()
    val macroRaw by prefs.dictate.maMacroSlots.collectAsState()
    val macroSlots = remember(macroRaw) { MaMacroSlots.parse(macroRaw) }

    // Rows that are off are absent rather than empty, so the ones below move up and the keyboard is
    // genuinely shorter. When everything is off everywhere this hands back the settings key alone,
    // which is the floor that keeps a route back to the screen that would restore the rest.
    val storedRows = remember(rowsRaw) {
        if (rowsRaw.isBlank()) MaRows.defaultRows() else MaRows.parse(rowsRaw)
    }
    val rows = MaRows.visibleRows(storedRows)

    Column(modifier = modifier.fillMaxWidth()) {
      rows.forEach { rowButtons ->
        Row(
            modifier = Modifier.fillMaxWidth().height(rowHeight),
            verticalAlignment = Alignment.CenterVertically,
        ) {
        val keyMod = Modifier.weight(1f).fillMaxHeight()

        // THE ARRANGEMENT IS THE USER'S. It was corrected by hand twice, at builds 139 and 146,
        // both times from a screenshot with arrows drawn on it and both times in the direction
        // opposite to what looked sensible from inside this file. That is the argument for the
        // editor: the person holding the keyboard should not have to send a picture and wait for a
        // build to move a key.
        //
        // The old ALWAYS_ON guarantee is gone with the fixed row, and it has to be. It could promise
        // backspace and enter survived because the row held every key there was and hiding was the
        // only edit; now a row is a list somebody builds, and a list can be built without them. What
        // replaces it is the editor warning before saving a set of rows with no way to delete or to
        // record, which is a warning about a real arrangement rather than a rule that makes some
        // arrangements unreachable.
        rowButtons.forEach { button ->
          when (button) {
            // A macro key: what the user wrote, on the face of it, running what he attached to it.
            //
            // Long press folds the row like every other key here, which means a macro cannot also
            // use a long press for anything of its own. That is the right trade: folding has to
            // work from anywhere on the row or the row cannot be got rid of, and a macro that
            // needed two gestures would need explaining.
            // M1 to M10. What the button does lives in MaMacroSlots, so the row holds only which
            // slot it is: the same macro can then sit in two rows, edited once.
            //
            // An empty slot still draws, showing its own name. A macro button that vanished until
            // it was configured would leave the user hunting for a key that is not there yet, and
            // the label is how he knows which slot to go and fill in.
            is MaRows.Button.Macro -> {
                val slot = MaMacroSlots.at(macroSlots, button.slot)
                ThemedTextKey(
                    label = slot.label,
                    modifier = keyMod,
                    tint = null,
                    onLongClick = fold,
                ) {
                    if (!slot.isEmpty) {
                        MaMacroSyntax.run(slot.macro, FlorisImeService.currentInputConnection())
                    }
                }
            }

            // C1 to C10, newest first. Paste-replace, the way AP pastes.
            is MaRows.Button.Clip -> {
                val item = MaClipboardSlots.itemAt(clipHistory, button.slot)
                ThemedKey(
                    code = KeyCode.NOOP,
                    // The key shows only its number: ten text previews across a row would be a few
                    // characters wide each and unreadable. What it holds is spoken instead, which is
                    // the only way to answer "what is on C4" without pasting it somewhere to find out.
                    modifier = keyMod.semantics {
                        contentDescription = MaClipboardSlots.describe(item, button.slot)
                    },
                    onClick = {
                        if (item != null) {
                            scope.launch {
                                if (clipReplace) {
                                    keyboardManager.activeState.isManualSelectionMode = false
                                    delay(MA_CLIP_LEAD_MS)
                                    FlorisImeService.currentInputConnection()
                                        ?.performContextMenuAction(android.R.id.selectAll)
                                    delay(MA_CLIP_LEAD_MS)
                                    FlorisImeService.currentInputConnection()?.commitText("", 1)
                                    // Five times the others, and not by accident: the paste is the
                                    // one step that actually gets dropped. Too short and the key
                                    // empties the field without refilling it, which is worse than
                                    // doing nothing because the old text is gone too.
                                    delay(MA_CLIP_PASTE_MS)
                                }
                                editorInstance.commitClipboardItem(item)
                            }
                        }
                    },
                    onLongClick = fold,
                ) { fg ->
                    Text(
                        text = "C${button.slot}",
                        color = if (item == null) fg.copy(alpha = 0.4f) else fg,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }

            is MaRows.Button.Builtin -> when (button.key) {
                MaFeatureKey.ALL_PASTE, MaFeatureKey.SELECT_ALL, MaFeatureKey.BACKSPACE,
                MaFeatureKey.ALL_CLEAR, MaFeatureKey.SPACE -> {
                    // The five borrowed from the copy row, drawn by that row's own code rather than
                    // rebuilt here, so a fix to AP or to backspace lands in both places at once.
                    // Backspace and space are the two keys from the keyboard proper that nothing
                    // else stands in for: with zone two closed there is no other way to delete a
                    // character, and no other way to put a space between two dictated sentences.
                    //
                    // None of these fold the row on a long press, and must not. Backspace holds to
                    // repeat and swipes to select, which is the behaviour it has everywhere else in
                    // this app, and a key that repeats cannot also mean something else when held.
                    LegacyActionKey(
                        action = when (button.key) {
                            MaFeatureKey.ALL_PASTE -> LegacyEditAction.ALL_PASTE
                            MaFeatureKey.SELECT_ALL -> LegacyEditAction.SELECT_ALL
                            MaFeatureKey.ALL_CLEAR -> LegacyEditAction.ALL_CLEAR
                            MaFeatureKey.SPACE -> LegacyEditAction.SPACE
                            else -> LegacyEditAction.BACKSPACE
                        },
                        modifier = keyMod,
                        keyboardManager = keyboardManager,
                        hasSelection = hasSelection,
                    )
                }

                MaFeatureKey.MIC -> {
                    // The record button. It was a door to the transcribe view until that view was
                    // removed; now it is the recording control itself, in the same place the thumb
                    // already knows. onMicClick is the same entry point volume up uses, so the two
                    // routes cannot drift apart: start when idle, stop and send when recording.
                    //
                    // Lit red while recording, dark otherwise. Colour is state here, not decoration,
                    // and this is the recording red the lamp and the level meter already use. It
                    // does not pulse and must not: a light that moves says "look at me" over and
                    // over, and this one only has to say whether the microphone is open.
                    val recording = DictateController.state.collectAsState().value is
                        DictateController.UiState.Recording
                    ThemedIconKey(
                        code = KeyCode.NOOP,
                        icon = Icons.Default.Mic,
                        contentDescription = stringRes(
                            if (recording) R.string.ma__feature_record_stop else R.string.ma__feature_record,
                        ),
                        modifier = keyMod,
                        tint = if (recording) MaRecordRed else null,
                        onLongClick = fold,
                    ) {
                        DictateController.onMicClick(context)
                    }
                }

                MaFeatureKey.SETTINGS -> {
                    // Settings, reopened where they were left rather than at the top.
                    //
                    // Reaching a setting is the expensive part on a phone driven by voice; changing
                    // it once you are there is not. MaSettingsResume remembers the screen and how
                    // far down it he was, and hands back a deep link to both.
                    ThemedIconKey(
                        code = KeyCode.NOOP,
                        icon = Icons.Default.Settings,
                        contentDescription = stringRes(R.string.ma__feature_settings),
                        modifier = keyMod,
                        onLongClick = fold,
                    ) {
                        MaSettingsResume.open(context)
                    }
                }

                MaFeatureKey.ZONE_1 -> {
                    // 1, the number row. Digits, or whichever set the row is showing.
                    ThemedTextKey("1", keyMod, if (zone1) onGreen else null, fold) {
                        scope.launch { prefs.dictate.maExtraRow.set(!zone1) }
                    }
                }

                MaFeatureKey.ZONE_2 -> {
                    // 2, the keyboard itself, all of it at once. This is the one that gives back real estate,
                    // and on a keyboard driven by voice it is off more often than it is on.
                    ThemedTextKey("2", keyMod, if (zone2) onGreen else null, fold) {
                        scope.launch { prefs.dictate.maZoneKeyboard.set(!zone2) }
                    }
                }

                MaFeatureKey.ZONE_3 -> {
                    // 3, the copy and paste row along the top. Paste, copy, history and the rest of it.
                    ThemedTextKey("3", keyMod, if (zone3) onGreen else null, fold) {
                        scope.launch { prefs.dictate.maEditRow.set(!zone3) }
                    }
                }

                MaFeatureKey.ENTER -> {
                    // Enter, last, which is where every keyboard ever made has put it. It was beside backspace
                    // until Marko moved it here, and the bottom right corner is a position the thumb finds
                    // without looking because it has been finding it on other keyboards for decades. Borrowing a
                    // habit somebody already has is worth more than any argument about grouping.
                    //
                    // Still the same key the bottom row draws rather than a new one that types a newline: tap
                    // for a newline, hold for the character popup from the settings screen. And still here for
                    // the reason backspace is, that with zone two folded away there is no enter key anywhere
                    // else, and a keyboard that cannot end a line has to be unfolded to finish a sentence.
                    //
                    // No fold on long press. The hold already means the popup.
                    LegacyEnterKey(
                        keyboardManager = keyboardManager,
                        modifier = keyMod,
                    )
                }
            }
          }
        }
        }
      }
    }
}

/** A round key carrying a numeral, styled exactly as every other key in the row. */
@Composable
private fun ThemedTextKey(
    label: String,
    modifier: Modifier,
    tint: Color?,
    onLongClick: () -> kotlin.Unit,
    onClick: () -> kotlin.Unit,
) {
    ThemedKey(
        code = KeyCode.NOOP,
        modifier = modifier,
        onLongClick = onLongClick,
        onClick = onClick,
    ) { fg ->
        Text(
            text = label,
            color = tint ?: fg,
            fontSize = 17.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

/**
 * The recording red, the same value the lamp, the level meter and the history's destructive actions
 * already use. Declared here rather than imported from LegacyDictateLayout because that file is
 * being deleted and a colour is cheaper to repeat than to route around.
 *
 * Colour is state in this app and never decoration. This one says the microphone is open, and it
 * neither pulses nor breathes while it says it.
 */
private val MaRecordRed = Color(0xFF9B3B33)

/**
 * The pause between the steps of a clipboard replace, matching the copy row's own.
 *
 * Repeated rather than imported from LegacyDictateLayout because that file is being deleted with the
 * transcribe view, and a hundred milliseconds is cheaper to state twice than to route around.
 */
private const val MA_CLIP_LEAD_MS = 100L

/** The wait before the paste itself. AP's value, for AP's reason. See the CH key. */
private const val MA_CLIP_PASTE_MS = 500L
