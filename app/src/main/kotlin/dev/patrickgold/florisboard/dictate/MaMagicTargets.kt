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
 * The search terms the magic button looks for, in the order it tries them.
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
    /**
     * @param term what to look for on screen
     * @param enabled whether the wand may use it
     * @param appPackage the app it was captured in, or null for "any app"
     *
     * The package is what stops one term meaning two things. "Send" in Claude and "Send" in Gemini
     * are different buttons in different apps, and a wand that pressed whichever it found first
     * would press the wrong one as soon as both were in the list. A term learned in an app belongs
     * to that app.
     *
     * Null means everywhere, which is what the built-in defaults are and what a hand-typed term
     * stays unless it was picked off a real screen.
     */
    data class Target(
        val term: String,
        val enabled: Boolean = true,
        val appPackage: String? = null,
        /**
         * What the key says, when that should not be what it searches for.
         *
         * "Stop responding" is the right thing to look for and the wrong thing to write on a key —
         * it is wider than four ordinary keys and pushes everything else off the row. A label lets
         * the term stay exact while the key stays short.
         *
         * Empty means use the term, so a target picked off a screen still shows something without
         * being named first, and naming is an improvement rather than a step.
         */
        val label: String = "",
    ) {
        /** What to draw on the key. */
        val face: String get() = label.ifBlank { term }
    }

    /**
     * The starting list: Marko's three buttons, then the obvious neighbours.
     *
     * `generate image` deliberately stops before the coin count that follows it on imgtoimg, because
     * that number changes with the model and matching the whole label would break every time the
     * price did.
     */
    /**
     * Nothing. The wand starts empty and is taught.
     *
     * It shipped with five guesses — send, add url, generate image and two spellings of pošalji —
     * and every one of them was wrong in the way that matters: they are labels somebody imagined
     * rather than labels read off a real screen, they belonged to no app so they fired everywhere,
     * and they filled the list so the terms Marko actually captured were buried among them.
     *
     * A wand that does nothing until it is taught is honest. A wand that does the wrong thing
     * because of a guess made here is not.
     */
    fun defaults(): List<Target> = emptyList()

    /** What the wand actually searches, in order. Unticked terms are absent, not empty strings. */
    fun activeTerms(targets: List<Target>): List<String> =
        targets.filter { it.enabled && it.term.isNotBlank() }.map { it.term.trim() }

    fun serialize(targets: List<Target>): String =
        targets.joinToString(SEP.toString()) {
            // The package goes last so that anything written before it existed still reads: an old
            // entry simply has two fields instead of three and comes back meaning "any app", which
            // is exactly what it meant when it was written.
            // Label last, for the same reason the package went last: anything written before it
            // existed has one field fewer and still reads, meaning "no label" — which is what it
            // meant when it was written.
            "${if (it.enabled) "1" else "0"}$FIELD${it.term}$FIELD${it.appPackage.orEmpty()}" +
                "$FIELD${it.label}"
        }

    /**
     * The terms the wand may use in [appPackage], enabled and in order.
     *
     * A term belonging to another app is skipped rather than tried. Trying it costs nothing when it
     * finds nothing, but the whole point is that it might find something — Gemini's send button
     * answering to Claude's term is the failure this exists to prevent.
     */
    fun activeTermsFor(targets: List<Target>, appPackage: String?): List<String> =
        targets.filter { it.enabled && (it.appPackage == null || it.appPackage == appPackage) }
            .map { it.term }

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
            // Two, three or four fields. Everything after the first is optional and was added over
            // time, so an older entry simply stops early and the missing parts take their defaults:
            // no app means any app, no label means show the term. An existing list keeps working
            // rather than being silently scoped to nothing or drawn blank.
            val parts = chunk.split(FIELD)
            val enabled = parts[0] == "1"
            val term = parts.getOrNull(1).orEmpty()
            val pkg = parts.getOrNull(2)?.takeIf { it.isNotBlank() }
            val label = parts.getOrNull(3).orEmpty()
            if (term.isBlank()) null else Target(term, enabled, pkg, label)
        }
    }

    fun defaultSerialized(): String = serialize(defaults())

    /** Moves a term. Out-of-range indices are ignored rather than throwing. */
    fun move(targets: List<Target>, from: Int, to: Int): List<Target> {
        if (from !in targets.indices || to !in targets.indices) return targets
        return targets.toMutableList().apply { add(to, removeAt(from)) }
    }
}
