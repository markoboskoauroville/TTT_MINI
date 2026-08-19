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

import dev.patrickgold.florisboard.app.FlorisPreferenceStore
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import dev.patrickgold.jetpref.datastore.runtime.ImportStrategy
import dev.patrickgold.jetpref.datastore.runtime.FileBasedStorage

/**
 * Every setting, written beside the keys and read back the same way.
 *
 * ### Why this exists when a backup screen already does
 *
 * The existing backup makes a zip through a file picker: he chooses a location, names it, and finds
 * it again later. That is the right shape for an occasional archive and the wrong shape for
 * somebody who reinstalls several times a day. **A backup you have to remember the location of is a
 * backup that is not there when the phone is wiped.**
 *
 * So this writes to the one place the app already knows how to find — the same folder as the key
 * backup — under a fixed name. One grant of storage access covers both, and a fresh install can look
 * for it without being told where it is.
 *
 * ### Why the same mechanism as the backup screen
 *
 * `FlorisPreferenceStore.export` and `.import` are what that screen uses, and they understand the
 * datastore's own format including which keys have been added or removed between versions. Writing
 * a private format here would be a second thing to keep correct, and it would drift.
 */
object MaSettingsVault {

    /**
     * Writes every preference to the vault. False when storage has not been granted.
     *
     * The folder is created on demand, exactly as the key backup does, so a first backup on a fresh
     * install does not need anything to exist first.
     */
    /**
     * A named profile: every setting in the app, saved under a name he chose.
     *
     * ### The Avid idea, and why it fits here
     *
     * Avid's user profiles hot-swap an entire configuration — layouts, keyboard maps, export
     * presets — because an editor works one way cutting rushes and another way finishing. He has
     * the same split: the keyboard he wants dictating at four in the morning is not the keyboard he
     * wants at the studio with a client behind him.
     *
     * A profile is the same bytes the timestamped backup writes. **The only difference is that the
     * name is his rather than a clock's**, which is what makes it a thing to choose rather than a
     * thing to recover.
     */
    data class Profile(val file: File, val name: String)

    private const val PROFILE_PREFIX = "profile-"

    /** Every named profile, alphabetical. */
    fun profiles(): List<Profile> = runCatching {
        val dir = MaVault.dir()
        if (!dir.exists()) return emptyList()
        dir.listFiles().orEmpty()
            .filter { it.isFile && it.name.startsWith(PROFILE_PREFIX) && it.name.endsWith(SUFFIX) }
            .sortedBy { it.name.lowercase() }
            .map { Profile(it, it.name.removePrefix(PROFILE_PREFIX).removeSuffix(SUFFIX)) }
    }.getOrDefault(emptyList())

    /**
     * Saves everything as [name], overwriting a profile of that name.
     *
     * Overwriting on purpose: saving under a name that already exists is how somebody updates a
     * profile, and a silent second copy called "studio 2" would be worse than replacing it.
     */
    suspend fun saveProfile(name: String): Boolean = runCatching {
        val clean = name.trim().replace(Regex("[^A-Za-z0-9 _-]"), "").take(40)
        if (clean.isEmpty()) return false
        val dir = MaVault.dir()
        if (!dir.exists() && !dir.mkdirs()) return false
        FlorisPreferenceStore.export(
            FileBasedStorage(File(dir, "$PROFILE_PREFIX$clean$SUFFIX").path),
        ).getOrThrow()
        MaLog.add("settings", "saved profile '$clean'")
        true
    }.getOrElse {
        MaLog.add("settings", "save profile failed: ${it.message}")
        false
    }

    /** Loads [profile] over the current settings. */
    suspend fun applyProfile(profile: Profile): Boolean = runCatching {
        FlorisPreferenceStore.import(
            ImportStrategy.Merge,
            FileBasedStorage(profile.file.path),
        ).getOrThrow()
        MaLog.add("settings", "switched to profile '${profile.name}'")
        true
    }.getOrElse {
        MaLog.add("settings", "apply profile failed: ${it.message}")
        false
    }

    /** Copies a profile under a new name. Avid's duplicate, and for the same reason. */
    suspend fun duplicateProfile(profile: Profile, newName: String): Boolean = runCatching {
        val clean = newName.trim().replace(Regex("[^A-Za-z0-9 _-]"), "").take(40)
        if (clean.isEmpty()) return false
        profile.file.copyTo(File(MaVault.dir(), "$PROFILE_PREFIX$clean$SUFFIX"), overwrite = true)
        MaLog.add("settings", "duplicated '${profile.name}' as '$clean'")
        true
    }.getOrDefault(false)

    fun deleteProfile(profile: Profile): Boolean =
        runCatching { profile.file.delete() }.getOrDefault(false)

    /** One saved backup: the file, and when it was written. */
    data class Snapshot(val file: File, val display: String)

    private const val PREFIX = "settings-"
    private const val SUFFIX = ".jetpref"

    /** How many stamped backups are kept. Older ones go as new ones arrive. */
    private const val KEEP = 20

    /**
     * Every kept backup, newest first.
     *
     * Read from the folder rather than from a list this class maintains. A list would drift the
     * first time a file was moved or deleted by hand, and the folder is the truth.
     */
    fun history(): List<Snapshot> = runCatching {
        val dir = MaVault.dir()
        if (!dir.exists()) return emptyList()
        dir.listFiles().orEmpty()
            .filter { it.isFile && it.name.startsWith(PREFIX) && it.name.endsWith(SUFFIX) }
            .sortedByDescending { it.name }
            .map { Snapshot(it, prettyStamp(it.name)) }
    }.getOrDefault(emptyList())

    /**
     * The filename stamp turned back into something readable.
     *
     * The name is big-endian — year, month, day, hour, minute — because sorting is by NAME and only
     * that order sorts correctly. What he reads is day-first. The two orders differ on purpose and
     * this is the one place that knows both.
     */
    private fun prettyStamp(name: String): String {
        val raw = name.removePrefix(PREFIX).removeSuffix(SUFFIX)
        return runCatching {
            val d = raw.split("-")
            "${d[2]}.${d[1]}. ${d[3]}:${d[4]}"
        }.getOrDefault(raw)
    }

    /** Restores one particular snapshot, chosen from the history. */
    suspend fun restore(snapshot: Snapshot): Boolean = runCatching {
        FlorisPreferenceStore.import(
            ImportStrategy.Merge,
            FileBasedStorage(snapshot.file.path),
        ).getOrThrow()
        MaLog.add("settings", "restored ${snapshot.file.name}")
        true
    }.getOrElse {
        MaLog.add("settings", "restore failed: ${it.message}")
        false
    }

    suspend fun backup(): Boolean = runCatching {
        val dir = MaVault.dir()
        if (!dir.exists() && !dir.mkdirs()) return false
        FlorisPreferenceStore.export(FileBasedStorage(MaVault.settingsFile().path)).getOrThrow()
        // And a stamped copy, so a backup no longer destroys the one before it.
        //
        // The plain file stays exactly as it was, so the setup step and anything else looking for
        // "the backup" finds it without knowing history exists. This is a second write of the same
        // bytes, which costs nothing at this size.
        val stamp = SimpleDateFormat("yyyy-MM-dd-HH-mm", Locale.US).format(Date())
        MaVault.settingsFile().copyTo(File(dir, "$PREFIX$stamp$SUFFIX"), overwrite = true)
        // Oldest out first. Twenty reaches back through a bad week and keeps the folder readable.
        history().drop(KEEP).forEach { runCatching { it.file.delete() } }
        MaLog.add("settings", "backed up, ${history().size} kept")
        true
    }.getOrElse {
        MaLog.add("settings", "backup failed: ${it.message}")
        false
    }

    /**
     * Reads every preference back. False when there is nothing there, or storage is not granted.
     *
     * **This overwrites what is currently set**, which is the point on a fresh install and a real
     * loss on a configured one — so nothing calls it automatically. The setup step offers it and he
     * decides, the same way the key restore works (§43).
     */
    suspend fun restore(): Boolean = runCatching {
        if (!MaVault.settingsExist()) return false
        // Merge rather than Erase: a setting the backup predates keeps whatever this version
        // decided for it, instead of being blanked because an older file had never heard of it.
        FlorisPreferenceStore.import(
            ImportStrategy.Merge,
            FileBasedStorage(MaVault.settingsFile().path),
        ).getOrThrow()
        MaLog.add("settings", "restored from ${MaVault.SETTINGS_DISPLAY_PATH}")
        true
    }.getOrElse {
        MaLog.add("settings", "restore failed: ${it.message}")
        false
    }
}
