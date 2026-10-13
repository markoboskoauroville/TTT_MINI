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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.patrickgold.florisboard.app.FlorisPreferenceStore
import dev.patrickgold.florisboard.dictate.MaFlow
import dev.patrickgold.florisboard.dictate.MaProofread
import dev.patrickgold.florisboard.dictate.MaPrompts
import dev.patrickgold.florisboard.dictate.MaProviders
import dev.patrickgold.florisboard.dictate.MaRewordPrompt
import dev.patrickgold.florisboard.dictate.MaSpellcheck
import dev.patrickgold.florisboard.dictate.provider.MaClaudeModels
import dev.patrickgold.florisboard.dictate.provider.OpenAiCompatibleClient
import dev.patrickgold.florisboard.lib.compose.FlorisScreen
import dev.patrickgold.jetpref.datastore.model.PreferenceData
import dev.patrickgold.jetpref.datastore.model.collectAsState
import kotlinx.coroutines.launch

/**
 * Prompts, as categories holding named instances — the Avid model where it earns its keep.
 *
 * ### What this screen is
 *
 * Two categories, Ctrl+P and Ctrl+F. Each holds as many named wordings as he likes and exactly one
 * carries the checkmark, which is the one the key sends. Choosing another visibly takes the mark
 * from the last, because it is a radio and not a tick-box: there is one wording in force and the
 * control should say so rather than leave two looking equally chosen.
 *
 * ### Duplicate is the main verb, not delete
 *
 * Nobody edits a working configuration directly. They copy it, rename the copy and edit that, so the
 * thing that was working is still there when the experiment turns out worse. The Default cannot be
 * edited at all for the same reason — its editor offers exactly one action, which is to duplicate
 * it and open the copy.
 *
 * ### Why not a tree with twist-downs
 *
 * Because two categories are not a tree. Section 96 records the order: instances first, and the
 * folder structure only if enough categories end up holding several of them. Nineteen settings
 * screens behind collapsed triangles, most of them holding one thing, would put every setting one
 * tap further away and call it an improvement.
 */
@Composable
fun MaPromptsScreen() = FlorisScreen {
    title = "Prompts"

    content {
        val prefs by FlorisPreferenceStore

        Text(
            text = "Ctrl+P and Ctrl+F each send an instruction to the rewording model. Keep as " +
                "many wordings as you like and tick the one in force. Default is the instruction " +
                "built into the app \u2014 it cannot be edited, only duplicated.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )

        PromptCategory(
            title = "Ctrl + P \u2014 Proofread",
            pref = prefs.dictate.maProofreadPrompt,
            shipped = MaProofread.INSTRUCTION,
            modelPref = prefs.dictate.maProofreadModel,
        )

        Spacer(Modifier.height(20.dp))

        PromptCategory(
            title = "Ctrl + F \u2014 Better flow",
            pref = prefs.dictate.maFlowPrompt,
            shipped = MaFlow.INSTRUCTION,
            modelPref = prefs.dictate.maFlowModel,
        )

        // THE CROATIAN SPELL CHECK, a third prompt of the same shape. It runs by itself after every
        // Croatian dictation rather than on a key, so it has a switch as well (MaSpellcheck).
        Spacer(Modifier.height(20.dp))
        val spellOn by prefs.dictate.maSpellcheckEnabled.collectAsState()
        val switchScope = rememberCoroutineScope()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { switchScope.launch { prefs.dictate.maSpellcheckEnabled.set(!spellOn) } }
                .padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Croatian spell check", style = MaterialTheme.typography.titleSmall)
                Text(
                    text = "After every Croatian dictation, before it is typed: puts back words the " +
                        "transcription invented or misheard, and fixes Croatian spelling. English " +
                        "dictations are left alone.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(
                checked = spellOn,
                onCheckedChange = { on -> switchScope.launch { prefs.dictate.maSpellcheckEnabled.set(on) } },
            )
        }
        PromptCategory(
            title = "Spell check wording",
            pref = prefs.dictate.maSpellcheckPrompt,
            shipped = MaSpellcheck.INSTRUCTION,
            modelPref = prefs.dictate.maSpellcheckModel,
        )

        // WHAT IS SENT, WORD FOR WORD. He asked to see the exact message and to change it. The
        // instruction above is only the first of four parts; the other three are shown and, where
        // they belong to this screen, editable here.
        Spacer(Modifier.height(20.dp))
        SentToClaudeSection()

        // GRAMMAR CORRECTION (MANTRA). One voice, stated rather than chosen.
        //
        // This was a two-option chooser one build ago and it was wrong. It offered Yshai's actual
        // lowercase as an alternative, and prose-voice.md §3 says that lowercase is the ONE thing we
        // deliberately do not copy. He said so as soon as he saw it.
        //
        // What replaces it is not a smaller chooser, it is a sentence. **There is one voice, so
        // there is nothing to choose**, and a control offering a choice that does not exist is worse
        // than no control: it invites him to pick the wrong one and then wonder why nothing changed.
        Spacer(Modifier.height(20.dp))
        Text(
            text = "Grammar correction (Mantra)",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        Text(
            text = "Ctrl + F writes in one voice, taken from Yshai Afterman's letters: short " +
                "paragraphs, the honest counterweight, witness rather than assert, one image, the " +
                "principle before the request, ending by taking pressure off.\n\n" +
                "With normal sentence capitalisation and apostrophes. He writes his own letters in " +
                "lower case; that is the one thing we do not copy.\n\n" +
                "It changes the shape and never the meaning. No new facts, nothing softened.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
        )

        Spacer(Modifier.height(28.dp))
    }
}

/** One category: its instances, the checkmark, and the three verbs. */
@Composable
private fun PromptCategory(
    title: String,
    pref: PreferenceData<String>,
    shipped: String,
    /** Which model this prompt runs on; empty means the default chosen under Sent to Claude. */
    modelPref: PreferenceData<String>? = null,
) {
    val scope = rememberCoroutineScope()
    val raw by pref.collectAsState()
    // Parsed from the stored string on every change rather than held in a separate state. The
    // preference is the truth; a copy kept beside it drifts the moment anything else writes it,
    // and a restored profile writes exactly this.
    val set = remember(raw) { MaPrompts.parse(raw) }

    var editing by remember { mutableStateOf<MaPrompts.Instance?>(null) }
    var confirmDelete by remember { mutableStateOf<MaPrompts.Instance?>(null) }

    fun save(next: MaPrompts.Set) {
        scope.launch { pref.set(MaPrompts.serialize(next)) }
    }

    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
    )

    for (inst in set.instances) {
        val isActive = inst.name == set.active.name
        // The wording this row would actually send, so the Default shows the shipped words rather
        // than an empty line. Nothing on screen should read as blank when it is not.
        val shown = inst.text.ifBlank { shipped }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { editing = inst }
                .padding(start = 8.dp, end = 8.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RadioButton(
                selected = isActive,
                onClick = { save(set.copy(activeName = inst.name)) },
            )
            Column(modifier = Modifier.weight(1f).padding(start = 4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = inst.name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal,
                    )
                    if (inst.locked) {
                        Text(
                            text = "  built in",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Text(
                    text = shown,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            IconButton(
                onClick = {
                    val name = MaPrompts.freeName(set.instances, inst.name)
                    val copy = MaPrompts.Instance(name, shown)
                    // The copy is added and opened, but NOT made active. Duplicating is the start of
                    // an experiment, and a key that changed what it does the moment you pressed
                    // copy would be a nasty surprise mid-sentence.
                    save(set.copy(instances = set.instances + copy))
                    editing = copy
                },
                modifier = Modifier.size(40.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = "Duplicate ${inst.name}",
                    modifier = Modifier.size(20.dp),
                )
            }
            if (!inst.locked) {
                IconButton(
                    onClick = { confirmDelete = inst },
                    modifier = Modifier.size(40.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete ${inst.name}",
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
    }

    TextButton(
        onClick = {
            val name = MaPrompts.freeName(set.instances, MaPrompts.DEFAULT_NAME)
            val copy = MaPrompts.Instance(name, shipped)
            save(set.copy(instances = set.instances + copy))
            editing = copy
        },
        modifier = Modifier.padding(start = 8.dp),
    ) { Text("New wording") }

    modelPref?.let { ModelChooser(it) }

    var previewing by remember { mutableStateOf(false) }
    TextButton(
        onClick = { previewing = true },
        modifier = Modifier.padding(start = 8.dp),
    ) { Text("Show exactly what is sent") }
    if (previewing) {
        SentPreviewDialog(
            title = title,
            instruction = set.active.text.ifBlank { shipped },
            onDismiss = { previewing = false },
            promptModel = modelPref?.get()?.trim()?.ifBlank { null },
        )
    }

    editing?.let { inst ->
        PromptEditor(
            instance = inst,
            shipped = shipped,
            existing = set.instances,
            onDismiss = { editing = null },
            onDuplicate = {
                val name = MaPrompts.freeName(set.instances, inst.name)
                val copy = MaPrompts.Instance(name, inst.text.ifBlank { shipped })
                save(set.copy(instances = set.instances + copy))
                editing = copy
            },
            onSave = { newName, newText ->
                val renamed = MaPrompts.Instance(newName, newText)
                val instances = set.instances.map { if (it.name == inst.name) renamed else it }
                // The checkmark follows a rename. It is stored by name, so leaving it pointing at
                // the old one would silently drop the category back to Default — the exact failure
                // storing by name was meant to prevent, arriving through the back door.
                val active = if (set.activeName == inst.name) newName else set.activeName
                save(MaPrompts.Set(instances, active))
                editing = null
            },
        )
    }

    confirmDelete?.let { inst ->
        AlertDialog(
            onDismissRequest = { confirmDelete = null },
            title = { Text("Delete ${inst.name}?") },
            text = { Text("The wording goes with it. Default cannot be deleted, so the key always has something to send.") },
            confirmButton = {
                TextButton(onClick = {
                    val instances = set.instances.filterNot { it.name == inst.name }
                    // Deleting the active one hands the checkmark back to Default rather than to
                    // whatever happened to be next in the list, which would be a wording he never
                    // chose taking over a key.
                    val active = if (set.activeName == inst.name) MaPrompts.DEFAULT_NAME else set.activeName
                    save(MaPrompts.Set(instances, active))
                    confirmDelete = null
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = null }) { Text("Cancel") }
            },
        )
    }
}

/**
 * The detail pane, as a dialog.
 *
 * A dialog rather than a second screen because these are paragraphs and the list is short: a
 * master-detail split pays on a desktop with room for both, and on a phone it is one more place to
 * navigate back from. Section 96 keeps the real routing for when the tree exists, if it ever does.
 */
@Composable
private fun PromptEditor(
    instance: MaPrompts.Instance,
    shipped: String,
    existing: List<MaPrompts.Instance>,
    onDismiss: () -> Unit,
    onDuplicate: () -> Unit,
    onSave: (String, String) -> Unit,
) {
    var name by remember(instance.name) { mutableStateOf(instance.name) }
    var text by remember(instance.name) { mutableStateOf(instance.text.ifBlank { shipped }) }

    val clash = !instance.locked &&
        name.trim() != instance.name &&
        existing.any { it.name.equals(name.trim(), ignoreCase = true) }
    val blank = name.trim().isEmpty()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(instance.name) },
        text = {
            Column {
                if (instance.locked) {
                    Text(
                        text = "This is the instruction built into the app. Duplicate it to make " +
                            "a version you can edit \u2014 the original stays as it is.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = shipped,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        singleLine = true,
                        isError = clash || blank,
                        label = { Text(if (clash) "That name is taken" else "Name") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = text,
                        onValueChange = { text = it },
                        minLines = 6,
                        maxLines = 14,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        },
        confirmButton = {
            if (instance.locked) {
                TextButton(onClick = onDuplicate) { Text("Duplicate") }
            } else {
                TextButton(
                    enabled = !clash && !blank,
                    onClick = { onSave(name.trim(), text) },
                ) { Text("Save") }
            }
        },
        dismissButton = {
            Row {
                if (!instance.locked) {
                    TextButton(onClick = onDuplicate) { Text("Duplicate") }
                }
                TextButton(onClick = onDismiss) { Text("Close") }
            }
        },
    )
}

/** The model Ctrl+P and Ctrl+F go to, as a line he can read: what is set, and what it means. */
private fun rewordingModelLine(): String {
    val account = MaProviders.rewordingAccount()
    val preset = MaProviders.presetFor(account)
    val set = MaClaudeModels.upgradeOldDefault(account.chatModel.ifBlank { preset.defaultChatModel.orEmpty() })
    val tier = MaClaudeModels.tierOfAlias(set)
    val ran = OpenAiCompatibleClient.lastClaudeModel
    return buildString {
        append("Model: ").append(set.ifBlank { "provider default" })
        if (tier != null) {
            append(" \u2014 the newest ").append(tier.replaceFirstChar { it.uppercase() })
            append(" on Anthropic's model list, looked up by the app")
        }
        if (ran != null) append("\nLast answered by: ").append(ran)
    }
}

/**
 * The whole message for one key, as [MaRewordPrompt.build] makes it, with his text replaced by a
 * marker. Three language cases, because the language line is the part that changes with the text.
 */
@Composable
private fun SentPreviewDialog(
    title: String,
    instruction: String,
    onDismiss: () -> Unit,
    /** The model chosen for this prompt, or null when it uses the default. */
    promptModel: String? = null,
) {
    val prefs by FlorisPreferenceStore
    // Collected so the preview follows the settings it is built from.
    val selection by prefs.dictate.systemPromptSelection.collectAsState()
    val custom by prefs.dictate.systemPromptCustom.collectAsState()
    val rule by prefs.dictate.maLanguageRule.collectAsState()
    val unsure by prefs.dictate.maLanguageRuleUnsure.collectAsState()
    var language by remember { mutableStateOf<String?>("Croatian") }

    val message = remember(instruction, selection, custom, rule, unsure, language) {
        MaRewordPrompt.build(
            instruction = instruction,
            systemPrompt = MaRewordPrompt.currentSystemPrompt(),
            languageTemplate = rule.ifBlank { MaRewordPrompt.LANGUAGE_RULE },
            languageUnsure = unsure.ifBlank { MaRewordPrompt.LANGUAGE_RULE_UNSURE },
            language = language,
            input = MaRewordPrompt.TEXT_PLACEHOLDER,
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    text = promptModel?.let { "This prompt runs on: $it (its own choice, above)" }
                        ?: rewordingModelLine(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                Row {
                    for (choice in listOf("Croatian", "English", null)) {
                        TextButton(onClick = { language = choice }) {
                            Text(
                                text = choice ?: "Unclear",
                                fontWeight = if (language == choice) FontWeight.Bold else FontWeight.Normal,
                            )
                        }
                    }
                }
                Text(
                    text = "Sent as one message, in this order: your instruction, the rewording " +
                        "system prompt, the language line, your text.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                SelectionContainer {
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                    )
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
    )
}

/** The language lines, editable, and the last message that really left the phone. */
@Composable
private fun SentToClaudeSection() {
    val prefs by FlorisPreferenceStore
    val scope = rememberCoroutineScope()
    val rule by prefs.dictate.maLanguageRule.collectAsState()
    val unsure by prefs.dictate.maLanguageRuleUnsure.collectAsState()
    var editingRule by remember { mutableStateOf<Int?>(null) }
    var refresh by remember { mutableStateOf(0) }

    Text(
        text = "Sent to Claude",
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(horizontal = 16.dp),
    )
    Text(
        text = rewordingModelLine(),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
    )

    ClaudeTierChooser()

    LanguageRuleRow(
        name = "Language line",
        hint = "{language} becomes Croatian or English",
        text = rule.ifBlank { MaRewordPrompt.LANGUAGE_RULE },
        custom = rule.isNotBlank(),
        onClick = { editingRule = 0 },
    )
    LanguageRuleRow(
        name = "Language line when the language is unclear",
        hint = "used when the text does not show which language it is",
        text = unsure.ifBlank { MaRewordPrompt.LANGUAGE_RULE_UNSURE },
        custom = unsure.isNotBlank(),
        onClick = { editingRule = 1 },
    )

    editingRule?.let { which ->
        val pref = if (which == 0) prefs.dictate.maLanguageRule else prefs.dictate.maLanguageRuleUnsure
        val shipped = if (which == 0) MaRewordPrompt.LANGUAGE_RULE else MaRewordPrompt.LANGUAGE_RULE_UNSURE
        var text by remember(which) { mutableStateOf(pref.get().ifBlank { shipped }) }
        AlertDialog(
            onDismissRequest = { editingRule = null },
            title = { Text(if (which == 0) "Language line" else "Language line, unclear") },
            text = {
                Column {
                    if (which == 0) {
                        Text(
                            text = "Write {language} where the language name goes.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                    OutlinedTextField(
                        value = text,
                        onValueChange = { text = it },
                        minLines = 3,
                        maxLines = 8,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    // Saved as empty when it matches the built-in, so improving the built-in later
                    // still reaches him — the same rule the Default wordings follow.
                    val value = text.trim().takeIf { it != shipped }.orEmpty()
                    scope.launch { pref.set(value) }
                    editingRule = null
                }) { Text("Save") }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = {
                        scope.launch { pref.set("") }
                        editingRule = null
                    }) { Text("Built-in") }
                    TextButton(onClick = { editingRule = null }) { Text("Cancel") }
                }
            },
        )
    }

    Spacer(Modifier.height(12.dp))
    Text(
        text = "Last message sent",
        style = MaterialTheme.typography.titleSmall,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
    )
    val last = remember(refresh) { MaRewordPrompt.last }
    if (last == null) {
        Text(
            text = "Nothing sent since the keyboard last started. Press Ctrl+P or Ctrl+F, then come back.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
    } else {
        val time = remember(last) {
            java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date(last.atMillis))
        }
        Text(
            text = "$time \u2014 asked for ${last.model}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        SelectionContainer {
            Text(
                text = last.message,
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )
        }
    }
    TextButton(onClick = { refresh++ }, modifier = Modifier.padding(start = 8.dp)) { Text("Refresh") }
    Text(
        text = "Kept in memory only, never saved: it holds your text.",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 16.dp),
    )
}

/**
 * Which Claude answers Ctrl+P and Ctrl+F: the newest Haiku, Sonnet or Opus.
 *
 * A tier, never a number — the number is looked up from Anthropic's list (MaClaudeModels), so this
 * choice never goes stale. Haiku stays the default; Sonnet and Opus are his to pick, with the price
 * beside them, because a tier change is a price change.
 */
@Composable
private fun ClaudeTierChooser() {
    val prefs by FlorisPreferenceStore
    val scope = rememberCoroutineScope()
    val accounts by prefs.dictate.providerAccounts.collectAsState()
    val account = MaProviders.rewordingAccount()
    if (account.providerId != "anthropic") return
    val preset = MaProviders.presetFor(account)
    val current = MaClaudeModels.upgradeOldDefault(
        accounts.getOrEmpty(account.providerId).chatModel.ifBlank { preset.defaultChatModel.orEmpty() },
    )
    val choices = listOf(
        "haiku" to "Newest Haiku \u2014 fastest, cheapest (default)",
        "sonnet" to "Newest Sonnet \u2014 better Croatian grammar, about 20\u00D7 the price",
        "opus" to "Newest Opus \u2014 most careful, about 40\u00D7 the price",
    )
    for ((tier, label) in choices) {
        val alias = MaClaudeModels.alias(tier)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    scope.launch {
                        prefs.dictate.providerAccounts.set(
                            accounts.edit(account.providerId) { it.copy(chatModel = alias) },
                        )
                    }
                }
                .padding(horizontal = 8.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RadioButton(
                selected = current == alias,
                onClick = {
                    scope.launch {
                        prefs.dictate.providerAccounts.set(
                            accounts.edit(account.providerId) { it.copy(chatModel = alias) },
                        )
                    }
                },
            )
            Text(text = label, style = MaterialTheme.typography.bodyMedium)
        }
    }
    if (MaClaudeModels.tierOfAlias(current) == null) {
        Text(
            text = "Pinned to $current. Pick one above to follow the newest instead.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
    }
    Spacer(Modifier.height(8.dp))
}

@Composable
private fun LanguageRuleRow(name: String, hint: String, text: String, custom: Boolean, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 6.dp),
    ) {
        Text(
            text = if (custom) "$name  (yours)" else "$name  (built in)",
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(
            text = hint,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * The model one prompt runs on: the default, or the newest Haiku, Sonnet or Opus.
 *
 * Tiers, never numbers, so the choice follows Anthropic's newest model of that tier by itself
 * (MaClaudeModels). Used only when the rewording provider is Anthropic.
 */
@Composable
private fun ModelChooser(pref: PreferenceData<String>) {
    val scope = rememberCoroutineScope()
    val current by pref.collectAsState()
    val options = listOf(
        "" to "Default",
        MaClaudeModels.alias("haiku") to "Haiku",
        MaClaudeModels.alias("sonnet") to "Sonnet",
        MaClaudeModels.alias("opus") to "Opus",
    )
    Row(
        modifier = Modifier.padding(start = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "Model:",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        for ((value, label) in options) {
            TextButton(onClick = { scope.launch { pref.set(value) } }) {
                Text(
                    text = label,
                    fontWeight = if (current.trim() == value) FontWeight.Bold else FontWeight.Normal,
                )
            }
        }
    }
    if (current.isNotBlank() && MaClaudeModels.tierOfAlias(current) == null) {
        Text(
            text = "Pinned to $current.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
    }
}
