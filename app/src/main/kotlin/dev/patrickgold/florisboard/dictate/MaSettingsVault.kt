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
    suspend fun backup(): Boolean = runCatching {
        val dir = MaVault.dir()
        if (!dir.exists() && !dir.mkdirs()) return false
        FlorisPreferenceStore.export(FileBasedStorage(MaVault.settingsFile().path)).getOrThrow()
        MaLog.add("settings", "backed up to ${MaVault.SETTINGS_DISPLAY_PATH}")
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
