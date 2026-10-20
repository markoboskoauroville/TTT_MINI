/*
 * Copyright (C) 2026 DevEmperor (Dictate)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

package dev.patrickgold.florisboard.dictate.provider

import android.content.Context
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.security.MessageDigest
import java.util.concurrent.TimeUnit
import kotlin.coroutines.coroutineContext

/**
 * Downloads, installs and removes the on-device transcription models consumed by
 * [LocalTranscriptionProvider] (issue #104). A model is a small set of files ([LocalModelSpec]) fetched
 * over HTTPS into the app's private storage; no audio or telemetry is ever sent.
 *
 * Installs are atomic: files download into a `.tmp-<id>` staging dir, are size/checksum-verified, and
 * only then replace the real model dir. A failed or cancelled download leaves any previously installed
 * model untouched. All work runs on [Dispatchers.IO] and honours coroutine cancellation.
 *
 * Dropped connections resume (Marko, 20.10.2026: "continue dropped downloads, little by little"): each
 * file is asked for again from the byte it reached (HTTP Range), up to [MAX_TRIES_WITHOUT_PROGRESS]
 * tries in a row that bring no new bytes. A download that still fails keeps its staging dir, so the
 * next start (or the app after a restart) carries on from there; only Cancel and a bad size or
 * checksum throw the bytes away.
 */
object LocalModelManager {

    /** Tries in a row that bring no new byte before a download gives up; any progress resets the count. */
    private const val MAX_TRIES_WITHOUT_PROGRESS = 8

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .callTimeout(0, TimeUnit.SECONDS) // model files are large; no overall cap on the download
            .build()
    }

    /** True if [modelId] is fully installed (all required files present). */
    fun isInstalled(context: Context, modelId: String): Boolean =
        LocalTranscriptionProvider.isInstalled(context, modelId)

    /** Ids of all catalog models currently installed on disk. */
    fun installedIds(context: Context): List<String> =
        LocalModelCatalog.all.map { it.id }.filter { isInstalled(context, it) }

    /** Removes an installed model. Returns true if the directory is gone afterwards. */
    fun delete(context: Context, modelId: String): Boolean {
        val dir = LocalTranscriptionProvider.modelDir(context, modelId)
        if (!dir.exists()) return true
        return dir.deleteRecursively()
    }

    /** On-disk size of an installed model in bytes (0 if not installed). */
    fun installedSizeBytes(context: Context, modelId: String): Long {
        val dir = LocalTranscriptionProvider.modelDir(context, modelId)
        if (!dir.exists()) return 0L
        return dir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
    }

    /**
     * Downloads and installs [spec]. [onProgress] is invoked with `(downloadedBytes, totalBytes)` as the
     * download proceeds (frequently — throttle on the UI side). Suspends until done. A dropped connection
     * is retried from where it stopped; when the tries run out it throws and keeps the staging dir for
     * the next start. Cancellation and a size/checksum mismatch clean the staging dir up.
     */
    suspend fun download(
        context: Context,
        spec: LocalModelSpec,
        onProgress: (downloaded: Long, total: Long) -> Unit = { _, _ -> },
    ): Unit = withContext(Dispatchers.IO) {
        val root = LocalTranscriptionProvider.modelsRoot(context)
        val finalDir = LocalTranscriptionProvider.modelDir(context, spec.id)
        val tmpDir = File(root, ".tmp-${spec.id}")
        // kept from an earlier attempt on purpose: its files are resumed, not fetched again
        tmpDir.mkdirs()
        check(tmpDir.isDirectory) { "could not create staging dir ${tmpDir.absolutePath}" }
        try {
            val total = spec.totalBytes
            var completed = 0L
            for (f in spec.files) {
                downloadFile(f, File(tmpDir, f.destName), completed, total, onProgress)
                completed += f.sizeBytes
                onProgress(completed, total)
            }
            // Swap staging dir into place atomically (same filesystem); copy-fallback if rename fails.
            finalDir.deleteRecursively()
            finalDir.parentFile?.mkdirs()
            if (!tmpDir.renameTo(finalDir)) {
                tmpDir.copyRecursively(finalDir, overwrite = true)
                tmpDir.deleteRecursively()
            }
        } catch (c: CancellationException) {
            tmpDir.deleteRecursively()
            throw c
        }
    }

    /** Fetches [file] into [dest], resuming a partial [dest] and retrying dropped connections. */
    private suspend fun downloadFile(
        file: LocalModelFile,
        dest: File,
        baseCompleted: Long,
        total: Long,
        onProgress: (downloaded: Long, total: Long) -> Unit,
    ) {
        if (dest.length() > file.sizeBytes) dest.delete() // left by a different file of the same name
        var tries = 0
        while (dest.length() < file.sizeBytes) {
            coroutineContext.ensureActive()
            val before = dest.length()
            try {
                fetchFrom(file, dest, baseCompleted, total, onProgress)
            } catch (e: IOException) {
                if (dest.length() > before) tries = 0
                if (++tries >= MAX_TRIES_WITHOUT_PROGRESS) {
                    throw IOException(
                        "${file.destName} stopped at ${dest.length()} of ${file.sizeBytes} bytes " +
                            "($tries tries without progress: ${e.message}). Start it again to continue.",
                        e,
                    )
                }
                delay(minOf(2_000L shl (tries - 1), 60_000L)) // 2, 4, 8 … 60 s
            }
        }
        if (dest.length() != file.sizeBytes) {
            val got = dest.length()
            dest.delete()
            error("size mismatch for ${file.destName}: expected ${file.sizeBytes}, got $got")
        }
        if (file.sha256 != null) {
            // hashed from disk: a resumed file arrived in pieces, so the stream never saw it whole
            val digest = MessageDigest.getInstance("SHA-256")
            dest.inputStream().buffered().use { input ->
                val buffer = ByteArray(256 * 1024)
                while (true) {
                    coroutineContext.ensureActive()
                    val read = input.read(buffer)
                    if (read < 0) break
                    digest.update(buffer, 0, read)
                }
            }
            val actual = digest.digest().joinToString("") { "%02x".format(it) }
            if (!actual.equals(file.sha256, ignoreCase = true)) {
                dest.delete()
                error("checksum mismatch for ${file.destName}")
            }
        }
    }

    /**
     * One request: from byte 0, or from the end of a partial [dest] with `Range`. A 206 appends; a 200
     * means the server sent the whole file, which then replaces the partial one. Network errors, 5xx
     * and 429 surface as [IOException] so [downloadFile] retries them; other HTTP errors are final.
     */
    private suspend fun fetchFrom(
        file: LocalModelFile,
        dest: File,
        baseCompleted: Long,
        total: Long,
        onProgress: (downloaded: Long, total: Long) -> Unit,
    ) {
        val have = dest.length()
        val request = Request.Builder().url(file.url).apply {
            if (have > 0) header("Range", "bytes=$have-")
        }.build()
        client.newCall(request).execute().use { response ->
            if (response.code == 416) {
                dest.delete() // the server disagrees about where the file ends: start it over
                throw IOException("HTTP 416 resuming ${file.destName} at $have")
            }
            if (response.code >= 500 || response.code == 429) throw IOException("HTTP ${response.code}")
            check(response.isSuccessful) { "HTTP ${response.code} downloading ${file.url}" }
            val append = have > 0 && response.code == 206
            val body = response.body ?: throw IOException("empty response body for ${file.url}")
            body.byteStream().use { input ->
                FileOutputStream(dest, append).buffered().use { output ->
                    val buffer = ByteArray(64 * 1024)
                    var fileDone = if (append) have else 0L
                    while (true) {
                        coroutineContext.ensureActive() // cancellation-aware
                        val read = input.read(buffer)
                        if (read < 0) break
                        output.write(buffer, 0, read)
                        fileDone += read
                        onProgress(baseCompleted + fileDone, total)
                    }
                }
            }
        }
    }
}
