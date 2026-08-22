/*
 * Copyright (C) 2024-2025 The FlorisBoard Contributors
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

package dev.patrickgold.florisboard.ime.smartbar

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventTimeoutCancellationException
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.rememberCoroutineScope
import dev.patrickgold.florisboard.FlorisImeService
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.app.FlorisPreferenceStore
import dev.patrickgold.florisboard.ime.nlp.NlpManager
import dev.patrickgold.florisboard.ime.nlp.SuggestionCandidate
import kotlinx.coroutines.launch
import org.florisboard.lib.android.showShortToast
import dev.patrickgold.florisboard.ime.theme.FlorisImeUi
import dev.patrickgold.florisboard.keyboardManager
import dev.patrickgold.florisboard.nlpManager
import dev.patrickgold.florisboard.subtypeManager
import dev.patrickgold.jetpref.datastore.model.collectAsState
import org.florisboard.lib.compose.conditional
import org.florisboard.lib.compose.florisHorizontalScroll
import org.florisboard.lib.snygg.SnyggSelector
import org.florisboard.lib.snygg.ui.SnyggBox
import org.florisboard.lib.snygg.ui.SnyggColumn
import org.florisboard.lib.snygg.ui.SnyggIcon
import dev.patrickgold.florisboard.dictate.MaLanguage
import dev.patrickgold.florisboard.dictate.nlp.MaAiPredict
import dev.patrickgold.florisboard.dictate.nlp.MaNgram
import dev.patrickgold.florisboard.dictate.DictateController
import dev.patrickgold.florisboard.ime.nlp.WordSuggestionCandidate
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import dev.patrickgold.florisboard.editorInstance
import org.florisboard.lib.snygg.ui.SnyggRow
import org.florisboard.lib.snygg.ui.SnyggSpacer
import androidx.compose.ui.text.font.FontWeight
import org.florisboard.lib.snygg.ui.SnyggText

val CandidatesRowScrollbarHeight = 2.dp

@Composable
fun CandidatesRow(modifier: Modifier = Modifier) {
    val prefs by FlorisPreferenceStore
    val context = LocalContext.current
    val keyboardManager by context.keyboardManager()
    val nlpManager by context.nlpManager()
    val subtypeManager by context.subtypeManager()
    val editorInstance by context.editorInstance()

    val scope = rememberCoroutineScope()
    val displayMode by prefs.suggestion.displayMode.collectAsState()
    val ordinaryCandidates by nlpManager.activeCandidatesFlow.collectAsState()

    // The AI suggestions, when he has asked for some.
    //
    // They REPLACE the row rather than being appended to it. Appending would leave him choosing
    // between two kinds of guess with nothing on screen saying which was which, and the whole reason
    // he pressed the key is that the ordinary guesses were wrong.
    val aiWords = MaAiPredict.words.value
    val aiBusy = MaAiPredict.busy.value
    val candidates = remember(ordinaryCandidates, aiWords) {
        if (aiWords.isEmpty()) {
            ordinaryCandidates
        } else {
            aiWords.map { word ->
                WordSuggestionCandidate(
                    text = word,
                    secondaryText = null,
                    confidence = 0.95,
                    // Never auto-committed. An auto-commit is the keyboard deciding for him, and a
                    // guess he paid for and has not read yet is the last thing that should be
                    // inserted without a tap.
                    isEligibleForAutoCommit = false,
                    sourceProvider = null,
                )
            }
        }
    }
    // Read once per composition instead of a synchronous pref get() per candidate on every keystroke
    // (the candidates row recomposes on each character — issue: typing jank).
    val longPressDelay by prefs.keyboard.longPressDelay.collectAsState()

    SnyggRow(
        elementName = FlorisImeUi.SmartbarCandidatesRow.elementName,
        modifier = modifier
            .fillMaxSize()
            .conditional(displayMode == CandidatesDisplayMode.DYNAMIC_SCROLLABLE && candidates.size > 1) {
                florisHorizontalScroll(scrollbarHeight = CandidatesRowScrollbarHeight)
            },
        horizontalArrangement = if (candidates.size > 1) {
            Arrangement.Start
        } else {
            Arrangement.Center
        },
    ) {
        if (candidates.isNotEmpty()) {
            val candidateModifier = if (candidates.size == 1) {
                Modifier
                    .fillMaxHeight()
                    .weight(1f, fill = false)
            } else {
                Modifier
                    .fillMaxHeight()
                    .conditional(displayMode == CandidatesDisplayMode.CLASSIC) {
                        weight(1f)
                    }
                    .conditional(displayMode != CandidatesDisplayMode.CLASSIC) {
                        wrapContentWidth().widthIn(max = 160.dp)
                    }
            }
            val list = when (displayMode) {
                CandidatesDisplayMode.CLASSIC -> candidates.subList(0, 3.coerceAtMost(candidates.size))
                else -> candidates
            }.let { ordered ->
                // The likeliest word goes in the MIDDLE, not on the left.
                //
                // Reading order puts the best guess where the eye starts, which sounds right and is
                // not: the thumb rests under the centre of the row, so the word most often wanted
                // was the one furthest from the thumb, and every acceptance cost a reach. The
                // middle is the cheapest place on a phone, so the likeliest word belongs there.
                //
                // Only for three. With two there is no middle to speak of, and with four or more the
                // centre stops being a single obvious place — so the rule that would make it better
                // for three would make it arbitrary for the rest.
                if (ordered.size == 3) listOf(ordered[1], ordered[0], ordered[2]) else ordered
            }
            for ((n, candidate) in list.withIndex()) {
                if (n > 0) {
                    SnyggSpacer(
                        elementName = FlorisImeUi.SmartbarCandidateSpacer.elementName,
                        modifier = Modifier
                            .width(1.dp)
                            .fillMaxHeight(0.6f)
                            .align(Alignment.CenterVertically),
                    )
                }
                // The word under the finger, captured by value.
                //
                // This read `candidates[n]` — the ORIGINAL list at the DISPLAYED position. That was
                // harmless while the two were the same list, and became a wrong-word bug the moment
                // the middle-weighted order above made position 0 hold candidate 1. Tapping the
                // left word would have committed the middle one.
                //
                // The loop variable is already the right object, and in Kotlin it is a fresh binding
                // per iteration, so capturing it in the lambda is safe. The old comment saying
                // otherwise was the reason the index was there at all.
                val tapped = candidate
                CandidateItem(
                    modifier = candidateModifier,
                    candidate = candidate,
                    displayMode = displayMode,
                    // With three, the middle one is the best guess (§68). With any other count the
                    // list is in plain order, so the first is.
                    emphasised = n == (if (list.size == 3) 1 else 0),
                    onClick = {
                        // Taught to the local model BEFORE the commit, while the context that
                        // produced this suggestion is still the context on screen. After the commit
                        // the chosen word is itself part of the text before the cursor, and the
                        // lesson would contain its own answer.
                        //
                        // Only for a word he chose out of the AI's list. An ordinary candidate came
                        // from the local model or the dictionary, and teaching the model its own
                        // output back is how a prediction engine ends up certain of one word.
                        if (aiWords.isNotEmpty()) {
                            MaNgram.learn(
                                MaAiPredict.lesson(tapped.text.toString()),
                                keyboardManager.activeState.isIncognitoMode,
                            )
                        }
                        MaAiPredict.clear()
                        keyboardManager.commitCandidate(tapped)
                    },
                    onLongPress = {
                        val candidateItem = tapped
                        when {
                            // Words only now. The clipboard branch went with the clipboard
                            // suggestion itself; long press teaches the personal dictionary
                            // (issue #241), which is all this row carries.
                            else -> {
                                val subtype = subtypeManager.activeSubtype
                                val result = nlpManager.addToUserDictionary(subtype, candidateItem)
                                val message = when (result) {
                                    NlpManager.AddToDictionaryResult.ADDED ->
                                        R.string.suggestion__added_to_dictionary
                                    NlpManager.AddToDictionaryResult.ALREADY_PRESENT ->
                                        R.string.suggestion__already_in_dictionary
                                    NlpManager.AddToDictionaryResult.UNAVAILABLE -> null
                                }
                                if (message != null) {
                                    // Haptic as well as the toast: Android suppresses toasts entirely when
                                    // the user has turned notifications off for the app, and a silent
                                    // long-press would look broken.
                                    FlorisImeService.inputFeedbackController()?.keyLongPress()
                                    scope.launch {
                                        context.showShortToast(
                                            message,
                                            "word" to candidateItem.text.toString(),
                                        )
                                    }
                                }
                                result != NlpManager.AddToDictionaryResult.UNAVAILABLE
                            }
                        }
                    },
                    longPressDelay = longPressDelay.toLong(),
                )
            }
        }

        // THE AI KEY, AT THE END OF THE ROW.
        //
        // Always the last thing, whether or not there are candidates: a key that moved with the
        // number of guesses would be somewhere different every keystroke, and this row is used
        // without being read.
        //
        // Pressed once, it asks; pressed again while its words are showing, it puts the ordinary
        // guesses back. Nothing else clears it — typing does, on the next prediction, because the
        // words would no longer be about the word being typed.
        SnyggText(
            elementName = FlorisImeUi.SmartbarCandidateWord.elementName,
            text = if (aiBusy) "\u2026" else "AI",
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .fillMaxHeight()
                .wrapContentWidth()
                .padding(horizontal = 12.dp)
                .clickable(enabled = !aiBusy) {
                    if (aiWords.isNotEmpty()) {
                        MaAiPredict.clear()
                        return@clickable
                    }
                    val content = editorInstance.activeContent
                    val before = content.textBeforeSelection.toString()
                    val current = content.currentWordText.toString()
                    MaAiPredict.remember(before)
                    MaAiPredict.busy.value = true
                    scope.launch {
                        val reply = DictateController.askCheapModel(
                            MaAiPredict.prompt(
                                before = before,
                                current = current,
                                // The badge, not the subtype. The two are linked today — the EN/HR
                                // key moves the subtype as well — but the badge is the control he
                                // presses and the one the personal model is now split by, so it is
                                // the single source the whole prediction stack reads.
                                language = if (MaLanguage.active() == MaLanguage.HR) "Croatian" else "English",
                            ),
                        )
                        MaAiPredict.busy.value = false
                        // Nothing back, or nothing usable, leaves the row exactly as it was. There
                        // is no failure worth a message here: he can see the words did not change,
                        // and the guesses he already had are still under his thumb.
                        MaAiPredict.words.value = reply
                            ?.let { MaAiPredict.readWords(it, current) }
                            .orEmpty()
                    }
                },
        )
    }
}

@Composable
private fun CandidateItem(
    candidate: SuggestionCandidate,
    displayMode: CandidatesDisplayMode,
    modifier: Modifier = Modifier,
    /** Drawn larger. True for the likeliest word, which §68 moved to the middle. */
    emphasised: Boolean = false,
    onClick: () -> Unit = { },
    onLongPress: () -> Boolean = { false },
    longPressDelay: Long,
) = with(LocalDensity.current) {
    var isPressed by remember { mutableStateOf(false) }

    val elementName = FlorisImeUi.SmartbarCandidateWord.elementName
    // Remembered so recomposing the row on each keystroke doesn't allocate a fresh map (which, as an
    // unstable arg to the Snygg composables below, would also defeat their skipping) — reduces the
    // per-keystroke recomposition + GC churn behind the typing jank.
    val autoCommit = candidate.isEligibleForAutoCommit
    val attributes = remember(autoCommit) { mapOf("auto-commit" to if (autoCommit) 1 else 0) }
    val selector = if (isPressed) SnyggSelector.PRESSED else SnyggSelector.NONE

    SnyggRow(
        elementName = elementName,
        attributes = attributes,
        selector = selector,
        modifier = modifier
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    isPressed = true
                    if (down.pressed != down.previousPressed) down.consume()
                    var upOrCancel: PointerInputChange? = null
                    try {
                        upOrCancel = withTimeout(longPressDelay) {
                            waitForUpOrCancellation()
                        }
                        upOrCancel?.let { if (it.pressed != it.previousPressed) it.consume() }
                    } catch (_: PointerEventTimeoutCancellationException) {
                        if (onLongPress()) {
                            upOrCancel = null
                            isPressed = false
                        }
                        waitForUpOrCancellation()?.let { if (it.pressed != it.previousPressed) it.consume() }
                    }
                    if (upOrCancel != null) {
                        onClick()
                    }
                    isPressed = false
                }
            },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (candidate.icon != null) {
            SnyggBox(
                elementName = "$elementName-icon",
                attributes = attributes,
                selector = selector,
            ) {
                SnyggIcon(imageVector = candidate.icon!!)
            }
        }
        SnyggColumn(
            modifier = if (displayMode == CandidatesDisplayMode.CLASSIC) Modifier.weight(1f) else Modifier,
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            SnyggText(
                elementName = "$elementName-text",
                attributes = attributes,
                selector = selector,
                // Gboard-style: bold the suggestion that will be auto-applied (autocorrect), so it's clear
                // what will replace the typed word; other suggestions stay normal weight (issue #150).
                // Bold marks the likeliest word, which is the one in the MIDDLE.
                //
                // It used to follow isEligibleForAutoCommit, which is a property of the FIRST
                // candidate — so after §68 moved the best guess to the centre, the bold stayed on
                // the left and pointed at the wrong word. He spotted it from a screenshot.
                //
                // Emphasis is now one decision, made by position, so the large word and the bold
                // word are the same word and cannot drift apart again.
                fontWeight = if (emphasised) FontWeight.Bold else null,
                // The likeliest word, larger than its neighbours.
                //
                // A scale rather than a size: it multiplies whatever the theme decided, so a bigger
                // or smaller keyboard font stays bigger or smaller and this stays a fifth larger
                // than its neighbours either way. A hardcoded size here would look right on his
                // theme and wrong on every other.
                //
                // Modest on purpose. It has to be readable as "this is the one" at a glance without
                // making the row jump about as the ranking changes between keystrokes.
                fontSizeScale = if (emphasised) 1.2f else null,
                text = candidate.text.toString(),
            )
            if (candidate.secondaryText != null) {
                SnyggText(
                    elementName = "$elementName-secondary-text",
                    attributes = attributes,
                    selector = selector,
                    text = candidate.secondaryText!!.toString(),
                )
            }
        }
    }
}
