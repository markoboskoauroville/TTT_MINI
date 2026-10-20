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
 * Whether a dictation needs a space in front of it. Found on the phone, build 398: "C2:" followed by
 * a dictation became "C2:Do you…".
 *
 * A space goes in when the field ends on a letter, a digit or closing punctuation and the dictation
 * starts with a letter or digit. Never after a space or line break (one is already there), never
 * after an opening bracket, quote, slash, @ or # (the next word belongs to it), and never in an
 * empty field. Pure.
 */
object MaDictationSpace {

    private const val AFTER = ".,:;!?)]}%…»”’\"'"

    fun needsLeadingSpace(before: Char?, first: Char?): Boolean {
        if (before == null || first == null) return false
        if (!first.isLetterOrDigit()) return false
        return before.isLetterOrDigit() || before in AFTER
    }
}
