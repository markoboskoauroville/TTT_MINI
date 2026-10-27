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

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.app.FlorisPreferenceStore
import dev.patrickgold.florisboard.app.LocalNavController
import dev.patrickgold.florisboard.app.Routes
import dev.patrickgold.florisboard.app.enumDisplayEntriesOf
import dev.patrickgold.florisboard.app.settings.search.settingsSearchAnchor
import dev.patrickgold.florisboard.dictate.MaProviders
import dev.patrickgold.florisboard.dictate.provider.MaClaudeModels
import dev.patrickgold.florisboard.ime.smartbar.CandidatesDisplayMode
import dev.patrickgold.florisboard.lib.compose.FlorisScreen
import dev.patrickgold.jetpref.datastore.model.collectAsState
import dev.patrickgold.jetpref.datastore.ui.ListPreference
import dev.patrickgold.jetpref.datastore.ui.Preference
import dev.patrickgold.jetpref.datastore.ui.SwitchPreference
import kotlinx.coroutines.launch
import org.florisboard.lib.android.AndroidVersion
import org.florisboard.lib.compose.stringRes

/**
 * AUTO PREDICTION: EVERYTHING ABOUT THE SUGGESTION ROW, ON ONE SCREEN.
 *
 * Marko, 27.10.2026: "everything related to it should be under the auto prediction menu entry,
 * including the AI model we are using … model switching and all the other settings related to word
 * prediction should be under that one menu item only." It was on five: the row and next-word
 * prediction under Typing, the display mode under Smartbar, Learn my words under Dictate and on its
 * own entry, and the AI key's model nowhere (it was always the cheapest Claude).
 *
 * The switches first, then what adjusts them (MANTRA_MANIFEST design language §1.9): how the row
 * shows its words, the Claude model the AI key asks, what has been learned, and teaching it from
 * past dictations.
 *
 * ### The row, and why the switch fixes the flicker
 *
 * On means on: the row keeps its height whether or not it has anything in it. It used to be drawn
 * only when there was something to show, so an empty moment removed a strip and every key below
 * moved while he typed.
 *
 * ### The AI key
 *
 * The last key of the row asks Claude for five words when the local guesses are wrong; the one he
 * picks is taught to the local model (MaAiPredict). It uses the Anthropic key loaded in the app, the
 * same one Ctrl+P uses. The model is stored as a tier, so the newest Haiku, Sonnet or Opus is used
 * as Anthropic release them, and named here by its real name.
 */
@Composable
fun MaPredictionsScreen() = FlorisScreen {
    title = stringRes(R.string.settings__ma_predictions__title)

    content {
        val prefs by FlorisPreferenceStore
        val navController = LocalNavController.current

        // ---------------------------------------------------------------- on and off
        SwitchPreference(
            prefs.suggestion.enabled,
            modifier = Modifier.settingsSearchAnchor("pref__suggestion__enabled__label"),
            title = stringRes(R.string.pref__suggestion__enabled__label),
            summary = "On, the row is always there and keeps its height, so no key moves while you type.",
        )
        SwitchPreference(
            prefs.suggestion.nextWordPrediction,
            modifier = Modifier.settingsSearchAnchor("pref__suggestion__next_word_prediction__label"),
            title = stringRes(R.string.pref__suggestion__next_word_prediction__label),
            summary = stringRes(R.string.pref__suggestion__next_word_prediction__summary),
            enabledIf = { prefs.suggestion.enabled isEqualTo true },
        )
        SwitchPreference(
            prefs.suggestion.autoCorrect,
            modifier = Modifier.settingsSearchAnchor("pref__suggestion__auto_correct__label"),
            title = stringRes(R.string.pref__suggestion__auto_correct__label),
            summary = stringRes(R.string.pref__suggestion__auto_correct__summary),
            enabledIf = { prefs.suggestion.enabled isEqualTo true },
        )
        SwitchPreference(
            prefs.suggestion.multilingualTyping,
            modifier = Modifier.settingsSearchAnchor("pref__suggestion__multilingual_typing__label"),
            title = stringRes(R.string.pref__suggestion__multilingual_typing__label),
            summary = stringRes(R.string.pref__suggestion__multilingual_typing__summary),
            enabledIf = { prefs.suggestion.enabled isEqualTo true },
        )
        SwitchPreference(
            prefs.dictate.maNgramEnabled,
            title = "Learn my words",
            summary = "Suggests from what you have written on this phone, including names and words " +
                "no dictionary has, and learns a typo you correct. Never leaves the phone; nothing " +
                "is learned in incognito mode.",
        )
        SwitchPreference(
            prefs.dictate.maAiPredictKey,
            title = "AI key in the row",
            summary = "The last key of the row asks Claude for five words when the guesses are " +
                "wrong. The word you pick is taught to Learn my words. Each press is one request.",
            enabledIf = { prefs.suggestion.enabled isEqualTo true },
        )
        SwitchPreference(
            prefs.suggestion.api30InlineSuggestionsEnabled,
            modifier = Modifier.settingsSearchAnchor("pref__suggestion__api30_inline_suggestions_enabled__label"),
            title = stringRes(R.string.pref__suggestion__api30_inline_suggestions_enabled__label),
            summary = stringRes(R.string.pref__suggestion__api30_inline_suggestions_enabled__summary),
            visibleIf = { AndroidVersion.ATLEAST_API30_R },
        )

        // ---------------------------------------------------------------- how the words show
        Spacer(Modifier.height(8.dp))
        ListPreference(
            prefs.suggestion.displayMode,
            modifier = Modifier.settingsSearchAnchor("pref__suggestion__display_mode__label"),
            title = stringRes(R.string.pref__suggestion__display_mode__label),
            entries = enumDisplayEntriesOf(CandidatesDisplayMode::class),
            enabledIf = { prefs.suggestion.enabled isEqualTo true },
        )

        // ---------------------------------------------------------------- the AI key's model
        val aiKeyOn by prefs.dictate.maAiPredictKey.collectAsState()
        PredictionModelChooser(enabled = aiKeyOn)

        // ---------------------------------------------------------------- what was learned
        Text(
            text = "Learned on this phone",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 20.dp),
        )
        MaNgramSetting(showSwitch = false)
        Preference(
            title = "Teach it from my dictations",
            summary = "Adds the names and words from your dictation history to the keyboard's dictionary.",
            onClick = { navController.navigate(Routes.Settings.MaVocabulary) },
        )

        Text(
            text = "Suggestions follow the HR/ENG key, the same one that sets the transcription " +
                "language. One choice for both, so they cannot disagree.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
        )
    }
}

/**
 * The Claude model the AI key asks: a radio list with each model's real name, as on Prompts.
 * Claude only, because the AI key is paid with the Anthropic key loaded in the app.
 */
@Composable
private fun PredictionModelChooser(enabled: Boolean) {
    val prefs by FlorisPreferenceStore
    val scope = rememberCoroutineScope()
    val current by prefs.dictate.maPredictModel.collectAsState()
    val account = MaProviders.rewordingAccount()
    val cached = account.cachedModels
    val hasKey = account.apiKey.isNotBlank() || MaProviders.transcriptionAccount().apiKey.isNotBlank()
    val notes = mapOf(
        "haiku" to "the fastest and cheapest; right for five words",
        "sonnet" to "about 10× the price of Haiku",
        "opus" to "about 50× the price of Haiku",
    )

    Text(
        text = "AI key model",
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 20.dp),
    )
    Text(
        text = if (hasKey) "Paid with the Anthropic key loaded in the app (API keys and permissions)."
        else "No Anthropic key loaded. Add one under API keys and permissions; until then the AI key does nothing.",
        style = MaterialTheme.typography.bodySmall,
        color = if (hasKey) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
    )
    for (family in listOf("haiku", "sonnet", "opus")) {
        val alias = MaClaudeModels.alias(family)
        val name = MaClaudeModels.displayName(MaClaudeModels.resolve(alias, cached))
        val choose = { scope.launch { prefs.dictate.maPredictModel.set(alias) }; Unit }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = enabled, onClick = choose)
                .padding(horizontal = 8.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RadioButton(selected = current == alias, onClick = choose, enabled = enabled)
            Column {
                Text(text = name, style = MaterialTheme.typography.bodyLarge)
                Text(
                    text = notes.getValue(family),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
