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
import android.os.Build
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * What happened, with times, so a problem can be read instead of guessed at.
 *
 * ### Why this exists
 *
 * Every diagnosis in this project has been inference from a chat message. When the accessibility
 * service stopped being possible to enable, it took three builds and a broken app to find, because
 * nothing anywhere recorded whether the service had ever started. Marko asked for this four times
 * before it was built, and he was right every time: a log is not a feature competing with the
 * others, it is the thing that makes finding the others' faults cheap.
 *
 * ### It writes to a file, not just to memory
 *
 * The interesting failures happen when something does **not** start. A keyboard, an accessibility
 * service and the settings app share one process, and that process dies and restarts constantly —
 * an in-memory list would be empty at exactly the moment it was wanted. So every line goes to a file
 * in `filesDir`, which survives everything except uninstalling.
 *
 * ### It is capped, and the cap is small
 *
 * [MAX_LINES] is enough to hold a few sessions and small enough that the whole thing can be copied
 * into a chat in one tap. A log nobody can paste is a log nobody reads.
 */
object MaLog {

    private const val FILE_NAME = "ma_log.txt"
    private const val MAX_LINES = 400

    /** Trimming costs a full rewrite, so it happens in batches rather than on every line. */
    private const val TRIM_SLACK = 100

    private val stamp = SimpleDateFormat("dd.MM. HH:mm:ss", Locale.US)

    @Volatile
    private var appContext: Context? = null

    /** Called once from the application, before anything else can want to log. */
    fun initialize(context: Context) {
        appContext = context.applicationContext
        add("app", "started, build ${appBuild(context)}, Android ${Build.VERSION.SDK_INT}, ${Build.MODEL}")
    }

    private fun appBuild(context: Context): String = runCatching {
        val info = context.packageManager.getPackageInfo(context.packageName, 0)
        @Suppress("DEPRECATION")
        info.versionCode.toString()
    }.getOrDefault("?")

    private fun file(): File? = appContext?.let { File(it.filesDir, FILE_NAME) }

    /**
     * Records one line. Never throws, and never blocks on anything that could fail.
     *
     * Logging must not be able to break the thing it is watching. A log write that threw inside the
     * accessibility service would take the service down and produce exactly the silence this is
     * meant to end.
     */
    @Synchronized
    fun add(tag: String, message: String) {
        val line = "${stamp.format(Date())}  [$tag] $message"
        runCatching {
            val f = file() ?: return
            f.appendText(line + "\n")
            if (f.length() > 0 && countLines(f) > MAX_LINES + TRIM_SLACK) {
                val kept = f.readLines().takeLast(MAX_LINES)
                f.writeText(kept.joinToString("\n") + "\n")
            }
        }
    }

    private fun countLines(f: File): Int = runCatching { f.readLines().size }.getOrDefault(0)

    /** Everything recorded, oldest first. Empty when nothing has been written yet. */
    fun read(): List<String> = runCatching {
        file()?.takeIf { it.exists() }?.readLines()?.filter { it.isNotBlank() } ?: emptyList()
    }.getOrDefault(emptyList())

    /** The whole log as one string, for copying. */
    fun readAll(): String = read().joinToString("\n")

    fun clear() {
        runCatching { file()?.writeText("") }
    }
}
