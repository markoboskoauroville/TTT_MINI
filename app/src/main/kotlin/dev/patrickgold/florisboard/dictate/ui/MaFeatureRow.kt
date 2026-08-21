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

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.SwapHoriz
import dev.patrickgold.florisboard.dictate.overlay.MaAppSwitcher
import dev.patrickgold.florisboard.dictate.overlay.DictateAccessibilityService
import android.widget.Toast
import dev.patrickgold.florisboard.dictate.overlay.MaScreenTargets
import androidx.compose.material.icons.filled.History
import dev.patrickgold.florisboard.dictate.MaReader
import dev.patrickgold.florisboard.dictate.MaLanguage
import dev.patrickgold.florisboard.dictate.MaMagicTargets
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.heightIn
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.ToggleOn
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.ContentPasteGo
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.KeyboardCapslock
import androidx.compose.material.icons.filled.Numbers
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material.icons.outlined.AudioFile
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Icon
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
import dev.patrickgold.florisboard.dictate.MaMacroSlots
import androidx.compose.foundation.layout.size
import kotlinx.coroutines.delay
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.Box
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
import androidx.compose.runtime.LaunchedEffect
import dev.patrickgold.florisboard.ime.ImeUiMode
import dev.patrickgold.florisboard.dictate.MaClipCapture
import dev.patrickgold.florisboard.dictate.DictateController
import dev.patrickgold.florisboard.dictate.MaSettingsResume
import dev.patrickgold.florisboard.dictate.DictateLongformMode
import dev.patrickgold.florisboard.editorInstance
import dev.patrickgold.florisboard.keyboardManager
import dev.patrickgold.florisboard.ime.input.InputShiftState
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
fun MaFeatureRow(
    modifier: Modifier = Modifier,
    rowHeight: Dp,
    /**
     * Draw ONLY the copy row, for the transcription view.
     *
     * That view wants the clipboard row and not the three feature rows, which belong to the typing
     * keyboard. Same composable either way, so a key behaves identically in both places — the caller
     * chooses which rows, not which implementation.
     */
    copyRowOnly: Boolean = false,
    /**
     * Draw the things that sit ABOVE the rows: the wand bar, the magic row, the reader dashboard.
     *
     * True everywhere except the typing keyboard's copy row, which is a second instance of this
     * composable on a screen that already has one. `maDashboardOpen` is file-level state and
     * `maMagicRowShown` is a preference, so both instances would draw them and the keyboard would
     * show two wand bars, two magic rows and two dashboards stacked on each other.
     *
     * The scroll stepper is deliberately NOT gated: it hangs off `scrollMenu`, which is local to
     * each instance, so it opens over the row whose key was actually held.
     */
    drawChrome: Boolean = true,
) {
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

    // The long press that hid the whole row is gone, on every key.
    //
    // It was a gesture with no sign of itself and no undo: a long press anywhere on the row made the
    // row vanish, and nothing on screen then said what had happened or how to bring it back. A key
    // held a moment too long — while thinking, or while the phone was slow — cost the feature, and
    // the way back was a settings screen the user had no reason to connect with it.
    //
    // Hiding the row now lives in one place, the switch at the top of the feature row editor, where
    // it is labelled and reversible.

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


    // What C1 to C10 currently hold, in the order they were copied.
    val spacerTenths by prefs.dictate.maSpacerTenths.collectAsState()
    val clipDelaySelect by prefs.dictate.maClipDelaySelect.collectAsState()
    val clipDelayDelete by prefs.dictate.maClipDelayDelete.collectAsState()
    val clipDelayPaste by prefs.dictate.maClipDelayPaste.collectAsState()
    val capturedRaw by prefs.dictate.maClipCaptured.collectAsState()
    val capturedSlots = remember(capturedRaw) { MaClipCapture.parse(capturedRaw) }


    val rowsRaw by prefs.dictate.maRows.collectAsState()
    val macroRaw by prefs.dictate.maMacroSlots.collectAsState()
    val macroSlots = remember(macroRaw) { MaMacroSlots.parse(macroRaw) }

    // Rows that are off are absent rather than empty, so the ones below move up and the keyboard is
    // genuinely shorter. When everything is off everywhere this hands back the settings key alone,
    // which is the floor that keeps a route back to the screen that would restore the rest.
    val storedRows = remember(rowsRaw) {
        if (rowsRaw.isBlank()) MaRows.defaultRows() else MaRows.parse(rowsRaw)
    }
    val copyRowRaw by prefs.dictate.maCopyRow.collectAsState()
    val copyRow = remember(copyRowRaw) { MaRows.parseCopyRow(copyRowRaw) }
    // Appended last, so it sits nearest the keys in the transcription view where the hand is.
    // visibleRows yields List<List<Button>>, so the copy row contributes its BUTTONS — appending
    // its entries would have been a type error hiding behind a plausible name.
    val copyButtons = if (copyRow.enabled) copyRow.visibleButtons else emptyList()

    // ONE COPY ROW IN THE WHOLE APP, AND THIS IS IT.
    //
    // It is drawn by `copyRowOnly = true` and by nothing else. Both views ask for it that way, so
    // both get the same buttons in the same order from the same preference, and the only difference
    // between them is *whether* it is asked for:
    //
    //   transcription view — always. Not a switch. That view has no letter keys and the clipboard is
    //                        its whole job, so a switch offering to remove it offers to empty the
    //                        screen.
    //   typing keyboard    — `maEditRow`, the copy-row key on the feature row. One row, one switch,
    //                        and it is the key he already presses.
    //
    // There used to be a second way in: the copy row appended to the feature rows here, behind
    // `maCopyRowOnKeyboard`. That is gone. A row with two ways of appearing and two switches is two
    // rows wearing one name, and it is how the keyboard ended up showing a copy row that did not
    // match the one in the transcription view.
    //
    // visibleRows yields List<List<Button>>, so the copy row contributes its BUTTONS — appending
    // its entries would have been a type error hiding behind a plausible name.
    val rows = if (copyRowOnly) {
        if (copyButtons.isNotEmpty()) listOf(copyButtons) else emptyList()
    } else {
        MaRows.visibleRows(storedRows)
    }
    // The buckets this user actually has, and whether they are all holding something. Derived from
    // the same rows the keyboard is drawing, so the answer here and the answer the capture uses
    // cannot disagree about how many buckets exist.
    val visibleClipSlots = remember(storedRows) { MaRows.visibleClipSlots(storedRows) }
    // Never full when they are off, so nothing turns red while he is not using them.
    // What the magic key looks for, newest first. Editable in settings so a new site costs a line
    // of text rather than a build.
    val magicRaw by prefs.dictate.maMagicTargets.collectAsState()
    // What the last scan found, held only while the picker is open.
    val activeLangCode by prefs.dictate.activeInputLanguage.collectAsState()
    val langMode by prefs.dictate.maLanguageMode.collectAsState()
    val scrollPages by prefs.dictate.maScrollPages.collectAsState()
    var scrollMenu by remember { mutableStateOf(false) }

    // The fill mark, and a clock that runs only while one is live.
    //
    // A minute is too long to leave to recomposition — nothing else would redraw the row in that
    // time, so the tick would sit there until something unrelated happened to wake the keyboard.
    // This ticks once a second while the mark is young and then stops, so the tick clears itself and
    // costs nothing for the rest of the day.
    val lastFill = MaClipCapture.lastFilled.value
    var markNow by remember { mutableStateOf(0L) }
    LaunchedEffect(lastFill) {
        while (android.os.SystemClock.elapsedRealtime() - lastFill.second < MaClipCapture.FILL_MARK_MS) {
            markNow = android.os.SystemClock.elapsedRealtime()
            delay(1000L)
        }
        markNow = android.os.SystemClock.elapsedRealtime()
    }

    // What the wand is doing, shown in a bar above the keys.
    //
    // Observed rather than read once when the keyboard is drawn. He presses the button in another
    // app while the keyboard is still up, so nothing of ours is redrawn — which is exactly why the
    // wand appeared to do nothing at all. The bar has to be told, not asked.
    val magicRowShown by prefs.dictate.maMagicRowShown.collectAsState()
    val learn by DictateAccessibilityService.learnState.collectAsState()

    val magicAll = remember(magicRaw) {
        MaMagicTargets.parse(magicRaw).ifEmpty { MaMagicTargets.defaults() }
    }
    val magicTargets = remember(magicRaw) {
        // Ticked terms only, in the order the user dragged them into: the wand presses the first it
        // finds, so the order is the user's answer to "which did you mean" on a screen with two.
        MaMagicTargets.activeTerms(MaMagicTargets.parse(magicRaw))
            .ifEmpty { MaMagicTargets.activeTerms(MaMagicTargets.defaults()) }
    }
    // Changing the default is not enough on its own. Anyone whose preferences already hold an
    // explicit false — written by the old default, or by the FlorisBoard screen that still offers
    // the switch — would keep an empty history and ten dead keys, and would have no way to guess
    // that a clipboard setting three screens away is why. So when a C key is actually on a row,
    // recording is switched on. The keys cannot work without it and nothing else here reads it.
    val clipKeysPresent = remember(storedRows) {
        storedRows.any { row -> row.entries.any { it.button is MaRows.Button.Clip } }
    }
    LaunchedEffect(clipKeysPresent) {
        if (clipKeysPresent && !prefs.clipboard.historyEnabled.get()) {
            prefs.clipboard.historyEnabled.set(true)
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
      // The wand's own bar, above the keys, in the place the recording bar uses.
      //
      // Everything this app does to somebody else's screen reports itself here. Waiting for a
      // button, and then asking before storing what it caught: he presses the thing, and the
      // keyboard asks whether that was the one. Before this the wand did its work in silence, which
      // is indistinguishable from doing nothing.
      if (drawChrome) MaWandBar(
          state = learn,
          onCopyDump = {
              // Onto the clipboard, which is where it has to go: it is far too long to read on a
              // phone and its whole purpose is to be pasted somewhere else.
              val dump = DictateAccessibilityService.dumpScreen()
              clipboardManager.addNewPlaintext(dump)
              Toast.makeText(
                  context,
                  context.getString(R.string.ma__wand_copied),
                  Toast.LENGTH_SHORT,
              ).show()
          },
          onDismiss = { MaScreenTargets.Learn.cancel() },
          onEdit = {
              // Straight to the wand's own screen, not to wherever the settings were last left.
              // The bookmark is right when he opens the settings to do something unrelated; here he
              // is already looking at the wand and asking for its list.
              MaScreenTargets.Learn.cancel()
              FlorisImeService.launchSettings("settings/dictate/magic")
          },
      )
      // The magic row: the wand, then one key per term he has taught it.
      //
      // Its own row rather than keys in the editor, because its contents are not a fixed set: it is
      // one button per term and the terms change as he learns them. A row that rewrites itself does
      // not belong in an editor where every other row is arranged by hand.
      //
      // Drawn above the feature rows, so it sits furthest from the typing. It is pressed
      // deliberately — send this, generate that — rather than reached for mid-sentence the way
      // backspace is.
      if (drawChrome && magicRowShown) {
        val magicKeys = remember(magicRaw) {
          MaMagicTargets.parse(magicRaw).filter { it.enabled }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(rowHeight)
                .horizontalScroll(rememberScrollState()),
            verticalAlignment = Alignment.CenterVertically,
        ) {
          // The wand itself, first and always. Long press arms the screen dump, which is how a new
          // term gets its name — so the way to grow this row is on the row.
          // The circled wand, not the loose one. Rewording draws the loose wand for the feature
          // that fixes grammar with a model; two features sharing a picture means the picture has
          // stopped saying which one you are looking at, and these two are worth telling apart —
          // one presses a button on screen, the other sends text away and costs money.
          ThemedKey(
              code = KeyCode.NOOP,
              modifier = Modifier.width(56.dp).fillMaxHeight().padding(2.dp),
              onLongClick = {
                  if (!DictateAccessibilityService.isRunning) {
                      maOpenAccessibilitySettings(context)
                  } else {
                      DictateAccessibilityService.armLearn()
                  }
              },
              onClick = {
                  if (!DictateAccessibilityService.isRunning) {
                      maOpenAccessibilitySettings(context)
                  } else {
                      FlorisImeService.launchSettings("settings/dictate/magic")
                  }
              },
          ) { fg ->
              // The same finger the settings list uses, in the row's own colour.
              //
              // Two pictures for one feature is the fault this was meant to fix, and drawing a
              // circled wand here while the settings drew a finger simply moved the mismatch rather
              // than removing it. One mark, everywhere.
              //
              // No colour of its own either. Sand said "this leaves the keyboard", which this key
              // no longer does — it dumps the screen — so the colour was describing something that
              // had stopped being true.
              Icon(
                  imageVector = Icons.Default.TouchApp,
                  contentDescription = stringRes(R.string.ma__magic_button),
                  tint = fg,
                  modifier = Modifier.size(22.dp),
              )
          }
          magicKeys.forEach { target ->
            if (target.isSpacer) {
              // Room, and nothing else — the same idea as the feature row spacer and deliberately
              // the same setting, so one number moves both.
              //
              // A fixed width here rather than a weight, because this row scrolls sideways. A
              // weight inside a scrolling row has no finite width to take its share of, and the
              // feature row can use one only because it does not scroll. Ten tenths is one wand
              // key, so the number still means what it says on the other screen.
              // Its own width, stored on the spacer itself.
              //
              // The label field is unused on a spacer — there is nothing to write on a gap — so the
              // width lives there and costs no change to how targets are stored or parsed. A blank
              // one falls back to the shared setting, which is what every spacer made before this
              // existed will have.
              //
              // Ten tenths is one wand key, the same unit the Feature row screen uses, so a number
              // means the same thing on both screens.
              val tenths = target.label.trim().toIntOrNull()?.coerceIn(1, 200) ?: spacerTenths
              Spacer(
                  modifier = Modifier
                      .width((56 * tenths / 10f).dp)
                      .fillMaxHeight(),
              )
            } else {
            // One key per term, carrying the term itself. Pressing it presses that button and no
            // other — which is the whole difference from the wand, which guesses from a list.
            ThemedTextKey(
                // The face, which is the label when he has given one and the term when he has
                // not. "Stop responding" is the right thing to search for and the wrong thing to
                // write on a key.
                label = target.face,
                modifier = Modifier.fillMaxHeight().padding(2.dp),
                tint = null,
            ) {
                if (!DictateAccessibilityService.isRunning) {
                    maOpenAccessibilitySettings(context)
                } else {
                    DictateAccessibilityService.pressScreenTarget(listOf(target.term))
                }
            }
            }
          }
        }
      }

      // The dashboard, above the rows and below whatever is reading.
      //
      // Shown only while something is being read: it is a set of dials for a thing in motion, and a
      // dashboard for silence would just be a settings screen in the wrong place. Closing itself
      // when the reading stops also means he can never be left with a panel he has to dismiss.
      if (drawChrome && maDashboardOpen && MaReader.currentIndex >= 0) {
        MaReaderDashboard(onClose = { maDashboardOpen = false })
      }

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
            // M1 to M10. What the button does lives in MaMacroSlots, so the row holds only which
            // slot it is: the same macro can then sit in two rows, edited once.
            //
            // A long press is free on these keys now that folding is gone, if a macro ever wants
            // one for something of its own.
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
                ) {
                    if (!slot.isEmpty) {
                        MaMacroSyntax.run(slot.macro, FlorisImeService.currentInputConnection())
                    }
                }
            }

            // C1 to C10. Always paste-replace: select all, delete, paste.
            is MaRows.Button.Clip -> {
                // The slot's own text, not the history's newest-first ordering. C4 is the fourth
                // thing copied since the row was cleared and stays that until it is cleared again.
                // No switch in front of it any more. A bucket on the row is a live bucket: what it
                // holds is what it pastes, and the only reason it is empty is that nothing has been
                // copied into it yet.
                val text = MaClipCapture.at(capturedSlots, button.slot)
                ThemedKey(
                    code = KeyCode.NOOP,
                    // The key shows only its number: ten text previews across a row would be a few
                    // characters wide each and unreadable. What it holds is spoken instead, which is
                    // the only way to answer "what is on C4" without pasting it somewhere to find out.
                    modifier = keyMod.semantics {
                        contentDescription = MaClipCapture.describeSlot(text, button.slot)
                    },
                    onClick = {
                        if (text != null) {
                            scope.launch {
                                // Always replace. Never insert.
                                //
                                // This was behind a preference, and the screen that could switch
                                // that preference back on was deleted three builds ago — so an
                                // install holding a stored false had these keys inserting into
                                // whatever was already in the field, with no way to correct it.
                                // That is not a setting anybody wants wrong: pasting a bucket into
                                // the middle of existing text is how a document gets damaged rather
                                // than edited. The condition is gone, not defaulted.
                                // Each wait is its own setting. They are three different waits and
                                // the field that is too slow for one is often fine with the others,
                                // so one number for all three could only ever be wrong somewhere.
                                keyboardManager.activeState.isManualSelectionMode = false
                                if (clipDelaySelect > 0) delay(clipDelaySelect.toLong())
                                FlorisImeService.currentInputConnection()
                                    ?.performContextMenuAction(android.R.id.selectAll)
                                if (clipDelayDelete > 0) delay(clipDelayDelete.toLong())
                                FlorisImeService.currentInputConnection()?.commitText("", 1)
                                if (clipDelayPaste > 0) delay(clipDelayPaste.toLong())
                                FlorisImeService.currentInputConnection()?.commitText(text, 1)
                                // The bucket is poured out once its contents are in the field.
                                // After the paste, not before: if the commit fails the text is
                                // still in the bucket to try again, whereas emptying first would
                                // lose it with nothing to show for it.
                                prefs.dictate.maClipCaptured.set(
                                    MaClipCapture.serialize(
                                        MaClipCapture.pour(capturedSlots, button.slot),
                                    ),
                                )
                            }
                        }
                    },
                    // Long press opens the clipboard history, which is
                    // what every other key here does on a long press. The exception is deliberate:
                    // the key shows a number and nothing else, so the only way to find out what is
                    // on C4 before pasting it is to look, and the place to look is one gesture away
                    // from the key itself. Folding is still reachable from any other key on the row.
                    onLongClick = {
                        keyboardManager.activeState.imeUiMode = ImeUiMode.CLIPBOARD
                    },
                ) { fg ->
                  // The tick, for a minute after this bucket caught a copy.
                  //
                  // A shape, not a colour change: colour is the state channel and it is already
                  // saying whether the bucket is holding something. This says something different —
                  // *this* is the one that just took the last copy — and it is the answer he was
                  // trying to read off another app's checkmark before it vanished.
                  //
                  // Over the key rather than beside the number, because the number is the thing
                  // pressed from memory and must not move to make room for a mark that comes and
                  // goes. Nothing in the layout shifts.
                  val justFilled = button.slot == lastFill.first &&
                      markNow - lastFill.second < MaClipCapture.FILL_MARK_MS
                  Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "C${button.slot}",
                        // Three states, read at a glance without pressing anything: dim means the
                        // bucket is empty and normal means it is holding something. Two states, and
                        // that is all there is to say.
                        //
                        // There was a third: red, for every bucket full. Full is not a fault — it is
                        // the ordinary end of filling them — and red on a keyboard reads as
                        // something being wrong, so it sent him looking for a problem that did not
                        // exist every time he used the feature as intended. The bin key empties them
                        // and they fill again.
                        color = if (text == null) fg.copy(alpha = 0.4f) else fg,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    if (justFilled) {
                        Text(
                            text = "\u2713",
                            color = onGreen,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.align(Alignment.TopEnd),
                        )
                    }
                  }
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
                    // The way into the transcribe view, not a record button.
                    //
                    // It was the recording control because the transcribe view had been deleted and
                    // this key was the only thing left in that position. The view is back, and
                    // recording already has two ways in that nothing can take away: volume up, and
                    // the mic on the recording bar itself. A third one on an editable row bought
                    // nothing and cost the only route to the view.
                    //
                    // Red while recording is gone with it. This key no longer starts or stops
                    // anything, so a colour saying "recording" would be describing something the
                    // key is not doing.
                    ThemedIconKey(
                        code = KeyCode.NOOP,
                        icon = Icons.Default.Mic,
                        contentDescription = stringRes(R.string.ma__feature_transcribe_view),
                        modifier = keyMod,
                    ) {
                        keyboardManager.activeState.imeUiMode = ImeUiMode.TRANSCRIBE
                    }
                }

                MaFeatureKey.CLIP_CLEAR -> {
                    // Empties C1 to C10 so they fill again from the next copy.
                    //
                    // The slots stopped moving, so a full row is a finished row: capturing stops and
                    // nothing changes until this is pressed. Without it the feature would work once
                    // per install.
                    //
                    // The clipboard history is deliberately left alone. This key resets the ten
                    // keys, and wiping somebody's whole clipboard as a side effect of tidying a row
                    // is not recoverable — the history is still there behind a long press on any C
                    // key, which is where anything wanted back can be found.
                    val filled = MaClipCapture.filledCount(capturedSlots, visibleClipSlots)
                    ThemedIconKey(
                        code = KeyCode.NOOP,
                        icon = Icons.Default.Delete,
                        contentDescription = stringRes(
                            if (filled == 0) R.string.ma__clip_clear_empty else R.string.ma__clip_clear,
                        ),
                        modifier = keyMod,
                        // Dim when there is nothing to clear, ordinary otherwise. It used to turn
                        // red alongside the buckets, on the reasoning that the row should show both
                        // the problem and its answer — but full is not a problem, so there was
                        // nothing to answer.
                        tint = if (filled == 0) MaDimmed else null,
                        ) {
                        // Emptying the buckets restarts the automatic bucket too.
                        //
                        // They are one mechanism: A-bucket presses copy buttons and the capture
                        // files each copy into the next free slot. Clearing the slots without
                        // clearing the rank left the two halves disagreeing — the buckets ready for
                        // the newest code block, the ladder still pointing eight blocks up the page
                        // — so the next press collected something from far above and dropped it in
                        // bucket one. The bin is the reset for both, which is what he expects and
                        // saves a second control nobody would remember.
                        maBucketRank = 0
                        scope.launch { prefs.dictate.maClipCaptured.set("") }
                    }
                }

                MaFeatureKey.SHIFT -> {
                    // The real shift, not a private flag: it writes the same inputShiftState the
                    // letter shift writes, so capitals still come out capital and one press still
                    // means one letter. Its whole reason to exist is that the letter shift is
                    // unreachable with the keys folded away, which is the state this row is for.
                    val shiftState = keyboardManager.activeState.inputShiftState
                    ThemedIconKey(
                        code = KeyCode.NOOP,
                        icon = Icons.Default.KeyboardCapslock,
                        contentDescription = "Shift",
                        modifier = keyMod,
                        // Lit while it is holding, so a modifier that outlives the press says
                        // so rather than leaving him to remember whether he armed it.
                        tint = if (shiftState != InputShiftState.UNSHIFTED) MaSand else null,
                    ) {
                        keyboardManager.activeState.inputShiftState =
                            when (keyboardManager.activeState.inputShiftState) {
                                // Three states in a ring, the same three the letter shift uses:
                                // off, once, locked. Locked matters here more than on the letters,
                                // because backwards through a long form is several presses of TAB
                                // and re-arming shift before each one would be the worse key.
                                InputShiftState.UNSHIFTED -> InputShiftState.SHIFTED_MANUAL
                                InputShiftState.SHIFTED_MANUAL -> InputShiftState.CAPS_LOCK
                                else -> InputShiftState.UNSHIFTED
                            }
                    }
                }

                // Drawn by its module. The row now says which key goes here and nothing about
                // what the key is — which is the whole point of the move.
                MaFeatureKey.PIN -> MaPinKey(modifier = keyMod, litColor = MaSand)

                // Both drawn by their modules. The row names the key and says nothing about what
                // the key is, which is the whole point of moving them out.
                // The four clipboard keys, now ordinary feature keys.
                //
                // Each goes through tapKey, the very path the Smartbar action used, so a paste from
                // this row is the same paste — the key moved, its behaviour did not.
                MaFeatureKey.PASTE -> ThemedIconKey(
                    code = KeyCode.CLIPBOARD_PASTE,
                    icon = Icons.Default.ContentPaste,
                    contentDescription = "Paste",
                    modifier = keyMod,
                    tint = null,
                ) { keyboardManager.tapKey(KeyCode.CLIPBOARD_PASTE) }

                MaFeatureKey.CUT -> ThemedIconKey(
                    code = KeyCode.CLIPBOARD_CUT,
                    icon = Icons.Default.ContentCut,
                    contentDescription = "Cut",
                    modifier = keyMod,
                    tint = null,
                ) { keyboardManager.tapKey(KeyCode.CLIPBOARD_CUT) }

                MaFeatureKey.COPY -> ThemedIconKey(
                    code = KeyCode.CLIPBOARD_COPY,
                    icon = Icons.Default.ContentCopy,
                    contentDescription = "Copy",
                    modifier = keyMod,
                    tint = null,
                ) { keyboardManager.tapKey(KeyCode.CLIPBOARD_COPY) }

                MaFeatureKey.CLIP_HISTORY -> ThemedIconKey(
                    code = KeyCode.NOOP,
                    icon = Icons.Default.ContentPasteGo,
                    contentDescription = "Clipboard history",
                    modifier = keyMod,
                    tint = null,
                ) { keyboardManager.activeState.imeUiMode = ImeUiMode.CLIPBOARD }

                MaFeatureKey.DUMP -> ThemedIconKey(
                    code = KeyCode.NOOP,
                    // A stack of layers: what this reads is the layer under the picture, which is
                    // exactly what the tree is. Not a bug icon — nothing here is broken, and an
                    // instrument should not look like an alarm.
                    icon = Icons.Default.Layers,
                    contentDescription = "Copy the screen's accessibility tree",
                    modifier = keyMod,
                    tint = null,
                ) {
                    // Straight to the clipboard, because it is far too long to read on a phone and
                    // its whole purpose is to be pasted somewhere else. The same path the wand's
                    // copy button uses, so the two cannot produce different dumps.
                    if (DictateAccessibilityService.isRunning) {
                        val dump = DictateAccessibilityService.dumpScreen()
                        clipboardManager.addNewPlaintext(dump)
                        Toast.makeText(
                            context,
                            "Screen copied \u2014 ${dump.length} characters",
                            Toast.LENGTH_SHORT,
                        ).show()
                    } else {
                        Toast.makeText(
                            context,
                            "Turn on the accessibility service to read the screen",
                            Toast.LENGTH_SHORT,
                        ).show()
                    }
                }

                MaFeatureKey.SUBTITLE -> MaSubtitleToggleKey(modifier = keyMod, litColor = MaSand)

                MaFeatureKey.READER -> MaReaderKey(
                    modifier = keyMod,
                    context = context,
                    litColor = MaSand,
                    // Long press opens the dashboard rather than the settings app.
                    //
                    // It used to leave the keyboard for the Reader screen, which meant walking away
                    // from the thing being adjusted. The three dials he reaches for while listening
                    // are one press away now and the reading keeps going underneath. Everything else
                    // about the reader still lives in settings, reachable from there.
                    onOpenSettings = { maDashboardOpen = !maDashboardOpen },
                    onMessage = { message ->
                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                    },
                )

                MaFeatureKey.AUTO_BUCKET -> {
                    // The face carries the count, because the count is the only thing about this
                    // key that cannot be guessed by looking at the screen. "A1" means the next
                    // press takes the last code block; "A3" means it takes the third one up.
                    // Without it he would be pressing blind and, on a mistake, would have no way to
                    // know he was collecting from last month.
                    ThemedTextKey(
                        label = "A${maBucketRank + 1}",
                        modifier = keyMod,
                        tint = null,
                        onLongClick = {
                            maBucketRank = 0
                            Toast.makeText(context, "Back to the last code block", Toast.LENGTH_SHORT).show()
                        },
                    ) {
                        if (!DictateAccessibilityService.isRunning) {
                            maOpenAccessibilitySettings(context)
                        } else {
                            val hit = DictateAccessibilityService.pressScreenTargetAt(
                                listOf("copy code"),
                                maBucketRank,
                            )
                            if (hit != null) {
                                // Only advance on a press that landed. A rank that ran past the end
                                // would otherwise keep climbing while nothing happened, and the
                                // number on the key would stop meaning anything.
                                maBucketRank++
                                // Then bring the NEXT block up into view.
                                //
                                // Two things at once, and they are the same action. The list
                                // scrolls to show the block he is about to collect, which pushes
                                // the one he just collected down to the bottom of the screen — so
                                // the last thing copied is the last thing visible, and he can read
                                // down the page to check the buckets instead of counting.
                                //
                                // And it is what makes a long chat reachable. Rows far from the
                                // viewport do not exist until something scrolls near them, so
                                // revealing the next block is what causes the ones above it to be
                                // built. Without this the ladder stops at whatever happened to be
                                // in memory when he started.
                                DictateAccessibilityService.revealScreenTargetAt(
                                    listOf("copy code"),
                                    maBucketRank,
                                )
                            } else {
                                Toast.makeText(
                                    context,
                                    "No more code blocks — hold to start again",
                                    Toast.LENGTH_SHORT,
                                ).show()
                            }
                        }
                    }
                }

                MaFeatureKey.CHANGE_CASE -> MaCaseKey(modifier = keyMod, context = context)

                // Shift stays here, at the call site, not inside the key.
                //
                // Reading the keyboard's shift state is this row's business — it is the row that
                // sits under a keyboard. The key itself only needs to know which way to walk, so it
                // is told, and the same module works on a surface that has no shift at all.
                MaFeatureKey.NEXT_FIELD -> MaNextFieldKey(
                    modifier = keyMod,
                    context = context,
                    backwards = {
                        val shift = keyboardManager.activeState.inputShiftState
                        val backwards = shift != InputShiftState.UNSHIFTED
                        if (shift == InputShiftState.SHIFTED_MANUAL) {
                            keyboardManager.activeState.inputShiftState = InputShiftState.UNSHIFTED
                        }
                        backwards
                    },
                )

                MaFeatureKey.APP_SWITCH -> {
                    // Alt+Tab. Dim when nothing has been seen to switch to yet, which is the state
                    // right after the phone starts or the accessibility service is switched off —
                    // both cases where the key would do nothing and should say so first.
                    ThemedIconKey(
                        code = KeyCode.NOOP,
                        icon = Icons.Default.SwapHoriz,
                        contentDescription = stringRes(R.string.ma__app_switch),
                        modifier = keyMod,
                        // No longer dimmed on what this app has observed. Recents knows the task
                        // order whether or not we saw it, so a dim key would understate what the key
                        // can actually do.
                        tint = null,
                        ) {
                        if (!DictateAccessibilityService.isRunning) {
                            // The whole reason this key fires blanks, and the user cannot guess it.
                            // Both these keys read the screen through the accessibility service,
                            // which is switched on by hand in the system settings and is off until
                            // somebody does. Opening that screen is more use than a message saying
                            // something did not work.
                            maOpenAccessibilitySettings(context)
                        } else if (!MaAppSwitcher.switchViaRecents(scope) &&
                            !MaAppSwitcher.switchToPrevious(context)
                        ) {
                            Toast.makeText(
                                context,
                                context.getString(R.string.ma__app_switch_none),
                                Toast.LENGTH_SHORT,
                            ).show()
                        }
                    }
                }

                MaFeatureKey.HISTORY -> {
                    // Everything dictated, ready to put back. The recovery route when a sentence
                    // went into the wrong app or was replaced by the next one.
                    ThemedIconKey(
                        code = KeyCode.NOOP,
                        icon = Icons.Default.History,
                        contentDescription = stringRes(R.string.ma__feature_history),
                        modifier = keyMod,
                        ) {
                        keyboardManager.activeState.imeUiMode = ImeUiMode.HISTORY
                    }
                }

                MaFeatureKey.LANGUAGE -> {
                    // The language it will transcribe in, on its face, and one tap changes it.
                    //
                    // The same MaLanguage.toggle the recording bar uses, so the two cannot disagree.
                    // It writes activeInputLanguage, which is what the transcription request reads,
                    // what the keyboard's own suggestions follow, and what decides whether a clip
                    // may take the fast path — one tap, all three.
                    // Derived from the observed preference, not from MaLanguage.badge() alone.
                    // badge() reads the store directly, which is not Compose state, so a key drawn
                    // from it would keep its old face after a tap until something else redrew the
                    // row — a toggle that appears not to have worked.
                    ThemedTextKey(
                        // EN, not ENG. Two letters, because this key is a circle.
                        //
                        // Every other key in this row holds one glyph; three letters do not fit
                        // inside a round face and the last one was being cut off at the edge. HR is
                        // already two, so the pair is even as well — and a badge whose width changes
                        // with the language would shift the keys beside it every time he switched.
                        //
                        // The history panel keeps ENG, where the row is wide and the extra letter
                        // costs nothing.
                        label = remember(activeLangCode, langMode) {
                            if (MaLanguage.active() == MaLanguage.EN) "EN" else "HR"
                        },
                        // ENG and HR is a switcher: it changes which language is heard, and types
                        // nothing.
                        switcher = true,
                        modifier = keyMod,
                        tint = null,
                    ) {
                        MaLanguage.cycleMode(context)
                    }
                }

                MaFeatureKey.SCROLL -> {
                    // S, and the number of pages beside it, so the key says what it will do before
                    // it is pressed rather than after. An up arrow appears when the count is
                    // negative: the sign is the direction, and a minus sign alone is easy to misread
                    // at this size.
                    val label = when {
                        scrollPages < 0 -> "S\u2191${-scrollPages}"
                        scrollPages > 1 -> "S\u2193$scrollPages"
                        else -> "S"
                    }
                    ThemedTextKey(
                        label = label,
                        modifier = keyMod,
                        tint = null,
                        onLongClick = { scrollMenu = true },
                    ) {
                        if (!DictateAccessibilityService.isRunning) {
                            maOpenAccessibilitySettings(context)
                        } else {
                            scope.launch {
                                val service = DictateAccessibilityService.gestureService()
                                if (service == null || !MaScreenTargets.scrollBy(service, scrollPages)) {
                                    Toast.makeText(
                                        context,
                                        context.getString(R.string.ma__scroll_none),
                                        Toast.LENGTH_SHORT,
                                    ).show()
                                }
                            }
                        }
                    }
                }

                MaFeatureKey.SWITCHBOARD -> {
                    // A switch, because that is what the screen is: a page of them.
                    ThemedIconKey(
                        code = KeyCode.NOOP,
                        icon = Icons.Default.ToggleOn,
                        contentDescription = stringRes(R.string.ma__feature_switchboard),
                        modifier = keyMod,
                    ) {
                        FlorisImeService.launchSettings("settings/dictate/switchboard")
                    }
                }

                MaFeatureKey.SPACER -> {
                    // Room, and nothing else. No background, no ripple, no click target — a spacer
                    // that could be pressed would be a key that appears broken, and one that drew a
                    // surface would be a key that appears blank.
                    // Weight, not a fixed width, because every key in this row is weighted — the
                    // row divides whatever width the phone has. A spacer measured in dp would be
                    // one size on his phone and another on a wider one, and would not stay in
                    // proportion when he adds a key beside it.
                    //
                    // Ten tenths is exactly one key, so the number means what it says.
                    Spacer(
                        modifier = Modifier
                            .weight(spacerTenths / 10f)
                            .fillMaxHeight(),
                    )
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
                        ) {
                        MaSettingsResume.open(context)
                    }
                }

                MaFeatureKey.ZONE_1 -> {
                    // 1, the number row. Digits, or whichever set the row is showing.
                    ThemedKey(
                        code = KeyCode.NOOP,
                        // A real icon, at the size and weight of every other icon in the row.
                        //
                        // These were a hand-drawn keyboard outline with a letter inside, and he was
                        // right that they looked like nothing else on the board — the neighbours are
                        // drawn glyphs from one set, and a shape drawn by hand beside them reads as
                        // an amateur patch however carefully it is proportioned. Colour still says
                        // whether the zone is open, the way it does everywhere in this app.
                        modifier = keyMod,
                        // A switcher: it changes what the keyboard shows rather than typing.
                        switcher = true,
                        onClick = { scope.launch { prefs.dictate.maExtraRow.set(!zone1) } },
                    ) { fg ->
                        Icon(
                            imageVector = Icons.Default.Numbers,
                            contentDescription = null,
                            tint = if (zone1) onGreen else fg,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                }

                MaFeatureKey.ZONE_2 -> {
                    // 2, the keyboard itself, all of it at once. This is the one that gives back real estate,
                    // and on a keyboard driven by voice it is off more often than it is on.
                    ThemedKey(
                        code = KeyCode.NOOP,
                        // A real icon, at the size and weight of every other icon in the row.
                        //
                        // These were a hand-drawn keyboard outline with a letter inside, and he was
                        // right that they looked like nothing else on the board — the neighbours are
                        // drawn glyphs from one set, and a shape drawn by hand beside them reads as
                        // an amateur patch however carefully it is proportioned. Colour still says
                        // whether the zone is open, the way it does everywhere in this app.
                        modifier = keyMod,
                        // A switcher: it changes what the keyboard shows rather than typing.
                        switcher = true,
                        onClick = { scope.launch { prefs.dictate.maZoneKeyboard.set(!zone2) } },
                    ) { fg ->
                        Icon(
                            imageVector = Icons.Default.Keyboard,
                            contentDescription = null,
                            tint = if (zone2) onGreen else fg,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                }

                MaFeatureKey.ZONE_3 -> {
                    // 3, the copy and paste row along the top. Paste, copy, history and the rest of it.
                    ThemedKey(
                        code = KeyCode.NOOP,
                        // A real icon, at the size and weight of every other icon in the row.
                        //
                        // These were a hand-drawn keyboard outline with a letter inside, and he was
                        // right that they looked like nothing else on the board — the neighbours are
                        // drawn glyphs from one set, and a shape drawn by hand beside them reads as
                        // an amateur patch however carefully it is proportioned. Colour still says
                        // whether the zone is open, the way it does everywhere in this app.
                        modifier = keyMod,
                        // A switcher: it changes what the keyboard shows rather than typing.
                        switcher = true,
                        onClick = { scope.launch { prefs.dictate.maEditRow.set(!zone3) } },
                    ) { fg ->
                        Icon(
                            imageVector = Icons.Default.ContentPaste,
                            contentDescription = null,
                            tint = if (zone3) onGreen else fg,
                            modifier = Modifier.size(24.dp),
                        )
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

      // The picker, over the row, listing what the last scan found.
      //
      // Drawn inside the keyboard's own window rather than as a system dialog: an input method
      // cannot put up a dialog, and it does not need to. The keyboard is already on screen and
      // already the thing the finger is on.
      // The stepper, set on the key while looking at the page it will scroll.
      if (scrollMenu) {
        MaScrollStepper(
          pages = scrollPages,
          onChange = { scope.launch { prefs.dictate.maScrollPages.set(it) } },
          onDismiss = { scrollMenu = false },
        )
      }

      // The scan picker was here. It is gone with the list it drew: nothing arms it any more,
      // because learning happens by pressing the real button instead of choosing from thirty
      // labels written for a screen reader.
    }
}

/**
 * How many pages the S key scrolls, set on the spot.
 *
 * Bounded at ten either way. Beyond that the page has usually stopped moving anyway — a list runs
 * out — and a number that can run to fifty is a number somebody sets by accident and then has to
 * count back down.
 *
 * Zero is allowed and means the key does nothing, which is the honest reading of a stepper that
 * passes through zero on its way from down to up. It is not a state anybody stays in.
 */
@Composable
private fun MaScrollStepper(
    pages: Int,
    onChange: (Int) -> kotlin.Unit,
    onDismiss: () -> kotlin.Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF16161A))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringRes(R.string.ma__scroll_pages),
            color = Color(0x99FFFFFF),
            fontSize = 13.sp,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = "\u2212",
            color = Color(0xFFE8B15C),
            fontSize = 22.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                // Steps over zero rather than through it. Zero pages is a key that does nothing,
                // which on a stepper reads as the key having broken rather than as a value chosen —
                // and it is passed through twice on the way from down to up.
                .clickable { onChange(maStepScroll(pages, -1)) }
                .padding(horizontal = 18.dp, vertical = 4.dp),
        )
        Text(
            text = if (pages < 0) "\u2191${-pages}" else "\u2193$pages",
            color = Color(0xFFECEAE3),
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = "+",
            color = Color(0xFFE8B15C),
            fontSize = 22.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .clickable { onChange(maStepScroll(pages, 1)) }
                .padding(horizontal = 18.dp, vertical = 4.dp),
        )
        Text(
            text = stringRes(R.string.ma__magic_scan_close),
            color = Color(0x99FFFFFF),
            fontSize = 13.sp,
            modifier = Modifier
                .clickable { onDismiss() }
                .padding(horizontal = 8.dp, vertical = 4.dp),
        )
    }
}

/**
 * A round key carrying a numeral, styled exactly as every other key in the row.
 *
 * `internal` rather than private because keys are becoming modules: a key that can only be drawn
 * inside this file is a key that can only live in this row, which is the thing being undone.
 * `ThemedIconKey` in LegacyDictateLayout has been internal all along, which is why the icon keys
 * were the easy half to move.
 */
@Composable
internal fun ThemedTextKey(
    label: String,
    modifier: Modifier,
    tint: Color?,
    /** A switcher rather than an action. See [ThemedIconKey]. */
    switcher: Boolean = false,
    // Optional and null by default. The fold gesture that every key once had is gone, so a long
    // press here means whatever the individual key decides — the scroll key uses it for its stepper
    // and most keys use it for nothing.
    onLongClick: (() -> kotlin.Unit)? = null,
    onClick: () -> kotlin.Unit,
) {
    ThemedKey(
        code = KeyCode.NOOP,
        modifier = modifier,
        switcher = switcher,
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
 * The pause between the steps of a clipboard replace, matching the copy row's own.
 *
 * Repeated rather than imported from LegacyDictateLayout because that file is being deleted with the
 * transcribe view, and a hundred milliseconds is cheaper to state twice than to route around.
 */


/** The wait before the paste itself. AP's value, for AP's reason. See the CH key. */
// 333ms, Marko's number. The sequence he specified is select all, 100ms, delete, 333ms, paste.

/** The grey a key wears when it is present but has nothing to act on. */
private val MaDimmed = Color(0xFF6B6B6B)

/**
 * Opens the system accessibility settings, where the service has to be switched on by hand.
 *
 * Android gives no way to turn it on from inside the app and no deep link to this app's own row in
 * that list, so this lands on the list and the user finds TTT mini in it. Still far better than a
 * message: the keys that need the service are useless until this is done, and nothing else in the
 * app says so at the moment it matters.
 */
internal fun maOpenAccessibilitySettings(context: android.content.Context) {
    runCatching {
        context.startActivity(
            android.content.Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS)
                .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }
    Toast.makeText(
        context,
        context.getString(R.string.ma__accessibility_needed),
        Toast.LENGTH_LONG,
    ).show()
}

/**
 * Moves the scroll count one step, skipping zero and stopping at ten either way.
 *
 * The sign is the direction, so the sequence runs ... -2, -1, 1, 2 ... and never rests on a value
 * that would make the key do nothing.
 */
private fun maStepScroll(pages: Int, delta: Int): Int {
    val next = pages + delta
    val stepped = if (next == 0) pages + delta * 2 else next
    return stepped.coerceIn(-10, 10)
}

/**
 * The wand's bar: copy the screen, or leave, or go and edit the list.
 *
 * ### Three controls, and nothing else
 *
 * It held a hint, a text field, Copy, Store, Cancel and a link, which is six things in a strip the
 * height of a key. The field was the worst of them: typing a button's name into a box the width of a
 * thumb, on a keyboard that is itself the thing being configured, when the same box exists on the
 * wand's own settings screen with room to read it. Removing it removes the reason for Store as well.
 *
 * What is left is the loop that actually works. Copy the tree, ask someone who can read it against a
 * picture of the screen, then add the name where terms are added.
 *
 * ### Yellow means this leaves the keyboard
 *
 * The gear is sand, and that is a rule rather than a colour: in this app sand marks a control that
 * takes you somewhere else, the way red marks recording and green marks a level that is safe. A
 * label saying "Wand" had to be read to know what it did; a gear in the colour of leaving does not.
 */
@Composable
private fun MaWandBar(
    state: MaScreenTargets.Learn.State,
    onCopyDump: () -> kotlin.Unit,
    onDismiss: () -> kotlin.Unit,
    onEdit: () -> kotlin.Unit,
) {
    if (state is MaScreenTargets.Learn.State.Idle) return
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF16161A))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringRes(R.string.ma__wand_dump_hint),
            color = Color(0x99FFFFFF),
            fontSize = 13.sp,
            maxLines = 2,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = stringRes(R.string.ma__wand_copy),
            color = MaSand,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .clickable { onCopyDump() }
                .padding(horizontal = 12.dp, vertical = 8.dp),
        )
        Text(
            text = stringRes(R.string.ma__wand_cancel),
            color = Color(0x99FFFFFF),
            fontSize = 15.sp,
            modifier = Modifier
                .clickable { onDismiss() }
                .padding(horizontal = 12.dp, vertical = 8.dp),
        )
        Icon(
            imageVector = Icons.Default.Settings,
            contentDescription = stringRes(R.string.ma__wand_settings),
            tint = MaSand,
            modifier = Modifier
                .clickable { onEdit() }
                .padding(horizontal = 8.dp, vertical = 8.dp)
                .size(22.dp),
        )
    }
}

/**
 * Sand: this control leaves the keyboard and takes you somewhere.
 *
 * One meaning, one colour, everywhere — the same discipline as the recording red. A person should be
 * able to learn it once from any screen and read it on every other.
 */
/**
 * How far up the page the automatic bucket has reached. 0 is the last code block.
 *
 * A Compose state at file level rather than `remember` inside the row, so it survives the keyboard
 * closing and reopening. Collecting code blocks means leaving the keyboard to look at what was
 * copied and coming back for the next one — a counter that reset on every close would make the key
 * useless for the one thing it is for.
 *
 * Not a stored preference either: it is about the screen in front of him now, and a rank restored
 * from last week would point somewhere meaningless. Reset by a long press, and by the process
 * ending, which are both moments when starting again is what he would want.
 */
private var maBucketRank by mutableStateOf(0)

/**
 * Whether the reader dashboard is showing.
 *
 * File-level rather than remembered inside the row, for the same reason `maBucketRank` is: the row
 * is recomposed constantly and rebuilt whenever the keyboard changes shape, and state remembered
 * inside it would close the dashboard every time he switched view while it was open.
 */
private var maDashboardOpen by mutableStateOf(false)

private val MaSand = Color(0xFFE8B15C)
