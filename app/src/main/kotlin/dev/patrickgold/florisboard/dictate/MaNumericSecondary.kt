/*
 * Copyright (C) 2026 Marko Boško, Mantra Productions
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dev.patrickgold.florisboard.dictate

/**
 * What each digit on the number row types when it is held.
 *
 * Ten slots, one per digit, in printed order: 1 to 9 and then 0. A slot may hold any string, or be
 * empty for a key with nothing behind it.
 *
 * Stored as the ten slots joined by a unit separator rather than by a comma. A second symbol is
 * exactly the kind of thing somebody assigns a comma to, and a separator that can appear inside a
 * value is a corruption waiting to happen. U+001F cannot be typed, so it can never collide with
 * what is stored.
 *
 * Parsing never fails and never throws. A stored string of the wrong length is padded or trimmed to
 * ten, because a preference written by an older build, or by hand, should degrade to something
 * usable rather than take the keyboard down with it.
 */
object MaNumericSecondary {
    /** How many slots there are, one per digit. */
    const val COUNT = 10

    /** The digits in printed order, so an editor can label its fields without guessing. */
    val LABELS = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0")

    private const val SEPARATOR = '\u001F'

    /**
     * The underscore on all ten, which is the reason this exists.
     *
     * It is the character that costs three layout switches to reach and appears constantly in
     * scripts and file names. Putting it under every digit means it is always under a thumb
     * wherever that thumb happens to be, and there is no tenth key to remember.
     */
    val DEFAULT: String = List(COUNT) { "_" }.joinToString(SEPARATOR.toString())

    /** The ten slots, always exactly ten, whatever [raw] holds. */
    fun parse(raw: String): List<String> {
        val parts = raw.split(SEPARATOR)
        return List(COUNT) { index -> parts.getOrNull(index)?.let { sanitize(it) } ?: "" }
    }

    /** [slots] back to a stored string, padded or trimmed to exactly ten. */
    fun serialize(slots: List<String>): String =
        List(COUNT) { index -> sanitize(slots.getOrNull(index) ?: "") }
            .joinToString(SEPARATOR.toString())

    /** The most a slot holds: a paragraph, not a document. */
    const val MAX_LENGTH = 2000

    /**
     * One slot's value, cleaned.
     *
     * ANY TEXT, like an AutoHotkey hotstring (10.10.2026, his ask). It was punctuation only and
     * capped at 16 — his earlier rule, which he has now replaced: a slot is a snippet, a sentence,
     * an address, a command, and the long press types all of it. Letters, digits and line breaks are
     * kept. Only the separator is removed, since it cannot survive a round trip, and a carriage
     * return, so a pasted Windows line break becomes one break rather than two.
     */
    fun sanitize(value: String): String =
        value.replace("\r", "")
            .replace(SEPARATOR.toString(), "")
            .take(MAX_LENGTH)

    /**
     * What the key prints in its corner for [value]: the first character, nothing more.
     *
     * His spec: the key shows the first symbol and the hold types the whole box. The first one that
     * is not blank, so a snippet starting with a space or a line break still marks its key, and a
     * whole code point, so an emoji is not cut in half.
     */
    fun hint(value: String): String? {
        val start = value.indexOfFirst { !it.isWhitespace() }
        if (start < 0) return null
        // A slot that starts with a special key shows that key's glyph rather than a brace, so
        // "{Enter}" marks its digit with ⏎ and not with "{", which would say nothing.
        TOKEN_AT_START.find(value.substring(start))?.let { m ->
            SPECIAL_KEYS.firstOrNull { it.token.equals(m.value, ignoreCase = true) }?.let { return it.glyph }
        }
        return value.substring(start, value.offsetByCodePoints(start, 1))
    }

    private val TOKEN_AT_START = Regex("^\\{[A-Za-z0-9+]+\\}")

    /**
     * One key that cannot be typed as a character, offered on the digit's long press in the editor.
     *
     * [token] is MaMacroSyntax's AutoHotkey brace form, the same syntax the macro keys already use,
     * so there is one spelling of Enter in this app and not two. [glyph] is what the number row
     * prints in a key's corner when its slot starts with this key.
     */
    data class SpecialKey(val token: String, val name: String, val glyph: String)

    /**
     * The menu, in the order a hand reaches for them: the keys that finish or move between fields,
     * then the deletes, then the cursor, then the clipboard combos.
     */
    val SPECIAL_KEYS: List<SpecialKey> = listOf(
        SpecialKey("{Enter}", "Enter", "\u23ce"),
        SpecialKey("{Tab}", "Tab", "\u21e5"),
        SpecialKey("{Esc}", "Escape", "\u238b"),
        SpecialKey("{Backspace}", "Backspace", "\u232b"),
        SpecialKey("{Del}", "Delete forwards", "\u2326"),
        SpecialKey("{Left}", "Left arrow", "\u2190"),
        SpecialKey("{Right}", "Right arrow", "\u2192"),
        SpecialKey("{Up}", "Up arrow", "\u2191"),
        SpecialKey("{Down}", "Down arrow", "\u2193"),
        SpecialKey("{Home}", "Start of line", "\u21e4"),
        SpecialKey("{End}", "End of line", "\u21e5"),
        SpecialKey("{PgUp}", "Page up", "\u21de"),
        SpecialKey("{PgDn}", "Page down", "\u21df"),
        SpecialKey("{Ctrl+A}", "Select all", "A"),
        SpecialKey("{Ctrl+C}", "Copy", "C"),
        SpecialKey("{Ctrl+X}", "Cut", "X"),
        SpecialKey("{Ctrl+V}", "Paste", "V"),
        SpecialKey("{Ctrl+Z}", "Undo", "\u21b6"),
    )
}
