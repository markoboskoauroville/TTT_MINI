/*
 * Copyright (C) 2026 Marko Bosko, Mantra Productions
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package dev.patrickgold.florisboard.dictate.llm

/**
 * KEEPING THE PHONE MODEL TO THE REWRITE. Found on the phone, build 398, test A4.
 *
 * Ctrl+F on Qwen3 4B rewrote the text and then kept going: "I'll send the link by 3pm. You can reach
 * me at jones@company.com. It's not perfect, but it's" — new sentences, an invented address, and a cut
 * mid-word. A small model decoding greedily does not decide to stop the way Claude does; it writes
 * until something stops it. Two things did not:
 *
 *  1. **The budget.** Three tokens a word of the whole message left room for twice the text again.
 *     It is now about the text's own length: a rewrite is roughly as long as what it rewrites.
 *  2. **The rules.** Nothing said "stop when it is rewritten". [RULES] says it, after his rules, in
 *     the plainest terms the small model reads.
 *
 * And if the budget is still reached, [fit] cuts back to the last whole sentence, so his message
 * never ends on half a word. Pure, compiled with kotlinc and run against its cases.
 */
object MaPhoneAnswer {

    /** Added after his rules, for the phone model only; Claude stops by itself. */
    const val RULES =
        "Reply with the rewritten text only. It must be about as long as the original text. " +
            "When the text is rewritten, stop. Do not continue it, and add no new sentences, facts, " +
            "names, times, links or contact details."

    /**
     * Tokens the answer may use: about two per word of [text] (Croatian takes more than English),
     * plus room for punctuation, and never fewer than 48. [text] is his text, not the whole message.
     */
    fun maxTokens(text: String): Int {
        val words = text.trim().split(Regex("\\s+")).count { it.isNotEmpty() }
        return maxOf(48, words * 2 + 24)
    }

    private val SENTENCE_END = Regex("""[.!?…]["”’)\]]*(?=\s|$)""")

    /**
     * [answer] as it may be typed. When the model used its whole budget ([hitCap]) the last sentence
     * may be cut, so the answer is taken back to its last whole sentence — unless that would leave
     * less than half of it, when the whole is kept rather than losing most of a message.
     */
    fun fit(answer: String, hitCap: Boolean): String {
        val t = answer.trim()
        if (!hitCap) return t
        val lastEnd = SENTENCE_END.findAll(t).lastOrNull()?.range?.last ?: return t
        val cut = t.substring(0, lastEnd + 1).trim()
        return if (cut.length >= t.length / 2) cut else t
    }
}
