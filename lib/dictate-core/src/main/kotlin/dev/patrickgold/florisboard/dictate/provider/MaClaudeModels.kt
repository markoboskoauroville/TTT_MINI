/*
 * Copyright (C) 2026 Marko Bosko, Mantra Productions
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package dev.patrickgold.florisboard.dictate.provider

/**
 * ALWAYS THE NEWEST CLAUDE OF A TIER, ASKED OF ANTHROPIC RATHER THAN WRITTEN DOWN.
 *
 * Anthropic name their models `claude-<tier>-<major>-<minor>`: `claude-haiku-4-5`, `claude-haiku-5-5`,
 * `claude-sonnet-5`, `claude-opus-5-5`. The tier stays; the number moves. Every id that was ever
 * written into this app went stale the same way — `claude-haiku-4-5-20251001` was the default for
 * months after Haiku 5.5 shipped, cheaper and better.
 *
 * So the app stores a TIER, not a number: `claude-haiku-latest`. That is not an id Anthropic serves;
 * it is resolved here against `GET /v1/models`, which lists every model the key can use. The newest
 * version of the tier on that list is the one sent. When Anthropic ship the next Haiku, the next
 * request after the catalog is re-read uses it, with no app update.
 *
 * ### The same evidence rule as MaModelHealing
 *
 * **Only a name the provider listed is ever sent.** Nothing here bumps a number and hopes. If the
 * list cannot be read, the [FALLBACK] ids are used — real ids, checked when this was written.
 *
 * ### A tier never changes
 *
 * Haiku resolves to a Haiku. An alias or a healed id never crosses to Sonnet or Opus, because a
 * tier change is a price change and that is his decision, not the code's.
 *
 * Pure: strings in, a decision out. Walked by `scripts/test_claude_models.py`.
 */
object MaClaudeModels {

    /** Cheapest first. Mythos is left out on purpose: it is not generally available. */
    val TIERS = listOf("haiku", "sonnet", "opus", "fable")

    private const val LATEST = "-latest"

    /** The stored form meaning "the newest of this tier". */
    fun alias(tier: String): String = "claude-$tier$LATEST"

    /** The default for every Claude job in this app: the newest Haiku. */
    val DEFAULT = alias("haiku")

    /**
     * Real ids, used only when the catalog cannot be read (no network, key refused the list).
     * Checked against Anthropic's model list on 12.10.2026.
     */
    val FALLBACK = mapOf(
        "haiku" to "claude-haiku-5-5",
        "sonnet" to "claude-sonnet-5-5",
        "opus" to "claude-opus-5-5",
        "fable" to "claude-fable-5-1",
    )

    /**
     * The id that shipped as the default before this file existed.
     *
     * Stored on a phone only because the picker wrote the default back, not because he chose it, so
     * it is read as "the newest Haiku". Any other concrete id is his choice and is respected.
     */
    private const val OLD_DEFAULT = "claude-haiku-4-5-20251001"

    data class Version(val tier: String, val major: Int, val minor: Int, val dated: Boolean)

    // claude-haiku-4-5, claude-haiku-4-5-20251001, claude-sonnet-5, claude-opus-4-20250514
    private val NEW_STYLE = Regex("""^claude-([a-z]+)-(\d+)(?:-(\d{1,2}))?(?:-(\d{8}))?$""")

    // claude-3-5-haiku-20241022, claude-3-opus-20240229, claude-3-5-sonnet-latest
    private val OLD_STYLE = Regex("""^claude-(\d+)(?:-(\d{1,2}))?-([a-z]+)(?:-(\d{8})|-latest)?$""")

    /** The tier and version of a Claude id, or null for anything that is not one. */
    fun parse(id: String): Version? {
        val s = id.trim().lowercase()
        NEW_STYLE.matchEntire(s)?.let { m ->
            val tier = m.groupValues[1]
            if (tier !in TIERS) return null
            return Version(tier, m.groupValues[2].toInt(), m.groupValues[3].toIntOrNull() ?: 0, m.groupValues[4].isNotEmpty())
        }
        OLD_STYLE.matchEntire(s)?.let { m ->
            val tier = m.groupValues[3]
            if (tier !in TIERS) return null
            return Version(tier, m.groupValues[1].toInt(), m.groupValues[2].toIntOrNull() ?: 0, m.groupValues[4].isNotEmpty())
        }
        return null
    }

    /**
     * The model's name as Anthropic write it: `claude-haiku-5-5` → "Claude Haiku 5.5",
     * `claude-sonnet-5` → "Claude Sonnet 5". An id that is not a Claude is returned as it is.
     */
    fun displayName(id: String): String {
        val v = parse(id) ?: return id
        val number = if (v.minor == 0) "${v.major}" else "${v.major}.${v.minor}"
        return "Claude ${v.tier.replaceFirstChar { it.uppercase() }} $number"
    }

    /** The tier an alias names, or null if [model] is not one of ours. */
    fun tierOfAlias(model: String): String? {
        val s = model.trim().lowercase()
        if (!s.startsWith("claude-") || !s.endsWith(LATEST)) return null
        return s.removePrefix("claude-").removeSuffix(LATEST).takeIf { it in TIERS }
    }

    fun isAlias(model: String): Boolean = tierOfAlias(model) != null

    /** Compared as numbers: 4.10 is newer than 4.9. */
    private fun newer(a: Version, b: Version): Boolean =
        if (a.major != b.major) a.major > b.major else a.minor > b.minor

    /**
     * The newest model of [tier] on [available], or null if there is none.
     *
     * Of two ids with the same version, the undated one wins (`claude-haiku-4-5` over
     * `claude-haiku-4-5-20251001`): it is the name Anthropic keep pointing at that version.
     */
    fun newest(tier: String, available: List<String>): String? =
        available.mapNotNull { id -> parse(id)?.takeIf { it.tier == tier }?.let { id.trim() to it } }
            .sortedWith(
                compareByDescending<Pair<String, Version>> { it.second.major }
                    .thenByDescending { it.second.minor }
                    .thenBy { it.second.dated }
                    .thenBy { it.first.length },
            )
            .firstOrNull()?.first

    /**
     * The id to send for [model].
     *
     * An alias becomes the newest of its tier on [available] — or the [FALLBACK] when the list is
     * empty or holds nothing of that tier. Anything else is sent as it is.
     */
    fun resolve(model: String, available: List<String>): String {
        val stored = upgradeOldDefault(model)
        val tier = tierOfAlias(stored) ?: return stored
        // An alias Anthropic really serve is used as served.
        if (available.any { it.equals(stored, ignoreCase = true) }) return stored
        return newest(tier, available) ?: FALLBACK.getValue(tier)
    }

    /** The old shipped default reads as the newest Haiku; every other id is left alone. */
    fun upgradeOldDefault(model: String): String =
        if (model.trim().equals(OLD_DEFAULT, ignoreCase = true)) DEFAULT else model

    /**
     * What to try instead of [current] after Anthropic refused it as a model.
     *
     * The newest of the same tier on [available], provided it is a different id. Null means stop
     * and report: nothing listed qualifies, and a guess is never made.
     */
    fun successor(current: String, available: List<String>): String? {
        val tier = parse(current)?.tier ?: tierOfAlias(current) ?: return null
        val next = newest(tier, available) ?: return null
        return next.takeIf { !it.equals(current.trim(), ignoreCase = true) }
    }

    /**
     * Whether Anthropic refused the request because of the MODEL — retired, renamed, or never
     * existed. Never a key, quota or rate problem: those belong to the key ring.
     */
    fun isModelFailure(status: Int?, message: String?, model: String): Boolean {
        if (status == null || status == 401 || status == 403 || status == 429) return false
        if (status == 410) return true
        if (status != 404 && status != 400) return false
        val hay = message.orEmpty().lowercase()
        val namesModel = model.isNotBlank() && hay.contains(model.lowercase())
        val saysModel = hay.contains("model") && (
            hay.contains("not found") || hay.contains("not_found") || hay.contains("does not exist") ||
                hay.contains("deprecated") || hay.contains("retired") || hay.contains("invalid") ||
                hay.contains("not supported") || hay.contains("unsupported") || status == 404
            )
        return namesModel || saysModel
    }

    /**
     * Whether [model] accepts `output_config.effort`.
     *
     * Sonnet and Opus from 4.6, every Haiku from 5, every Fable. Haiku 4.5 and the 4.5-and-older
     * Sonnets reject it with a 400, so it is left off for them rather than sent and retried.
     */
    fun supportsEffort(model: String): Boolean {
        val v = parse(model) ?: return false
        return when (v.tier) {
            "fable" -> true
            "haiku" -> v.major >= 5
            else -> v.major > 4 || (v.major == 4 && v.minor >= 6)
        }
    }

    /**
     * How to switch thinking OFF for [model], or null to leave the request as it is.
     *
     * Found on the phone (build 398): the spell check showed 1,051 output tokens for 480 in. On the
     * newest Haiku thinking is on by default and billed as output, and a spell check gains nothing
     * from it. Only where Anthropic accept switching it off: `disabled` on Haiku 5.x, Sonnet 5 and
     * Opus 5; `between_tools` on Sonnet 5.5. Opus 5.5 and Fable cannot switch it off and older
     * models do not think unless asked, so they get nothing. A 400 about thinking is retried without
     * it (OpenAiCompatibleClient), so a wrong guess costs one request, never the answer.
     */
    fun thinkingOff(model: String): String? {
        val v = parse(model) ?: return null
        return when {
            v.tier == "haiku" && v.major >= 5 -> "disabled"
            v.tier == "sonnet" && v.major == 5 && v.minor == 0 -> "disabled"
            v.tier == "sonnet" && v.major == 5 && v.minor == 5 -> "between_tools"
            v.tier == "opus" && v.major == 5 && v.minor == 0 -> "disabled"
            else -> null
        }
    }

    /**
     * The effort to send, from the app's reasoning setting.
     *
     * The newest models cannot switch thinking off, so "off" and "minimal" become `low`, which is
     * the quickest they go. Proofreading a paragraph is not improved by more, and is slower.
     */
    fun effortFor(reasoningWire: String?): String = when (reasoningWire?.trim()?.lowercase()) {
        "medium" -> "medium"
        "high" -> "high"
        "xhigh" -> "xhigh"
        "max" -> "max"
        else -> "low"
    }
}
