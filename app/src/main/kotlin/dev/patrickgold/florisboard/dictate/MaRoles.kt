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

import dev.patrickgold.florisboard.dictate.provider.ProviderAccounts

/**
 * Which provider does which job. Decided here, not on a screen.
 *
 * ### Why this exists
 *
 * The API keys screen used to ask. Each provider showed a `transcription` and a `rewording` chip,
 * and whichever was selected got the job. That is a reasonable design for somebody choosing between
 * interchangeable services, and it was wrong here, because these three are not interchangeable and
 * the roles have not been in question for a long time.
 *
 * What it cost: Groq arrived, advertised transcription because it genuinely can transcribe, and
 * ended up holding the transcription role. Every word Marko dictated went to Groq's Whisper instead
 * of AssemblyAI — silently, with the screen showing it as a correct configuration. The Croatian
 * async rule lives in the AssemblyAI path, so while that was true the rule was protecting nothing,
 * because AssemblyAI was not being called.
 *
 * A setting that can be wrong in a way nothing announces is not a setting, it is a trap. The roles
 * are fixed now and there is nowhere left to set them incorrectly.
 *
 * ### Why resolving beats writing once
 *
 * These functions are read at the moment the job is done rather than written into preferences at
 * setup. A stored value can drift — an import, a migration, a stale install, a screen somewhere
 * else — and drift is the whole failure. Reading the answer fresh means a wrong value cannot
 * survive, because nothing ever consults it.
 *
 * The stored preference is still honoured as a **fallback**, and only when the fixed provider has
 * no key at all. That keeps a custom endpoint or an unusual setup working instead of pointing the
 * app at a provider it cannot reach.
 */
object MaRoles {

    /** Speech to text. */
    const val TRANSCRIPTION = "assemblyai"

    /** Rewording, proofreading, Ctrl+P, and the restyle prompts. */
    const val REWORDING = "anthropic"

    /**
     * Working out which language is being spoken, from the first seconds of audio.
     *
     * Named here before the feature that uses it exists, so that Groq has a role of its own and is
     * never offered the transcription job it is not meant to hold. See §28.
     */
    /** Removed with the language probe. Nothing holds this role now. */
    const val LANGUAGE = ""

    private fun hasKey(accounts: ProviderAccounts, id: String): Boolean =
        accounts.accounts[id]?.apiKey.orEmpty().isNotBlank()

    /**
     * The provider that transcribes.
     *
     * AssemblyAI whenever it has a key, and it is the only one that should. Groq can transcribe and
     * must never be picked for it here: its job is the language, and letting it take this role is
     * exactly the failure this object was written for.
     */
    fun transcription(accounts: ProviderAccounts, stored: String): String = when {
        hasKey(accounts, TRANSCRIPTION) -> TRANSCRIPTION
        // No AssemblyAI key: fall back to what is stored, except never to the language provider,
        // which would transcribe everything through a service chosen for something else.
        stored == LANGUAGE -> TRANSCRIPTION
        else -> stored
    }

    /** The provider that rewords and proofreads. */
    fun rewording(accounts: ProviderAccounts, stored: String): String = when {
        hasKey(accounts, REWORDING) -> REWORDING
        stored == LANGUAGE -> REWORDING
        else -> stored
    }

    /** The label shown on the keys screen, or null for a provider with no fixed job. */
    fun roleLabel(providerId: String): String? = when (providerId) {
        TRANSCRIPTION -> "transcription"
        REWORDING -> "rewording and proofreading"
        LANGUAGE -> "language detection"
        else -> null
    }
}
