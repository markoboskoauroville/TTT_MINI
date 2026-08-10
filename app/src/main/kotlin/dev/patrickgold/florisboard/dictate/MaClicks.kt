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

import kotlin.math.roundToInt

/**
 * Recorded click sequences, kept in numbered slots, played back into a live app.
 *
 * ### Why there is a pointer, and why that is the whole design
 *
 * The obvious version of this feature is impossible on Android, and it is worth writing down why so
 * that nobody spends a week rediscovering it. An app cannot watch your finger touch another app and
 * let that app react to the same touch. `TouchInteractionController` is the closest the platform
 * comes; it needs Android 13 where this app supports Android 8, and even there the moment it passes
 * an interaction through to the app it stops receiving it. A transparent capture layer can see
 * everything, but only by taking the touch away from whatever is underneath — so the app freezes,
 * menus never open, and a sequence that depends on the interface changing as it goes cannot be
 * recorded at all.
 *
 * A pointer we draw ourselves sidesteps every part of that. Its position is ours already, so nothing
 * needs observing. A click is dispatched through the accessibility service as a real gesture, so the
 * app underneath is live: windows open, dialogs appear, lists scroll, and the next position is
 * picked against the screen as it now actually is. Recording a step is then nothing more than
 * writing down where the pointer was when the click was sent.
 *
 * The pointer is moved from the keyboard's own surface, which is this app's window and therefore the
 * one place its touches can be read without fighting the platform for them. The target app stays
 * visible above it the whole time.
 *
 * ### Delays are set, not recorded
 *
 * Marko's call, and the right one. A recorded delay captures how long he happened to take deciding
 * where to click next, which is meaningless on playback and usually far too long. A set delay says
 * how long this app needs to open its dialog, which is the thing actually being waited for. Each
 * step carries its own, because the wait after opening a settings page and the wait after ticking a
 * checkbox are not the same wait.
 *
 * ### Positions are fractions of the screen, not pixels
 *
 * A sequence recorded upright and replayed after a font size change, or moved to another phone,
 * would otherwise land somewhere arbitrary. Fractions do not survive an interface that has been
 * rearranged — nothing could — but they do survive the same screen at a different size.
 */
object MaClicks {

    private const val SLOT_SEP = '\u001C'
    private const val META_SEP = '\u001B'
    private const val STEP_SEP = '\u001E'
    private const val FIELD_SEP = '\u001D'

    /** Slots, numbered from one, as they are named on the keys that trigger them. */
    const val SLOT_COUNT = 8

    /** What one step does when it is reached. */
    enum class Kind(val id: String) {
        /** A tap where the pointer is. */
        TAP("tap"),

        /** A press held long enough for an app to read it as a long press. */
        LONG_PRESS("hold"),

        /**
         * A drag from this step's position to the next step's position.
         *
         * Two positions, one step: a swipe that stored its start and end as separate steps would tap
         * its own start point on the way past whenever the sequence was edited or replayed one step
         * at a time.
         */
        SWIPE("swipe"),

        /** Nothing at all, for its delay. A pause while an app finishes loading. */
        WAIT("wait"),

        /** The system back gesture, which is a button on no screen and needed on most of them. */
        BACK("back");

        companion object {
            fun byId(id: String): Kind? = entries.firstOrNull { it.id == id }
        }
    }

    /**
     * @param x fraction of screen width, 0 to 1
     * @param y fraction of screen height, 0 to 1
     * @param toX end of a swipe, fraction of width; ignored by every other kind
     * @param toY end of a swipe, fraction of height; ignored by every other kind
     * @param delayAfterMs how long to wait once this step has been sent, before the next one
     */
    data class Step(
        val kind: Kind,
        val x: Float,
        val y: Float,
        val toX: Float = 0f,
        val toY: Float = 0f,
        val delayAfterMs: Long = DEFAULT_DELAY_MS,
    )

    /**
     * Long enough that most screens have finished changing, short enough not to feel broken.
     *
     * Chosen rather than measured, and deliberately generous: a step that fires before the screen it
     * was recorded against has appeared clicks whatever happens to be under it instead, which is how
     * an automation ends up tapping something nobody intended. Waiting too long only wastes a moment.
     */
    const val DEFAULT_DELAY_MS = 600L

    /** A press this long reads as a long press in every app that distinguishes one. */
    const val LONG_PRESS_MS = 600L

    /** A swipe slow enough to read as a drag rather than a fling. */
    const val SWIPE_MS = 300L

    /**
     * One slot. An empty step list means recorded over and cleared, and the slot stays in the list
     * so the numbering holds: slot three must remain slot three after slot two is emptied, or every
     * key that triggers a slot silently changes meaning.
     */
    data class Slot(val name: String, val steps: List<Step>) {
        val isEmpty: Boolean get() = steps.isEmpty()

        /** What playback will take, ignoring the time the gestures themselves occupy. */
        val totalDelayMs: Long get() = steps.sumOf { it.delayAfterMs }
    }

    fun emptySlots(): List<Slot> = (1..SLOT_COUNT).map { Slot("Slot $it", emptyList()) }

    /**
     * Three decimals on the way out: under two pixels on any phone screen, and a third of the
     * characters that a full float costs. These strings hold hundreds of steps.
     */
    private fun enc(v: Float): String = ((v * 1000f).roundToInt() / 1000f).toString()

    fun serialize(slots: List<Slot>): String =
        slots.joinToString(SLOT_SEP.toString()) { slot ->
            val body = slot.steps.joinToString(STEP_SEP.toString()) { s ->
                listOf(s.kind.id, enc(s.x), enc(s.y), enc(s.toX), enc(s.toY), s.delayAfterMs.toString())
                    .joinToString(FIELD_SEP.toString())
            }
            "${slot.name}$META_SEP$body"
        }

    /**
     * Parses the stored string, dropping anything malformed rather than throwing.
     *
     * Read while the keyboard is opening, in front of whatever he was about to type into. A damaged
     * preference has to cost a sequence and never a keyboard that will not draw, because there is no
     * route to settings from behind a keyboard that does not appear.
     */
    fun parse(raw: String): List<Slot> {
        if (raw.isBlank()) return emptyList()
        return raw.split(SLOT_SEP).map { chunk ->
            val idx = chunk.indexOf(META_SEP)
            val name = if (idx >= 0) chunk.substring(0, idx) else chunk
            val body = if (idx >= 0) chunk.substring(idx + 1) else ""
            val steps = if (body.isBlank()) emptyList() else {
                body.split(STEP_SEP).mapNotNull { parseStep(it) }
            }
            Slot(name.ifBlank { "Slot" }, steps)
        }
    }

    private fun parseStep(raw: String): Step? {
        val f = raw.split(FIELD_SEP)
        if (f.size != 6) return null
        val kind = Kind.byId(f[0]) ?: return null
        val x = f[1].toFloatOrNull() ?: return null
        val y = f[2].toFloatOrNull() ?: return null
        val toX = f[3].toFloatOrNull() ?: return null
        val toY = f[4].toFloatOrNull() ?: return null
        val delay = f[5].toLongOrNull() ?: return null
        if (x.isNaN() || y.isNaN() || toX.isNaN() || toY.isNaN()) return null
        return Step(
            kind = kind,
            x = x.coerceIn(0f, 1f),
            y = y.coerceIn(0f, 1f),
            toX = toX.coerceIn(0f, 1f),
            toY = toY.coerceIn(0f, 1f),
            // Negative would run the playback loop backwards through its own timer. Nothing sane
            // produces one, but this string can be edited by hand and restored from a backup.
            delayAfterMs = delay.coerceIn(0L, 600_000L),
        )
    }

    /**
     * Reads back at a fixed length, padding and truncating.
     *
     * The keys that trigger slots are numbered, so the list they index into must be the length they
     * expect whatever the stored string happens to hold. A short read is a missing slot, and a
     * missing slot indexed by a key is a crash instead of a key that does nothing.
     */
    fun slotsOrEmpty(raw: String): List<Slot> {
        val parsed = parse(raw)
        return (0 until SLOT_COUNT).map { i ->
            parsed.getOrNull(i) ?: Slot("Slot ${i + 1}", emptyList())
        }
    }

    fun defaultSerialized(): String = serialize(emptySlots())
}
