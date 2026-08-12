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
 * The search terms the magic wand looks for, in the order it tries them.
 *
 * ### Order is the whole feature
 *
 * The wand presses the first term it finds on screen, so the list is a priority list rather than a
 * set. That matters on a screen carrying two of them: a page with both a `Send` and a `Save` will
 * press whichever sits higher here, and reordering is how the user says which they meant. A set
 * would leave that to chance.
 *
 * ### Why entries switch off instead of being deleted
 *
 * A term that misfires on one site is usually still wanted on another. Unticking parks it at the
 * position it already holds so it can come back without being retyped and without losing its place
 * in the order. Same reasoning as the feature row keys, and the editor is deliberately the same
 * screen shape so that one thing learned covers both.
 */
object MaMagicTargets {

    private const val SEP = '\u001E'
    private const val FIELD = '\u001D'

    /** @param term what to look for, matched against the label a screen reader would announce */
    data class Target(val term: String, val enabled: Boolean = true)

    /**
     * The starting list: Marko's three buttons, then the obvious neighbours.
     *
     * `generate image` deliberately stops before the coin count that follows it on imgtoimg, because
     * that number changes with the model and matching the whole label would break every time the
     * price did.
     */
    fun defaults(): List<Target> = listOf(
        Target("send"),
        Target("add url"),
        Target("generate image"),
        Target("pošalji"),
        Target("posalji"),
    )

    /** What the wand actually searches, in order. Unticked terms are absent, not empty strings. */
    fun activeTerms(targets: List<Target>): List<String> =
        targets.filter { it.enabled && it.term.isNotBlank() }.map { it.term.trim() }

    fun serialize(targets: List<Target>): String =
        targets.joinToString(SEP.toString()) { "${if (it.enabled) "1" else "0"}$FIELD${it.term}" }

    /**
     * Parses the stored string, dropping anything malformed rather than throwing.
     *
     * Read while the keyboard is opening. A damaged preference has to cost a search term, never a
     * keyboard that refuses to draw, because there is no route to settings from behind a keyboard
     * that never appears.
     *
     * An empty result falls back to [defaults] at the call site rather than here, so that a user who
     * has deliberately emptied the list still gets the defaults instead of a wand that does nothing
     * with no way to tell why.
     */
    fun parse(raw: String): List<Target> {
        if (raw.isBlank()) return emptyList()
        return raw.split(SEP).mapNotNull { chunk ->
            val idx = chunk.indexOf(FIELD)
            if (idx < 0) return@mapNotNull null
            // The term is the whole remainder, so it may contain anything except the separators,
            // which are control characters nobody can type on a phone.
            val term = chunk.substring(idx + 1)
            if (term.isBlank()) null else Target(term, chunk.substring(0, idx) == "1")
        }
    }

    fun defaultSerialized(): String = serialize(defaults())

    /** Moves a term. Out-of-range indices are ignored rather than throwing. */
    fun move(targets: List<Target>, from: Int, to: Int): List<Target> {
        if (from !in targets.indices || to !in targets.indices) return targets
        return targets.toMutableList().apply { add(to, removeAt(from)) }
    }
}
