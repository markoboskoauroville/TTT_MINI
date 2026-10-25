/*
 * Copyright (C) 2026 Marko Boško, Mantra Productions
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package dev.patrickgold.florisboard.dictate.nlp

import android.content.Context
import android.text.InputType
import android.view.inputmethod.ExtractedTextRequest
import dev.patrickgold.florisboard.FlorisImeService
import dev.patrickgold.florisboard.app.FlorisPreferenceStore
import dev.patrickgold.florisboard.dictate.MaLanguage
import dev.patrickgold.florisboard.dictate.MaLog
import dev.patrickgold.florisboard.ime.editor.FlorisEditorInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import kotlin.jvm.Volatile

/**
 * Watches the last dictation in the field, learns what he corrects in it, and keeps the rules.
 *
 * The decisions are all in [MaCorrectionRules]; this is where they meet the keyboard: the dictation
 * as it went in, the field as it is later, the file the rules live in, and the personal model that
 * must stop offering the wrong word.
 *
 * Nothing leaves the phone and nothing costs a request. The rules are one small file in the app's
 * own folder; only how many were learned is logged, never the words, which are his writing.
 */
object MaDictationCorrections {

    private const val FILE = "ma_dictation_corrections.tsv"

    /** How far past the dictation the field is read: room for a longer word put in its place. */
    private const val SLACK = 80

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var file: File? = null

    private val _rules = MutableStateFlow<List<MaCorrectionRules.Rule>>(emptyList())

    /** Every rule, learned and his own. The settings screen draws from this. */
    val rules: StateFlow<List<MaCorrectionRules.Rule>> = _rules.asStateFlow()

    /** A dictation as it went into the field, and where. */
    private class Pending(
        val text: String,
        val start: Int,
        val before: String,
        val language: String,
        val field: String,
    )

    @Volatile
    private var pending: Pending? = null

    fun initialize(context: Context) {
        val f = File(context.filesDir, FILE)
        file = f
        scope.launch {
            _rules.value = MaCorrectionRules.parse(runCatching { f.readText() }.getOrNull())
        }
    }

    private fun enabled(): Boolean {
        val prefs by FlorisPreferenceStore
        return prefs.dictate.maCorrectionsEnabled.get()
    }

    private fun fieldOf(info: FlorisEditorInfo): String =
        "${info.packageName}:${info.base.fieldId}:${info.base.inputType}"

    private fun isSecret(info: FlorisEditorInfo): Boolean {
        val t = info.base.inputType
        val variation = t and InputType.TYPE_MASK_VARIATION
        return when (t and InputType.TYPE_MASK_CLASS) {
            InputType.TYPE_CLASS_TEXT -> variation == InputType.TYPE_TEXT_VARIATION_PASSWORD ||
                variation == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD ||
                variation == InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD
            InputType.TYPE_CLASS_NUMBER -> variation == InputType.TYPE_NUMBER_VARIATION_PASSWORD
            else -> false
        }
    }

    /**
     * Remembers [text], just written into the field by dictation from [start] (absolute), with
     * [textBefore] the field's text before it, both read just before the commit.
     */
    fun noteDictation(text: String, start: Int, textBefore: String, info: FlorisEditorInfo, incognito: Boolean) {
        pending = null
        if (!enabled() || incognito || isSecret(info) || text.isBlank()) return
        if (start < 0) return
        pending = Pending(text, start, textBefore.takeLast(24), MaLanguage.active(), fieldOf(info))
    }

    /** Forgets the dictation being watched, without learning from it: Ctrl+P, Ctrl+F and the like rewrote it. */
    fun drop() {
        pending = null
    }

    /**
     * Reads the field again where the last dictation went in, and learns whatever he corrected there.
     * Called when the next dictation is about to be written, before a prompt rewrites the text, and
     * when the field closes. Once per dictation: what is watched is let go here, learned or not.
     */
    fun settle(info: FlorisEditorInfo) {
        val p = pending ?: return
        pending = null
        if (!enabled() || fieldOf(info) != p.field) return
        val now = runCatching { readRegion(p) }.getOrNull() ?: return
        val swaps = MaCorrectionRules.diff(p.text, now)
        if (swaps.isEmpty()) return
        var next = _rules.value
        for (s in swaps) next = MaCorrectionRules.record(next, s, p.language)
        _rules.value = next
        save()
        // The wrong words leave the personal model, so they stop being offered. Only words that are
        // not also part of the right form: "Mantra production" to "Mantra Productions" keeps "Mantra".
        for (s in swaps) {
            val keep = s.right.split(' ').map { it.lowercase() }.toSet()
            for (w in s.wrong.split(' ')) if (w.lowercase() !in keep) MaNgram.unlearn(w, p.language)
        }
        MaLog.add("corrections", "learned ${swaps.size} from the last dictation")
    }

    /** The stretch of the field that began where [p] was written, or null when it cannot be found. */
    private fun readRegion(p: Pending): String? {
        val ic = FlorisImeService.currentInputConnection() ?: return null
        val et = ic.getExtractedText(ExtractedTextRequest().apply { hintMaxChars = 20000 }, 0) ?: return null
        val all = et.text?.toString() ?: return null
        var at = p.start - et.startOffset
        // Text written or deleted above the dictation moves it; the words just before it say where.
        if (at < 0 || at > all.length || !all.substring(0, at).endsWith(p.before)) {
            if (p.before.isEmpty()) return null
            val from = (at - 400).coerceAtLeast(0)
            val found = all.indexOf(p.before, from)
            if (found < 0) return null
            at = found + p.before.length
        }
        return all.substring(at, (at + p.text.length + SLACK).coerceAtMost(all.length))
    }

    /** [text] with his corrections applied, in the language being written. */
    fun apply(text: String): String {
        if (!enabled()) return text
        return MaCorrectionRules.apply(text, _rules.value, MaLanguage.active())
    }

    /** The right forms, for the speech model's word list. */
    fun glossary(): List<String> =
        if (enabled()) MaCorrectionRules.glossary(_rules.value, MaLanguage.active()) else emptyList()

    // ---------------------------------------------------------------- edited from the settings screen

    fun setEnabled(rule: MaCorrectionRules.Rule, on: Boolean) = update { list ->
        list.map { if (it.key() == rule.key()) it.copy(enabled = on) else it }
    }

    fun delete(rule: MaCorrectionRules.Rule) = update { list -> list.filter { it.key() != rule.key() } }

    /** Adds a rule of his own, applied at once, for every language. */
    fun add(wrong: String, right: String) {
        val w = wrong.trim()
        val r = right.trim()
        if (w.isEmpty() || r.isEmpty() || w == r) return
        val rule = MaCorrectionRules.Rule(w, r, MaCorrectionRules.ANY, 0, true, MaCorrectionRules.Source.MINE)
        update { list -> list.filter { it.key() != rule.key() } + rule }
    }

    fun addSeeds() = update { MaCorrectionRules.withSeeds(it) }

    fun deleteAll() = update { emptyList() }

    private fun update(change: (List<MaCorrectionRules.Rule>) -> List<MaCorrectionRules.Rule>) {
        _rules.value = change(_rules.value)
        save()
    }

    private fun save() {
        val f = file ?: return
        val text = MaCorrectionRules.serialize(_rules.value)
        scope.launch {
            runCatching {
                val tmp = File(f.path + ".tmp")
                tmp.writeText(text)
                if (!tmp.renameTo(f)) {
                    f.writeText(text)
                    tmp.delete()
                }
            }
        }
    }
}
