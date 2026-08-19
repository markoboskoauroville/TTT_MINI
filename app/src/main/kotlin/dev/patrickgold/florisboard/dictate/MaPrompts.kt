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

import org.json.JSONArray
import org.json.JSONObject

/**
 * Several named wordings for one shortcut, with a checkmark on the one in force.
 *
 * ### The Avid idea, applied where it actually pays
 *
 * In Avid a setting is not one configuration; it is a **category holding several named instances**,
 * one of which carries the checkmark. Three keyboard maps, four export presets, and you switch by
 * clicking. Marko asked for that model for this whole app, and section 96 of NEXT_DEFAULTS records
 * why it must not be built everywhere at once: nineteen screens, and most of them hold one thing
 * that nobody would ever want two of. A category with one possible instance is a folder with one
 * file in it, and an Avid user reads that as noise.
 *
 * Ctrl+P and Ctrl+F are where it pays. A proofreading instruction is exactly the kind of thing to
 * have several of — one that only fixes spelling, one that tightens, one for Croatian — and until
 * now each key held a single wording that had to be edited in place, which meant the previous
 * wording was gone the moment you tried a new one.
 *
 * ### The Default instance is real, and it is locked
 *
 * Every category ships with an instance called Default whose text is empty, which means *use the
 * instruction compiled into the app*. Empty rather than a copy of the shipped words, so improving
 * the built-in text later reaches everybody instead of only the people who never touched it.
 *
 * It cannot be renamed, edited or deleted, and that is the Avid behaviour rather than a limitation:
 * **nobody edits a factory setting, they duplicate it and edit the copy.** So the Default's editor
 * offers one action — duplicate — and the copy is what opens for writing.
 *
 * ### Migration is a shape check, not a flag
 *
 * The old preference held his custom wording as plain text. A stored value that does not begin with
 * `{` is therefore that, and it is read as a set of two instances: the locked Default and one called
 * "Mine" holding what he wrote, active. No migration flag, no version number, and nothing to run
 * once — a value written by the old version is simply legible to the new one, forever.
 */
object MaPrompts {

    /** The name of the locked instance. Not localised: it is an identifier he can see. */
    const val DEFAULT_NAME = "Default"

    /** The name a legacy plain-text wording is promoted to. */
    private const val LEGACY_NAME = "Mine"

    /**
     * One named wording.
     *
     * [text] empty means "use the instruction that ships with the app", which is only ever true of
     * the Default. [locked] is the factory flag: it is derived from the name rather than stored, so
     * a hand-edited file cannot produce a second locked row or an unlocked Default.
     */
    data class Instance(val name: String, val text: String) {
        val locked: Boolean get() = name == DEFAULT_NAME
    }

    /**
     * A category: every instance it holds, and the name of the one in force.
     *
     * Active by NAME rather than by index or position, so reordering, deleting or duplicating
     * cannot silently move the checkmark to a different wording — the failure that would be
     * invisible until a shortcut quietly started doing something else.
     */
    data class Set(val instances: List<Instance>, val activeName: String) {

        /** The instance in force, falling back to the Default if the stored name has gone. */
        val active: Instance
            get() = instances.firstOrNull { it.name == activeName }
                ?: instances.firstOrNull { it.locked }
                ?: instances.first()

        /** The wording to send, or empty to mean the shipped instruction. */
        val activeText: String get() = active.text
    }

    /** A set holding nothing but the locked Default. */
    private fun bare(): Set = Set(listOf(Instance(DEFAULT_NAME, "")), DEFAULT_NAME)

    /**
     * Reads a stored value in either shape.
     *
     * Blank is a first run. A value starting with `{` is a set. Anything else is a wording written
     * by the version before this one, and is promoted rather than discarded — losing what he had
     * written in order to introduce a feature about keeping several of them would be a poor trade.
     */
    fun parse(raw: String): Set {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return bare()
        if (!trimmed.startsWith("{")) {
            return Set(
                listOf(Instance(DEFAULT_NAME, ""), Instance(LEGACY_NAME, raw)),
                LEGACY_NAME,
            )
        }
        return runCatching {
            val root = JSONObject(trimmed)
            val arr = root.optJSONArray("instances") ?: return bare()
            val out = mutableListOf<Instance>()
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                val name = o.optString("name").trim()
                if (name.isEmpty()) continue
                // First one wins on a duplicate name, because the name is the identity here and two
                // rows answering to it would make the checkmark ambiguous.
                if (out.any { it.name.equals(name, ignoreCase = true) }) continue
                out.add(Instance(name, o.optString("text")))
            }
            // The Default is not optional. A file without it — hand-edited, or written by something
            // that did not know the rule — gets it back at the top rather than becoming a category
            // with no way home.
            val withDefault = if (out.any { it.locked }) out else listOf(Instance(DEFAULT_NAME, "")) + out
            Set(withDefault, root.optString("active").ifBlank { DEFAULT_NAME })
        }.getOrElse { bare() }
    }

    /** Writes the set. Always JSON, so what is read back is never guessed at. */
    fun serialize(set: Set): String {
        val arr = JSONArray()
        set.instances.forEach { inst ->
            arr.put(JSONObject().put("name", inst.name).put("text", inst.text))
        }
        return JSONObject().put("active", set.activeName).put("instances", arr).toString()
    }

    /** The wording in force in a stored value, or empty for the shipped one. */
    fun activeText(raw: String): String = parse(raw).activeText

    /**
     * A name not already taken, by adding "copy", then "copy 2" and so on.
     *
     * Duplicating twice should give two rows rather than an error or a silent overwrite, since the
     * whole point of duplicate is trying a variation without losing the thing it came from.
     */
    fun freeName(existing: List<Instance>, base: String): String {
        val taken = existing.map { it.name.lowercase() }.toSet()
        val first = "$base copy"
        if (first.lowercase() !in taken) return first
        var n = 2
        while ("$first $n".lowercase() in taken) n++
        return "$first $n"
    }
}
