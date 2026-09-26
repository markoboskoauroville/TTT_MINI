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

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.Composable
import dev.patrickgold.florisboard.lib.compose.FlorisScreen

/**
 * THE NUMBER ROW: what sits behind each digit.
 *
 * ### Why this screen exists when the editor already did
 *
 * `MaNumericSecondarySetting` has been written and working for weeks. It was inside
 * `DictateLayoutScreen`, and **`DictateLayoutScreen` has no entry in the settings list** — so the
 * only way to reach it was a deep link nobody types. He asked for this as a new feature because from
 * where he sits it did not exist.
 *
 * That is the third time this month: build 369's route crashed for a missing annotation, the
 * Claude.ai reader was missing from `DEFAULT`, and this was built and never given a door. **A
 * feature that compiles, passes its tests and cannot be opened is not a feature.**
 *
 * ### Why its own entry rather than adding "Layout" to the list
 *
 * `DictateLayoutScreen` also holds the prompt-row count and the enter long-press characters —
 * inherited settings he has never mentioned. Putting all of it in his menu to reach one of them
 * would be handing him three doors to find one room.
 *
 * The number row is the thing he asked for, so the number row is the entry.
 */
@Composable
fun MaNumberRowScreen() = FlorisScreen {
    title = "Number row"

    content {
        Text(
            text = "The row the hash key shows. Each digit can carry one more character, reached by " +
                "a long press \u2014 a slash, a bracket, a quote, anything but a number or a letter, " +
                "which the digits already have.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )

        // The editor itself, unchanged. It was never the problem — it just had no door.
        MaNumericSecondarySetting()
    }
}
