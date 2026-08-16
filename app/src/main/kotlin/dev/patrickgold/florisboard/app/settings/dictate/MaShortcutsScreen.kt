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

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.patrickgold.florisboard.lib.compose.FlorisScreen

/**
 * Every Ctrl shortcut the keyboard understands, written down.
 *
 * ### Why a list and not a settings screen
 *
 * Nothing here is configurable, and that is honest rather than unfinished. These are bound in
 * `KeyboardManager.maHandleCtrlCombo`, and the six that are handled there go to editor operations
 * that understand the composing region and the phantom space — remapping them from a screen would
 * mean routing them somewhere that does not.
 *
 * What was missing was not configuration but knowledge. The Ctrl key sits on the bottom row of every
 * layout and nothing anywhere said what it did with a letter, so the shortcuts existed and were
 * unusable by anyone who had not read the source.
 *
 * ### The rows are the code
 *
 * This list was written by reading `maHandleCtrlCombo`, not from memory of what such a keyboard
 * usually does. If a shortcut is added there, add it here in the same commit; a list of shortcuts
 * that is wrong is worse than no list, because it is believed.
 */
@Composable
fun MaShortcutsScreen() = FlorisScreen {
    title = "Keyboard shortcuts"

    content {
        Text(
            text = "Tap Ctrl on the bottom row, then a letter. Ctrl turns itself off after one " +
                "letter — hold it instead to lock it on for several.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )

        Spacer(Modifier.height(8.dp))

        Section("Writing")
        Shortcut("Ctrl + P", "Proofread", "Punctuation, spelling and grammar only. Your words and your style are left alone. Works on the selection, or on the whole field when nothing is selected.")
        Shortcut("Ctrl + F", "Better flow", "Rewrites for flow: cuts the repetition dictation leaves behind and puts the ideas in an order that follows. Keeps your voice, your facts and your language. This one moves your words \u2014 Ctrl + P never does.")

        Spacer(Modifier.height(16.dp))

        Section("Editing")
        Shortcut("Ctrl + Z", "Undo")
        Shortcut("Ctrl + Shift + Z", "Redo")
        Shortcut("Ctrl + Y", "Redo, the other way round")

        Spacer(Modifier.height(16.dp))

        Section("Clipboard")
        Shortcut("Ctrl + A", "Select all")
        Shortcut("Ctrl + C", "Copy")
        Shortcut("Ctrl + X", "Cut")
        Shortcut("Ctrl + V", "Paste")

        Spacer(Modifier.height(16.dp))

        Section("Everything else")
        Text(
            text = "Any other letter is sent on to the app you are typing in, as a real Ctrl press. " +
                "So Ctrl + B and Ctrl + I give bold and italics in an editor that understands them, " +
                "and do nothing at all in one that does not. A letter an app ignores is ignored — it " +
                "never types itself into your text.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        )

        Spacer(Modifier.height(16.dp))

        Section("Proofreading and flow need a key")
        Text(
            text = "Ctrl + P and Ctrl + F send your text to the rewording model set in API keys, so they need a " +
                "key with credit on it. Anthropic's Claude Sonnet is the one to choose for this: " +
                "it follows \"change only what is wrong\" more faithfully than the cheaper models, " +
                "which tend to improve your writing when you asked them to correct it.\n\n" +
                "Set it under API keys, as the rewording provider. Without a key, they show an " +
                "error with a button that takes you there.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        )

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun Section(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
    )
}

/**
 * One shortcut: the keys, what they do, and optionally why.
 *
 * The key combination is monospaced and heavier than the description, because it is the thing being
 * looked up. A list scanned for `Ctrl + V` should let the eye find `Ctrl + V` rather than read four
 * descriptions on the way past.
 */
@Composable
private fun Shortcut(keys: String, action: String, detail: String? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = keys,
            style = MaterialTheme.typography.bodyMedium,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.width(132.dp),
        )
        Spacer(Modifier.width(12.dp))
        androidx.compose.foundation.layout.Column(modifier = Modifier.weight(1f)) {
            Text(text = action, style = MaterialTheme.typography.bodyMedium)
            if (detail != null) {
                Text(
                    text = detail,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
