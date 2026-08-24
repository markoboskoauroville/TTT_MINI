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

    /**
     * The four clipboard keys, brought into the feature row.
     *
     * They existed only as Smartbar quick actions — a separate arrangement, with its own editor and
     * its own key set — so a row could hold paste OR AP, never both, and no amount of dragging
     * could put them side by side. Adding them here is what makes his row possible at all, and it
     * follows the rule the rebuild is for: any key, any row, any order.
     */
    PASTE("paste", "Paste"),
    CUT("cut", "Cut"),
    COPY("copy", "Copy"),
    CLIP_HISTORY("clip_history", "Clipboard history"),
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
     * Dump the accessibility tree of whatever is on screen, onto the clipboard.
     *
     * It exists because everything this keyboard does to other applications depends on that tree,
     * and when the finger cannot find a button or the reader cannot find the words, the tree is the
     * only place the answer lives. Until now reading it meant summoning the wand, pressing the
     * thing, and copying from the bar it raised — a diagnostic that needed a rehearsal.
     *
     * One key instead. Press it and the screen underneath is on the clipboard, ready to paste into
     * a chat where somebody can read it.
     */
    DUMP("dump", "Dump the screen for diagnosis"),

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

    ZONE_1("zone1", "n, the number row"),
    ZONE_2("zone2", "k, the keys"),
    ZONE_3("zone3", "c, the copy row"),
    /**
     * Undo and redo, as keys on the row.
     *
     * They were available only on the old edit strip, which is gone, and through a hardware Ctrl+Z
     * nobody has on a phone. Marko asked for them here, and this is where they belong now that undo
     * covers the buckets as well as the field: the key that reverses a wrongly collected code block
     * should be reachable from the same row the A key is on, not from a keyboard shortcut.
     *
     * Both go through `KeyCode.UNDO` / `KeyCode.REDO` and the keyboard manager, so this key and any
     * other way of firing undo are the same press — the rule that already binds the two sends
     * together. Nothing about the bucket rule lives in the key.
     */
    /**
     * Volume keys on or off, as a key on the row.
     *
     * The one control for `maVolumeKeysLive`. Green while the keys are live, plain while they are
     * not, so the state he can otherwise only discover by pressing a volume key is on the row in
     * front of him.
     */
    /**
     * Send: the same press volume-down makes, as a key.
     *
     * It goes through `MaMagicTargets.pressSend()`, which is the magic finger reading his configured
     * term and pressing that button on screen — not a hardcoded "Send". So the key, the volume key
     * and the wand all send the same way, and changing the term changes all three at once.
     */
    /**
     * F1, F2, F3: show or hide feature row 1, 2 or 3, from the keyboard.
     *
     * The switchboard has had these switches all along, three screens away. These are the same
     * three, under his thumb, where the rows are — **a control for what is on screen belongs on the
     * screen it controls.**
     *
     * Each wears the green ring when its row is showing, the same ring the buckets and the volume
     * key wear. One ring, one green, one meaning: this is on.
     *
     * A key can switch off the row it is standing on. That is not a trap, it is the point — the key
     * is on another row, or it is the last thing he sees before the row goes, and the same key on a
     * remaining row brings it back. `visibleRows` already refuses to leave him with nothing.
     */
    ROW_1("row_1", "F1, show row 1"),
    ROW_2("row_2", "F2, show row 2"),
    ROW_3("row_3", "F3, show row 3"),
    ROW_4("row_4", "F4, show row 4"),
    ROW_5("row_5", "F5, show row 5"),
    ROW_6("row_6", "F6, show row 6"),

    /**
     * The four arrows: left, right, up, down.
     *
     * Plain cursor movement, and the reason they are worth a key each is the reason this whole row
     * exists. He dictates rather than types, and moving a cursor by touch means aiming at a caret
     * a few pixels wide in text he can barely see — the gesture with the worst accuracy on the
     * phone. **A key press cannot miss by three characters.**
     *
     * They send the same key codes the letter keyboard's own arrows send, so long-press repeat,
     * shift-selection and every editor's handling of them are whatever they already were.
     */
    ARROW_LEFT("arrow_left", "Left"),
    ARROW_RIGHT("arrow_right", "Right"),
    ARROW_UP("arrow_up", "Up"),
    ARROW_DOWN("arrow_down", "Down"),

    SEND("send", "Send"),

    /**
     * Record: start, and stop.
     *
     * `DictateController.onMicClick`, the same call volume-up makes and the same one the mic on the
     * recording bar makes. Recording has never had a key on the feature row — the MIC key here is
     * the way into the transcribe view and says so — and he asked for one.
     *
     * That is not a reversal of the note on MIC. The objection there was to a THIRD route replacing
     * the only route to the view. This is a fourth route that takes nothing away.
     */
    RECORD("record", "Record"),

    VOLUME_KEYS("volume_keys", "Volume keys"),

    UNDO("undo", "Undo"),
    REDO("redo", "Redo"),

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
        MaFeatureKey.PASTE,
        MaFeatureKey.CUT,
        MaFeatureKey.COPY,
        MaFeatureKey.CLIP_HISTORY,
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
        MaFeatureKey.DUMP,
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

/**
 * What a key is FOR, which is how the picker is arranged.
 *
 * ### Why the model holds this and not the screen
 *
 * The picker used to group with a `when` written inside itself: three headings, one of them "Keys",
 * holding twenty-six unrelated things in the order the enum happened to declare them. A key added
 * to the app landed in that bucket silently and stayed there.
 *
 * Here it is a property of the key, so **a new key cannot be added without saying what it is for** —
 * the `when` below is exhaustive and the compiler refuses anything else. That is the good kind of
 * failure: it happens at the moment the key is written, by the person who knows the answer.
 *
 * ### The declaration order IS the order of the sections
 *
 * Reading down the screen: what he does constantly, then the buckets, then the text, then dictating,
 * then reading, then getting about, then the keyboard's own shape, then the things touched once.
 */
enum class MaFeatureGroup(val heading: String) {
    CLIPBOARD("Clipboard"),

    /**
     * The buckets, and the two keys that fill and empty them.
     *
     * Marko asked for this by name: **A belongs with the buckets, because A is the bucket system.**
     * It presses a code block's copy button and the capture files the result into the next free
     * bucket — one mechanism in two halves, and the halves were in different sections of the list.
     * Somebody looking for the way to collect code blocks looks under the buckets, and the bin that
     * empties them is on the same reasoning.
     */
    BUCKETS("Copy buckets, C1 to C10"),
    EDITING("Editing the text"),
    DICTATION("Dictation"),
    READING("Reading aloud"),
    MOVING("Getting about"),
    KEYBOARD("The keyboard itself"),
    TOOLS("Settings and diagnosis"),
    MACROS("Your macros, M1 to M10"),
}

/** What this key is for. Exhaustive on purpose: a new key must choose a section. */
val MaFeatureKey.group: MaFeatureGroup
    get() = when (this) {
        MaFeatureKey.PASTE,
        MaFeatureKey.COPY,
        MaFeatureKey.CUT,
        MaFeatureKey.CLIP_HISTORY,
        // AP and AC replace the whole field from the clipboard, or empty it. They are clipboard
        // work, not editing: both are about what is being carried between two places.
        MaFeatureKey.ALL_PASTE,
        MaFeatureKey.ALL_CLEAR,
        -> MaFeatureGroup.CLIPBOARD

        MaFeatureKey.AUTO_BUCKET,
        MaFeatureKey.CLIP_CLEAR,
        -> MaFeatureGroup.BUCKETS

        MaFeatureKey.SELECT_ALL,
        MaFeatureKey.BACKSPACE,
        MaFeatureKey.SPACE,
        MaFeatureKey.ENTER,
        MaFeatureKey.SHIFT,
        MaFeatureKey.CHANGE_CASE,
        // Editing rather than buckets, although undo reaches the buckets. The key is the general
        // one — it reverses whatever happened last, in the field or in a bucket — and filing it
        // under buckets would promise it only did the second.
        MaFeatureKey.UNDO,
        MaFeatureKey.REDO,
        -> MaFeatureGroup.EDITING

        MaFeatureKey.MIC,
        MaFeatureKey.LANGUAGE,
        MaFeatureKey.HISTORY,
        // Dictation rather than "the keyboard itself": what it governs is whether the volume keys
        // start and stop recordings, which is a dictation question wearing a hardware button.
        MaFeatureKey.VOLUME_KEYS,
        MaFeatureKey.RECORD,
        MaFeatureKey.SEND,
        -> MaFeatureGroup.DICTATION

        MaFeatureKey.READER,
        MaFeatureKey.SUBTITLE,
        -> MaFeatureGroup.READING

        MaFeatureKey.APP_SWITCH,
        MaFeatureKey.NEXT_FIELD,
        MaFeatureKey.SCROLL,
        // Moving about, which is what an arrow does. Not "editing the text": an arrow changes where
        // he is, not what is written.
        MaFeatureKey.ARROW_LEFT,
        MaFeatureKey.ARROW_RIGHT,
        MaFeatureKey.ARROW_UP,
        MaFeatureKey.ARROW_DOWN,
        -> MaFeatureGroup.MOVING

        // The shape of the keyboard rather than anything typed with it: which zones show, whether it
        // stays up, and the gap that spaces a row out.
        MaFeatureKey.ZONE_1,
        MaFeatureKey.ZONE_2,
        MaFeatureKey.ZONE_3,
        MaFeatureKey.SWITCHBOARD,
        MaFeatureKey.PIN,
        MaFeatureKey.SPACER,
        // Which rows are showing is the shape of the keyboard, not something typed with it.
        MaFeatureKey.ROW_1,
        MaFeatureKey.ROW_2,
        MaFeatureKey.ROW_3,
        MaFeatureKey.ROW_4,
        MaFeatureKey.ROW_5,
        MaFeatureKey.ROW_6,
        -> MaFeatureGroup.KEYBOARD

        MaFeatureKey.SETTINGS,
        MaFeatureKey.DUMP,
        -> MaFeatureGroup.TOOLS
    }
