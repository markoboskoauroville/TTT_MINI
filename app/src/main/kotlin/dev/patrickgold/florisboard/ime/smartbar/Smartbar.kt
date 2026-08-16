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

package dev.patrickgold.florisboard.ime.smartbar

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.UnfoldLess
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.isUnspecified
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.app.FlorisPreferenceStore
import dev.patrickgold.florisboard.dictate.ui.MaBucketStrip
import dev.patrickgold.florisboard.dictate.ui.maBucketStripHasContent
import dev.patrickgold.florisboard.ime.keyboard.FlorisImeSizing
import dev.patrickgold.florisboard.ime.nlp.NlpInlineAutofill
import dev.patrickgold.florisboard.dictate.DictateController
import dev.patrickgold.florisboard.dictate.ui.DictateSmartbarUi
import dev.patrickgold.florisboard.editorInstance
import dev.patrickgold.florisboard.ime.smartbar.quickaction.QuickActionButton
import dev.patrickgold.florisboard.ime.smartbar.quickaction.QuickActionsRow
import dev.patrickgold.florisboard.ime.theme.FlorisImeUi
import dev.patrickgold.florisboard.keyboardManager
import dev.patrickgold.florisboard.nlpManager
import dev.patrickgold.florisboard.subtypeManager
import dev.patrickgold.florisboard.dictate.MaLanguage
import dev.patrickgold.jetpref.datastore.model.collectAsState
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.florisboard.lib.android.AndroidVersion
import org.florisboard.lib.compose.horizontalTween
import org.florisboard.lib.compose.verticalTween
import org.florisboard.lib.snygg.ui.SnyggBox
import org.florisboard.lib.snygg.ui.SnyggColumn
import org.florisboard.lib.snygg.ui.SnyggIcon
import org.florisboard.lib.snygg.ui.SnyggIconButton
import org.florisboard.lib.snygg.ui.SnyggRow
import org.florisboard.lib.snygg.ui.SnyggText
import org.florisboard.lib.snygg.ui.rememberSnyggThemeQuery

const val AnimationDuration = 200

val VerticalEnterTransition = EnterTransition.verticalTween(AnimationDuration)
val VerticalExitTransition = ExitTransition.verticalTween(AnimationDuration)

private val HorizontalEnterTransition = EnterTransition.horizontalTween(AnimationDuration)
private val HorizontalExitTransition = ExitTransition.horizontalTween(AnimationDuration)

private val NoEnterTransition = EnterTransition.horizontalTween(0)
private val NoExitTransition = ExitTransition.horizontalTween(0)

private val AnimationTween = tween<Float>(AnimationDuration)
private val NoAnimationTween = tween<Float>(0)

@Composable
fun Smartbar() {
    val prefs by FlorisPreferenceStore
    val context = LocalContext.current
    val smartbarEnabled by prefs.smartbar.enabled.collectAsState()
    val extendedActionsPlacement by prefs.smartbar.extendedActionsPlacement.collectAsState()

    AnimatedVisibility(
        visible = smartbarEnabled,
        enter = VerticalEnterTransition,
        exit = VerticalExitTransition,
    ) {
        Column {
            // MA TWIST: the strip exists only when it has something to say. Marko's ask, and the
            // reason is one glance at the screenshot: a full row of a phone screen, above a keyboard
            // that is already the bottom half of the display, holding a language badge and a stretch
            // of nothing.
            //
            // NOT COMPOSED rather than hidden, and with no animation. The height it reserves is
            // computed separately in FlorisImeSizing, from the same [maSmartbarHasContent], and the
            // two have to change in the same frame. A fade against an instant height change is the
            // build 138 fold bug in slow motion: the content goes and the space stays behind.
            //
            // The prompt row above is deliberately outside this. It has its own switch, its own
            // height in panelUiHeight, and it is a row of controls rather than a row of answers.
            val hasContent = maSmartbarHasContent()
            when (extendedActionsPlacement) {
                ExtendedActionsPlacement.ABOVE_CANDIDATES -> {
                    SnyggColumn(FlorisImeUi.Smartbar.elementName) {
                        SmartbarSecondaryRow()
                        if (hasContent) SmartbarMainRow()
                    }
                }

                ExtendedActionsPlacement.BELOW_CANDIDATES -> {
                    SnyggColumn(FlorisImeUi.Smartbar.elementName) {
                        if (hasContent) SmartbarMainRow()
                        SmartbarSecondaryRow()
                    }
                }

                ExtendedActionsPlacement.OVERLAY_APP_UI -> {
                    SnyggBox(FlorisImeUi.Smartbar.elementName,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(FlorisImeSizing.smartbarHeight),
                        allowClip = false,
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(FlorisImeSizing.smartbarHeight * 2)
                                .absoluteOffset(y = -FlorisImeSizing.smartbarHeight),
                            contentAlignment = Alignment.BottomStart,
                        ) {
                            SmartbarSecondaryRow()
                        }
                        if (hasContent) SmartbarMainRow()
                    }
                }
            }
        }
    }
}

@Composable
private fun SmartbarMainRow(modifier: Modifier = Modifier) {
    val prefs by FlorisPreferenceStore
    val context = LocalContext.current
    val keyboardManager by context.keyboardManager()
    val nlpManager by context.nlpManager()
    // Read here so both layouts below can ask the same question.
    //
    // Suggestions take the strip whenever there are any, and the bucket legend has it the rest of
    // the time. It used to be the other way round, and the effect was that anybody who actually
    // uses the buckets never saw a word suggestion again: a bucket keeps its text until it is
    // replaced, so the legend held the strip permanently and the row meant to appear the moment he
    // starts typing could not appear at all.
    //
    // Candidates exist only while a word is being typed, which is exactly when they are wanted and
    // exactly when nobody is reading the legend. The moment he stops, the legend returns on its own.
    val maCandidates by nlpManager.activeCandidatesFlow.collectAsState()
    val subtypeManager by context.subtypeManager()
    val scope = rememberCoroutineScope()

    val inlineSuggestions by NlpInlineAutofill.suggestions.collectAsState()
    LaunchedEffect(inlineSuggestions) {
        nlpManager.autoExpandCollapseSmartbarActions(null, inlineSuggestions)
    }
    val shouldShowInlineSuggestionsUi = AndroidVersion.ATLEAST_API30_R && inlineSuggestions.isNotEmpty()

    val smartbarLayout by prefs.smartbar.layout.collectAsState()
    val flipToggles by prefs.smartbar.flipToggles.collectAsState()
    val sharedActionsExpanded by prefs.smartbar.sharedActionsExpanded.collectAsState()
    val extendedActionsExpanded by prefs.smartbar.extendedActionsExpanded.collectAsState()

    val shouldAnimate by prefs.smartbar.sharedActionsExpandWithAnimation.collectAsState()

    // Drives the in-Smartbar dictation indicator (recording timer / transcribing spinner).
    val dictateState by DictateController.state.collectAsState()

    /**
     * The language, written like a suggestion.
     *
     * One badge for one language. It is the transcription language and the suggestion language at
     * once, because those are now the same thing (see [MaLanguage]); this is the indicator and the
     * switch in one, and a tap moves to the other language.
     *
     * Typed like a candidate rather than drawn as a button: same element name, so it takes the
     * theme's suggestion size, weight and colour. A button here was taller than the strip and its
     * label ran past its own edge.
     */
    @Composable
    fun LanguageToggle() {
        // Recomposes with the subtype so the badge follows a switch made anywhere else, including
        // the volume key and the transcribe view's own row.
        val activeSubtype by subtypeManager.activeSubtypeFlow.collectAsState()
        val activeCode by prefs.dictate.activeInputLanguage.collectAsState()
        val badge = remember(activeSubtype, activeCode) { MaLanguage.badge() }
        SnyggBox(
            elementName = FlorisImeUi.SmartbarCandidateWord.elementName,
            modifier = Modifier
                .fillMaxHeight()
                .clickable { MaLanguage.toggle(context) }
                .padding(horizontal = 10.dp),
            contentAlignment = Alignment.Center,
        ) {
            SnyggText(
                elementName = "${FlorisImeUi.SmartbarCandidateWord.elementName}-text",
                text = badge,
            )
        }
    }

    @Composable
    fun RowScope.CenterContent() {
        // No expanded state left. This strip shows the suggestions, or the recording interface in
        // the same place while dictating, and nothing else competes for it.
        val expanded = false
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
        ) {
            val enterTransition = if (shouldAnimate) HorizontalEnterTransition else NoEnterTransition
            val exitTransition = if (shouldAnimate) HorizontalExitTransition else NoExitTransition
            val isDictating = dictateState !is DictateController.UiState.Idle
            this@CenterContent.AnimatedVisibility(
                visible = !expanded && !isDictating,
                enter = enterTransition,
                exit = exitTransition,
            ) {
                if (shouldShowInlineSuggestionsUi) {
                    InlineSuggestionsUi(inlineSuggestions)
                } else {
                    // One or the other, never both. AnimatedVisibility lays its content out in a
                    // Box, so composing the two here drew them on top of each other: the bucket
                    // legend and the word suggestions in the same strip, overlapping, unreadable.
                    //
                    // The bucket strip owns the slot whenever a bucket holds something, because
                    // that legend is the only way to tell which bucket holds which text, and it
                    // steps aside the moment they are all empty. hasContent() asks exactly what the
                    // strip itself asks before drawing, so the two cannot disagree about the slot.
                    if (maCandidates.isEmpty() && maBucketStripHasContent()) {
                        MaBucketStrip()
                    } else {
                        CandidatesRow()
                    }
                }
            }
            this@CenterContent.AnimatedVisibility(
                visible = expanded && !isDictating,
                enter = enterTransition,
                exit = exitTransition,
            ) {
                QuickActionsRow(
                    FlorisImeUi.SmartbarSharedActionsRow.elementName,
                    modifier = modifier.fillMaxSize(),
                )
            }
            this@CenterContent.AnimatedVisibility(
                visible = isDictating,
                enter = enterTransition,
                exit = exitTransition,
            ) {
                DictateSmartbarUi(dictateState)
            }
        }
    }

    @Composable
    fun ExtendedActionsToggle() {
        SnyggIconButton(
            FlorisImeUi.SmartbarExtendedActionsToggle.elementName,
            onClick = {
                if (/* was */ extendedActionsExpanded) {
                    keyboardManager.activeState.isActionsOverflowVisible = false
                }
                scope.launch {
                    prefs.smartbar.extendedActionsExpanded.set(!extendedActionsExpanded)
                }
            },
            modifier = Modifier.sizeIn(maxHeight = FlorisImeSizing.smartbarHeight).aspectRatio(1f)
        ) {
            val transition = updateTransition(extendedActionsExpanded, label = "smartbarSecondaryRowToggleBtn")
            val alpha by transition.animateFloat(label = "alpha") { if (it) 1f else 0f }
            val rotation by transition.animateFloat(label = "rotation") { if (it) 180f else 0f }
            // Expanded icon
            SnyggIcon(
                FlorisImeUi.SmartbarExtendedActionsToggle.elementName,
                modifier = Modifier
                    .alpha(alpha)
                    .rotate(rotation),
                imageVector = Icons.Default.UnfoldLess,
            )
            // Not expanded icon
            SnyggIcon(
                FlorisImeUi.SmartbarExtendedActionsToggle.elementName,
                modifier = Modifier
                    .alpha(1f - alpha)
                    .rotate(rotation - 180f),
                imageVector = Icons.Default.UnfoldMore,
            )
        }
    }

    @Composable
    fun StickyAction() {
        val actionArrangement by prefs.smartbar.actionArrangement.collectAsState()
        val evaluator by keyboardManager.activeSmartbarEvaluator.collectAsState()

        val action = when {
            actionArrangement.stickyAction != null -> {
                actionArrangement.stickyAction
            }

            else -> null
        }

        if (action != null) {
            QuickActionButton(
                modifier = Modifier.padding(horizontal = 4.dp),
                action = action,
                evaluator = evaluator,
            )
        } else {
            Spacer(
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .aspectRatio(1f),
            )
        }
    }

    SideEffect {
        if (!shouldAnimate) {
            scope.launch {
                prefs.smartbar.sharedActionsExpandWithAnimation.set(true)
            }
        }
    }

    SnyggRow(
        modifier = modifier
            .fillMaxWidth()
            .height(FlorisImeSizing.smartbarHeight),
    ) {
        when (smartbarLayout) {
            SmartbarLayout.SUGGESTIONS_ONLY -> {
                if (shouldShowInlineSuggestionsUi) {
                    InlineSuggestionsUi(inlineSuggestions)
                } else {
                    // One or the other, never both. AnimatedVisibility lays its content out in a
                    // Box, so composing the two here drew them on top of each other: the bucket
                    // legend and the word suggestions in the same strip, overlapping, unreadable.
                    //
                    // The bucket strip owns the slot whenever a bucket holds something, because
                    // that legend is the only way to tell which bucket holds which text, and it
                    // steps aside the moment they are all empty. hasContent() asks exactly what the
                    // strip itself asks before drawing, so the two cannot disagree about the slot.
                    if (maCandidates.isEmpty() && maBucketStripHasContent()) {
                        MaBucketStrip()
                    } else {
                        CandidatesRow()
                    }
                }
            }

            SmartbarLayout.ACTIONS_ONLY -> {
                if (shouldShowInlineSuggestionsUi) {
                    InlineSuggestionsUi(inlineSuggestions)
                } else {
                    QuickActionsRow(FlorisImeUi.SmartbarSharedActionsRow.elementName)
                }
            }

            SmartbarLayout.SUGGESTIONS_ACTIONS_SHARED -> {
                // The language badge, then suggestions, and nothing else. The dashboard button and
                // the microphone both left this row: the dashboard is a long press on the
                // microphone now, and the microphone itself moved down into the copy and paste row,
                // where it is the same size and colour as every other key. Both were spending the
                // width of a suggestion each, on a strip whose whole job is showing suggestions.
                //
                // The badge stands down while dictating, because the recording bar carries its own
                // now and two of them would be two answers to one question. Anything at all to the
                // left of the centre content also shifts that centre right by its own width, and a
                // stopwatch that is nearly centred reads as a mistake rather than as a measurement.
                //
                // Since the strip itself now comes and goes, this badge is visible exactly while
                // there are suggestions to sit beside, which is while typing. That is the moment the
                // suggestion language matters, so the indicator is present precisely when it means
                // something and gone when it would only be taking up a row.
                val isDictatingNow = dictateState !is DictateController.UiState.Idle
                if (!isDictatingNow) {
                    LanguageToggle()
                }
                CenterContent()
            }

            SmartbarLayout.SUGGESTIONS_ACTIONS_EXTENDED -> {
                if (!flipToggles) {
                    ExtendedActionsToggle()
                    CenterContent()
                    StickyAction()
                } else {
                    StickyAction()
                    CenterContent()
                    ExtendedActionsToggle()
                }
            }
        }
    }
}

@Composable
private fun SmartbarSecondaryRow(modifier: Modifier = Modifier) {
    val prefs by FlorisPreferenceStore
    val smartbarLayout by prefs.smartbar.layout.collectAsState()
    val secondaryRowStyle = rememberSnyggThemeQuery(FlorisImeUi.SmartbarExtendedActionsRow.elementName)
    val windowStyle = rememberSnyggThemeQuery(FlorisImeUi.Window.elementName)
    val extendedActionsExpanded by prefs.smartbar.extendedActionsExpanded.collectAsState()
    val extendedActionsPlacement by prefs.smartbar.extendedActionsPlacement.collectAsState()
    val background = secondaryRowStyle.background().let { color ->
        if (extendedActionsPlacement == ExtendedActionsPlacement.OVERLAY_APP_UI) {
            if (color.isUnspecified || color.alpha == 0f) {
                windowStyle.background(default = Color.Black)
            } else {
                color
            }
        } else {
            color
        }
    }

    AnimatedVisibility(
        visible = smartbarLayout == SmartbarLayout.SUGGESTIONS_ACTIONS_EXTENDED && extendedActionsExpanded,
        enter = VerticalEnterTransition,
        exit = VerticalExitTransition,
    ) {
        QuickActionsRow(
            FlorisImeUi.SmartbarExtendedActionsRow.elementName,
            modifier = modifier
                .fillMaxWidth()
                .height(FlorisImeSizing.smartbarHeight)
                .background(background),
        )
    }
}
