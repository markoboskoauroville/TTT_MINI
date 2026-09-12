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

import java.io.File

/**
 * THE CLOUD READER'S LOG: everything a chat said, kept as it arrives.
 *
 * He reads Claude on his phone while it writes. The reader speaks it once and it is gone — scrolled
 * past, or lost when the chat is closed. **A conversation he listened to is a conversation he cannot
 * go back to**, and the answers are often the only place a decision was explained.
 *
 * So the same screen text the reader speaks is appended to a file, and a screen in settings lists
 * the files, opens one, and lets him jump to any sentence and read from there.
 *
 * ### What decides a "chat"
 *
 * Nothing tells this app which conversation is on screen — there is no API, only pixels read through
 * accessibility. So a log is keyed by **the first text seen when capture started**, folded to a
 * handful of words.
 *
 * That is a guess, and its failure modes are worth stating rather than hiding:
 *
 *  - Two conversations that begin with the same words share a log. Rare, and the result is an
 *    appended file rather than a lost one.
 *  - Scrolling to the top of an old chat and capturing looks like a new session. It appends to the
 *    same key, which is right.
 *  - A chat opened, closed and reopened continues its log, which is what he would want.
 *
 * **A wrong guess here costs a merged file. It never costs text**, because nothing is ever
 * overwritten and nothing is ever deleted by the capture path.
 *
 * ### Everything below is pure
 *
 * Keys, names, appending, sentence splitting — strings in, strings out. No Android, so the whole
 * thing is walked in a test before it touches a file.
 */
object MaCloudLog {

    /** Where the logs live. `filesDir`, which survives everything except uninstalling. */
    fun dir(base: File): File = File(base, "cloud_logs").apply { mkdirs() }

    /**
     * A stable key for a conversation, from the first thing it said.
     *
     * Letters only, lowercased, first eight words. Eight because it has to survive the differences
     * between two readings of the same screen — a wrapped line, a timestamp, a spinner — and still
     * separate two genuinely different conversations. Fewer collides; more breaks on a re-render.
     */
    fun keyFor(firstText: String): String {
        val words = firstText.lowercase()
            .map { if (it.isLetter()) it else ' ' }
            .joinToString("")
            .split(' ')
            .filter { it.isNotBlank() }
            .take(8)
        return if (words.isEmpty()) "untitled" else words.joinToString("-")
    }

    /**
     * The file name for a key and a day.
     *
     * The date is in the name rather than only in the metadata, because he reads file lists and a
     * name that sorts by date is a list already in the order he wants it.
     */
    fun fileNameFor(key: String, dateStamp: String): String = "$dateStamp--${key.take(60)}.txt"

    /**
     * A human title from a file name: the key, its dashes turned back into spaces.
     *
     * Cut at 60 characters in [fileNameFor], so a title is never longer than a line he can read.
     */
    fun titleOf(fileName: String): String {
        val body = fileName.removeSuffix(".txt").substringAfter("--", fileName.removeSuffix(".txt"))
        return body.replace('-', ' ').trim().ifBlank { "Untitled" }
    }

    /** The date part of a file name, or blank. For the list, which groups by day. */
    fun dateOf(fileName: String): String =
        fileName.substringBefore("--", "").takeIf { it.isNotBlank() } ?: ""

    /**
     * What to append, given what the file already ends with and what is on screen now.
     *
     * Reuses the reader's rule: if the screen begins with what was last seen, only the tail is new.
     * **The log and the reading must agree about what "new" means**, or the file will hold sentences
     * he never heard and miss ones he did.
     *
     * Returns null when there is nothing to add, which is the common case on a still screen.
     */
    fun tailToAppend(lastSeen: String, now: String): String? {
        val a = normalise(lastSeen)
        val b = normalise(now)
        if (b.isBlank() || a == b) return null
        if (a.isNotBlank() && b.startsWith(a)) {
            val addedWords = b.removePrefix(a).trim().split(' ').filter { it.isNotBlank() }.size
            if (addedWords == 0) return null
            val raw = now.trim().split(Regex("\\s+"))
            if (addedWords >= raw.size) return now.trim()
            return raw.takeLast(addedWords).joinToString(" ")
        }
        return now.trim().ifBlank { null }
    }

    /** Letters only, for comparison. The same normalisation the reader uses, for the same reasons. */
    fun normalise(text: String): String =
        text.lowercase().map { if (it.isLetter()) it else ' ' }
            .joinToString("").split(' ').filter { it.isNotBlank() }.joinToString(" ")

    /**
     * Splits a log into sentences he can tap.
     *
     * Kept here rather than reusing `MaReadChunks.sentences`, and that is a deliberate duplication:
     * the reader splits for SYNTHESIS, where a chunk too long costs latency. This splits for
     * TAPPING, where a chunk too short is a target he cannot hit and a chunk too long reads past
     * where he wanted to stop. One rule serving both would be tuned for neither.
     *
     * Blank lines survive as paragraph breaks, because a wall of text is unreadable on a phone and
     * he has low vision.
     */
    fun sentences(log: String): List<String> {
        val out = mutableListOf<String>()
        for (para in log.split("\n\n")) {
            val trimmed = para.trim()
            if (trimmed.isBlank()) continue
            var start = 0
            for (i in trimmed.indices) {
                val c = trimmed[i]
                if (c == '.' || c == '!' || c == '?') {
                    val next = trimmed.getOrNull(i + 1)
                    if (next == null || next == ' ' || next == '\n') {
                        val s = trimmed.substring(start, i + 1).trim()
                        if (s.isNotBlank()) out += s
                        start = i + 1
                    }
                }
            }
            val rest = trimmed.substring(start).trim()
            if (rest.isNotBlank()) out += rest
        }
        return out
    }

    /** Every log, newest first. The list he sees. */
    fun logsIn(base: File): List<File> =
        dir(base).listFiles()?.filter { it.isFile && it.name.endsWith(".txt") }
            ?.sortedByDescending { it.name } ?: emptyList()
}
