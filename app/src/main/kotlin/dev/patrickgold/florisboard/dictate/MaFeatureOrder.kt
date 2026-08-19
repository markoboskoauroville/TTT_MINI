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
    // Named after the letters on the key, not after what the key sounds like it does.
    // "Paste all" reads as "paste everything" when it actually replaces the field, and the key
    // itself says AP, so the list said one thing and the keyboard said another.
    ALL_PASTE("ap", "AP (all paste)"),
    SELECT_ALL("select_all", "Select all"),
    BACKSPACE("backspace", "Backspace"),

    /**
     * AC: select all and delete, without going near the clipboard.
     *
     * Marko's name and his distinction. The C is not cut: cutting would overwrite whatever is being
     * carried, which on a keyboard driven by voice is usually the very thing about to be pasted.
     */
    ALL_CLEAR("ac", "AC (all clear)"),

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
    MIC("mic", "Dictation view"),

    /** Settings, reopened where they were left. */
    SETTINGS("settings", "Settings"),

    /**
     * The trash key: empties C1 to C10 so they can fill again from the next copy.
     *
     * Needed the moment the slots stopped moving. Filling in fixed order means a full row is a
     * finished row — capturing stops and nothing changes again — so without a way to empty it the
     * feature works exactly once per install. This key is that way.
     */
    CLIP_CLEAR("cclear", "Empty the C buckets"),

    /**
     * TAB: back to the app you were just in, and back again. Alt+Tab, on a phone.
     *
     * The buckets made carrying text between two apps easy while the switching stayed as slow as it
     * ever was — recents, find the card, tap it. This is that, as one key.
     */
    APP_SWITCH("tab", "TAB, the last app"),

    /**
     * TAB for fields: focus the next text box on the screen.
     *
     * Named TAB because that is the key it replaces on a desktop, and kept separate from
     * [APP_SWITCH] because they are different journeys that happen to share a word — one moves
     * between apps, this moves inside one.
     *
     * A real `KEYCODE_TAB` exists already through the macro syntax and does not do this. Tested in
     * Suno: it moves the caret inside the field it is already in and nothing else. That is not the
     * app misbehaving — Tab only moves focus between views marked focusable in touch mode, which
     * almost nothing is, because until recently no phone had a Tab key.
     *
     * So this reads the node tree instead, through the accessibility service that is already
     * running for the magic finger. The tree exists because screen readers need it, which means it
     * works in a native app, a web view and a browser alike, without any of them having been built
     * with a keyboard in mind.
     */
    NEXT_FIELD("nextfield", "TAB, the next field"),

    /**
     * Shift, on the feature row, where it can be reached with the keyboard folded away.
     *
     * The letters have a shift already, and it is unreachable in the state this is for: the feature
     * row alone on screen with the keys collapsed. A modifier that only exists on the thing that is
     * put away is not a modifier, and [NEXT_FIELD] needed one to go backwards.
     *
     * It sets the same `inputShiftState` the letter shift sets, so it is the real shift rather than
     * a private flag that happens to share a name — capitals still come out capital, and one press
     * still means one letter.
     */
    SHIFT("shift", "Shift, for the feature row"),

    /**
     * Aa: through the four cases, one press at a time.
     *
     * lower, UPPER, Sentence case, Title Case, and round again. A cycle rather than four keys
     * because it is the same wish four times over and the row has no room for the other three; and
     * because the wrong case is recognised on sight, so pressing until it looks right is faster
     * than choosing from a menu.
     */
    CHANGE_CASE("case", "Aa, cycle the case"),

    /**
     * The automatic bucket: one press per code block, walking up the page.
     *
     * Press once and the last code block on screen is copied; press again and the one above it goes
     * to the next bucket; again for the one above that. The buckets fill themselves — the clipboard
     * capture in ClipboardManager already files every copy into the next free slot and stops when
     * they are full — so this key only has to press the right button in the right order.
     *
     * It looks for `copy code` and nothing else. Code blocks announce that name; the copy button
     * under a whole answer announces `copy message`. So a chat full of both gives up only its code,
     * which is what he wanted it for.
     *
     * Long press resets the count to the first block. Without that, one mistaken press leaves the
     * counter pointing at last month, and there would be no way back down the page.
     */
    AUTO_BUCKET("autobucket", "A-bucket, code blocks into the buckets"),

    /**
     * The pin: keep the keyboard up when the app it is typing into would close it.
     *
     * It had a strip of its own along the bottom — half a key row, permanently, for a switch set
     * once and then forgotten. That is a bad trade on a screen this size, and he made it: put the
     * pin in the row with everything else and take the strip back.
     *
     * The old strip existed because a pin that can be removed from the row cannot be reached to put
     * back. That is true and it is survivable: the same switch is in Settings under the keyboard,
     * so unticking the key here is inconvenient rather than a locked door.
     */
    PIN("pin", "Pin, keep the keyboard up"),

    /**
     * The reader: speak what is on screen, pause, resume.
     *
     * Short press cycles; a long press opens the reader settings, where the voice is chosen. The
     * settings live behind a long press rather than a key of their own because the voice is picked
     * once and the reading happens constantly.
     */
    READER("reader", "Reader, speak the screen"),

    /**
     * S: show or hide the subtitle row.
     *
     * A key rather than only a setting, because it is the kind of thing he changes while reading —
     * the row is wanted for a passage he is following closely and in the way for one he is only
     * half listening to. A trip into settings for that is a trip he would not make.
     */
    SUBTITLE("subtitle", "S, show the reading subtitle"),

    /**
     * The dictation history: everything transcribed, ready to put back into a field.
     *
     * A key rather than a menu because it is the recovery route. A dictation that went into the
     * wrong app, or was replaced by the next one, is only retrievable here, and hunting for it
     * through a panel is exactly the wrong amount of effort at the moment somebody has just lost a
     * sentence they spoke.
     */
    HISTORY("history", "History, what you dictated"),

    /**
     * HR / ENG, on the row rather than only in the recording bar.
     *
     * Marko changes language between recordings as he works, and the recording bar only exists while
     * a recording is running — so the one moment he cannot reach it is the moment before he starts,
     * which is exactly when the choice is made.
     */
    LANGUAGE("lang", "Language, HR or ENG"),

    /**
     * S, the page scroller. Tap to scroll, long press to set how far.
     *
     * One key rather than two, because the page count carries its own sign: negative scrolls up.
     * Marko worked that out himself mid-sentence and it is the better design — two keys would have
     * needed two settings and twice the row space to say the same thing.
     */
    SCROLL("scroll", "S, scroll the page"),

    /**
     * The switchboard, straight from the keyboard.
     *
     * It answers "what is on my keyboard right now", and the fastest place to ask that is the
     * keyboard itself rather than walking the settings tree to reach a screen whose whole purpose
     * is to save that walk.
     */
    SWITCHBOARD("switchboard", "Switchboard, rows on and off"),

    /**
     * Empty width. Draws nothing and does nothing.
     *
     * Marko sends with his right thumb, and as the magic row grew, send drifted left until it was
     * across the board from the thumb that presses it. He had been padding the row with real keys he
     * did not want, which is improvisation the app should not have made necessary.
     *
     * A spacer is the honest answer: a key that occupies room and no more, so a row can be pushed
     * toward whichever hand is holding the phone. Add several for a wider gap.
     */
    SPACER("spacer", "Spacer, empty room on the row"),

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
        MaFeatureKey.AUTO_BUCKET,
        MaFeatureKey.CLIP_CLEAR,
        MaFeatureKey.APP_SWITCH,
        MaFeatureKey.NEXT_FIELD,
        MaFeatureKey.HISTORY,
        MaFeatureKey.LANGUAGE,
        MaFeatureKey.SCROLL,
        MaFeatureKey.SWITCHBOARD,
        MaFeatureKey.SHIFT,
        MaFeatureKey.CHANGE_CASE,
        MaFeatureKey.PIN,
        MaFeatureKey.READER,
        MaFeatureKey.SUBTITLE,
        MaFeatureKey.SPACER,
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
