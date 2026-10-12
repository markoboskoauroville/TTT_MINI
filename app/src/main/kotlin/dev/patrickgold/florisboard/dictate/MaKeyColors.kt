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

import java.lang.Character
import kotlin.collections.LinkedHashMap

/**
 * A colour per key, like the Logitech G815: every key can wear its own.
 *
 * His ask (12.10.2026): all the keys on one grid in settings, tap one, give it a colour. The point
 * is finding things — the macro digits, the keys he reaches for most — by colour before reading.
 * A key with no colour of its own keeps the theme's, so nothing changes until he paints it.
 *
 * ### What a key is called
 *
 * Main keyboard keys by their key CODE, lower-cased: `k97` is a, whether shifted or not, so a letter
 * keeps its colour through shift. Specials by their own code: `k-11` and so on. The number row by
 * POSITION, `n0` … `n9`, because its labels change with the set (digits, Croatian, brackets…) and
 * the colour belongs to the place the thumb goes, not to what happens to be printed there.
 *
 * ### Stored as text
 *
 * `k97=#C0392B n0=#2E86DE`, space separated. Readable in an export, and a value that cannot be read
 * is skipped rather than allowed to take the keyboard down.
 *
 * Pure: strings in, colours out. Walked by `scripts/test_key_colors.py`.
 */
object MaKeyColors {

    /** A named colour on the picker. [argb] includes full alpha. */
    data class Swatch(val name: String, val argb: Int)

    /**
     * The picker: G815-style saturated colours that stay readable with light and dark text, plus a
     * few muted ones for keys that should be quietly marked rather than shout.
     */
    val PALETTE: List<Swatch> = listOf(
        Swatch("Red", 0xFFC0392B.toInt()),
        Swatch("Orange", 0xFFE67E22.toInt()),
        Swatch("Amber", 0xFFF1C40F.toInt()),
        Swatch("Lime", 0xFF7CB342.toInt()),
        Swatch("Green", 0xFF27AE60.toInt()),
        Swatch("Teal", 0xFF16A085.toInt()),
        Swatch("Cyan", 0xFF00ACC1.toInt()),
        Swatch("Blue", 0xFF2E86DE.toInt()),
        Swatch("Indigo", 0xFF3F51B5.toInt()),
        Swatch("Violet", 0xFF8E44AD.toInt()),
        Swatch("Magenta", 0xFFD81B60.toInt()),
        Swatch("Pink", 0xFFF06292.toInt()),
        Swatch("White", 0xFFECEFF1.toInt()),
        Swatch("Grey", 0xFF78909C.toInt()),
        Swatch("Brown", 0xFF795548.toInt()),
        Swatch("Rust", 0xFF9B3B33.toInt()),
    )

    /** The id of a main keyboard key from its code. Letters fold to lower case. */
    fun idForCode(code: Int): String =
        "k" + (if (code > 0 && Character.isValidCodePoint(code)) Character.toLowerCase(code) else code)

    /**
     * The id of a feature-row key from its token: the built-in key's name, `C3` for a bucket, `M2`
     * for a macro. By what the key IS, so a key moved between rows keeps its colour.
     */
    fun idForFeature(token: String): String = "f:$token"

    /** The id of the number row's key at [index], 0 for the leftmost. */
    fun idForNumberRow(index: Int): String = "n$index"

    private val HEX = Regex("^#?([0-9A-Fa-f]{6})$")

    /** The stored map. Never throws; a malformed entry is skipped. */
    fun parse(raw: String): Map<String, Int> {
        val out = LinkedHashMap<String, Int>()
        for (entry in raw.split(' ', '\n', ';')) {
            val eq = entry.indexOf('=')
            if (eq <= 0) continue
            val id = entry.substring(0, eq).trim()
            val hex = HEX.matchEntire(entry.substring(eq + 1).trim())?.groupValues?.get(1) ?: continue
            if (id.isEmpty()) continue
            out[id] = (0xFF000000.toInt()) or hex.toInt(16)
        }
        return out
    }

    fun serialize(map: Map<String, Int>): String =
        map.entries.joinToString(" ") { (id, argb) -> "$id=${hex(argb)}" }

    /** `#RRGGBB`, upper case. */
    fun hex(argb: Int): String = "#%06X".format(argb and 0xFFFFFF)

    /** [map] with [id] painted [argb], or cleared when [argb] is null. */
    fun with(map: Map<String, Int>, id: String, argb: Int?): Map<String, Int> =
        LinkedHashMap(map).apply { if (argb == null) remove(id) else put(id, argb) }

    /**
     * Whether text on [argb] should be dark.
     *
     * Whichever of near-black and white has the higher WCAG contrast against it — relative
     * luminance compared at the crossover, about 0.18. Only the deep colours (red, indigo, violet,
     * magenta, brown, rust) keep white text; the bright ones read better in near-black. He has low vision; a label the colour behind it swallows is a key he cannot read,
     * which is worse than no colour.
     */
    fun wantsDarkText(argb: Int): Boolean {
        fun channel(c: Int): Double {
            val v = c / 255.0
            return if (v <= 0.03928) v / 12.92 else Math.pow((v + 0.055) / 1.055, 2.4)
        }
        val l = 0.2126 * channel((argb shr 16) and 0xFF) +
            0.7152 * channel((argb shr 8) and 0xFF) +
            0.0722 * channel(argb and 0xFF)
        // Contrast with black (l + 0.05) / 0.05 against contrast with white 1.05 / (l + 0.05).
        return (l + 0.05) / 0.05 > 1.05 / (l + 0.05)
    }

    /** The colour while pressed: the same hue, darker, so a press still shows. */
    fun pressed(argb: Int): Int {
        fun dim(c: Int) = (c * 0.7).toInt().coerceIn(0, 255)
        return (0xFF shl 24) or (dim((argb shr 16) and 0xFF) shl 16) or
            (dim((argb shr 8) and 0xFF) shl 8) or dim(argb and 0xFF)
    }
}
