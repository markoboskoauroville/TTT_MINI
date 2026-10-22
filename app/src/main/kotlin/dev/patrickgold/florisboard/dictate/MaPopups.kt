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

import kotlin.collections.LinkedHashMap
import kotlin.jvm.Volatile

/**
 * What a long press on a letter offers — his, and nothing else (Marko, 22.10.2026: "I want to see
 * just Croatian letters popping up ... what is empty can be again configured in settings. Plus other
 * buttons can be added, not only one").
 *
 * ### Letters only, and only what is here
 *
 * A letter's long press used to collect accents from the English mapping, symbol hints and number
 * hints, merged by FlorisBoard's priority rules — so `a` offered à á â ä æ ã å ā and a symbol, none of
 * which he writes. The keyboard is Croatian and English, so a letter now offers exactly the pop-ups
 * listed here for it, in this order, and a letter with none offers nothing. The defaults are the five
 * Croatian letters: č ć on c, đ on d, š on s, ž on z. Everything else starts empty, for him to fill
 * in Settings → Pop-up Configuration.
 *
 * Non-letter keys (the period, the comma, the view switcher) keep their own popups: this decides
 * a–z and nothing more.
 *
 * ### A pop-up is any text
 *
 * One character is typed as that character and follows shift (č → Č). Several characters (`lj`,
 * `.hr`, a name) are typed as they are, capitalised with shift.
 *
 * ### Stored as text
 *
 * One letter per line, the letter then its pop-ups, separated by tabs: `c\tč\tć`. A tab and a line
 * break cannot be typed into a pop-up, so nothing he types can break the format. A line that cannot
 * be read is skipped rather than allowed to take the keyboard down.
 *
 * Pure: strings in, lists out. Walked by `scripts/test_popups.py`.
 */
object MaPopups {

    /** The five Croatian letters, where a long press finds them on every Croatian keyboard. */
    val DEFAULT: String = listOf(
        "c\tč\tć",
        "d\tđ",
        "s\tš",
        "z\tž",
    ).joinToString("\n")

    /** The keys this decides: the 26 letters, lower case. */
    val LETTERS: List<Char> = ('a'..'z').toList()

    /** The letters as they sit on the keyboard, for the settings grid when no layout has been drawn. */
    val QWERTY: List<String> = listOf("qwertyuiop", "asdfghjkl", "zxcvbnm")

    /**
     * What the keyboard reads while it lays out its keys. Set by the keyboard manager whenever the
     * preference changes, so a key computes from memory and never waits on the store.
     */
    @Volatile
    var current: Map<Char, List<String>> = parse(DEFAULT)

    /** Is [label] a letter this decides? */
    fun decides(label: String): Boolean = label.length == 1 && label[0].lowercaseChar() in LETTERS

    /** A letter's pop-ups, in order; empty when it has none. */
    fun forLabel(label: String): List<String> =
        if (decides(label)) current[label[0].lowercaseChar()].orEmpty() else emptyList()

    fun parse(raw: String?): Map<Char, List<String>> {
        val out = LinkedHashMap<Char, List<String>>()
        raw.orEmpty().split('\n').forEach { line ->
            val parts = line.split('\t')
            val key = parts.firstOrNull()?.trim()?.lowercase() ?: return@forEach
            if (key.length != 1 || key[0] !in LETTERS) return@forEach
            val popups = parts.drop(1).map { it.trim() }.filter { it.isNotEmpty() }.distinct()
            if (popups.isNotEmpty()) out[key[0]] = popups
        }
        return out
    }

    /** Letters in alphabetical order, so an export reads the same however it was edited. */
    fun serialize(map: Map<Char, List<String>>): String =
        map.entries
            .filter { it.key in LETTERS }
            .map { (k, v) -> k to v.map { clean(it) }.filter { it.isNotEmpty() }.distinct() }
            .filter { it.second.isNotEmpty() }
            .sortedBy { it.first }
            .joinToString("\n") { (k, v) -> (listOf(k.toString()) + v).joinToString("\t") }

    /** [letter]'s pop-ups replaced by [popups]; an empty list leaves it with none. */
    fun with(map: Map<Char, List<String>>, letter: Char, popups: List<String>): Map<Char, List<String>> {
        val out = LinkedHashMap(map)
        val kept = popups.map { clean(it) }.filter { it.isNotEmpty() }.distinct()
        if (kept.isEmpty()) out.remove(letter.lowercaseChar()) else out[letter.lowercaseChar()] = kept
        return out
    }

    /** A tab or a line break would split the stored line; neither can be part of a pop-up. */
    fun clean(text: String): String = text.replace('\t', ' ').replace('\n', ' ').replace('\r', ' ').trim()

    /** One code point: typed as a character that follows shift. More: typed as text. */
    fun isSingle(text: String): Boolean = text.codePointCount(0, text.length) == 1
}
