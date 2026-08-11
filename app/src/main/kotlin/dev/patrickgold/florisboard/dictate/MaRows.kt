/*
 * Copyright (C) 2026 Marko Bosko, Mantra Productions
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package dev.patrickgold.florisboard.dictate

/**
 * One model for every row of keys above the keyboard, built-in and custom together.
 *
 * ### Why the two became one
 *
 * There were two systems doing the same job from opposite ends. The feature row was a fixed set of
 * nine built-in keys that could be reordered but not extended, and the macro bar was an unlimited
 * set of custom keys that could not contain a single built-in one. So the microphone could never sit
 * beside a macro, and a bar of macros could not have a backspace at the end of it without leaving the
 * bar to reach one.
 *
 * That split also had an expiry date. The macro bar was drawn by `MaCursorRow`, inside the transcribe
 * view, and that view is being deleted. Moving macros into the row model is what keeps them.
 *
 * Here a button is either a built-in ([Button.Builtin]) or a macro ([Button.Macro]), and a row is
 * just a list of them. Any button can go in any row, in any order, as many times as wanted.
 *
 * ### Rows are unlimited
 *
 * There is no cap on how many rows there can be. Marko asked for exactly that: if he wants to fill
 * the screen with his own keys, the app should let him. The keyboard already folds rows away by zone
 * and the row heights already come out of the same arithmetic, so a tall stack costs screen space
 * and nothing else. [SANE_ROWS] exists only as the number the editor stops *encouraging* at, not as
 * a limit it enforces.
 *
 * ### Storage
 *
 * One string, separated by ASCII control characters that cannot be typed on a phone. Nothing needs
 * escaping: a macro may contain commas, quotes, braces and newlines and arrive back exactly as it
 * was written. This is [MaMacros]'s scheme, kept deliberately, because it has been carrying his
 * macros for months and the failure it avoids — a label containing the separator and eating the rest
 * of the row — is one nobody would diagnose from the symptom.
 *
 * Each button carries a type marker as its first field, so a built-in and a macro can never be read
 * as one another. The macro text is always the *last* field and is taken as the whole remainder, so
 * it may itself contain the field separator without being cut short.
 */
object MaRows {

    private const val ROW_SEP = '\u001E'
    private const val BTN_SEP = '\u001F'
    private const val FIELD_SEP = '\u001D'

    private const val T_BUILTIN = "b"
    private const val T_MACRO = "m"

    /**
     * Three characters, and the reason is the key rather than the label.
     *
     * These are keys on a phone keyboard, sized for a thumb, sitting a dozen to a row. "AP" and "AC"
     * fit; a word does not, and a label that does not fit is either drawn too small to read — which
     * on this phone means not at all — or silently clipped into something that says nothing. Three
     * is what the widest key in the narrowest row can show at a legible size.
     */
    const val MAX_LABEL = 3

    /** What the editor guides towards. Not enforced: more rows are allowed, and are Marko's choice. */
    const val SANE_ROWS = 8

    /** Kept from the macro bar, where a row long enough to need scrolling was found to be unusable. */
    const val MAX_BUTTONS_PER_ROW = 24

    sealed interface Button {

        /** One of the app's own keys: the record button, backspace, AP, a zone toggle, the gear. */
        data class Builtin(val key: MaFeatureKey) : Button

        /**
         * A key the user made: up to [MAX_LABEL] characters, and macro text in [MaMacroSyntax]'s
         * AutoHotkey form. Plain text types itself, anything in braces is a real key press.
         */
        data class Macro(val label: String, val macro: String) : Button
    }

    /**
     * Builds a macro button, truncating the label rather than rejecting it.
     *
     * Truncating: a label arriving too long comes from a restored backup or a hand-edited preference,
     * and dropping the whole button would lose the macro attached to it. The macro is the valuable
     * half and the label is only what it says on its face.
     */
    fun macro(label: String, macro: String): Button.Macro =
        Button.Macro(label.take(MAX_LABEL), macro)

    /**
     * The row Marko has now, as the starting point: his feature keys, in his order.
     *
     * Built from [MaFeatureOrder.DEFAULT] rather than repeated here, so the two cannot disagree, and
     * so a key added to the enum later appears here without anybody remembering to add it twice.
     */
    /** The clipboard row, in the order it is counted along: the badge, then newest to oldest. */
    private val CLIP_ROW = listOf(
        MaFeatureKey.CLIP_LABEL,
        MaFeatureKey.CLIP_1,
        MaFeatureKey.CLIP_2,
        MaFeatureKey.CLIP_3,
        MaFeatureKey.CLIP_4,
        MaFeatureKey.CLIP_5,
        MaFeatureKey.CLIP_6,
        MaFeatureKey.CLIP_7,
        MaFeatureKey.CLIP_8,
        MaFeatureKey.CLIP_9,
    )

    /**
     * Two rows to start with: his feature keys, then the clipboard history.
     *
     * [MaFeatureOrder.DEFAULT] is the list of every key that exists, which is what lets the editor
     * offer all of them — it is not a row. Splitting happens here. Keys marked hidden by default are
     * left out, because there is no hidden flag in this model: a key that is not wanted is a key
     * that is not in a row, and putting one there by default would show it to somebody who never
     * asked for it.
     */
    fun defaultRows(): List<List<Button>> = listOf(
        MaFeatureOrder.DEFAULT
            .filter { it !in CLIP_ROW && it !in MaFeatureOrder.DEFAULT_HIDDEN }
            .map { Button.Builtin(it) },
        CLIP_ROW.map { Button.Builtin(it) },
    )

    fun serialize(rows: List<List<Button>>): String =
        rows.joinToString(ROW_SEP.toString()) { row ->
            row.joinToString(BTN_SEP.toString()) { button ->
                when (button) {
                    is Button.Builtin -> "$T_BUILTIN$FIELD_SEP${button.key.id}"
                    is Button.Macro -> "$T_MACRO$FIELD_SEP${button.label}$FIELD_SEP${button.macro}"
                }
            }
        }

    /**
     * Parses the stored string, skipping anything malformed rather than throwing.
     *
     * A damaged preference degrades to a smaller set of rows. It must never become a keyboard that
     * refuses to draw: this string is read while the keyboard is opening, in front of whatever the
     * user was about to type into, and there is no way to reach settings to fix it from there.
     *
     * A built-in naming a key that no longer exists is dropped, not guessed at. Keys have been
     * removed from this app before — the reader's book key went with the reader — and a stored row
     * written before that is otherwise still perfectly good.
     */
    fun parse(raw: String): List<List<Button>> {
        if (raw.isBlank()) return emptyList()
        return raw.split(ROW_SEP)
            .map { rowText ->
                rowText.split(BTN_SEP).mapNotNull { parseButton(it) }
            }
            .filter { it.isNotEmpty() }
    }

    private fun parseButton(raw: String): Button? {
        if (raw.isEmpty()) return null
        val firstSep = raw.indexOf(FIELD_SEP)
        if (firstSep < 0) return null
        return when (raw.substring(0, firstSep)) {
            T_BUILTIN -> MaFeatureKey.byId(raw.substring(firstSep + 1))?.let { Button.Builtin(it) }
            T_MACRO -> {
                val rest = raw.substring(firstSep + 1)
                val secondSep = rest.indexOf(FIELD_SEP)
                if (secondSep < 0) return null
                val label = rest.substring(0, secondSep)
                // Everything after the second separator, whatever it contains. Taking the remainder
                // rather than splitting is what lets a macro hold the separator character itself.
                val macroText = rest.substring(secondSep + 1)
                if (label.isBlank() && macroText.isBlank()) null else macro(label, macroText)
            }
            else -> null
        }
    }

    fun defaultSerialized(): String = serialize(defaultRows())

    /**
     * Carries the old two-system setup into the new one, once.
     *
     * The feature row's stored order becomes the first row, minus anything it had hidden, since a
     * hidden key was a key he did not want on screen and there is no hidden flag here — a button he
     * does not want is a button he removes. Every row of the macro bar's presets follows, flattened:
     * presets were a way of swapping one bar for another when only one bar could exist at a time,
     * and with unlimited rows that reason is gone. Nothing is lost, and anything unwanted is now one
     * deletion away instead of trapped in a preset he never selects.
     */
    fun migrate(featureOrderRaw: String, featureHiddenRaw: String, macroRaw: String): List<List<Button>> {
        val hidden = MaFeatureOrder.parseHidden(featureHiddenRaw)
        val featureRow = MaFeatureOrder.visible(MaFeatureOrder.parse(featureOrderRaw), hidden)
            .map { Button.Builtin(it) }
        val macroRows = MaMacros.parse(macroRaw)
            .flatMap { preset -> preset.rows }
            .map { row -> row.map { macro(it.label, it.macro) } }
            .filter { it.isNotEmpty() }
        return (listOf(featureRow) + macroRows).filter { it.isNotEmpty() }
    }
}
