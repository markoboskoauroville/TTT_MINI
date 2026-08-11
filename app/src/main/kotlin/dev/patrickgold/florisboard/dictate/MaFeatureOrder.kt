/*
 * MA TWIST, Mantra Productions.
 * Licensed under the Apache License, Version 2.0.
 */

package dev.patrickgold.florisboard.dictate

/**
 * The keys of the feature row, as data, and the order they are drawn in.
 *
 * The count is deliberately not written down anywhere, here or on screen. It has been wrong twice
 * already, once by a key being added and once by this fork removing several, and a number in prose
 * is the one part of a document nothing ever recompiles.
 *
 * The order has been corrected by hand twice, at builds 139 and 146, from screenshots with arrows
 * drawn on them. Both corrections went the opposite way to what looked sensible from inside the
 * code, which is the argument for this file: the person using the keyboard should not have to send a
 * screenshot and wait for a build to move a key.
 *
 * **Order only. Nothing can be hidden, and that is a safety rule rather than a simplification.**
 * This row is the one that survives when every other row is folded away. `MIC` is the only way to
 * reach the dictation screen with zone two shut, and `BACKSPACE` and `ENTER` are the only keys from
 * the keyboard proper with no substitute anywhere. An editor that could hide those could leave the
 * keyboard with no way to delete a character, no way to end a line, and no way to reach the feature
 * that the app is named after, with no way back except the settings app. Rearranging cannot lock
 * anybody out of anything; hiding can.
 */
enum class MaFeatureKey(val id: String, val label: String) {
    ALL_PASTE("ap", "Paste all"),
    SELECT_ALL("select_all", "Select all"),
    BACKSPACE("backspace", "Backspace"),

    /**
     * AC: select all and delete, without going near the clipboard.
     *
     * Marko's name and his distinction. The C is not cut: cutting would overwrite whatever is being
     * carried, which on a keyboard driven by voice is usually the very thing about to be pasted.
     */
    ALL_CLEAR("ac", "Clear all"),

    /**
     * A spacebar, in the row that survives folding.
     *
     * Not a duplicate of the keyboard's own. With zone two closed there is no keyboard on screen,
     * and a dictated sentence still needs a space before the next one begins.
     */
    SPACE("space", "Space"),

    /**
     * The record key. Tap to start, tap to stop and send, lit red while recording.
     *
     * It was a door to the transcribe view until that view was removed. The id stays "mic" so that
     * a stored row order written before the change still finds it.
     */
    MIC("mic", "Record"),

    /** Settings, reopened where they were left. */
    SETTINGS("settings", "Settings"),

    /**
     * The trash key: empties C1 to C10 so they can fill again from the next copy.
     *
     * Needed the moment the slots stopped moving. Filling in fixed order means a full row is a
     * finished row — capturing stops and nothing changes again — so without a way to empty it the
     * feature works exactly once per install. This key is that way.
     */
    CLIP_CLEAR("cclear", "Clear the C keys"),

    ZONE_1("zone1", "1, the number row"),
    ZONE_2("zone2", "2, the keys"),
    ZONE_3("zone3", "3, the copy row"),
    ENTER("enter", "Enter");

    companion object {
        fun byId(id: String): MaFeatureKey? = entries.firstOrNull { it.id == id }
    }
}

/**
 * Reads and writes the stored order.
 *
 * Pure, so it can be tested in the sandbox rather than discovered to be wrong by CI, and separate
 * from the composable that draws the row so that both the row and the editor read the same thing.
 */
object MaFeatureOrder {

    /**
     * The three keys that can be moved but never switched off.
     *
     * This row is the one that survives when every other row is folded away. `BACKSPACE` and `ENTER`
     * are the only keys from the keyboard proper with no substitute anywhere once zone two is shut:
     * hiding either leaves a keyboard that cannot delete a character or cannot end a line.
     *
     * `MIC` was in this set and is deliberately no longer. It was locked because it was the only
     * on-screen route to dictation; now that it is the record button and volume up does the same
     * job, that argument is spent, and Marko asked for it to be editable like every other key. He
     * was told what it costs — hidden key plus a dead volume rocker means no way to record — and
     * chose it. Do not put it back on the old reasoning: that reasoning was about a different key.
     *
     * Rearranging can never lock anybody out of anything. Switching off can, and this is the line.
     */
    val ALWAYS_ON: Set<MaFeatureKey> = setOf(
        MaFeatureKey.BACKSPACE,
        MaFeatureKey.ENTER,
    )

    // DECLARED FIRST ON PURPOSE. DEFAULT_HIDDEN_RAW below calls serializeHidden, which reads this
    // set, and an object initialises its properties top to bottom: with this declared underneath it
    // would still be null at that moment, and the first touch of MaFeatureOrder would throw. Nothing
    // in the compiler warns about it and nothing in the editor does either.


    /**
     * Marko's order at build 146, and the one a reset returns to.
     *
     * The busy keys first, at the end the thumb starts from. The two view swaps together, because
     * they are the only pair here that changes what is on screen. The zone switches in the middle,
     * reading left to right as the parts of the keyboard they control. Enter last, where every
     * keyboard ever made has put it.
     */
    val DEFAULT: List<MaFeatureKey> = listOf(
        MaFeatureKey.ALL_PASTE,
        MaFeatureKey.SELECT_ALL,
        MaFeatureKey.BACKSPACE,
        MaFeatureKey.ALL_CLEAR,
        MaFeatureKey.MIC,
        MaFeatureKey.SPACE,
        MaFeatureKey.ZONE_1,
        MaFeatureKey.ZONE_2,
        MaFeatureKey.ZONE_3,
        MaFeatureKey.ENTER,
        MaFeatureKey.SETTINGS,
        MaFeatureKey.CLIP_CLEAR,
    )

    val DEFAULT_RAW: String = serialize(DEFAULT)

    /**
     * Switched off to begin with: the Little Man's own key.
     *
     * A key added to this row appears in the editor immediately and on the keyboard only when asked
     * for. A row that grows a key on its own is a row whose other keys all moved, and every one of
     * those positions is something a thumb had learned.
     */
    // Nothing is hidden by default any more. The Little Man was the only entry and he is
    // gone; the set stays because MaFeatureOrder still serializes it for the legacy value, and
    // reads it off the old preference when carrying an existing install across.
    val DEFAULT_HIDDEN: Set<MaFeatureKey> = emptySet()

    val DEFAULT_HIDDEN_RAW: String = serializeHidden(DEFAULT_HIDDEN)

    /**
     * Parses a stored order, and **always returns every key in the enum**.
     *
     * Unknown ids are dropped, duplicates collapse to their first appearance, and anything missing is
     * appended in default order. That last part is what makes this safe to change later: a tenth key
     * added in some future build appears at the end for everybody who already has a saved order,
     * instead of being invisible to exactly the people who had customised the row. And a truncated or
     * garbled preference degrades to the default rather than to a keyboard with no enter key.
     */
    fun parse(raw: String?): List<MaFeatureKey> {
        val wanted = raw.orEmpty()
            .split(',')
            .mapNotNull { MaFeatureKey.byId(it.trim()) }
            .distinct()
        return wanted + DEFAULT.filterNot { it in wanted }
    }

    fun serialize(order: List<MaFeatureKey>): String = order.joinToString(",") { it.id }

    /**
     * The keys switched off, read from the stored list.
     *
     * [ALWAYS_ON] is subtracted here rather than only in the editor, so a preference edited by hand,
     * restored from an old backup, or written by a future bug still cannot produce a keyboard with no
     * enter key. The guarantee belongs at the point the value is read, not at the point it is set.
     */
    fun parseHidden(raw: String?): Set<MaFeatureKey> =
        raw.orEmpty()
            .split(',')
            .mapNotNull { MaFeatureKey.byId(it.trim()) }
            .toSet() - ALWAYS_ON

    fun serializeHidden(hidden: Set<MaFeatureKey>): String =
        (hidden - ALWAYS_ON).joinToString(",") { it.id }

    /** The keys actually drawn, in order. */
    fun visible(order: List<MaFeatureKey>, hidden: Set<MaFeatureKey>): List<MaFeatureKey> =
        order.filterNot { it in hidden - ALWAYS_ON }

    /**
     * Moves the key at [from] to [to], shifting the rest along.
     *
     * A move, not a swap. Dragging a key from one end of a row to the other should slide everything
     * it passes over by one place, which is what the eye expects from watching the drag; a swap would
     * fling whatever happened to be at the far end back to where the drag began.
     */
    fun move(order: List<MaFeatureKey>, from: Int, to: Int): List<MaFeatureKey> {
        if (from !in order.indices) return order
        val target = to.coerceIn(0, order.size - 1)
        if (from == target) return order
        val out = order.toMutableList()
        out.add(target, out.removeAt(from))
        return out
    }
}
