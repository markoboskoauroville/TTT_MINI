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

import kotlin.BooleanArray
import kotlin.collections.HashMap
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.RandomAccessFile
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import kotlin.coroutines.coroutineContext

/**
 * ONE FILE, DOWNLOADED SO THAT A DROP COSTS LITTLE: aria2's way, inside the app.
 *
 * Large files that the server serves in byte ranges come down in [MaPieces] — eight connections,
 * 8 MB pieces, each written at its offset, a piece map beside the file so that only missing pieces
 * are fetched after a dropped connection, an app kill or a restart. Small files, and servers that
 * ignore Range, come down on one connection that resumes from where it stopped (Range: bytes=N-).
 * Either way the size and SHA-256 are checked from disk at the end.
 *
 * No Android in here — only OkHttp, coroutines and files — so it runs under plain Kotlin against a
 * real HTTP server that drops connections (scripts/test_parallel_download.py records what was run).
 * [LocalModelManager] decides where a file goes; this decides how it arrives.
 */
object MaFileDownloader {

    /** The piece map beside a file being fetched in pieces, like aria2's `.aria2`. */
    fun controlFile(dest: File): File = File(dest.path + ".pieces")

    /**
     * Whether the server answers a byte range with 206. One tiny request; a server that ignores Range
     * answers 200, and the file is then fetched on one connection.
     */
    private suspend fun supportsRanges(client: OkHttpClient, file: LocalModelFile): Boolean {
        var tries = 0
        while (true) {
            coroutineContext.ensureActive()
            try {
                val request = Request.Builder().url(file.url).header("Range", "bytes=0-0").build()
                client.newCall(request).execute().use { response ->
                    if (response.code >= 500 || response.code == 429) throw IOException("HTTP ${response.code}")
                    return response.code == 206
                }
            } catch (e: IOException) {
                if (++tries >= MaPieces.MAX_TRIES_WITHOUT_PROGRESS) throw e
                delay(MaPieces.backoffMillis(tries))
            }
        }
    }

    /**
     * Fetches [file] into [dest] in pieces, [MaPieces.CONNECTIONS] at a time — aria2 inside the app.
     *
     * [dest] is made the final size up front and every piece is written at its own offset. The piece
     * map beside it is rewritten after each whole piece, so a dropped connection, an app kill or a
     * restart refetches only what is missing. A partial file left by the one-connection code, with no
     * map, carries on from the pieces its first bytes already hold.
     */
    private suspend fun downloadPieces(
        client: OkHttpClient,
        file: LocalModelFile,
        dest: File,
        baseCompleted: Long,
        total: Long,
        onProgress: (downloaded: Long, total: Long) -> Unit,
    ) {
        val size = file.sizeBytes
        val ctl = controlFile(dest)
        val mapText = ctl.takeIf { it.isFile }?.readText()
        val done = MaPieces.parse(mapText, size)
            ?: if (!ctl.exists() && dest.isFile && dest.length() in 1 until size) {
                MaPieces.fromPrefix(dest.length(), size)
            } else {
                BooleanArray(MaPieces.count(size))
            }
        // How far unfinished pieces got last time; only trusted together with a valid map.
        val partial = HashMap(if (MaPieces.parse(mapText, size) != null) MaPieces.parsePartial(mapText, size) else emptyMap())
        RandomAccessFile(dest, "rw").use { it.setLength(size) }
        val lock = Any()
        fun saveMap() {
            val tmp = File(ctl.path + ".tmp")
            tmp.writeText(MaPieces.serialize(size, done, partial = partial))
            if (!tmp.renameTo(ctl)) { ctl.delete(); tmp.renameTo(ctl) }
        }
        synchronized(lock) { saveMap() }

        val whole = AtomicLong(MaPieces.doneBytes(done, size))
        // Bytes so far of each unfinished piece, counted from the piece's first byte.
        val inFlight = ConcurrentHashMap<Int, Long>(partial)
        fun report() = onProgress(baseCompleted + whole.get() + inFlight.values.sum(), total)
        fun checkpoint(index: Int, got: Long) = synchronized(lock) {
            partial[index] = got
            saveMap()
        }
        report()

        val queue = Channel<Int>(Channel.UNLIMITED)
        val missing = MaPieces.missing(done)
        missing.forEach { queue.trySend(it) }
        queue.close()

        coroutineScope {
            repeat(minOf(MaPieces.CONNECTIONS, missing.size)) {
                launch {
                    for (index in queue) {
                        var tries = 0
                        while (true) {
                            coroutineContext.ensureActive()
                            val from = inFlight[index] ?: 0L
                            try {
                                fetchPiece(client, file, dest, index, size, from, inFlight, ::report, ::checkpoint)
                                inFlight.remove(index)
                                whole.addAndGet(MaPieces.length(index, size))
                                synchronized(lock) {
                                    done[index] = true
                                    partial.remove(index)
                                    saveMap()
                                }
                                report()
                                break
                            } catch (c: CancellationException) {
                                // Killed or cancelled: keep how far this piece got, for the next start.
                                inFlight[index]?.let { checkpoint(index, it) }
                                throw c
                            } catch (e: IOException) {
                                val now = inFlight[index] ?: 0L
                                if (now > from) tries = 0
                                if (now > 0) checkpoint(index, now)
                                if (++tries >= MaPieces.MAX_TRIES_WITHOUT_PROGRESS) {
                                    throw IOException(
                                        "${file.destName}: piece ${index + 1} of ${done.size} failed $tries times " +
                                            "in a row (${e.message}). ${MaPieces.missing(done).size} pieces left. " +
                                            "Start it again to continue.",
                                        e,
                                    )
                                }
                                report()
                                delay(MaPieces.backoffMillis(tries))
                            }
                        }
                    }
                }
            }
        }
        ctl.delete()
    }

    /**
     * One piece, from [from] bytes into it, written at its offset. Anything short of the whole piece is
     * an [IOException]; how far it got is in [inFlight] and saved through [checkpoint] every 2 MB.
     */
    private suspend fun fetchPiece(
        client: OkHttpClient,
        file: LocalModelFile,
        dest: File,
        index: Int,
        size: Long,
        from: Long,
        inFlight: ConcurrentHashMap<Int, Long>,
        report: () -> Unit,
        checkpoint: (Int, Long) -> Unit,
    ) {
        val r = MaPieces.range(index, size)
        val pieceLength = r.last - r.first + 1
        val start = r.first + from
        val request = Request.Builder().url(file.url).header("Range", "bytes=$start-${r.last}").build()
        client.newCall(request).execute().use { response ->
            if (response.code >= 500 || response.code == 429 || response.code == 416) {
                throw IOException("HTTP ${response.code}")
            }
            if (response.code != 206) error("HTTP ${response.code} for a byte range of ${file.url}")
            val body = response.body ?: throw IOException("empty response body for piece ${index + 1}")
            RandomAccessFile(dest, "rw").use { out ->
                out.seek(start)
                body.byteStream().use { input ->
                    val buffer = ByteArray(64 * 1024)
                    var got = from
                    var sinceReport = 0L
                    var sinceCheckpoint = 0L
                    while (got < pieceLength) {
                        coroutineContext.ensureActive()
                        val read = input.read(buffer, 0, minOf(buffer.size.toLong(), pieceLength - got).toInt())
                        if (read < 0) break
                        out.write(buffer, 0, read)
                        got += read
                        inFlight[index] = got
                        sinceReport += read
                        sinceCheckpoint += read
                        if (sinceReport >= 512 * 1024) {
                            sinceReport = 0
                            report()
                        }
                        if (sinceCheckpoint >= 2 * 1024 * 1024 && got < pieceLength) {
                            sinceCheckpoint = 0
                            checkpoint(index, got)
                        }
                    }
                    if (got != pieceLength) throw IOException("piece ${index + 1} ended at $got of $pieceLength bytes")
                }
            }
        }
    }

    /** Fetches [file] into [dest], resuming a partial [dest] and retrying dropped connections. */
    suspend fun downloadFile(
        client: OkHttpClient,
        file: LocalModelFile,
        dest: File,
        baseCompleted: Long,
        total: Long,
        onProgress: (downloaded: Long, total: Long) -> Unit,
    ) {
        if (dest.length() > file.sizeBytes) { // left by a different file of the same name
            dest.delete()
            controlFile(dest).delete()
        }
        // In pieces, eight at a time, when the file is large and the server takes byte ranges; a piece
        // map beside [dest] means a download in pieces was under way and is continued.
        val inPieces = controlFile(dest).isFile ||
            (dest.length() < file.sizeBytes && file.sizeBytes >= MaPieces.PARALLEL_MIN_BYTES && supportsRanges(client, file))
        if (inPieces) downloadPieces(client, file, dest, baseCompleted, total, onProgress)
        var tries = 0
        while (dest.length() < file.sizeBytes) {
            coroutineContext.ensureActive()
            val before = dest.length()
            try {
                fetchFrom(client, file, dest, baseCompleted, total, onProgress)
            } catch (e: IOException) {
                if (dest.length() > before) tries = 0
                if (++tries >= MaPieces.MAX_TRIES_WITHOUT_PROGRESS) {
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
            controlFile(dest).delete()
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
                controlFile(dest).delete()
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
        client: OkHttpClient,
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
