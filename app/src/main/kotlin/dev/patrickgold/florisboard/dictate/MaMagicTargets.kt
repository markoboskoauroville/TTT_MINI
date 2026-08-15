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

    /**
     * The term that means "this is not a term, it is room".
     *
     * A reserved string rather than a fifth field, because [parse] throws away any entry whose term
     * is blank — a spacer has to carry something through storage or it will not survive being
     * written and read back. Everything that walks this list already had to skip disabled entries,
     * so skipping spacers is the same shape of check in the same places rather than a new concept.
     */
    const val SPACER = "\u001Fspacer"

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
        /** Room on the row, not a button to press. */
        val isSpacer: Boolean get() = term == SPACER

        /** What to draw on the key. */
        val face: String get() = label.ifBlank { term }
    }

    /** A new piece of room, ready to be dragged where he wants it. */
    fun spacer(): Target = Target(term = SPACER)

    /**
     * The version of the built-in defaults a stored list has already been brought up to.
     *
     * Raise this by one whenever a term is added to [defaults], and every existing list gains it
     * once. Never lower it and never reuse a number: it is a high-water mark rather than a count,
     * so a phone that skipped three versions catches up in a single step.
     */
    const val DEFAULTS_VERSION = 1

    /**
     * Adds any built-in default the stored list has never been offered, once.
     *
     * ### The problem this exists for
     *
     * [defaults] was only ever reached through `parse(raw).ifEmpty { defaults() }`, which means a
     * new default arrived on a **fresh install and nowhere else**. Marko has had a stored list for
     * months, so three keys shipped for him in build 110 were invisible on the one phone they were
     * written for, and the only way to see them was Reset, which throws away everything he taught.
     * A default that cannot reach the person who asked for it is a note in a changelog, not a
     * feature.
     *
     * ### Why it appends rather than reconciles
     *
     * A term he **deleted** must stay deleted. So this cannot ask "which defaults are missing" —
     * that question undeletes things every time it is asked. It asks a different one: has this list
     * ever been shown the current crop. Each built-in is therefore offered exactly once in the life
     * of the install, and what he does with it afterwards is his.
     *
     * Matching is by term, case-insensitively, so a default he already added by hand is not added
     * twice. New arrivals go on the end, so a row he has already arranged stays arranged.
     */
    fun mergeNewDefaults(stored: List<Target>, fromVersion: Int): List<Target> {
        if (fromVersion >= DEFAULTS_VERSION) return stored
        if (stored.isEmpty()) return defaults()
        val have = stored.map { it.term.lowercase() }.toSet()
        val additions = defaults().filter { it.term.lowercase() !in have }
        return if (additions.isEmpty()) stored else stored + additions
    }

    /**
     * The starting list: Marko's three buttons, then the obvious neighbours.
     *
     * `generate image` deliberately stops before the coin count that follows it on imgtoimg, because
     * that number changes with the model and matching the whole label would break every time the
     * price did.
     */
    /**
     * The three Marko uses, shipped so a fresh install is useful before it is taught anything.
     *
     * These are not guesses. They were read off Claude's own screen with the dump, checked against
     * a screenshot, and have been in daily use since — which is the difference between a default
     * and the five invented ones that were removed for firing everywhere and finding nothing.
     *
     * They belong to no app on purpose. The button labels are Claude's, but "Send" and "Copy
     * message" are what dozens of apps call those controls, so scoping them to one package would
     * make them useless everywhere else for no gain.
     *
     * The imgtoimg workflow, added as three more defaults: use URL, paste, add, generate.
     *
     * Shipped as one group because they are one workflow rather than three keys — the first opens
     * the dialog, the second confirms it, the third starts the render. Shipping any one without the
     * others leaves him reaching for the screen halfway through, which is the reaching this feature
     * exists to remove.
     *
     * `Generate Images` is stored without the number that follows it on screen. That number is the
     * coin cost and changes with the model, so a term carrying it would break the day he switches
     * away from Nano Banana Pro. Phrase matching finds the characters inside the longer label.
     *
     * Unscoped, like the three above. These belong to a web page rather than an app, so pinning them
     * to Firefox would break the moment he opens the same site in another browser.
     */
    fun defaults(): List<Target> = listOf(
        Target(term = "Copy message", label = "copy"),
        Target(term = "Stop responding", label = "stop"),
        Target(term = "Send", label = "send"),
        Target(term = "Use Image URL", label = "url"),
        Target(term = "Add URL", label = "+url"),
        Target(term = "Generate Images", label = "gen"),
    )

    /**
     * What the wand actually searches, in order. Unticked terms are absent, not empty strings.
     *
     * Spacers drop out here. A spacer that reached the search would be a term nobody typed being
     * hunted for on screen, and the one thing worse than a wand that finds nothing is a wand that
     * finds something.
     */
    fun activeTerms(targets: List<Target>): List<String> =
        targets.filter { it.enabled && !it.isSpacer && it.term.isNotBlank() }.map { it.term.trim() }

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
        targets.filter {
            it.enabled && !it.isSpacer && (it.appPackage == null || it.appPackage == appPackage)
        }.map { it.term }

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
