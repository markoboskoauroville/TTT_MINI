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

import android.content.Context
import dev.patrickgold.florisboard.dictate.provider.MaClaudeModels
import java.io.File
import java.lang.System
import kotlin.jvm.Volatile

/**
 * WHAT EACH CLAUDE REQUEST COST, WRITTEN DOWN, AND WHAT IS LEFT.
 *
 * His ask (13.10.2026): ping the API for how much money is left, and keep a log of what every
 * request took from the budget.
 *
 * ### What Anthropic will and will not say
 *
 * **There is no endpoint that tells an ordinary API key its remaining credit.** The balance lives in
 * the Console. Cost reports exist only under the Admin API, which needs an admin key (`sk-ant-admin…`)
 * and reports what was spent, not what remains. So "what is left" cannot be asked of the API with the
 * key this keyboard holds.
 *
 * What CAN be known exactly is what each request used: every answer carries its token counts and the
 * model that answered. Tokens × the published price is the cost, and that is what this log records.
 * For the balance, he types in what the Console shows, once; the app subtracts every request since
 * and shows the estimate, with the date it was set so he knows how far it has drifted.
 *
 * ### Prices
 *
 * Per million tokens, input and output, from Anthropic's price list on 13.10.2026. Thinking is billed
 * as output and is already inside the output count the API returns. A model newer than this table
 * is priced as the newest of its tier and marked estimated (`~`), rather than recorded as free.
 *
 * The log is a file of lines, newest last, kept to [MAX_LINES]. It holds no text — only time,
 * purpose, model, tokens and cost.
 */
object MaClaudeCost {

    data class Price(val inputPerMTok: Double, val outputPerMTok: Double)

    /** By exact model family; matched on the id with any date suffix removed. */
    private val PRICES: Map<String, Price> = mapOf(
        "claude-fable-5-1" to Price(10.0, 50.0),
        "claude-fable-5" to Price(10.0, 50.0),
        "claude-opus-5-5" to Price(4.0, 20.0),
        "claude-opus-5" to Price(5.0, 25.0),
        "claude-opus-4-8" to Price(5.0, 25.0),
        "claude-opus-4-7" to Price(5.0, 25.0),
        "claude-opus-4-6" to Price(5.0, 25.0),
        "claude-opus-4-5" to Price(5.0, 25.0),
        "claude-opus-4-1" to Price(15.0, 75.0),
        "claude-opus-4" to Price(15.0, 75.0),
        "claude-sonnet-5-5" to Price(2.0, 10.0),
        "claude-sonnet-5" to Price(2.0, 10.0),
        "claude-sonnet-4-6" to Price(3.0, 15.0),
        "claude-sonnet-4-5" to Price(3.0, 15.0),
        "claude-sonnet-4" to Price(3.0, 15.0),
        "claude-haiku-5-5" to Price(0.10, 0.50),
        "claude-haiku-4-5" to Price(1.0, 5.0),
        "claude-3-5-haiku" to Price(0.80, 4.0),
    )

    /** Haiku 5.5 charges more once a prompt passes 100K tokens. */
    private val HAIKU_55_LONG = Price(0.50, 2.50)

    /** The price for a model of a tier this table does not know yet: its newest known sibling. */
    private val TIER_FALLBACK: Map<String, Price> = mapOf(
        "haiku" to Price(0.10, 0.50),
        "sonnet" to Price(2.0, 10.0),
        "opus" to Price(4.0, 20.0),
        "fable" to Price(10.0, 50.0),
    )

    private val DATE_SUFFIX = Regex("-\\d{8}$")

    /** The price and whether it is exact, or null when the model is not a Claude at all. */
    fun priceFor(model: String, inputTokens: Long): Pair<Price, Boolean>? {
        val base = model.trim().lowercase().replace(DATE_SUFFIX, "")
        if (base == "claude-haiku-5-5" && inputTokens > 100_000) return HAIKU_55_LONG to true
        PRICES[base]?.let { return it to true }
        val tier = MaClaudeModels.parse(model)?.tier ?: return null
        return TIER_FALLBACK[tier]?.let { it to false }
    }

    /** Cost in US dollars, and whether it is exact. */
    fun cost(model: String, inputTokens: Long, outputTokens: Long): Pair<Double, Boolean> {
        val (price, exact) = priceFor(model, inputTokens) ?: return 0.0 to false
        val usd = inputTokens * price.inputPerMTok / 1_000_000.0 + outputTokens * price.outputPerMTok / 1_000_000.0
        return usd to exact
    }

    /** One request, as logged. */
    data class Entry(
        val atMillis: Long,
        val purpose: String,
        val model: String,
        val inputTokens: Long,
        val outputTokens: Long,
        val usd: Double,
        val exact: Boolean,
    )

    fun format(e: Entry): String = listOf(
        e.atMillis.toString(), e.purpose.replace('\t', ' ').replace('\n', ' '), e.model,
        e.inputTokens.toString(), e.outputTokens.toString(), "%.8f".format(java.util.Locale.ROOT, e.usd),
        if (e.exact) "1" else "0",
    ).joinToString("\t")

    fun parseLine(line: String): Entry? {
        val p = line.split('\t')
        if (p.size < 7) return null
        return Entry(
            atMillis = p[0].toLongOrNull() ?: return null,
            purpose = p[1],
            model = p[2],
            inputTokens = p[3].toLongOrNull() ?: return null,
            outputTokens = p[4].toLongOrNull() ?: return null,
            usd = p[5].toDoubleOrNull() ?: return null,
            exact = p[6] == "1",
        )
    }

    /** Spend since [sinceMillis], in dollars. */
    fun spentSince(entries: List<Entry>, sinceMillis: Long): Double =
        entries.filter { it.atMillis >= sinceMillis }.sumOf { it.usd }

    /** The estimate of what is left: the balance he entered minus everything spent since. */
    fun remaining(balanceUsd: Double, setAtMillis: Long, entries: List<Entry>): Double =
        balanceUsd - spentSince(entries, setAtMillis)

    // ---------------------------------------------------------------- the file

    const val MAX_LINES = 2000
    private const val FILE_NAME = "claude_costs.tsv"

    @Volatile
    private var dir: File? = null

    /** What the next recorded request was for. Set by the caller just before it sends. */
    @Volatile
    var purpose: String = "Rewording"

    /** Called once from the application, so recording needs no context of its own. */
    fun attach(context: Context) {
        dir = context.applicationContext.filesDir
    }

    private fun file(): File? = dir?.let { File(it, FILE_NAME) }

    /**
     * Records one answered request. Never throws: this is a meter, not the work, and a failed write
     * must not take the proofread down with it.
     */
    fun record(model: String, inputTokens: Long, outputTokens: Long) {
        runCatching {
            val f = file() ?: return
            val (usd, exact) = cost(model, inputTokens, outputTokens)
            val line = format(Entry(System.currentTimeMillis(), purpose, model, inputTokens, outputTokens, usd, exact))
            synchronized(this) {
                f.appendText(line + "\n")
                // Trimmed rarely and in one go, so the common case is a single append.
                if (f.length() > 400_000) {
                    val keep = f.readLines().takeLast(MAX_LINES)
                    f.writeText(keep.joinToString("\n", postfix = "\n"))
                }
            }
        }
    }

    /** Every logged request, oldest first. */
    fun load(): List<Entry> = runCatching {
        file()?.takeIf { it.exists() }?.readLines()?.mapNotNull { parseLine(it) }.orEmpty()
    }.getOrDefault(emptyList())

    fun clear() {
        runCatching { file()?.delete() }
    }
}
