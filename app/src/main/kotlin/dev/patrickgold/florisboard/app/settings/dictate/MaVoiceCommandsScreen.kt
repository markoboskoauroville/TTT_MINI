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

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.patrickgold.florisboard.app.FlorisPreferenceStore
import dev.patrickgold.florisboard.lib.compose.FlorisScreen
import dev.patrickgold.jetpref.datastore.ui.SwitchPreference

/**
 * Voice commands: the words that press instead of being typed.
 *
 * ### Why this screen exists before it can configure anything
 *
 * Nothing here is editable yet and that is on purpose. The rule is already running in every
 * dictation, and a rule that runs with nowhere written down is a rule nobody can check when it
 * surprises them. Marko asked for it to be documented somewhere, and somewhere means inside the app
 * rather than in a file on a laptop he does not carry.
 *
 * When the commands do become configurable, and when the hardware trigger arrives, they belong here
 * — which is the second reason to open the door now rather than later.
 */
@Composable
fun MaVoiceCommandsScreen() = FlorisScreen {
    title = "Voice commands"

    content {
        val prefs by FlorisPreferenceStore

        SwitchPreference(
            prefs.dictate.maVoiceCommands,
            title = "Voice commands",
            summary = "Off, everything you say is written as text and nothing is ever pressed.",
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = "Say the name of a button and it is pressed instead of typed. It uses the same " +
                "magic finger as the row on the keyboard, so anything the finger can press, a " +
                "command can press.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )

        Text(
            text = "The words that mean press",
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
        Text(
            text = "press  ·  pritisni  ·  stisni  ·  klikni",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(horizontal = 16.dp),
        )

        Spacer(Modifier.height(16.dp))

        Text(
            text = "Where a command may sit",
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
        Text(
            text = "At the END of what you say, or as the whole of it. Never in the middle — a " +
                "command in the middle of a sentence would send a message you had not finished " +
                "writing.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp),
        )

        Spacer(Modifier.height(12.dp))

        Text(
            text = "\"press send\"\n" +
                "→ presses send, types nothing\n\n" +
                "\"Hello Kerstin, I am late tonight. Press send.\"\n" +
                "→ types the message, then presses send\n\n" +
                "\"I will press send later\"\n" +
                "→ just words, nothing is pressed",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(horizontal = 16.dp),
        )

        Spacer(Modifier.height(16.dp))

        Text(
            text = "Pause before the command",
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
        Text(
            text = "A small pause before saying the command is what tells it apart from a sentence " +
                "about pressing something. The pause becomes a full stop in the transcription, and " +
                "the full stop is the signal. No pause means the words are simply typed, which is " +
                "the safe way for it to be wrong.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp),
        )

        Spacer(Modifier.height(16.dp))

        Text(
            text = "If nothing on screen has that name, the words are typed as usual. A command " +
                "that misses costs nothing.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp),
        )

        Spacer(Modifier.height(24.dp))
    }
}
