/*
 * Copyright (C) 2026 Marko Bosko, Mantra Productions
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package dev.patrickgold.florisboard.dictate.ui

import android.content.Context
import android.widget.Toast
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.KeyboardTab
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import dev.patrickgold.florisboard.app.FlorisPreferenceStore
import dev.patrickgold.florisboard.dictate.MaCaseCycle
import dev.patrickgold.florisboard.dictate.MaReader
import dev.patrickgold.florisboard.dictate.overlay.DictateAccessibilityService
import dev.patrickgold.florisboard.editorInstance
import dev.patrickgold.florisboard.ime.text.key.KeyCode
import dev.patrickgold.jetpref.datastore.model.collectAsState
import kotlinx.coroutines.launch

/**
 * Keys as modules: one composable each, placeable on any surface.
 *
 * ### Why this file exists
 *
 * Every key in this app was written inside the row it belonged to — a single `when` of 556 lines
 * closing over eighteen locals in `MaFeatureRow`. That made each key a piece of that row rather than
 * a thing in itself, so asking for a key on the transcription view meant asking for it to be built a
 * second time.
 *
 * A key here takes what it needs as arguments and nothing else. It can be drawn by the feature row,
 * the transcription view, a future row nobody has thought of, or a test. **Lego, not a moulded
 * shell**: the same blocks make a castle or an aeroplane because a block does not know what it is
 * part of.
 *
 * ### How this is being done, and why it is slow on purpose
 *
 * He uses this keyboard every day, so the row is not being rewritten. Keys move out one at a time,
 * each move behaviour-neutral, and the row keeps working throughout. These three came first because
 * they depend on the least: a context, and in one case the editor.
 *
 * **The rule for the ones still to come:** a key module may take a `Context`, an `EditorInstance`, a
 * `KeyboardManager` or a preference — things that exist everywhere. It may not take the feature
 * row's own state. Where a key seems to need that, the state belongs somewhere shared, and moving it
 * there is part of the job rather than a reason to leave the key where it is.
 */

/**
 * TAB: focus the next editable field on screen.
 *
 * Needs only a context, to open accessibility settings when the service is off and to say so when
 * the screen holds nothing else to focus.
 */
@Composable
fun MaNextFieldKey(
    modifier: Modifier,
    context: Context,
    backwards: () -> Boolean = { false },
) {
    ThemedIconKey(
        code = KeyCode.NOOP,
        icon = Icons.Default.KeyboardTab,
        contentDescription = "Next field",
        modifier = modifier,
        tint = null,
    ) {
        if (!DictateAccessibilityService.isRunning) {
            maOpenAccessibilitySettings(context)
        } else if (!DictateAccessibilityService.focusNextField(backwards())) {
            Toast.makeText(context, "No other field on this screen", Toast.LENGTH_SHORT).show()
        }
    }
}

/**
 * Aa: cycle the case of the selection, or of the whole field.
 *
 * Takes the editor rather than reaching for it, so the same key can be pointed at a different editor
 * — which is what a module is for.
 */
@Composable
fun MaCaseKey(
    modifier: Modifier,
    context: Context,
) {
    val editorInstance by context.editorInstance()
    ThemedTextKey(
        label = "Aa",
        modifier = modifier,
        tint = null,
    ) {
        val content = editorInstance.activeContent
        val selected = content.selectedText
        if (selected.isNotEmpty()) {
            MaCaseCycle.next(selected)?.let { editorInstance.commitText(it) }
        } else {
            val whole = buildString {
                append(content.textBeforeSelection)
                append(content.textAfterSelection)
            }
            MaCaseCycle.next(whole)?.let { next ->
                editorInstance.setSelection(0, whole.length)
                editorInstance.commitText(next)
            }
        }
    }
}

/**
 * S: show or hide the subtitle row.
 *
 * Reads and writes its own preference and takes nothing else, which makes it the simplest kind of
 * module — droppable anywhere without the surface knowing what a subtitle is.
 */
@Composable
fun MaSubtitleToggleKey(
    modifier: Modifier,
    litColor: Color,
) {
    val prefs by FlorisPreferenceStore
    val scope = rememberCoroutineScope()
    val display by prefs.dictate.maReaderDisplay.collectAsState()
    val on = display == "subtitle"
    ThemedTextKey(
        label = "S",
        modifier = modifier,
        // Lit when on, because the row it controls is blank between sentences — without this a
        // press would look like it had done nothing at all.
        tint = if (on) litColor else null,
    ) {
        scope.launch { prefs.dictate.maReaderDisplay.set(if (on) "off" else "subtitle") }
    }
}

/**
 * The reader: speak the screen, pause, resume. Long press opens its settings.
 *
 * Takes a context and a way to say something, and nothing else. [MaReader] holds the state, so the
 * same key drawn on two surfaces shows the same thing without either surface tracking it.
 */
@Composable
fun MaReaderKey(
    modifier: Modifier,
    context: Context,
    litColor: Color,
    onOpenSettings: () -> Unit,
    onMessage: (String) -> Unit,
) {
    val state = MaReader.state
    ThemedIconKey(
        code = KeyCode.NOOP,
        // The face shows what the NEXT press does, not what is happening now. A key that describes
        // its own state leaves him working out what pressing it would achieve.
        icon = when (state) {
            MaReader.State.SPEAKING -> Icons.Default.Pause
            MaReader.State.PAUSED -> Icons.Default.PlayArrow
            else -> Icons.AutoMirrored.Filled.VolumeUp
        },
        contentDescription = when (state) {
            MaReader.State.SPEAKING -> "Pause reading"
            MaReader.State.PAUSED -> "Continue reading"
            MaReader.State.LOADING -> "Stop"
            else -> "Read this screen"
        },
        modifier = modifier,
        // Lit whenever it is doing something, so a synthesis that takes a moment does not look
        // like a press that missed.
        tint = if (state == MaReader.State.IDLE) null else litColor,
        onLongClick = onOpenSettings,
    ) {
        MaReader.toggle(context, onMessage)
    }
}

/**
 * The pin: keep the keyboard up when the app would close it.
 *
 * Reads and writes its own preference, so it carries no state from wherever it is placed. That is
 * what makes it droppable into any row without the row knowing what a pin is.
 */
@Composable
fun MaPinKey(
    modifier: Modifier,
    litColor: Color,
) {
    val prefs by FlorisPreferenceStore
    val scope = rememberCoroutineScope()
    val pinned by prefs.dictate.maKeyboardPinned.collectAsState()
    ThemedIconKey(
        code = KeyCode.NOOP,
        icon = if (pinned) Icons.Default.PushPin else Icons.Outlined.PushPin,
        contentDescription = if (pinned) "Unpin the keyboard" else "Pin the keyboard up",
        modifier = modifier,
        tint = if (pinned) litColor else null,
    ) {
        scope.launch { prefs.dictate.maKeyboardPinned.set(!pinned) }
    }
}
