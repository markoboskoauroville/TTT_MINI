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

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import dev.patrickgold.florisboard.app.FlorisPreferenceStore
import dev.patrickgold.florisboard.dictate.MaClaudeCost
import dev.patrickgold.florisboard.lib.compose.FlorisScreen
import dev.patrickgold.jetpref.datastore.model.collectAsState
import kotlinx.coroutines.launch
import java.lang.System
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Claude costs: what is left, and what each request took.
 *
 * Anthropic does not tell an ordinary API key its remaining credit (MaClaudeCost says why), so the
 * balance is the one he reads in the Console and types here; the app subtracts every logged request
 * since and says when the number was entered. The log below it is exact per request: tokens from
 * Anthropic's own answer, times the published price.
 */
@Composable
fun MaClaudeCostScreen() = FlorisScreen {
    title = "Claude costs"

    content {
        val prefs by FlorisPreferenceStore
        val scope = rememberCoroutineScope()
        val balanceRaw by prefs.dictate.maClaudeBalanceUsd.collectAsState()
        val balanceAt by prefs.dictate.maClaudeBalanceAt.collectAsState()
        var refresh by remember { mutableStateOf(0) }
        val entries = remember(refresh) { MaClaudeCost.load() }
        val dayStart = remember(refresh) {
            java.util.Calendar.getInstance().apply {
                set(java.util.Calendar.HOUR_OF_DAY, 0); set(java.util.Calendar.MINUTE, 0)
                set(java.util.Calendar.SECOND, 0); set(java.util.Calendar.MILLISECOND, 0)
            }.timeInMillis
        }
        val monthStart = remember(refresh) {
            java.util.Calendar.getInstance().apply {
                set(java.util.Calendar.DAY_OF_MONTH, 1); set(java.util.Calendar.HOUR_OF_DAY, 0)
                set(java.util.Calendar.MINUTE, 0); set(java.util.Calendar.SECOND, 0)
                set(java.util.Calendar.MILLISECOND, 0)
            }.timeInMillis
        }
        val stamp = remember { SimpleDateFormat("d.M. HH:mm", Locale.getDefault()) }

        // ---------------------------------------------------------------- what is left
        Text(
            text = "Left on your Anthropic account",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
        val balance = balanceRaw.replace(',', '.').toDoubleOrNull()
        if (balance != null && balanceAt > 0L) {
            val left = MaClaudeCost.remaining(balance, balanceAt, entries)
            Text(
                text = "about ${usd(left)}",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            Text(
                text = "${usd(balance)} entered on ${stamp.format(Date(balanceAt))}, minus " +
                    "${usd(MaClaudeCost.spentSince(entries, balanceAt))} spent by this phone since. " +
                    "Other apps using the same account are not counted, so check the Console now and " +
                    "then and enter the new figure.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )
        } else {
            Text(
                text = "Anthropic does not let the app ask for your remaining credit. Type in what " +
                    "console.anthropic.com shows under Billing, and the app will count down from it " +
                    "with every request.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )
        }
        var typed by remember { mutableStateOf("") }
        Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = typed,
                onValueChange = { typed = it },
                singleLine = true,
                label = { Text("Balance in the Console, $") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f),
            )
            TextButton(
                enabled = typed.replace(',', '.').toDoubleOrNull() != null,
                onClick = {
                    val value = typed.replace(',', '.').trim()
                    scope.launch {
                        prefs.dictate.maClaudeBalanceUsd.set(value)
                        prefs.dictate.maClaudeBalanceAt.set(System.currentTimeMillis())
                    }
                    typed = ""
                },
            ) { Text("Set") }
        }

        // ---------------------------------------------------------------- totals
        Spacer(Modifier.height(16.dp))
        Text(
            text = "Spent by this phone",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        Text(
            text = "Today ${usd(MaClaudeCost.spentSince(entries, dayStart))}   ·   " +
                "This month ${usd(MaClaudeCost.spentSince(entries, monthStart))}   ·   " +
                "${entries.size} " + (if (entries.size == 1) "request" else "requests") + " logged",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        )

        // ---------------------------------------------------------------- the log
        Spacer(Modifier.height(12.dp))
        Row(modifier = Modifier.padding(horizontal = 8.dp)) {
            TextButton(onClick = { refresh++ }) { Text("Refresh") }
            TextButton(onClick = { MaClaudeCost.clear(); refresh++ }) { Text("Clear log") }
        }
        if (entries.isEmpty()) {
            Text(
                text = "No requests logged yet. Every Ctrl+P, Ctrl+F, spell check and other Claude " +
                    "request is written here once it is answered.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
            // Newest first, the last two hundred: enough to see a pattern, short enough to scroll.
            entries.asReversed().take(200).forEach { e ->
                Text(
                    text = "${stamp.format(Date(e.atMillis))}  ${e.purpose}",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 6.dp),
                )
                Text(
                    text = "${e.model}  ${e.inputTokens} in / ${e.outputTokens} out  " +
                        (if (e.exact) "" else "~") + usd(e.usd),
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Text(
            text = "~ marks a model newer than the app's price list, priced as its tier's newest.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        )
    }
}

/**
 * Dollars to two decimals from a cent up, four under a cent, six under a hundredth of a cent. A Haiku
 * 5.5 proofread costs about $0.00004 and was shown as $0.0000 (phone test, build 403).
 */
private fun usd(value: Double): String {
    val a = kotlin.math.abs(value)
    val digits = when {
        a == 0.0 || a >= 0.01 -> 2
        a >= 0.0001 -> 4
        else -> 6
    }
    return "$" + "%.${digits}f".format(Locale.ROOT, value)
}
