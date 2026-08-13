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
 * The commands worth reaching for, ready to drop into a macro.
 *
 * ### Why this exists
 *
 * [MaMacroSyntax] accepts a generous set of names and aliases, which is the right way round for
 * *reading* a macro — nobody should have to guess whether it is `esc` or `escape`. It is the wrong
 * way round for writing one, because a syntax that accepts anything gives no clue what to type. The
 * editor's text field is a blank space with no hint of what belongs in it, and the answer cannot be
 * dictated: `{Ctrl+Shift+Z}` said out loud into a text field arrives as prose.
 *
 * So the editor offers these instead of asking him to remember them. Tapping one inserts its token
 * at the cursor. Termux does the same thing with its extra-keys row, and this is deliberately the
 * same workflow: a short list of things a terminal or an editor actually needs, picked rather than
 * typed.
 *
 * ### Why the label is suggested too
 *
 * Every entry carries a label that fits in [MaMacroSlots.MAX_LABEL] characters. Picking a command fills in
 * both halves of the button, so the common case is one tap and done. It is only a suggestion and the
 * label stays editable: the point is that the field is never blank, not that the name is fixed.
 */
object MaCommandPalette {

    /**
     * @param label a suggested face for the key, at most [MaMacroSlots.MAX_LABEL] characters
     * @param token what gets inserted into the macro text
     * @param description what it does, in plain words, for the list
     */
    data class Entry(val label: String, val token: String, val description: String)

    data class Group(val name: String, val entries: List<Entry>)

    /** Moving the cursor. The keys a phone keyboard has no room for and an editor cannot do without. */
    private val NAVIGATION = Group(
        "Moving around",
        listOf(
            Entry("\u2190", "{Left}", "Cursor left"),
            Entry("\u2192", "{Right}", "Cursor right"),
            Entry("\u2191", "{Up}", "Cursor up"),
            Entry("\u2193", "{Down}", "Cursor down"),
            Entry("\u21e4", "{Home}", "Start of the line"),
            Entry("\u21e5", "{End}", "End of the line"),
            Entry("PgU", "{PgUp}", "Page up"),
            Entry("PgD", "{PgDn}", "Page down"),
        ),
    )

    /** Editing. Real key events, so any app listening for them responds as it would to a keyboard. */
    private val EDITING = Group(
        "Editing",
        listOf(
            Entry("all", "{Ctrl+A}", "Select all"),
            Entry("cut", "{Ctrl+X}", "Cut"),
            Entry("cpy", "{Ctrl+C}", "Copy"),
            Entry("pst", "{Ctrl+V}", "Paste"),
            Entry("und", "{Ctrl+Z}", "Undo"),
            Entry("red", "{Ctrl+Shift+Z}", "Redo"),
            Entry("del", "{Del}", "Delete forwards"),
            Entry("\u232b", "{BS}", "Backspace"),
        ),
    )

    /** The keys that are keys and nothing else. */
    private val KEYS = Group(
        "Plain keys",
        listOf(
            Entry("\u23ce", "{Enter}", "Enter"),
            Entry("tab", "{Tab}", "Tab"),
            Entry("ESC", "{Esc}", "Escape"),
            Entry("\u2423", "{Space}", "Space"),
            Entry("ins", "{Insert}", "Insert"),
            Entry("mnu", "{Menu}", "Menu"),
        ),
    )

    /**
     * Terminal work, which is where the whole idea came from.
     *
     * Ctrl+C, Ctrl+D and Ctrl+L are the three a shell cannot be used without, and none of them are
     * reachable from an Android keyboard. The punctuation below them is there for the same reason:
     * a pipe, a tilde and a slash are three taps deep on a phone layout and constant in a shell.
     *
     * A caution worth knowing before wiring these: a key event is delivered correctly and then
     * ignored by any app not listening for it. In a terminal these work. In a chat box, Ctrl+D does
     * nothing at all, and that is the app's decision rather than a fault here.
     */
    private val TERMINAL = Group(
        "Terminal",
        listOf(
            Entry("^C", "{Ctrl+C}", "Interrupt"),
            Entry("^D", "{Ctrl+D}", "End of input"),
            Entry("^L", "{Ctrl+L}", "Clear the screen"),
            Entry("^R", "{Ctrl+R}", "Search the history"),
            Entry("^U", "{Ctrl+U}", "Clear the line"),
            Entry("|", "|", "Pipe"),
            Entry("~", "~", "Home directory"),
            Entry("/", "/", "Slash"),
            Entry("-", "-", "Dash"),
        ),
    )

    /** Punctuation that a dictated sentence needs and a folded keyboard cannot give. */
    private val PUNCTUATION = Group(
        "Punctuation",
        listOf(
            Entry(",", ", ", "Comma and a space"),
            Entry(".", ". ", "Full stop and a space"),
            Entry("?", "? ", "Question mark and a space"),
            Entry("!", "! ", "Exclamation mark and a space"),
            Entry(":", ": ", "Colon and a space"),
            Entry("\u2026", "\u2026 ", "Ellipsis and a space"),
            Entry("\u201e\u201d", "\u201e\u201d", "Croatian quotation marks"),
        ),
    )

    /** Function keys, folded into one group because a list of twelve is a list nobody reads. */
    private val FUNCTION = Group(
        "Function keys",
        (1..12).map { Entry("F$it", "{F$it}", "Function key $it") },
    )

    /**
     * Changing the case of what is already in the field.
     *
     * The only commands that read before they write. Grouped apart for that reason: everything else
     * on this list types something, and these four replace what is there.
     */
    private val CASE = Group(
        "Change the case",
        listOf(
            Entry("ABC", "{upper}", "ALL CAPS"),
            Entry("abc", "{lower}", "all lowercase"),
            Entry("Abc", "{title}", "Title Case"),
            Entry("Ab.", "{sentence}", "Sentence case"),
        ),
    )

    val GROUPS: List<Group> = listOf(CASE, EDITING, NAVIGATION, KEYS, TERMINAL, PUNCTUATION, FUNCTION)

    /** Flat, for a search field. */
    val ALL: List<Entry> = GROUPS.flatMap { it.entries }

    /**
     * Every suggested label fits on a key.
     *
     * Checked in a test rather than trusted: these are written by hand, the limit is three, and a
     * four-character label added here would be silently truncated into something that reads as a
     * typo on the key rather than as a mistake in this file.
     */
    fun oversizedLabels(): List<Entry> = ALL.filter { it.label.length > MaMacroSlots.MAX_LABEL }
}
