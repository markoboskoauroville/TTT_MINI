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

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import dev.patrickgold.florisboard.app.FlorisPreferenceStore
import dev.patrickgold.jetpref.datastore.model.collectAsState
import kotlinx.coroutines.launch

/**
 * The pin that keeps the keyboard up, in the top-left corner of the rows.
 *
 * ### What it actually does
 *
 * Pinned, three things change, all of them in FlorisImeService. The input view is shown whenever
 * this IME is running rather than only when the system judges it necessary, which covers a hardware
 * keyboard being attached and the configurations where the on-screen view is treated as optional.
 * Fullscreen extract mode is refused, which is the most complete version of the keyboard vanishing:
 * the app's field is replaced by the system's own full-height editor and every row goes with it.
 *
 * ### What it cannot do, and this is not a limitation of the code
 *
 * It cannot hold the keyboard open over an app with no text field focused. Android owns that window
 * and takes it down when nothing is accepting input; there is no flag an input method can set to
 * refuse. Anything claiming otherwise on this screen would be a promise the app cannot keep, and a
 * pin that visibly fails is worse than no pin.
 *
 * What it removes is the collapsing and re-raising while moving between fields and apps that do take
 * input, which is where the time was actually going.
 *
 * ### Why it is here rather than a key on the row
 *
 * Every key on the row can be reordered, unticked and deleted. This one must not be, because it is
 * the control that recovers the situation it governs: somebody who removed the pin would have no way
 * to reach the pin to put it back. It is drawn over the first row's leading corner rather than in
 * it, so the row's own arrangement is untouched.
 */
@Composable
fun MaKeyboardPin(modifier: Modifier = Modifier) {
    val prefs by FlorisPreferenceStore
    val scope = rememberCoroutineScope()
    val pinned by prefs.dictate.maKeyboardPinned.collectAsState()

    Box(
        modifier = modifier
            .size(22.dp)
            .clickable { scope.launch { prefs.dictate.maKeyboardPinned.set(!pinned) } }
            .semantics {
                contentDescription = if (pinned) {
                    "Keyboard pinned up, tap to unpin"
                } else {
                    "Pin the keyboard up"
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            // Filled when pinned, outlined when not: the same pair the clipboard panel's pin uses,
            // so a pin means the same thing wherever it appears in this app.
            imageVector = if (pinned) Icons.Default.PushPin else Icons.Outlined.PushPin,
            contentDescription = null,
            // Quiet either way. It sits over the corner of a key row and must not read as a key
            // itself, and the state it reports is one somebody sets once and then forgets.
            tint = if (pinned) MaPinOn else MaPinOff,
            modifier = Modifier.size(14.dp),
        )
    }
}

private val MaPinOn = Color(0xFFB9A06B)
private val MaPinOff = Color(0x66FFFFFF)
