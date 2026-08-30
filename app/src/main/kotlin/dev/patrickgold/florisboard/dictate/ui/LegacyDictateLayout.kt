/*
 * Copyright (C) 2026 DevEmperor (Dictate)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

package dev.patrickgold.florisboard.dictate.ui

import android.os.SystemClock
import android.view.inputmethod.ExtractedTextRequest
import android.view.inputmethod.InputConnection
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardReturn
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Deselect
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.KeyboardHide
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Numbers
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.PushPin
import dev.patrickgold.florisboard.ime.text.keyboard.MaCursorPad
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.filled.SpaceBar
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import dev.patrickgold.florisboard.FlorisImeService
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.app.FlorisPreferenceStore
import dev.patrickgold.florisboard.dictate.DictateController
import dev.patrickgold.florisboard.dictate.MaLanguage
import dev.patrickgold.florisboard.clipboardManager
import dev.patrickgold.florisboard.editorInstance
import dev.patrickgold.florisboard.ime.ImeUiMode
import dev.patrickgold.florisboard.ime.input.InputShiftState
import dev.patrickgold.florisboard.ime.input.LocalInputFeedbackController
import dev.patrickgold.florisboard.ime.keyboard.FlorisImeSizing
import dev.patrickgold.florisboard.ime.keyboard.KeyboardManager
import dev.patrickgold.florisboard.ime.keyboard.KeyboardMode
import dev.patrickgold.florisboard.ime.text.gestures.SwipeAction
import dev.patrickgold.florisboard.ime.text.key.KeyCode
import dev.patrickgold.florisboard.ime.text.keyboard.TextKeyData
import dev.patrickgold.florisboard.ime.theme.FlorisImeUi
import dev.patrickgold.florisboard.ime.smartbar.quickaction.QuickActionsOverflowPanel
import dev.patrickgold.florisboard.keyboardManager
import org.florisboard.lib.android.showShortToast
import dev.patrickgold.jetpref.datastore.model.collectAsState
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withTimeoutOrNull
import org.florisboard.lib.compose.stringRes
import org.florisboard.lib.snygg.SnyggSelector
import org.florisboard.lib.snygg.ui.SnyggColumn
import org.florisboard.lib.snygg.ui.SnyggText
import org.florisboard.lib.snygg.ui.rememberSnyggThemeQuery
import androidx.compose.foundation.Canvas
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.Offset
import androidx.compose.runtime.mutableFloatStateOf

/**
 * Cross-composable state for the legacy layout. [suppressGlide] is set while the modern typing keyboard
 * is shown via the SWIPE mode so glide typing is disabled there – otherwise a horizontal glide would
 * swallow the swipe-back gesture and the user could never return to the dictation UI.
 */
object LegacyLayoutState {
    val suppressGlide = MutableStateFlow(false)

    // True while the current finger-down owns a horizontal swipe that must NOT flip keyboards: space or
    // backspace (their cursor-move / delete gestures, issue #188), or a key whose long-press popup is open
    // so the user can swipe to pick an accent/umlaut (e.g. o → ö, issue #221). The SWIPE-mode swipe-toggle
    // checks this and steps aside instead of flipping back to the dictation UI.
    val keyOwnsSwipe = MutableStateFlow(false)
}

/** Which full-panel overlay (if any) replaces the legacy layout. Emoji uses the app's own MEDIA panel. */
private enum class LegacyOverlay { NONE, NUMBERS }

/** Uniform key corner radius, matching the Record button, so all buttons share the same rounding. */
private val LegacyKeyShape = RoundedCornerShape(16.dp)
/** Horizontal margin around every key; adjacent keys sit [KeyMarginH] × 2 apart. */
private val KeyMarginH = 3.dp
/** Vertical margin around every key – a touch larger, so rows breathe more top-to-bottom. */
private val KeyMarginV = 5.dp
/** Height of the record and bottom rows – the keys are as tall as the Record button. */
private val SideRowHeight = 56.dp
/** Height of the editing-action row (select-all etc.) – shorter than the main key rows. */
private val EditRowHeight = 46.dp

/** The current input connection of the running IME, or null if detached. */
private fun ic(): InputConnection? = FlorisImeService.currentInputConnection()

/** Dispatch a single key code through the normal input pipeline (down + up). */
internal fun KeyboardManager.tapKey(code: Int) = inputEventDispatcher.sendDownUp(TextKeyData(code = code))

/** Query attributes that make the theme resolve its `key` styling for the given key [code]. */
private fun keyAttributes(code: Int) = mapOf(
    FlorisImeUi.Attr.Code to code,
    FlorisImeUi.Attr.Mode to KeyboardMode.CHARACTERS.toString(),
    FlorisImeUi.Attr.ShiftState to InputShiftState.UNSHIFTED.toString(),
)

/**
 * A horizontal-swipe detector that flips between the legacy dictation panel and the modern typing
 * keyboard (SWIPE mode, issue #125). A clearly horizontal drag past the threshold calls [onToggle].
 *
 * @param intercept when true the gesture is handled on the Initial pass and consumed, so it wins over
 *   the keys below (used on the modern keyboard – a horizontal swipe anywhere returns to the dictation
 *   UI). When false (legacy side) it runs on the Main pass and bails the moment a child gesture (space
 *   cursor, backspace select, prompt-strip scroll) consumes the event, so those never trigger a switch.
 */
fun Modifier.legacySwipeToggle(
    intercept: Boolean = false,
    onToggle: () -> Unit,
): Modifier = this.pointerInput(intercept) {
    val thresholdPx = 56.dp.toPx()
    val pass = if (intercept) PointerEventPass.Initial else PointerEventPass.Main
    awaitEachGesture {
        awaitFirstDown(requireUnconsumed = false, pass = pass)
        var totalDx = 0f
        var totalDy = 0f
        while (true) {
            val event = awaitPointerEvent(pass)
            val change = event.changes.firstOrNull() ?: break
            if (!change.pressed) break
            if (!intercept && change.isConsumed) break
            // On the modern keyboard, let a key that owns its swipe keep it — space/backspace cursor+delete
            // gestures (#188), or an open long-press popup for picking an accent/umlaut (#221): step aside
            // instead of flipping keyboards.
            if (intercept && LegacyLayoutState.keyOwnsSwipe.value) break
            totalDx += change.position.x - change.previousPosition.x
            totalDy += change.position.y - change.previousPosition.y
            if (abs(totalDx) > thresholdPx && abs(totalDx) > abs(totalDy) * 1.5f) {
                change.consume()
                onToggle()
                break
            }
        }
    }
}

/**
 * The classic, keyboard-less "legacy" dictation layout (issue #125) – a faithful Compose reproduction
 * of the Dictate 3.x record-first UI. Rendered in place of the typing keyboard (the `ImeUiMode.TEXT`
 * branch in `ImeWindow`) when [dev.patrickgold.florisboard.dictate.DictateLegacyLayout] is enabled.
 *
 * Every key takes the active theme's `key` colours (including the accent the theme puts on special keys
 * like enter) but a uniform [LegacyKeyShape] rounding; the only deliberate extra accent is the Record
 * button. Rows top→bottom: the always-visible prompt strip (only when rewording is enabled), an
 * editing-action row (select-all · undo · redo · cut · copy · paste · emoji · numbers), the record row
 * and the bottom row (switch · space · enter).
 */
@Composable
fun LegacyDictateLayout(
    modifier: Modifier = Modifier,
    /**
     * Voice Type: how to leave this screen and return to the typing keyboard.
     *
     * Null in the original use, where this layout *is* the keyboard by the user's choice and the
     * bottom-left key hands over to another keyboard app. Non-null when it is opened as a mode from
     * the microphone key, where that would be a trap: there would be no way back to QWERTY.
     */
    onExitToKeyboard: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    val keyboardManager by context.keyboardManager()
    val prefs by FlorisPreferenceStore
    val prompts by DictateController.prompts.collectAsState()
    val dictateState by DictateController.state.collectAsState()
    val accent by prefs.theme.accentColor.collectAsState()
    val rewordingEnabled by prefs.dictate.rewordingEnabled.collectAsState()
    val promptRows by prefs.dictate.legacyPromptRows.collectAsState()

    // Keep the screen awake while recording so the auto-timeout can't cut the recording short. The modern

    // The Smartbar (which normally loads the prompts) is replaced by this layout, so trigger the load
    // here whenever the panel appears / rewording toggles.
    LaunchedEffect(rewordingEnabled) {
        if (rewordingEnabled) DictateController.refreshPrompts(context)
    }

    var overlay by remember { mutableStateOf(LegacyOverlay.NONE) }

    // The dashboard opens where it was asked for.
    //
    // It was only ever drawn by the typing view, so pressing db here set the flag, the panel
    // appeared on a screen that was not showing, and this view carried on as though nothing had
    // happened. Worse, the flag stayed set, so the next trip to the typing keyboard landed in a
    // panel nobody had asked for. Both views draw it now, and it covers this one the same way it
    // covers that one.
    val state by keyboardManager.activeState.collectAsState()
    if (state.isActionsOverflowVisible) {
        Box(modifier = modifier.fillMaxWidth()) {
            QuickActionsOverflowPanel()
        }
        return
    }

    Box(modifier = modifier.fillMaxWidth()) {
        when (overlay) {
            LegacyOverlay.NUMBERS -> LegacyNumberPadOverlay { overlay = LegacyOverlay.NONE }
            LegacyOverlay.NONE -> Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = KeyMarginH, vertical = KeyMarginV),
            ) {
                // Row 1: normally the prompt strip (when rewording is on), but it doubles as the status
                // surface – errors, the interrupted-recording chip and the rate/donate/milestone promos
                // reuse the same DictateSmartbarUi the main keyboard shows, so all of those work here too.
                val showStatus = dictateState is DictateController.UiState.Error ||
                    dictateState is DictateController.UiState.Interrupted ||
                    dictateState is DictateController.UiState.Promo
                // The Little Man's row is gone, and with it the only thing that drew him. What is
                // left here is the status surface: an error, an interrupted recording, a promo. The
                // row appears when there is something to say and takes no space when there is not.
                //
                // The rewording branch went with the row rather than being switched off, because a
                // switched-off row is a row somebody turns back on by accident.
                if (showStatus) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(FlorisImeSizing.smartbarHeight * 1.2f)
                            .padding(bottom = KeyMarginV),
                    ) {
                        DictateSmartbarUi(dictateState, modifier = Modifier.fillMaxSize())
                    }
                }

                // Row 1b: the quick row. Language buttons, one per enabled language, and the current
                // transcription model as a dropdown, so both can be checked and changed in the second
                // before speaking rather than by leaving for the settings app.
                // Hideable from the Menu Macro panel, like every other section. Screen height is the
                // scarcest thing on a keyboard and a row nobody uses costs exactly as much of it as
                // one used constantly.
                val maShowQuickRow by prefs.dictate.maShowQuickRow.collectAsState()
                if (maShowQuickRow) {
                    MaQuickRow(
                        modifier = Modifier.fillMaxWidth().height(EditRowHeight),
                    )
                }

                // The macro bar was here, removed with the one in the keyboard view. Both views draw
                // the same rows, so a row deleted from one has to go from the other or they drift.


                // Row 2: editing actions (select-all first, so it sits in the row below the strip).
                //
                // Zone three, and gated here as well as in the keyboard view. Key 3 in the feature
                // row switches this row, and a switch that works in one view and not the other is a
                // switch that looks broken from whichever view it was pressed in. Same row, same
                // code, same switch.
                // THE OLD EDIT STRIP IS GONE FROM THIS VIEW.
                //
                // There were two clipboard rows here — this one and the arrangeable copy row below
                // it — in different orders, obeying different settings, neither aware of the other.
                // He could not tell which one his arrangement applied to, because the answer was
                // "one of them".
                //
                // The copy row is the master: it is the one the editor arranges, so it takes this
                // position and this strip is removed. **Two controls for one job is not a choice, it
                // is a bug with an options screen.**
                //
                // The strip still exists on the typing keyboard, where it is not duplicated and
                // where `maEditRow` still switches it.

                // ABOVE THE RECORD ROW, ALWAYS, AND NOT A SETTING.
                //
                // It used to sit under the record row and it used to be switchable. Both are now
                // fixed: this view has no letter keys, so select-all, paste, cut and the two
                // histories are most of what the screen is FOR — a switch offering to remove them
                // is offering to empty the screen.
                //
                // Directly under the language badge and directly above the record button, because
                // that is where his thumb already is between speaking and pasting.
                //
                // Same composable as the feature row, so a key behaves identically in both places.
                MaFeatureRow(rowHeight = SideRowHeight, copyRowOnly = true)
                // Row 3: the record row.
                LegacyRecordRow(
                    modifier = Modifier.fillMaxWidth().height(SideRowHeight),
                    dictateState = dictateState,
                    accent = accent,
                )

                // Row 4: switch keyboard · space · enter (same height as the record row for equal keys).
                LegacyBottomRow(
                    modifier = Modifier.fillMaxWidth().height(SideRowHeight),
                    keyboardManager = keyboardManager,
                    onExitToKeyboard = onExitToKeyboard,
                )

                // The arrow strip was here, removed with the one in the keyboard view.

            }
        }

        // The cursor pad, over this view too, drawn last so it takes the touches.
        //
        // It must be drawn wherever it can be opened. A long press that raises a pad the screen
        // never renders is the trap of build 150 again, in a place with even fewer ways out.
        if (MaCursorPad.active) {
            MaCursorPad.Overlay(
                // Selection is the editor's own manual-selection mode, not a second mechanism.
                // KeyboardManager already extends the selection on an arrow whenever that flag is
                // set, so turning it on here makes every arrow the pad sends select instead of
                // move — with no special cases, and with the same behaviour as holding shift.
                onSelectToggle = {
                    MaCursorPad.toggleSelecting()
                    keyboardManager.activeState.isManualSelectionMode = MaCursorPad.selecting
                },
            ) { code ->
                keyboardManager.inputEventDispatcher.sendDownUp(TextKeyData(code = code))
            }
        }
    }
}



/**
 * A single legacy key: the active theme's `key` colours for the given [code] (so special keys such as
 * enter keep their accent), rendered with the uniform [LegacyKeyShape] and a pressed/ripple state. The
 * [content] receives the themed foreground colour so icons/labels match the theme.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun ThemedKey(
    code: Int,
    modifier: Modifier = Modifier,
    /**
     * A click that knows WHERE along the key it landed, as a fraction of the width from 0 to 1.
     *
     * Only the spacebar uses it. When given, it replaces [onClick] entirely — a key cannot have two
     * answers to one tap, and a key that ran both would send a space AND an arrow.
     */
    onClickAt: ((Float) -> Unit)? = null,
    /**
     * A HOLD that repeats a key code, chosen by where along the key the finger is.
     *
     * The lambda answers "which code for this position", and the key sends it every [REPEAT_MS]
     * until the finger lifts. Only the spacebar uses it.
     *
     * A repeat rather than a single press on release, because the thing being repeated is movement:
     * he holds until the cursor is where he wants it and lets go. A hold that fired once at the end
     * would be a slow tap.
     */
    onLongClickAt: ((Float) -> Unit)? = null,
    /** Draws the switcher ring. See [ThemedIconKey]. */
    switcher: Boolean = false,
    /**
     * A ring in a colour of its own, for a key whose state is worth seeing from across the row.
     *
     * Only the buckets use it. The switcher ring says what KIND of key this is and is monochrome for
     * exactly that reason — a row of tinted rings would read as decoration. This one says what STATE
     * the key is in, which is the job colour has everywhere else in the app, so it is a colour and
     * the two cannot be confused: ink-coloured means "this switches something", green means "this
     * bucket is holding something".
     *
     * Marko asked for the outline in place of the tick that used to appear for a minute after a
     * copy landed. It is the better mark: a tick is an event and it has to be caught, while a ring
     * is a state and it is simply there until the bucket is poured out.
     */
    ring: Color? = null,
    onLongClick: (() -> Unit)? = null,
    onClick: () -> Unit,
    content: @Composable (foreground: Color) -> Unit,
) {
    val feedback = LocalInputFeedbackController.current
    val interaction = remember { MutableInteractionSource() }
    // For the hold-repeat on the spacebar. Scoped to this key's composition, so lifting the
    // finger cancels it and so does the key leaving the screen — a repeat that outlived its key
    // would type arrows into whatever came next.
    val keyScope = rememberCoroutineScope()
    val pressed by interaction.collectIsPressedAsState()
    val style = rememberSnyggThemeQuery(
        FlorisImeUi.Key.elementName,
        keyAttributes(code),
        if (pressed) SnyggSelector.PRESSED else SnyggSelector.NONE,
    )
    val bg = style.background(default = Color.White.copy(alpha = 0.08f))
    val fg = style.foreground(default = Color.White)
    Box(
        modifier = modifier
            .padding(horizontal = KeyMarginH, vertical = KeyMarginV)
            .clip(LegacyKeyShape)
            .background(bg)
            // After the background and on the same shape, so the ring sits on the key's own edge
            // rather than outside its margins or cutting across its corners.
            .then(
                when {
                    // The state ring wins if both are asked for. Nothing wears both today — no
                    // bucket is a switcher — and if something ever does, what it is holding matters
                    // more at a glance than what kind of key it is.
                    ring != null -> Modifier.border(1.5.dp, ring, LegacyKeyShape)
                    // A switcher with no state ring given still gets the plain one: it says "this
                    // key changes what the keyboard shows" for keys that have no on/off to report.
                    switcher -> Modifier.border(1.5.dp, MaSwitcherRing, LegacyKeyShape)
                    else -> Modifier
                },
            )
            .then(
                if (onClickAt == null) {
                    Modifier.combinedClickable(
                        interactionSource = interaction,
                        indication = ripple(),
                        onClick = { feedback.keyPress(); onClick() },
                        onLongClick = onLongClick?.let { { feedback.keyPress(); it() } },
                    )
                } else {
                    // The positional path. `detectTapGestures` gives the offset that
                    // `combinedClickable` throws away, and it carries the long press itself so the
                    // spacebar keeps its cursor pad.
                    //
                    // No ripple here: an indication would need an interaction source fed by hand,
                    // and a half-fed one leaves a key stuck looking pressed. The haptic tick is the
                    // feedback, and it is the one he feels rather than sees.
                    Modifier.pointerInput(onClickAt, onLongClick, onLongClickAt) {
                        detectTapGestures(
                            onTap = { offset ->
                                feedback.keyPress()
                                val width = size.width.toFloat().coerceAtLeast(1f)
                                onClickAt((offset.x / width).coerceIn(0f, 1f))
                            },
                            onPress = { offset ->
                                // The repeat runs HERE rather than in onLongPress, because
                                // onLongPress fires once and this has to keep going until he lifts.
                                // `tryAwaitRelease` is what gives us the lift, and it is only
                                // available inside onPress.
                                if (onLongClickAt != null) {
                                    val width = size.width.toFloat().coerceAtLeast(1f)
                                    val where = (offset.x / width).coerceIn(0f, 1f)
                                    val job = keyScope.launch {
                                        // The first wait is longer: it is what separates a hold from
                                        // a tap. Without it every tap would fire one arrow before
                                        // the tap handler ever ran.
                                        delay(HOLD_START_MS)
                                        while (true) {
                                            feedback.keyPress()
                                            onLongClickAt(where)
                                            delay(REPEAT_MS)
                                        }
                                    }
                                    tryAwaitRelease()
                                    job.cancel()
                                } else {
                                    tryAwaitRelease()
                                }
                            },
                            onLongPress = onLongClick?.let { { _ -> feedback.keyPress(); it() } },
                        )
                    }
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        content(fg)
    }
}

/** Convenience: a themed key showing a single icon. [tint] overrides the themed foreground (e.g. red). */
@Composable
internal fun ThemedIconKey(
    code: Int,
    icon: ImageVector,
    contentDescription: String,
    modifier: Modifier = Modifier,
    tint: Color? = null,
    iconSize: Dp = 22.dp,
    /**
     * Whether this key SWITCHES something rather than doing it.
     *
     * A ring in the ordinary ink, no colour. The row mixes two kinds of key that look identical and
     * behave nothing alike: a clipboard icon that pastes, and a clipboard icon that shows the copy
     * row. Nothing on the face can tell them apart — they are the same picture — so the distinction
     * has to be around the key rather than inside it.
     *
     * Deliberately not a colour. Colour is the state channel: green means this switcher is ON, and
     * a second colour meaning "this is a switcher" would give one channel two jobs. The ring says
     * *kind*; the ink says *state*. Monochrome, because he asked for an app that is not colourful
     * and because a row of tinted keys reads as decoration.
     */
    switcher: Boolean = false,
    onLongClick: (() -> Unit)? = null,
    onClick: () -> Unit,
) {
    ThemedKey(
        code = code,
        modifier = modifier,
        switcher = switcher,
        onLongClick = onLongClick,
        onClick = onClick,
    ) { fg ->
        Icon(imageVector = icon, contentDescription = contentDescription, tint = tint ?: fg, modifier = Modifier.size(iconSize))
    }
}


// LegacyEditRow is gone.
//
// It drew `legacyActionRow` — the LegacyEditAction key set — as a row above the number row on the
// typing keyboard, and it was described in two files as being the same row the transcription view
// draws. It was not. That view draws `maCopyRow` from the MaFeatureKey set, which is why the two
// rows ended in different keys while both were called the copy row.
//
// The typing keyboard draws the real copy row now, from MaFeatureRow, switched by `maEditRow`.
//
// LegacyActionKey and the LegacyEditAction enum stay: the feature row draws AP, AC, select-all and
// backspace through them, and those are the same keys from the same code, not copies.

/**
 * Renders a single [LegacyEditAction] as a themed key with the right icon and behaviour.
 *
 * Internal rather than private because the feature row draws three of these keys itself (AP,
 * select-all and backspace). It is the same key from the same code, not a copy that looks like it,
 * so a fix to AP's timing or to backspace's swipe reaches both rows at once. This is the same
 * mistake `tapKey` made when it was private to its file and the new row could not send a key.
 */
@Composable
internal fun LegacyActionKey(
    action: LegacyEditAction,
    modifier: Modifier,
    keyboardManager: KeyboardManager,
    hasSelection: Boolean,
    onEmoji: () -> Unit = { keyboardManager.activeState.imeUiMode = ImeUiMode.MEDIA },
    onNumbers: () -> Unit = { keyboardManager.activeState.keyboardMode = KeyboardMode.NUMERIC_ADVANCED },
) {
    val context = LocalContext.current
    val prefs by FlorisPreferenceStore
    val label = stringRes(action.labelRes)
    val actionScope = rememberCoroutineScope()
    // The same three waits the copy buckets use, and deliberately not settings of their own.
    //
    // AP, AC and a bucket key do the same three things to the same field: select everything, delete
    // it, put text in. What makes a field too slow is the field, not which key asked — so tuning
    // them apart would mean finding the same number twice, and forgetting the second one when an
    // app got slower.
    val delaySelect by prefs.dictate.maClipDelaySelect.collectAsState()
    val delayDelete by prefs.dictate.maClipDelayDelete.collectAsState()
    val delayPaste by prefs.dictate.maClipDelayPaste.collectAsState()
    // Named apart from the row's own locals: this composable is called per key and these two are
    // only wanted by the snippet gesture.
    val snippetClipboard by context.clipboardManager()
    val snippetEditor by context.editorInstance()
    when (action) {
        // The clipboard panel: everything copied recently, to pick from rather than to guess at.
        // This is not the history key further along the row, which is past dictations. Two different
        // pasts, and the one worth reaching for while editing is this one.
        LegacyEditAction.CLIPBOARD_HISTORY -> ThemedIconKey(
            code = KeyCode.IME_UI_MODE_CLIPBOARD,
            icon = action.icon,
            contentDescription = label,
            modifier = modifier,
            // Hold to save a snippet, tap to get it back. One key, because saving and reusing are
            // two ends of the same act and putting them anywhere but together means hunting for the
            // second one.
            //
            // Saves the selection if there is one, otherwise whatever is on the clipboard, which
            // covers both ways somebody arrives at a piece of text worth keeping: they selected it,
            // or they just copied it.
            onLongClick = {
                actionScope.launch {
                    // Copy first, always, then save whatever the clipboard ends up holding.
                    //
                    // Reading the selection out of the input connection was the first attempt and it
                    // does not work where it matters. A selection inside a web view, which is where
                    // most of this text is selected, is the browser's selection and not the editor's,
                    // so the input connection reports nothing at all. Asking the field to copy is a
                    // request the browser understands, and it puts the words on the system clipboard
                    // where they can actually be read.
                    //
                    // With nothing selected the copy is a no-op and the clipboard keeps what it had,
                    // which pins the last thing copied. That is the right fallback rather than a
                    // failure: it is still a piece of text deliberately put on the clipboard.
                    snippetEditor.performClipboardCopy()
                    // The clipboard is a system service and the copy travels through it, so the new
                    // contents are not readable in the same breath.
                    delay(150)
                    val text = snippetClipboard.primaryClip?.text?.toString().orEmpty()
                    val saved = snippetClipboard.saveSnippet(text)
                    context.showShortToast(
                        if (saved) {
                            R.string.dictate__snippet_saved
                        } else {
                            R.string.dictate__snippet_nothing
                        },
                    )
                }
            },
            onClick = { keyboardManager.tapKey(KeyCode.IME_UI_MODE_CLIPBOARD) },
        )

        // AP: replace the whole field with what is on the clipboard.
        //
        // Select all, delete, wait, paste. The delete is not redundant even though pasting over a
        // selection replaces it: some fields, and password fields in particular, treat a paste onto
        // a selection differently from a paste into an empty field, and clearing first makes the
        // result the same everywhere.
        //
        // The waits are the reason this is a macro and not three key presses. Selecting, deleting and
        // pasting are all handled by the field being typed into rather than by this keyboard, and a
        // field still settling from the last step can drop the next one entirely and silently.
        //
        // The three waits are NOT equal, and that is the whole design. A short lead before the select
        // and before the delete is enough, because those two only need the field to have noticed the
        // last thing that happened. The paste is the step that actually failed in some apps, so it
        // gets five times as long: a rich text editor reflowing a document, or a web view rebuilding
        // its selection after a delete, takes far longer to settle than a plain text box and never
        // reports that it was not ready.
        //
        // Half a second of that is spent on one step for a reason. Spreading the same total evenly
        // would make the macro feel slower and fix less, because the two cheap steps do not need it
        // and the expensive one would get less.
        //
        // Drawn as two letters because there is no picture of this. Every clipboard glyph already
        // means one of the four keys beside it.
        LegacyEditAction.ALL_PASTE -> ThemedKey(
            code = KeyCode.NOOP,
            modifier = modifier,
            onClick = {
                actionScope.launch {
                    keyboardManager.activeState.isManualSelectionMode = false
                    if (delaySelect > 0) delay(delaySelect.toLong())
                    FlorisImeService.currentInputConnection()
                        ?.performContextMenuAction(android.R.id.selectAll)
                    if (delayDelete > 0) delay(delayDelete.toLong())
                    // The connection is fetched again at every step rather than held. Most of a
                    // second is a long time in an input method and the field can be replaced
                    // underneath a macro, at which point a stale connection writes into nothing.
                    FlorisImeService.currentInputConnection()?.commitText("", 1)
                    if (delayPaste > 0) delay(delayPaste.toLong())
                    FlorisImeService.currentInputConnection()
                        ?.performContextMenuAction(android.R.id.paste)
                }
            },
        ) { fg ->
            Text(
                text = "AP",
                color = fg,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }

        // AC: select all, delete, stop. AP without the paste, and deliberately not CUT.
        //
        // The delays are the same ones AP needs and for the same reason. performContextMenuAction
        // and commitText are two round trips to another process, and firing the second before the
        // first has landed deletes a selection that does not exist yet, which reads as "the key did
        // nothing" rather than as a race. The connection is fetched again at each step rather than
        // held: the field can be replaced underneath a macro, and a stale connection writes into
        // nothing at all.
        //
        // Nothing here touches the clipboard. That is the whole reason this is its own key.
        LegacyEditAction.ALL_CLEAR -> ThemedKey(
            code = KeyCode.NOOP,
            modifier = modifier,
            onClick = {
                actionScope.launch {
                    keyboardManager.activeState.isManualSelectionMode = false
                    if (delaySelect > 0) delay(delaySelect.toLong())
                    FlorisImeService.currentInputConnection()
                        ?.performContextMenuAction(android.R.id.selectAll)
                    if (delayDelete > 0) delay(delayDelete.toLong())
                    FlorisImeService.currentInputConnection()?.commitText("", 1)
                }
            },
        ) { fg ->
            Text(
                text = "AC",
                color = fg,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }

        // The spacebar, as an action. Goes through tapKey like every other real key so that it
        // reaches the same place a space from the keyboard proper does: word committal, the
        // suggestion strip, double-space-for-full-stop and auto-capitalisation all hang off that
        // path, and a raw commitText(" ") would silently skip every one of them.
        LegacyEditAction.SPACE -> ThemedKey(
            code = KeyCode.SPACE,
            modifier = modifier,
            // The cursor pad here too, which is where he most wants it.
            //
            // This view has no letter keys, so moving through a long transcription meant tapping at
            // the text — the gesture the pad exists to replace, on the screen where the text is
            // longest. The same hold does the same thing on both keyboards, which is the only way a
            // gesture becomes something the hand does without deciding to.
            // HOLD A HALF, AND THE CURSOR RUNS.
            //
            // The long press used to open the cursor pad — a drag surface over the keyboard. It is
            // deleted. **A gesture that replaces the keyboard to move a cursor asks him to change
            // mode to do the smallest thing there is**, and he had to learn a second surface to do
            // what an arrow key does.
            //
            // What the hold does now is the obvious thing: it repeats the arrow for the half he is
            // holding. Left half runs left, right half runs right, at 111ms — fast enough to cross a
            // sentence, slow enough to stop on a word.
            //
            // TWO HALVES ON THE HOLD, THREE ZONES ON THE TAP, AND NO SEAM DRAWN.
            //
            // A tap has to leave room for the space, which is what the bar is for. A hold does not:
            // nobody holds a spacebar to type spaces, so the middle can belong to whichever arrow is
            // nearer and every hold lands on one. The two rules differ because the two gestures
            // ask different questions, and the seam is invisible because a line drawn on the bar
            // would advertise a boundary that only exists while he is holding it.
            onLongClickAt = { fraction ->
                keyboardManager.tapKey(
                    if (fraction < 0.5f) KeyCode.ARROW_LEFT else KeyCode.ARROW_RIGHT,
                )
            },
            // THREE ZONES ACROSS THE SPACEBAR: left arrow, space, right arrow.
            //
            // The spacebar is the widest key on the keyboard and it does the least — one character,
            // and no ambiguity about which one. **The width is the opportunity**: the two things he
            // reaches for most while dictating are the space and the cursor, and the cursor keys are
            // the smallest targets on the row.
            //
            // Zones by WIDTH, not by separate keys: three keys would need three shapes, three gaps
            // and a decision about which of them wears the spacebar glyph. One key with a wide middle
            // stays the spacebar it has always been, and the arrows are room that was there anyway.
            //
            // The middle is HALF the width and the arrows a quarter each. A quarter of a spacebar is
            // still a bigger target than a key on the letter row, and half is still the widest thing
            // on the keyboard — so nothing he already does gets harder.
            //
            // The long press is unchanged and belongs to the whole key: the cursor pad is what the
            // zones are a shortcut for, and it must not depend on where along the bar he pressed.
            onClickAt = { fraction ->
                when {
                    fraction < 0.25f -> keyboardManager.tapKey(KeyCode.ARROW_LEFT)
                    fraction > 0.75f -> keyboardManager.tapKey(KeyCode.ARROW_RIGHT)
                    else -> keyboardManager.tapKey(KeyCode.SPACE)
                }
            },
            onClick = { keyboardManager.tapKey(KeyCode.SPACE) },
        ) { fg ->
            Text(
                // U+23B5, the bottom square bracket: the shape a spacebar has on every keyboard
                // anybody has used. Three dashes read as a dash key or a strikethrough, which is a
                // key that appears to do something else entirely.
                text = "\u23B5",
                color = fg,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }

        // Select-all toggles: with a selection it becomes "deselect" and collapses the cursor.
        LegacyEditAction.SELECT_ALL -> ThemedIconKey(
            code = KeyCode.CLIPBOARD_SELECT_ALL,
            icon = if (hasSelection) Icons.Default.Deselect else Icons.Default.SelectAll,
            contentDescription = label,
            modifier = modifier,
        ) {
            if (hasSelection) {
                ic()?.let { c ->
                    val et = c.getExtractedText(ExtractedTextRequest(), 0)
                    if (et != null) c.setSelection(et.selectionEnd, et.selectionEnd)
                }
            } else {
                keyboardManager.tapKey(KeyCode.CLIPBOARD_SELECT_ALL)
            }
        }
        LegacyEditAction.LANGUAGE -> LegacyLanguageKey(modifier)
        LegacyEditAction.UNDO -> ThemedIconKey(KeyCode.UNDO, action.icon, label, modifier) { keyboardManager.tapKey(KeyCode.UNDO) }
        LegacyEditAction.REDO -> ThemedIconKey(KeyCode.REDO, action.icon, label, modifier) { keyboardManager.tapKey(KeyCode.REDO) }
        LegacyEditAction.CUT -> ThemedIconKey(KeyCode.CLIPBOARD_CUT, action.icon, label, modifier) { keyboardManager.tapKey(KeyCode.CLIPBOARD_CUT) }
        LegacyEditAction.COPY -> ThemedIconKey(KeyCode.CLIPBOARD_COPY, action.icon, label, modifier) { keyboardManager.tapKey(KeyCode.CLIPBOARD_COPY) }
        LegacyEditAction.PASTE -> ThemedIconKey(KeyCode.CLIPBOARD_PASTE, action.icon, label, modifier) { keyboardManager.tapKey(KeyCode.CLIPBOARD_PASTE) }
        LegacyEditAction.EMOJI -> ThemedIconKey(KeyCode.IME_UI_MODE_MEDIA, action.icon, label, modifier, onClick = onEmoji)
        LegacyEditAction.NUMBERS -> ThemedIconKey(KeyCode.VIEW_NUMERIC, action.icon, label, modifier, onClick = onNumbers)
        LegacyEditAction.HISTORY -> ThemedIconKey(KeyCode.NOOP, action.icon, label, modifier) {
            keyboardManager.activeState.imeUiMode = ImeUiMode.HISTORY
        }
        LegacyEditAction.GIF -> ThemedIconKey(KeyCode.NOOP, action.icon, label, modifier) {
            keyboardManager.activeState.imeUiMode = ImeUiMode.GIF
        }
        LegacyEditAction.REINSERT -> ThemedIconKey(KeyCode.NOOP, action.icon, label, modifier) {
            DictateController.reinsertLastDictation(context)
        }
        // The view switch, and the way into the dashboard.
        //
        // This slot used to say "back to the typing keyboard" in both views, which meant that in the
        // typing view it did nothing at all: you were already there. It is the view swap now, and it
        // shows where it will take you rather than where you are. A microphone in the typing view, a
        // keyboard in the transcribe view, one place on the screen, so the thumb stops hunting.
        //
        // The microphone that used to sit in the corner of the suggestion strip, gold and ringed, is
        // this key. It has no colour of its own here because it never carried state, only emphasis,
        // and the strip it left is now entirely suggestions.
        //
        // Holding it opens the Menu Macro dashboard. The dashboard lost its own button in the same
        // clear-out, and a long press on a key that is always in the same place costs no width at
        // all. There is nowhere else on this keyboard left to put it.
        LegacyEditAction.KEYBOARD -> {
            val inTranscribe = keyboardManager.activeState.imeUiMode == ImeUiMode.TRANSCRIBE
            ThemedIconKey(
                code = KeyCode.NOOP,
                icon = if (inTranscribe) Icons.Default.Keyboard else Icons.Default.Mic,
                contentDescription = label,
                modifier = modifier,
                onLongClick = {
                    keyboardManager.activeState.isActionsOverflowVisible = true
                },
                onClick = {
                    keyboardManager.tapKey(
                        if (inTranscribe) KeyCode.IME_UI_MODE_TEXT else KeyCode.IME_UI_MODE_DICTATE,
                    )
                },
            )
        }
        LegacyEditAction.SWITCH -> ThemedIconKey(
            code = KeyCode.SYSTEM_PREV_INPUT_METHOD,
            icon = action.icon,
            contentDescription = label,
            modifier = modifier,
            onLongClick = { keyboardManager.tapKey(KeyCode.SYSTEM_INPUT_METHOD_PICKER) },
            onClick = { keyboardManager.tapKey(KeyCode.SYSTEM_PREV_INPUT_METHOD) },
        )
        // A backspace in the always-visible action row (#196): unlike the record-row backspace it stays
        // reachable while recording / realtime dictation, which is exactly when it was missing. Reuses the
        // record-row key verbatim, so it has the identical behaviour — tap deletes one character, holding
        // auto-repeats, and swiping left progressively selects whole words / single characters (per the
        // shared "Delete key swipe left" setting) that are deleted on release. Its swipe consumes the
        // gesture, so it never flips to the modern keyboard.
        LegacyEditAction.BACKSPACE -> LegacyBackspaceKey(modifier = modifier)
    }
}

/**
 * The language action: **HR** or **ENG**, and one tap moves to the other.
 *
 * It used to cycle a selected subset and long-press a picker of that subset, which was the shape for
 * an app that offered ninety-nine languages. This one offers two, permanently, so there is no subset
 * to select from, no cycle that could land somewhere unexpected, and nothing for a long press to
 * open. Auto-detect went with the picker: it only ever meant "choose from the list for me".
 *
 * Through MaLanguage rather than DictateController.cycleLanguage, so the transcription language and
 * the keyboard's suggestion language move together. They are the same decision and there is exactly
 * one place that writes them.
 */
@Composable
private fun LegacyLanguageKey(modifier: Modifier) {
    val prefs by FlorisPreferenceStore
    val context = LocalContext.current
    val activeCode by prefs.dictate.activeInputLanguage.collectAsState()
    // Keyed on the mode too, or the badge would keep saying HR after a tap moved it to AUTO.
    val langMode by prefs.dictate.maLanguageMode.collectAsState()
    val badge = remember(activeCode, langMode) { MaLanguage.badge() }
    ThemedKey(
        code = KeyCode.NOOP,
        modifier = modifier,
        onClick = { MaLanguage.cycleMode(context) },
    ) { fg ->
        Text(badge, color = fg, fontWeight = FontWeight.SemiBold)
    }
}

/**
 * The record row. Idle: settings · big Record button (tap = start, long-press = pick an audio file) ·
 * backspace. Recording: cancel (red) · Record button showing a pulsing dot + elapsed timer (tap = stop)
 * · pause/resume – so the prompt strip above stays put, matching the old UI. Transcribing/rewording: a
 * spinner + "Transcribing…".
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LegacyRecordRow(
    modifier: Modifier,
    dictateState: DictateController.UiState,
    accent: Color,
) {
    val prefs by FlorisPreferenceStore
    val context = LocalContext.current
    val keyboardManager by context.keyboardManager()
    val recording = dictateState as? DictateController.UiState.Recording
    val rewording = dictateState as? DictateController.UiState.Rewording
    // The button is non-interactive while the audio is being transcribed or reworded.
    val busy = dictateState is DictateController.UiState.Transcribing || rewording != null
    // Gold on near-black for everything drawn on the record key. The old rule chose black or white
    // against the accent fill, which is how white ended up on orange and hard to read at a glance.
    val onAccent = MaRecordInk
    val sideKey = Modifier.fillMaxHeight().aspectRatio(1f)

    // Long-form segmented dictation (#170): whether the "Next segment" button replaces pause and how many
    // cut segments are transcribing in the background; plus a one-shot flash of the Next button on each cut.
    val segmented by DictateController.segmentedRecording.collectAsState()
    val segmentsInFlight by DictateController.segmentsInFlight.collectAsState()
    val flushCount by DictateController.segmentFlushCount.collectAsState()
    val nextFlash = remember { Animatable(0f) }
    LaunchedEffect(flushCount) {
        if (flushCount > 0) {
            nextFlash.snapTo(1f)
            nextFlash.animateTo(0f, tween(550))
        }
    }
    // Realtime streaming (#128): tapping the record button ends the live stream — hint that with a send glyph.
    val realtime = recording != null && DictateController.isRealtimeRecording()

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Left slot: settings when idle, cancel (red) while recording.
        if (recording != null) {
            ThemedIconKey(
                code = KeyCode.NOOP,
                icon = Icons.Default.Delete,
                contentDescription = stringRes(R.string.dictate__action_cancel),
                modifier = sideKey,
                tint = Color(0xFF9B3B33),
                // Hard discard: ends the recording outright and throws the audio away, rather than
                // dropping only the current long-form segment and carrying on. One red button that
                // always means the same thing beats two that mean nearly the same thing, which is
                // why the separate X is gone and this took its job.
                onClick = { DictateController.discardRecording(context) },
            )
        } else {
            // db, the dashboard, in lowercase.
            //
            // Two letters rather than an icon, because there is no glyph that says "the panel where
            // this keyboard is configured" and three dots said only "more". Lowercase d and b are
            // mirror images of each other, which makes the pair read as a mark rather than as an
            // abbreviation, and it costs nothing to draw.
            //
            // It takes this slot because the gear moved down to the corner, where a left thumb
            // reaches without the hand shifting. Settings is opened rarely and deliberately; the
            // dashboard is opened mid sentence, so the dashboard gets the better position of the two
            // and the gear gets the easier one to find.
            ThemedKey(
                code = KeyCode.TOGGLE_ACTIONS_OVERFLOW,
                modifier = sideKey,
                onClick = {
                    keyboardManager.activeState.isActionsOverflowVisible = true
                },
            ) { fg ->
                Text(
                    text = "db",
                    color = fg,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }

        // Center: the big Record button – the one deliberate accent element (like the Smartbar mic).
        // Its movement follows the same user choice as the Smartbar dot (issue #238): a steady pulse,
        // the live mic level, or nothing at all. Kept subtle either way — this key is the size of a
        // thumb, so the same factors that read well on a 12 dp dot would be jarring here.
        // The button does not move. It used to breathe, scaling with the pulse or the mic level, and
        // a thumb-sized rectangle that swells and shrinks a few times a second right where the eye
        // is resting is genuinely unpleasant to watch for the length of a dictation. The meter
        // across it already shows the level, moving in the one place movement belongs.
        val isRecording = recording != null && !recording.paused
        val interaction = remember { MutableInteractionSource() }
        val feedback = LocalInputFeedbackController.current
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(horizontal = KeyMarginH, vertical = KeyMarginV)
                .clip(LegacyKeyShape)
                // A key like the others, not a slab of orange. The old fill was the loudest thing on
                // screen and it fought the readings printed on it; on a dark keyboard, dark with a
                // warm outline reads as "this one is special" without shouting.
                //
                // The outline is red while recording and the accent otherwise, which is the whole
                // visual language in one line: colour is reserved for state, and a red edge means
                // this thing is running.
                .background(MaRecordFill)
                // Red ring when idle, none while recording, and that way round on purpose. Idle, the
                // ring is the invitation: this is the key that records. Recording, the ring was
                // drawing a box around the meter and cutting across the very line it exists to show,
                // so it goes: nothing to press for, nothing to fence in.
                .border(
                    width = if (isRecording) 0.dp else 2.dp,
                    color = if (isRecording) Color.Transparent else MaRecordRing,
                    shape = LegacyKeyShape,
                )
                .then(
                    if (busy) {
                        // Transcribing/rewording: a tap now cancels the in-flight request (issue #192),
                        // matching the Smartbar stop button. No long-press (file transcription) while busy.
                        Modifier.clickable(
                            interactionSource = interaction,
                            indication = ripple(),
                        ) { feedback.keyPress(); DictateController.onMicClick(context) }
                    } else {
                        // Push-to-talk (#235) deliberately does not reach this button. It is the classic
                        // layout's one big key: a tap records, a long press picks a file to transcribe, and
                        // that stays true whether or not hold-to-record is on for the Smartbar mic.
                        Modifier.combinedClickable(
                            interactionSource = interaction,
                            indication = ripple(),
                            onClick = { feedback.keyPress(); DictateController.onMicClick(context) },
                            onLongClick = { feedback.keyPress(); DictateController.startFileTranscription(context) },
                        )
                    },
                ),
            contentAlignment = Alignment.Center,
        ) {
            // Voice Type: the oscilloscope lives inside this button, behind its content, so the
            // bar keeps the shape and the surrounding editing keys the original author designed.
            MaScopeCanvas(active = isRecording, tint = onAccent)
            // The clock lives in the meter's own readings row now, centred and bold. Keeping this
            // second one would put two timers on one button, disagreeing by a frame or two.
            // Long-form segments in flight still need saying, and nothing else reports them.
            if (recording != null && segmentsInFlight > 0) {
                MaSegmentsBadge(
                    count = segmentsInFlight,
                    tint = onAccent,
                    modifier = Modifier.align(Alignment.TopEnd).padding(end = 14.dp, top = 2.dp),
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                when {
                    recording != null -> Unit
                    rewording != null -> {
                        // Reworded, not transcribed: show the rewording label (prompt name / "Rewording…").
                        // A trailing stop icon signals a tap cancels the request (issue #192).
                        MaBrailleSpinner(color = onAccent, fontSize = 18.sp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(text = rewording.label.ifBlank { stringRes(R.string.dictate__status_rewording) }, color = onAccent)
                        Spacer(modifier = Modifier.width(10.dp))
                        Icon(Icons.Default.Stop, contentDescription = null, tint = onAccent, modifier = Modifier.size(20.dp))
                    }
                    busy -> {
                        // Voice Type: braille spinner instead of the material ring.
                        MaBrailleSpinner(color = onAccent, fontSize = 18.sp)
                        Spacer(modifier = Modifier.width(10.dp))
                        // Voice Type: say what is actually happening, not just that something is.
                        val maLine by DictateController.maStatus.collectAsState()
                        Text(
                            text = maLine.ifBlank { stringRes(R.string.dictate__status_transcribing) },
                            color = onAccent,
                            fontSize = MaStatusFontSize,
                            fontFamily = MaStatusFontFamily,
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Icon(Icons.Default.Stop, contentDescription = null, tint = onAccent, modifier = Modifier.size(20.dp))
                    }
                    else -> {
                        // One microphone, centred, and nothing else. The word "Record" restated what
                        // the icon and the red ring already say twice over, and the folder glyph
                        // advertised a long press that a glyph cannot explain anyway. Long press
                        // still picks a file; it just no longer needs a label to prove it exists.
                        Icon(
                            Icons.Default.Mic,
                            contentDescription = stringRes(R.string.dictate__legacy_record),
                            tint = onAccent,
                            modifier = Modifier.size(26.dp),
                        )
                    }
                }
            }
        }

        // Right slot: backspace when idle; while recording, the "Next segment" cut button in long-form
        // mode (#170, replacing pause — pausing is redundant there), otherwise pause/resume.
        when {
            recording != null && segmented -> {
                ThemedKey(
                    code = KeyCode.NOOP,
                    modifier = sideKey.scale(1f + nextFlash.value * 0.3f),
                    onClick = { DictateController.flushSegment(context) },
                ) { fg ->
                    Icon(
                        imageVector = Icons.Default.FastForward,
                        contentDescription = stringRes(R.string.dictate__action_next_segment),
                        tint = lerp(fg, accent, nextFlash.value),
                        modifier = Modifier.size(24.dp),
                    )
                }
            }
            recording != null -> {
                // Pause, and beside it the way out. A recording that now keeps running while the
                // keyboard is nowhere in sight needs a stop that does not depend on the rest of the
                // machinery behaving: this one releases the microphone and ends the dictation
                // whatever state it thinks it is in.
                ThemedIconKey(
                    code = KeyCode.NOOP,
                    icon = if (recording.paused) Icons.Default.PlayArrow else Icons.Default.Pause,
                    contentDescription = stringRes(
                        if (recording.paused) R.string.dictate__action_resume else R.string.dictate__action_pause,
                    ),
                    modifier = sideKey,
                    onClick = { DictateController.togglePause() },
                )
            }
            else -> LegacyBackspaceKey(modifier = sideKey)
        }
    }
}

/** Bottom row: switch-keyboard · space (cursor-move swipe) · enter. */
@Composable
private fun LegacyBottomRow(
    modifier: Modifier,
    keyboardManager: KeyboardManager,
    onExitToKeyboard: (() -> Unit)? = null,
) {
    val prefs by FlorisPreferenceStore
    val scope = rememberCoroutineScope()
    val openingView by prefs.dictate.maOpeningView.collectAsState()
    val sideKey = Modifier.fillMaxHeight().aspectRatio(1f)
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // The corner, where a left thumb reaches it without the hand moving.
        //
        // The little man's switch was here and is gone. It was a third way to do one thing: his row
        // is shared by both views, so it belongs in the dashboard with every other show-and-hide
        // switch rather than having a key of its own on one screen. One switch in one place beats
        // the same switch in three.
        //
        // The pin takes the corner, and settings has gone from this view.
        //
        // Settings was here on the reasoning that it is opened rarely and deliberately — which is
        // exactly the argument for it NOT having a key on the screen he uses most. It is two taps
        // away on the feature row and in the app itself, and the corner of a screen this size is
        // worth more than a third route to it.
        //
        // What earns the corner is the pin: make this the view the keyboard opens on. He dictates
        // far more than he types, so the keyboard is his secondary screen, and until now every
        // single open began on the wrong one.
        //
        // Filled when pinned, outlined when not — the pair used everywhere in this app — and it can
        // always be unpinned from the same corner it was pinned in, which is what stops a default
        // that opens somewhere unexpected from being a trap.
        val pinnedHere = openingView == "dictation"
        ThemedIconKey(
            code = KeyCode.NOOP,
            icon = if (pinnedHere) Icons.Default.PushPin else Icons.Outlined.PushPin,
            contentDescription = if (pinnedHere) {
                "Stop opening on this view"
            } else {
                "Open the keyboard on this view"
            },
            modifier = sideKey,
            onClick = {
                scope.launch {
                    prefs.dictate.maOpeningView.set(if (pinnedHere) "keyboard" else "dictation")
                }
            },
        )

        // The way back to the typing keyboard, which this view did not have.
        //
        // It is the most obvious thing somebody needs on a screen that has replaced their keyboard,
        // and it was reachable only from a key on the feature row — which is drawn on the keyboard
        // this view replaces. The way out was on the other side of the door.
        ThemedIconKey(
            code = KeyCode.NOOP,
            icon = Icons.Default.Keyboard,
            contentDescription = stringRes(R.string.dictate__legacy_action_keyboard),
            modifier = sideKey,
            onClick = {
                onExitToKeyboard?.invoke()
                    ?: run { keyboardManager.activeState.imeUiMode = ImeUiMode.TEXT }
            },
        )

        LegacySpaceKey(
            keyboardManager = keyboardManager,
            modifier = Modifier.weight(1f).fillMaxHeight(),
        )

        // Enter: tap inserts a newline; long-press opens the character popup (#196). Carries the ENTER code
        // so the theme paints it with its usual accent (as on the keyboard).
        LegacyEnterKey(
            keyboardManager = keyboardManager,
            modifier = sideKey,
        )
    }
}

/**
 * The bottom-row Enter key. A tap inserts a newline as usual; holding it opens a small popup above the key
 * showing the user's configured characters (Settings → Dictation layout → "Enter key characters", up to
 * 8). While held, swiping left/right moves the highlight; releasing inserts the highlighted character.
 * This reproduces the character picker from the very first Dictate versions (issue #196). With no
 * characters configured the long-press falls back to a normal Enter.
 *
 * Internal, because the feature row draws this same key. Marko drew an arrow from the bottom row's
 * Enter down to the feature row, and that is what he meant: this key, not a second one that types a
 * newline. The popup opens leftward from the key, which works from either position.
 */
@Composable
internal fun LegacyEnterKey(
    keyboardManager: KeyboardManager,
    modifier: Modifier,
) {
    val prefs by FlorisPreferenceStore
    val feedback = LocalInputFeedbackController.current
    val accent by prefs.theme.accentColor.collectAsState()
    val charsRaw by prefs.dictate.enterLongPressChars.collectAsState()
    val chars = remember(charsRaw) { parseEnterChars(charsRaw) }

    var showPopup by remember { mutableStateOf(false) }
    var selectedIndex by remember { mutableIntStateOf(0) }

    val style = rememberSnyggThemeQuery(FlorisImeUi.Key.elementName, keyAttributes(KeyCode.ENTER))
    val bg = style.background(default = Color.White.copy(alpha = 0.08f))
    val fg = style.foreground(default = Color.White)

    Box(
        modifier = modifier
            .padding(horizontal = KeyMarginH, vertical = KeyMarginV)
            .clip(LegacyKeyShape)
            .background(bg)
            .pointerInput(chars) {
                // One cell per ~cell-width of travel, so the highlight tracks the finger 1:1.
                val stepPx = 36.dp.toPx()
                awaitEachGesture {
                    val down = awaitFirstDown()
                    val startX = down.position.x
                    var released = false
                    // Phase 1: tap vs. hold. A release within the long-press window is a plain Enter.
                    withTimeoutOrNull(350L) {
                        while (true) {
                            val change = awaitPointerEvent().changes.firstOrNull() ?: return@withTimeoutOrNull
                            if (!change.pressed) {
                                released = true
                                return@withTimeoutOrNull
                            }
                        }
                    }
                    if (released) {
                        keyboardManager.tapKey(KeyCode.ENTER)
                        feedback.keyPress()
                        return@awaitEachGesture
                    }
                    if (chars.isEmpty()) {
                        // Nothing configured: wait for release, then behave like a normal Enter.
                        while (true) {
                            val change = awaitPointerEvent().changes.firstOrNull() ?: break
                            if (!change.pressed) break
                        }
                        keyboardManager.tapKey(KeyCode.ENTER)
                        feedback.keyPress()
                        return@awaitEachGesture
                    }
                    // Phase 2: popup open. The Enter key sits at the right edge and the popup extends left
                    // from it, so the highlight starts on the rightmost cell (under the finger) and each
                    // step of leftward travel walks it one cell left; swiping back right returns toward it.
                    val lastIndex = chars.size - 1
                    selectedIndex = lastIndex
                    showPopup = true
                    feedback.keyPress()
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull() ?: break
                        if (!change.pressed) break
                        val dx = change.position.x - startX
                        val idx = (lastIndex + (dx / stepPx).roundToInt()).coerceIn(0, lastIndex)
                        if (idx != selectedIndex) {
                            selectedIndex = idx
                            feedback.keyPress()
                        }
                        // Consume so the panel's swipe-to-switch gesture stands aside (see legacySwipeToggle).
                        change.consume()
                    }
                    showPopup = false
                    chars.getOrNull(selectedIndex)?.let { ch ->
                        ic()?.commitText(ch, 1)
                        feedback.keyPress()
                    }
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardReturn,
            contentDescription = stringRes(R.string.dictate__legacy_enter),
            tint = fg,
            modifier = Modifier.size(22.dp),
        )
        if (showPopup) {
            EnterCharPopup(chars = chars, selectedIndex = selectedIndex, accent = accent)
        }
    }
}

/** The floating character strip shown above the Enter key while it is long-pressed. */
@Composable
private fun EnterCharPopup(
    chars: List<String>,
    selectedIndex: Int,
    accent: Color,
) {
    val onAccent = if (accent.luminance() > 0.5f) Color.Black else Color.White
    val positionProvider = remember {
        object : PopupPositionProvider {
            override fun calculatePosition(
                anchorBounds: IntRect,
                windowSize: IntSize,
                layoutDirection: LayoutDirection,
                popupContentSize: IntSize,
            ): IntOffset {
                val x = (anchorBounds.left + anchorBounds.width / 2 - popupContentSize.width / 2)
                    .coerceIn(0, (windowSize.width - popupContentSize.width).coerceAtLeast(0))
                val gap = anchorBounds.height / 6
                val y = (anchorBounds.top - popupContentSize.height - gap).coerceAtLeast(0)
                return IntOffset(x, y)
            }
        }
    }
    Popup(popupPositionProvider = positionProvider) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF2B2B2B))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            chars.forEachIndexed { i, ch ->
                val selected = i == selectedIndex
                Box(
                    modifier = Modifier
                        .size(width = 34.dp, height = 40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (selected) accent else Color.Transparent),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = ch,
                        color = if (selected) onAccent else Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 18.sp,
                    )
                }
            }
        }
    }
}

/** Splits the configured string into up to 8 individual characters (by code point), skipping whitespace. */
fun parseEnterChars(raw: String): List<String> {
    if (raw.isEmpty()) return emptyList()
    val out = ArrayList<String>(8)
    var i = 0
    while (i < raw.length && out.size < 8) {
        val cp = raw.codePointAt(i)
        val s = String(Character.toChars(cp))
        if (!s[0].isWhitespace()) out.add(s)
        i += Character.charCount(cp)
    }
    return out
}

/**
 * A themed surface (theme `key` colours + uniform corners) that hosts a custom pointer gesture instead
 * of a click – used by the space and backspace keys.
 */
@Composable
private fun GestureKey(
    code: Int,
    icon: ImageVector,
    contentDescription: String,
    modifier: Modifier,
    gesture: Modifier,
) {
    val style = rememberSnyggThemeQuery(FlorisImeUi.Key.elementName, keyAttributes(code))
    val bg = style.background(default = Color.White.copy(alpha = 0.08f))
    val fg = style.foreground(default = Color.White)
    Box(
        modifier = modifier.padding(horizontal = KeyMarginH, vertical = KeyMarginV).clip(LegacyKeyShape).background(bg).then(gesture),
        contentAlignment = Alignment.Center,
    ) {
        Icon(imageVector = icon, contentDescription = contentDescription, tint = fg, modifier = Modifier.size(22.dp))
    }
}

/**
 * Space key with the legacy cursor-move gesture: tap inserts a space, swiping horizontally moves the
 * caret via arrow-key events (never inserting or deleting). Small ~10dp steps make it responsive.
 */
@Composable
private fun LegacySpaceKey(
    keyboardManager: KeyboardManager,
    modifier: Modifier,
) {
    val feedback = LocalInputFeedbackController.current
    val gesture = Modifier.pointerInput(Unit) {
        val stepPx = 10.dp.toPx()
        awaitEachGesture {
            val down = awaitFirstDown()
            var lastX = down.position.x
            var swiped = false
            while (true) {
                val event = awaitPointerEvent()
                val change = event.changes.firstOrNull() ?: break
                if (change.pressed) {
                    val dx = change.position.x - lastX
                    if (dx > stepPx) {
                        keyboardManager.tapKey(KeyCode.ARROW_RIGHT)
                        lastX = change.position.x
                        swiped = true
                        feedback.keyPress()
                        change.consume()
                    } else if (dx < -stepPx) {
                        keyboardManager.tapKey(KeyCode.ARROW_LEFT)
                        lastX = change.position.x
                        swiped = true
                        feedback.keyPress()
                        change.consume()
                    }
                } else {
                    if (!swiped) {
                        keyboardManager.tapKey(KeyCode.SPACE)
                        feedback.keyPress()
                    }
                    break
                }
            }
        }
    }
    GestureKey(KeyCode.SPACE, Icons.Default.SpaceBar, stringRes(R.string.dictate__legacy_space), modifier, gesture)
}

/**
 * Backspace key with the legacy gestures: a tap deletes one character, holding auto-repeats the delete,
 * and swiping left progressively selects text (whole words or single characters) which is deleted on
 * release. Whether the swipe works by words or characters follows the same global setting the modern
 * keyboard uses (Settings → Gestures → "Delete key swipe left"), so both layouts behave identically.
 */
@Composable
private fun LegacyBackspaceKey(modifier: Modifier) {
    val context = LocalContext.current
    val keyboardManager by context.keyboardManager()
    val prefs by FlorisPreferenceStore
    val feedback = LocalInputFeedbackController.current
    val gesture = Modifier.pointerInput(Unit) {
        val activationPx = 12.dp.toPx()
        awaitEachGesture {
            val down = awaitFirstDown()
            val startX = down.position.x
            // Word- vs character-granularity for the swipe, shared with the modern keyboard's setting.
            val wordMode = when (prefs.gestures.deleteKeySwipeLeft.get()) {
                SwipeAction.DELETE_WORD,
                SwipeAction.DELETE_WORDS_PRECISELY,
                SwipeAction.SELECT_WORDS_PRECISELY -> true
                else -> false
            }
            val stepPx = (if (wordMode) 24.dp else 12.dp).toPx()
            var mode = 0 // 0 = undecided, 1 = swipe-select, 2 = hold-repeat

            while (mode == 0) {
                val event = withTimeoutOrNull(350L) { awaitPointerEvent() }
                if (event == null) {
                    mode = 2
                    break
                }
                val change = event.changes.firstOrNull() ?: return@awaitEachGesture
                if (!change.pressed) {
                    keyboardManager.tapKey(KeyCode.DELETE)
                    feedback.keyPress()
                    return@awaitEachGesture
                }
                if (change.position.x - startX < -activationPx) {
                    mode = 1
                    change.consume()
                }
            }

            if (mode == 2) {
                keyboardManager.tapKey(KeyCode.DELETE)
                feedback.keyPress()
                while (true) {
                    val event = withTimeoutOrNull(55L) { awaitPointerEvent() }
                    if (event == null) {
                        keyboardManager.tapKey(KeyCode.DELETE)
                        feedback.keyPress()
                        continue
                    }
                    val change = event.changes.firstOrNull() ?: return@awaitEachGesture
                    if (!change.pressed) return@awaitEachGesture
                }
            }

            // mode == 1: swipe-select (whole words or single characters, per [wordMode]); delete on release.
            var base = -1
            var boundaries: List<Int> = emptyList()
            var steps = 0
            ic()?.let { conn ->
                val et = conn.getExtractedText(ExtractedTextRequest(), 0)
                val text = et?.text
                if (text != null) {
                    base = maxOf(et.selectionStart, et.selectionEnd)
                    boundaries = if (wordMode) {
                        computeWordBoundaries(text.subSequence(0, base).toString())
                    } else {
                        // One boundary per character back to the start, so each step selects one more char.
                        (base downTo 0).toList()
                    }
                }
            }
            if (boundaries.isEmpty()) {
                boundaries = listOf(0)
                base = 0
            }
            while (true) {
                val event = awaitPointerEvent()
                val change = event.changes.firstOrNull() ?: break
                val dx = change.position.x - startX
                if (!change.pressed) {
                    if (steps > 0) {
                        ic()?.commitText("", 1)
                        feedback.keyPress()
                    } else if (base >= 0) {
                        ic()?.setSelection(base, base)
                    }
                    break
                }
                val maxSteps = boundaries.size - 1
                val s = ((-dx) / stepPx).toInt().coerceIn(0, maxSteps)
                if (s != steps) {
                    steps = s
                    ic()?.setSelection(boundaries[s], base)
                    feedback.keyPress()
                }
                change.consume()
            }
        }
    }
    GestureKey(KeyCode.DELETE, Icons.Default.Backspace, stringRes(R.string.dictate__legacy_backspace), modifier, gesture)
}

/** Sentinel tags for the non-character number-pad keys. */
private const val NUMPAD_SPACE = " space"
private const val NUMPAD_DELETE = " delete"
private const val NUMPAD_ENTER = " enter"

private val NUMPAD_ROWS = listOf(
    listOf("1", "2", "3", "-"),
    listOf("4", "5", "6", NUMPAD_SPACE),
    listOf("7", "8", "9", NUMPAD_DELETE),
    listOf(",", "0", ".", NUMPAD_ENTER),
)

/** Full-panel 4×4 number pad overlay, reproducing `[1 2 3 −][4 5 6 ␣][7 8 9 ⌫][, 0 . ✓]`. */
@Composable
private fun LegacyNumberPadOverlay(onClose: () -> Unit) {
    val context = LocalContext.current
    val keyboardManager by context.keyboardManager()
    SnyggColumn(
        elementName = FlorisImeUi.Media.elementName,
        modifier = Modifier.fillMaxWidth().height(FlorisImeSizing.imeUiLayoutHeight()).padding(horizontal = KeyMarginH, vertical = KeyMarginV),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(FlorisImeSizing.smartbarHeight),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SnyggText(
                elementName = FlorisImeUi.MediaEmojiSubheader.elementName,
                modifier = Modifier.weight(1f).padding(start = KeyMarginH * 2),
                text = stringRes(R.string.dictate__legacy_numbers_title),
            )
            ThemedIconKey(
                code = KeyCode.NOOP,
                icon = Icons.Default.Close,
                contentDescription = stringRes(R.string.dictate__legacy_close),
                modifier = Modifier.fillMaxHeight().aspectRatio(1f),
                onClick = onClose,
            )
        }
        Column(modifier = Modifier.fillMaxWidth().weight(1f)) {
            NUMPAD_ROWS.forEach { row ->
                Row(modifier = Modifier.fillMaxWidth().weight(1f)) {
                    row.forEach { key ->
                        val keyMod = Modifier.weight(1f).fillMaxHeight()
                        when (key) {
                            NUMPAD_SPACE -> ThemedIconKey(KeyCode.SPACE, Icons.Default.SpaceBar, stringRes(R.string.dictate__legacy_space), keyMod) { keyboardManager.tapKey(KeyCode.SPACE) }
                            // Full backspace behaviour here too (tap / hold-repeat / swipe-select), like the record row.
                            NUMPAD_DELETE -> LegacyBackspaceKey(modifier = keyMod)
                            NUMPAD_ENTER -> ThemedIconKey(KeyCode.ENTER, Icons.AutoMirrored.Filled.KeyboardReturn, stringRes(R.string.dictate__legacy_enter), keyMod) { keyboardManager.tapKey(KeyCode.ENTER) }
                            // Commit through the input pipeline (like the other keys) rather than raw
                            // InputConnection.commitText: the latter replaces the suggestion engine's active
                            // composing region, so each digit clobbered the previous one instead of appending.
                            else -> ThemedKey(code = key[0].code, modifier = keyMod, onClick = { keyboardManager.tapKey(key[0].code) }) { fg ->
                                Text(text = key, color = fg, fontWeight = FontWeight.SemiBold, fontSize = 20.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Computes absolute caret indices (0..cursor) for the backspace swipe-select, ported verbatim from the
 * legacy `DictateInputMethodService`: `boundaries[0]` = cursor, each further entry moves the selection
 * start back by one "space + word", so N swipe steps select N words.
 */
private fun computeWordBoundaries(before: String): List<Int> {
    val res = ArrayList<Int>()
    var pos = before.length
    res.add(pos)
    while (pos > 0) {
        var i = pos
        while (i > 0 && before[i - 1].isWhitespace()) i--
        while (i > 0 && !before[i - 1].isLetterOrDigit() && !before[i - 1].isWhitespace()) i--
        while (i > 0 && before[i - 1].isLetterOrDigit()) i--
        while (i > 0 && before[i - 1].isWhitespace()) i--
        if (i == pos) i--
        pos = i
        res.add(pos)
    }
    return res
}

private fun formatElapsed(ms: Long): String {
    val totalSec = (ms / 1000L).coerceAtLeast(0L)
    return "%d:%02d".format(totalSec / 60L, totalSec % 60L)
}

// ---------------------------------------------------------------------------------------
// Voice Type additions. Both live inside the original record button rather than replacing
// anything around it: the layout the upstream author built here works, and the editing keys
// surrounding this button stay useful precisely because voice typing still needs editing.
// ---------------------------------------------------------------------------------------

private const val MA_BRAILLE = "\u280B\u2819\u2839\u2838\u283C\u2834\u2826\u2827\u2807\u280F"

/**
 * The braille spinner, in place of a material ring, while a request is in flight.
 *
 * It also has to prove it is alive. A spinner that stops looks identical to an app that has died,
 * so after [BLINK_AFTER_MS] of the same request it starts pulsing its opacity as well as turning:
 * a still frame then reads as waiting rather than as frozen, and the line beside it says what for.
 */
@Composable
internal fun MaBrailleSpinner(color: Color, fontSize: androidx.compose.ui.unit.TextUnit) {
    var frame by remember { mutableIntStateOf(0) }
    var waitedMs by remember { mutableLongStateOf(0L) }
    LaunchedEffect(Unit) {
        while (true) {
            frame++
            waitedMs += 90L
            delay(90L)
        }
    }
    val longWait = waitedMs > BLINK_AFTER_MS
    val blink by animateFloatAsState(
        targetValue = if (longWait && (frame / 6) % 2 == 0) 0.35f else 1f,
        animationSpec = tween(420),
        label = "maBlink",
    )
    Text(
        text = MA_BRAILLE[frame % MA_BRAILLE.length].toString(),
        color = color.copy(alpha = blink),
        fontSize = fontSize,
        fontWeight = FontWeight.SemiBold,
    )
}

/** After this long on one request the spinner starts blinking, so a pause never reads as a crash. */
private const val BLINK_AFTER_MS = 6_000L

/** How many long-form segments are transcribing in the background. Nothing else reports this. */
@Composable
private fun MaSegmentsBadge(count: Int, tint: Color, modifier: Modifier = Modifier) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = Icons.Default.Sync,
            contentDescription = null,
            tint = tint.copy(alpha = 0.7f),
            modifier = Modifier.size(11.dp),
        )
        Spacer(modifier = Modifier.width(2.dp))
        Text(text = "$count", color = tint.copy(alpha = 0.7f), fontSize = 10.sp)
    }
}

/** Near-black, so the record key sits with the other keys instead of on top of them. */
private val MaRecordFill = Color(0xFF11151C)

/** The red edge that means recording. The one place red is used, so it always means one thing. */
private val MaRecordRing = Color(0xFF9B3B33)

/** Warm gold for everything printed on the record key. */
internal val MaRecordInk = Color(0xFFE8B15C)

/**
 * The status line's type: small, monospaced, gold.
 *
 * This line is machine output. It reports sizes, elapsed seconds, transfer rates and which key is
 * being tried, and monospace is what that kind of reading wants: the digits stop shifting sideways
 * as they change, so a number that is growing reads as growing rather than as the whole line
 * twitching. Shared by both views so the keyboard view cannot drift from the transcribe view again.
 */
internal val MaStatusFontSize = 13.sp
internal val MaStatusFontFamily = FontFamily.Monospace

/**
 * The switcher ring: the ordinary ink, held back.
 *
 * Not a colour of its own. Colour is the state channel — green means a switcher is on — and a second
 * colour meaning "this is a switcher" would give one channel two jobs. Low alpha so a row of them
 * reads as a set rather than as an alarm.
 */
/**
 * How long a press has to last before it counts as a hold.
 *
 * Longer than a tap and shorter than a thought. Without it every tap would fire one arrow before
 * the tap handler ran — the repeat starts after this, so a tap never reaches it.
 */
private const val HOLD_START_MS = 350L

/**
 * How fast the cursor runs while held. His number.
 *
 * Fast enough to cross a sentence without waiting, slow enough to stop on a word — about nine
 * characters a second, which is reading speed rather than typing speed.
 */
private const val REPEAT_MS = 111L

private val MaSwitcherRing = Color(0xFFF2DDB4).copy(alpha = 0.55f)

/**
 * The switcher ring when the thing it switches is OFF.
 *
 * Cream, the app's ink colour, against the green of on. It is the same ring in a different colour
 * rather than an absent ring, so the key does not change size or weight when the zone closes — a row
 * where a border appears and disappears is a row that flickers as he works.
 *
 * Public because the feature row draws these three keys and the ring lives here with the key it
 * belongs to.
 */
val MaSwitcherRingOff = Color(0xFFF2DDB4).copy(alpha = 0.55f)
