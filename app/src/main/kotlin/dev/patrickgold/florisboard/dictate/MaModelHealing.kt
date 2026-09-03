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
 * WHEN A MODEL IS RETIRED, FIND ITS REPLACEMENT AND KEEP GOING.
 *
 * Speechify are retiring `simba-english` and `simba-multilingual` on 21 November 2026. That notice
 * arrived by email. **The next one might not**, or might arrive while he is on a shoot, and the first
 * he would know is a reading key that does nothing.
 *
 * So the app repairs itself: a request that fails because of the MODEL asks the provider what models
 * exist, picks the successor, writes it down, and retries. The next run starts on the new name with
 * nothing to do.
 *
 * ### The rule that makes this possible, and its limit
 *
 * **The family name stays; the number moves.** `simba-english` → `simba-3.2`,
 * `simba-multilingual` → `simba-3.0`, `whisper-large-v2` → `whisper-large-v3`. Providers rename the
 * generation, not the product, because their own customers have the old name written down.
 *
 * That is a pattern, not a law, and this code treats it as one: it will only rewire to a candidate
 * that shares the family AND is offered by the provider right now. **A guess that cannot be checked
 * against the provider's own list is not made.**
 *
 * ### Never on the happy path
 *
 * The probe runs only after a request has failed in a way that names the model. A working setup makes
 * no extra calls, ever — the same discipline the key ring uses, and for the same reason: a check that
 * costs something on every run is a check that gets switched off.
 *
 * ### Why it writes the answer down
 *
 * Repairing in memory fixes one session. He would pay the failed request, the probe and the retry
 * again tomorrow, and every day after, without ever being told why the first reading of the day is
 * slow. Persisted, the repair happens once in the life of the model.
 *
 * Everything here is pure: strings in, a decision out. No network, so the whole thing can be walked.
 */
object MaModelHealing {

    /**
     * Whether a failure is about the MODEL, as opposed to the key, the network or the text.
     *
     * Conservative on purpose. **A probe fired at the wrong failure is a wasted round trip and a
     * rewire aimed at a model that was never the problem** — worse than the failure it was answering,
     * because it can write a wrong name into the settings and make tomorrow fail too.
     *
     * 404 and 400 with the model named; 410 outright, which is what a retired endpoint returns.
     * Never 401, 403 or 429: those are the key ring's, and it already knows what to do with them.
     */
    fun isModelFailure(status: Int, body: String, model: String): Boolean {
        if (status == 401 || status == 403 || status == 429) return false
        val hay = body.lowercase()
        if (status == 410) return true
        val namesModel = model.isNotBlank() && hay.contains(model.lowercase())
        val saysModel = hay.contains("model") &&
            (hay.contains("not found") || hay.contains("deprecated") || hay.contains("retired") ||
                hay.contains("unavailable") || hay.contains("invalid") || hay.contains("unsupported"))
        return (status == 404 || status == 400) && (namesModel || saysModel)
    }

    /**
     * The family of a model id: everything before the version.
     *
     * `simba-english` → `simba`, `simba-3.2` → `simba`, `whisper-large-v3` → `whisper-large`.
     *
     * A trailing segment counts as a version if it is a number, a dotted number, or `v` followed by
     * one. **A word is not a version** — `simba-english` keeps `english` out of the family precisely
     * so it can match `simba-3.0`, which is the migration Speechify is asking for.
     */
    fun family(model: String): String {
        val parts = model.lowercase().trim().split('-')
        if (parts.size < 2) return parts.firstOrNull().orEmpty()
        val last = parts.last()
        val isVersion = last.removePrefix("v").isNotBlank() &&
            last.removePrefix("v").all { it.isDigit() || it == '.' }
        // A trailing VERSION is dropped and nothing else. An earlier version of this also dropped a
        // trailing word, so `whisper-large` collapsed to `whisper` — and `whisper-tiny` with it,
        // which would have let a tiny model replace a large one. The test caught it.
        //
        // Keeping the word means `simba-english` has family `simba-english`, which matches no
        // successor on its own. That is handled in `successor` by a second, narrower fallback rather
        // than by loosening this — see there.
        val head = if (isVersion) parts.dropLast(1) else parts
        return head.joinToString("-")
    }

    /** A version as a comparable list of integers. `3.2` → [3, 2]; nothing → empty. */
    fun version(model: String): List<Int> {
        val last = model.lowercase().trim().split('-').last().removePrefix("v")
        if (last.isBlank() || !last.all { it.isDigit() || it == '.' }) return emptyList()
        return last.split('.').mapNotNull { it.toIntOrNull() }
    }

    /**
     * Compares two version lists, shorter padded with zeros. `3.2` beats `3`; `3.10` beats `3.9`.
     *
     * Segment by segment as INTEGERS, never as text: `"3.10" > "3.9"` is false as a string and true
     * as a version, and that is the comparison every naive implementation of this gets wrong.
     */
    fun newer(a: List<Int>, b: List<Int>): Boolean {
        val n = maxOf(a.size, b.size)
        for (i in 0 until n) {
            val x = a.getOrElse(i) { 0 }
            val y = b.getOrElse(i) { 0 }
            if (x != y) return x > y
        }
        return false
    }

    /**
     * Picks the successor to [current] from what the provider says it offers.
     *
     * Rules, in order, and each exists because of a way this could go wrong:
     *
     *  1. **Same family only.** Never swap `whisper` for `simba`; the voices would not exist.
     *  2. **Must be in [available].** The provider's own list is the only evidence that a name works.
     *     A constructed guess — bump the number and hope — is what turns one broken model into two.
     *  3. **Highest version wins**, compared as numbers.
     *  4. **Never return [current]**, or a retry would repeat the failure for ever.
     *
     * Returns null when nothing qualifies, and null means STOP — report the failure to him rather
     * than substituting something that might sound like a stranger reading his text.
     */
    fun successor(current: String, available: List<String>): String? {
        val fam = family(current)
        if (fam.isBlank()) return null
        val cur = version(current)
        val pool = available.map { it.trim() }
            .filter { it.isNotBlank() && it.lowercase() != current.lowercase() && version(it).isNotEmpty() }

        fun best(candidates: List<String>) = candidates
            .filter { newer(version(it), cur) || cur.isEmpty() }
            .maxByOrNull { version(it).let { v -> v.getOrElse(0) { 0 } * 1000 + v.getOrElse(1) { 0 } } }

        // FIRST: the exact family. `whisper-large-v2` finds `whisper-large-v3` and never
        // `whisper-tiny-v3`, because the size is part of the family and losing it loses the model.
        best(pool.filter { family(it) == fam })?.let { return it }

        // THEN, and only for a model with NO VERSION of its own: the leading segment.
        //
        // `simba-english` and `simba-multilingual` are named variants rather than numbered ones, and
        // their successors are `simba-3.2` and `simba-3.0` — a different shape of name entirely.
        // This is the case Speechify's migration actually is.
        //
        // Restricted to unversioned models on purpose: with it applied to `whisper-large-v2` the
        // leading segment would be `whisper` and a tiny model would qualify again.
        if (cur.isNotEmpty()) return null
        val stem = fam.substringBefore('-')
        if (stem.isBlank()) return null
        return best(pool.filter { family(it).substringBefore('-') == stem && version(it).isNotEmpty() })
    }

    /**
     * The stored rewires, as `old=new` pairs separated by spaces.
     *
     * A map rather than a single value: several models can be retired in one round, and a run that
     * repaired one must not forget it while repairing the next.
     */
    fun parseRewires(raw: String): Map<String, String> =
        raw.split(' ').filter { it.contains('=') }.associate {
            val (from, to) = it.split('=', limit = 2)
            from.trim() to to.trim()
        }

    fun serializeRewires(map: Map<String, String>): String =
        map.entries.joinToString(" ") { "${it.key}=${it.value}" }

    /**
     * Applies the stored rewires to a model id.
     *
     * Followed ONCE, not chased through a chain. `a=b b=c` resolves `a` to `b`, not to `c` — a chain
     * can contain a loop, and a loop here is a keyboard that hangs before it can say anything.
     * Successive retirements each write their own direct entry when they happen.
     */
    fun apply(model: String, rewires: Map<String, String>): String = rewires[model] ?: model
}
