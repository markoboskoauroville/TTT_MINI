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
 * THE PIECES OF A PARALLEL DOWNLOAD, AND WHICH ARE DONE — aria2's `.aria2` file, for this app.
 *
 * Marko (20.10.2026): "build aria2 inside my application, so it downloads in parallel and can resume
 * a broken download." Hugging Face's CDN reset the connection twice in the first 70 MB on his phone;
 * aria2 with eight connections finished the same 2.4 GB at 2.6 MB/s and passed its checksum.
 *
 * A file is cut into pieces of [PIECE_BYTES]. Each piece is fetched with its own HTTP Range request
 * and written at its own offset into one file of the final size. A small control file beside it
 * says which pieces are whole, written after every piece, so a dropped connection, an app kill or a
 * phone restart costs at most the pieces that were in flight.
 *
 * Pure: numbers and strings in and out, walked by `scripts/test_parallel_download.py` and compiled
 * with kotlinc against its cases.
 */
object MaPieces {

    /** 8 MB: small enough that a dropped piece costs little, large enough to keep a request busy. */
    const val PIECE_BYTES = 8L * 1024 * 1024

    /** Connections at once, as `aria2c -x 8 -s 8`. */
    const val CONNECTIONS = 8

    /** Below this a file is fetched on one connection: splitting a small file only adds requests. */
    const val PARALLEL_MIN_BYTES = 64L * 1024 * 1024

    /** Tries in a row on one piece that bring nothing before the download stops. */
    const val MAX_TRIES_WITHOUT_PROGRESS = 8

    /** How many pieces [total] bytes make. */
    fun count(total: Long, pieceBytes: Long = PIECE_BYTES): Int =
        if (total <= 0) 0 else ((total + pieceBytes - 1) / pieceBytes).toInt()

    /** First and last byte of piece [index], inclusive, as an HTTP Range wants them. */
    fun range(index: Int, total: Long, pieceBytes: Long = PIECE_BYTES): LongRange {
        val start = index * pieceBytes
        val end = minOf(total, start + pieceBytes) - 1
        return start..end
    }

    /** Bytes in piece [index]. */
    fun length(index: Int, total: Long, pieceBytes: Long = PIECE_BYTES): Long =
        range(index, total, pieceBytes).let { it.last - it.first + 1 }

    /** Wait before try number [tries] (1-based): 2, 4, 8, 16, 32, then 60 seconds. */
    fun backoffMillis(tries: Int): Long =
        if (tries <= 0) 0L else minOf(2_000L shl (minOf(tries, 6) - 1), 60_000L)

    /**
     * The control file: the size and piece size it was made for, then one character per piece,
     * `1` for whole. A file made for another size or piece size is not trusted, because its marks
     * would point at the wrong bytes.
     */
    fun serialize(
        total: Long,
        done: BooleanArray,
        pieceBytes: Long = PIECE_BYTES,
        partial: Map<Int, Long> = emptyMap(),
    ): String =
        "size=$total\npiece=$pieceBytes\ndone=" + done.joinToString("") { if (it) "1" else "0" } + "\n" +
            "partial=" + partial.entries.filter { it.value > 0 && !done.getOrElse(it.key) { true } }
                .sortedBy { it.key }.joinToString(",") { "${it.key}:${it.value}" } + "\n"

    /**
     * How far each unfinished piece had got, from its own first byte — so a piece cut off at 7 MB
     * carries on from 7 MB instead of from 0. Entries that do not fit the file are dropped.
     */
    fun parsePartial(raw: String?, total: Long, pieceBytes: Long = PIECE_BYTES): Map<Int, Long> {
        if (raw.isNullOrBlank()) return emptyMap()
        val line = raw.lines().firstOrNull { it.startsWith("partial=") }?.removePrefix("partial=") ?: return emptyMap()
        val n = count(total, pieceBytes)
        return line.split(',').mapNotNull { e ->
            val (i, b) = e.split(':').takeIf { it.size == 2 } ?: return@mapNotNull null
            val index = i.trim().toIntOrNull() ?: return@mapNotNull null
            val bytes = b.trim().toLongOrNull() ?: return@mapNotNull null
            if (index !in 0 until n || bytes <= 0 || bytes >= length(index, total, pieceBytes)) null else index to bytes
        }.toMap()
    }

    /** Which pieces are whole, or null when [raw] is not a control file for this size and piece. */
    fun parse(raw: String?, total: Long, pieceBytes: Long = PIECE_BYTES): BooleanArray? {
        if (raw.isNullOrBlank()) return null
        val fields = raw.lines().mapNotNull { line ->
            val eq = line.indexOf('=')
            if (eq <= 0) null else line.substring(0, eq).trim() to line.substring(eq + 1).trim()
        }.toMap()
        if (fields["size"]?.toLongOrNull() != total) return null
        if (fields["piece"]?.toLongOrNull() != pieceBytes) return null
        val marks = fields["done"] ?: return null
        val n = count(total, pieceBytes)
        if (marks.length != n || marks.any { it != '0' && it != '1' }) return null
        return BooleanArray(n) { marks[it] == '1' }
    }

    /**
     * Pieces already whole in a file that was being fetched from the start on ONE connection: every
     * piece that ends inside its first [have] bytes. This is how a download begun by the older,
     * single-connection code carries on in pieces instead of starting again.
     */
    fun fromPrefix(have: Long, total: Long, pieceBytes: Long = PIECE_BYTES): BooleanArray {
        val n = count(total, pieceBytes)
        return BooleanArray(n) { i -> range(i, total, pieceBytes).last < have }
    }

    /** Pieces still to fetch, in order. */
    fun missing(done: BooleanArray): List<Int> = done.indices.filter { !done[it] }

    /** Bytes already whole. */
    fun doneBytes(done: BooleanArray, total: Long, pieceBytes: Long = PIECE_BYTES): Long =
        done.indices.filter { done[it] }.sumOf { length(it, total, pieceBytes) }
}
