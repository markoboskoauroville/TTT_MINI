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
import dev.patrickgold.florisboard.dictate.llm.MaSharedModels
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import java.io.File
import java.util.concurrent.TimeUnit

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
        // A phone model goes to the shared Download/models/ when the app may write there, under
        // Hugging Face's own name, so aria2 and every other tool use the same copy (MaSharedModels).
        val shared = MaSharedModels.downloadTarget(context, spec.id)
        if (shared != null && spec.files.size == 1) {
            downloadShared(context, spec.files.single(), shared, onProgress)
            return@withContext
        }
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
                MaFileDownloader.downloadFile(client, f, File(tmpDir, f.destName), completed, total, onProgress)
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

    /**
     * Downloads one file straight into the shared folder: into `<name>.part` beside it, then renamed
     * into place when whole and checked. A file already there at the exact size is not fetched again.
     * Cancel deletes the part and its piece map; any other failure keeps them for the next start.
     */
    private suspend fun downloadShared(
        context: Context,
        file: LocalModelFile,
        target: File,
        onProgress: (downloaded: Long, total: Long) -> Unit,
    ) {
        if (target.length() == file.sizeBytes) {
            onProgress(file.sizeBytes, file.sizeBytes)
            return
        }
        val part = File(target.path + ".part")
        try {
            MaFileDownloader.downloadFile(client, file, part, 0L, file.sizeBytes, onProgress)
            target.delete()
            if (!part.renameTo(target)) error("could not move ${part.name} to ${target.name}")
            // Just checked whole by downloadFile, so the background check need not hash it again.
            MaSharedModels.rememberGood(context, target)
        } catch (c: CancellationException) {
            part.delete()
            MaFileDownloader.controlFile(part).delete()
            throw c
        }
    }

}
