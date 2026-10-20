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

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import dev.patrickgold.florisboard.dictate.provider.LocalTranscriptionProvider
import dev.patrickgold.florisboard.dictate.provider.MaPieces
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.security.MessageDigest

/**
 * ONE COPY OF EACH PHONE MODEL, IN `Download/models/`, SHARED WITH ARIA2 AND EVERY OTHER TOOL.
 *
 * Marko (20.10.2026): the app, aria2 and other tools share one copy, under Hugging Face's own file
 * names. A 2.4 GB model downloaded twice — once by a tool, once by the app into its private folder —
 * is a phone full of duplicates.
 *
 * ### What counts as downloaded
 *
 * A file there with the model's exact size is used at once, on every start and right after an update,
 * with no download and no question. Its SHA-256 is checked once in the background and remembered by
 * path, size and modified time, so it is hashed again only if the file changes. A wrong size or a
 * wrong hash means not downloaded, and [reason] says why, in red under the chooser.
 *
 * ### Permission
 *
 * Reading (and llama.cpp's mmap of) a file another app put in Download/ needs All files access on
 * Android 11 and later — asked once, from Settings → Prompts → Models on the phone, never at start.
 * Without it the app's private folder is used, as before.
 */
object MaSharedModels {

    enum class State { MISSING, CHECKING, READY, BAD }

    private const val PREFS = "ma_shared_models"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** Bumped when a background check finishes, so the settings screen redraws. */
    private val _tick = MutableStateFlow(0)
    val tick: StateFlow<Int> = _tick.asStateFlow()

    fun dir(): File =
        File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "models")

    /** Hugging Face's file name, from the end of the download address. */
    fun fileName(m: MaPhoneModels.PhoneModel): String =
        m.url.substringBefore('?').substringAfterLast('/')

    fun sharedFile(m: MaPhoneModels.PhoneModel): File = File(dir(), fileName(m))

    /** Whether this app may read and write Download/models/. */
    fun hasAccess(context: Context): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            context.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) ==
                PackageManager.PERMISSION_GRANTED
        }

    private fun key(f: File) = f.absolutePath
    private fun stamp(f: File) = "${f.length()}|${f.lastModified()}"

    /** The remembered verdict for [f] as it is now, or null when it has not been checked. */
    private fun verdict(context: Context, f: File): String? {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(key(f), null) ?: return null
        val s = stamp(f)
        return if (raw.startsWith("$s|")) raw.removePrefix("$s|") else null
    }

    private fun remember(context: Context, f: File, verdict: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(key(f), "${stamp(f)}|$verdict").apply()
    }

    /** Called when a download into the shared folder has just been checked whole. */
    fun rememberGood(context: Context, f: File) = remember(context, f, "ok")

    fun forget(context: Context, f: File) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().remove(key(f)).apply()
    }

    /** The state of [m]'s shared copy. */
    fun state(context: Context, m: MaPhoneModels.PhoneModel): State {
        if (!hasAccess(context)) return State.MISSING
        val f = sharedFile(m)
        if (!f.isFile) return State.MISSING
        if (f.length() != m.sizeBytes) return State.BAD
        val v = verdict(context, f) ?: return State.CHECKING
        return if (v == "ok") State.READY else State.BAD
    }

    /** Why the shared copy cannot be used, or null when it can or is not there. */
    fun reason(context: Context, m: MaPhoneModels.PhoneModel): String? {
        if (!hasAccess(context)) return null
        val f = sharedFile(m)
        if (!f.isFile) return null
        if (f.length() != m.sizeBytes) {
            return "${f.name} in Download/models is ${f.length()} bytes, not ${m.sizeBytes}: not used."
        }
        val v = verdict(context, f) ?: return null
        return if (v == "ok") null else "${f.name} in Download/models: ${v.removePrefix("bad:")}. Not used."
    }

    /** The shared file to run, when it is there at the exact size and not known to be bad. */
    fun usableFile(context: Context, m: MaPhoneModels.PhoneModel): File? =
        when (state(context, m)) {
            State.READY, State.CHECKING -> sharedFile(m)
            else -> null
        }

    /**
     * Checks, in the background, every shared model at the exact size that has not been checked in its
     * current form. Run at every start, which includes the first start after an update.
     */
    fun verifyInBackground(context: Context) {
        val app = context.applicationContext
        scope.launch {
            for (m in MaPhoneModels.all) {
                if (state(app, m) != State.CHECKING) continue
                val f = sharedFile(m)
                val verdict = runCatching {
                    if (sha256(f).equals(m.sha256, ignoreCase = true)) "ok" else "bad:checksum does not match"
                }.getOrElse { "bad:could not be read (${it.javaClass.simpleName})" }
                remember(app, f, verdict)
                _tick.update { it + 1 }
            }
        }
    }

    private fun sha256(f: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        f.inputStream().buffered(1 shl 20).use { input ->
            val buffer = ByteArray(1 shl 20)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    /**
     * Where a download of [modelId] goes: its shared file, when access is granted and the folder can
     * be made, or null for the app's private folder.
     */
    fun downloadTarget(context: Context, modelId: String): File? {
        val m = MaPhoneModels.all.firstOrNull { it.id == modelId } ?: return null
        if (!hasAccess(context)) return null
        val d = dir()
        if (!d.isDirectory && !d.mkdirs()) return null
        return sharedFile(m)
    }

    /**
     * How much of a stopped download of [m] is kept on disk, in percent, or null when none is.
     *
     * Found on the phone (build 398): after a killed download the row said only "Download", with 9 %
     * already kept and waiting. Read from the piece map when there is one (whole pieces plus how far
     * the unfinished ones got), else from the length of a one-connection partial file.
     */
    fun partialPercent(context: Context, m: MaPhoneModels.PhoneModel): Int? {
        val candidates = listOf(
            File(sharedFile(m).path + ".part"),
            File(File(LocalTranscriptionProvider.modelsRoot(context), ".tmp-${m.id}"), "model.gguf"),
        )
        for (part in candidates) {
            if (!part.isFile) continue
            val map = File(part.path + ".pieces")
            val kept = if (map.isFile) {
                val raw = runCatching { map.readText() }.getOrNull()
                val done = MaPieces.parse(raw, m.sizeBytes) ?: continue
                MaPieces.doneBytes(done, m.sizeBytes) + MaPieces.parsePartial(raw, m.sizeBytes).values.sum()
            } else {
                part.length().takeIf { it < m.sizeBytes } ?: continue
            }
            if (kept <= 0) continue
            return (kept * 100 / m.sizeBytes).toInt().coerceIn(1, 99)
        }
        return null
    }

    /** Deletes the shared copy and anything a download left beside it. */
    fun delete(context: Context, m: MaPhoneModels.PhoneModel): Boolean {
        val f = sharedFile(m)
        forget(context, f)
        File(f.path + ".part").delete()
        File(f.path + ".part.pieces").delete()
        return !f.exists() || f.delete()
    }
}
