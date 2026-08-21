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


/**
 * What C1 to C10 hold: copies caught in the order they were made, and then held still.
 *
 * ### Why this exists instead of reading the clipboard history
 *
 * The keys used to be a window onto the history, sorted newest first. That meant every key changed
 * meaning on every copy: what was on C1 slid to C2, then C3, and a key pressed from memory pasted
 * whatever had since moved under it. For a row navigated by number and pressed without looking, that
 * is the worst possible behaviour.
 *
 * So the slots are buckets. A copy goes into the lowest empty bucket and stays in it. Pasting from
 * a bucket pours it out and leaves it empty, and the next copy fills the lowest empty bucket again —
 * which may well be the one just emptied. When every bucket is full, capturing stops until one is
 * poured out or the row is cleared.
 *
 * The consequence worth stating: a bucket never changes contents while it is holding something. C4
 * is whatever went into C4, until C4 is used. Nothing slides.
 *
 * ### Why the text is stored, not a reference to the history entry
 *
 * A history row can be trimmed, deleted or unpinned out from under a slot, and a slot pointing at a
 * vanished row is a key that silently stops working. The text is what gets pasted, so the text is
 * what is kept.
 *
 * The cost is that only text is captured. An image copied while slots are free is skipped rather
 * than taking one, because a C key that pastes nothing would occupy a slot that a usable copy could
 * have had. Images are still in the history panel, reachable by a long press on any C key.
 */
object MaClipCapture {

    private const val SEP = '\u001E'

    /** Ten buckets, matching the ten keys. Beyond the last key a copy would be unreachable. */
    const val CAPACITY = MaRows.CLIP_SLOTS

    // FILL_MARK_MS is gone with the tick it timed.
    //
    // The tick appeared on a bucket for a minute after a copy landed, and Marko asked twice for it
    // to last longer — the second time as "until I empty the bucket". At that point it is not a
    // mark with a duration at all, it is the state of the bucket, and the key already knows that
    // state. It wears a green ring while it is holding something. No clock, nothing to miss.


    // autoRank is gone with the ladder it counted. The A key reads what is in the frame, so there
    // is no position to store, to rewind, or to disagree with the screen.

    // lastFilled and noteFilled are gone too, with the tick they existed to place. They recorded
    // WHICH bucket had just taken a copy, and nothing needs to know that any more: the ring is on
    // every bucket that is holding something, not on the one that filled most recently. State that
    // nothing reads is state that drifts, so it goes rather than waiting for a use.

    /** Ten buckets, all empty. */
    val Empty: List<String?> = List(CAPACITY) { null }

    /**
     * Pours [text] into the lowest empty bucket, or returns the buckets unchanged.
     *
     * Lowest rather than next-after-the-last: the buckets are numbered and pressed from memory, so
     * filling the first free one keeps the used range contiguous and makes C1 the thing to reach for
     * after a clear, every time. Filling after the highest used bucket would scatter the contents
     * into gaps nobody can see on a row of numbered keys.
     *
     * Unchanged in three cases. Full: capturing stops rather than pushing anything out, because a
     * row that rewrites itself is exactly what this replaced. Blank: whitespace would burn a bucket.
     * Already held: people re-copy the same text constantly to be sure, and that must not fill the
     * row with one repeated value.
     */
    fun capture(slots: List<String?>, text: String, visible: Set<Int>): List<String?> {
        if (text.isBlank()) return slots
        if (slots.any { it == text }) return slots
        // The lowest empty bucket *that is on the keyboard*. Filling one the user cannot see
        // swallows the copy: the text is captured, the row looks unchanged, and there is no key to
        // paste it from. With no buckets on screen at all there is nowhere for a copy to go, and
        // that is a correct answer rather than a failure.
        val free = visible.sorted().firstOrNull { slots.getOrNull(it - 1) == null } ?: return slots
        return slots.toMutableList().also { it[free - 1] = text }
    }

    /** What C[slot] pastes, 1-based, or null when that bucket is empty. */
    fun at(slots: List<String?>, slot: Int): String? =
        if (slot < 1 || slot > CAPACITY) null else slots.getOrNull(slot - 1)

    /**
     * Empties one bucket, after its contents have been pasted.
     *
     * Marko's rule and the reason the whole thing is called a bucket: text is carried from one place
     * to another, and once it has been poured out the bucket is free rather than still holding a
     * copy of what was moved. It also means the row empties as it is used, so the keys still
     * showing a number are exactly the ones still holding something.
     */
    fun pour(slots: List<String?>, slot: Int): List<String?> {
        if (slot < 1 || slot > CAPACITY) return slots
        if (slots.getOrNull(slot - 1) == null) return slots
        return slots.toMutableList().also { it[slot - 1] = null }
    }

    /** Which bucket holds [text], or null. Used to label the history panel. */
    fun slotFor(slots: List<String?>, text: String): Int? =
        slots.indexOfFirst { it != null && it == text }.takeIf { it >= 0 }?.plus(1)

    /**
     * Whether every bucket on the keyboard is holding something.
     *
     * Measured against the visible set, not against all ten. Four buckets on the row means full at
     * four, and the row says so by turning red — the next copy has nowhere to go until the trash key
     * empties them. An empty visible set is not full: there are no buckets, which is a different
     * thing from full ones and must not paint the row red.
     */
    fun isFull(slots: List<String?>, visible: Set<Int>): Boolean =
        visible.isNotEmpty() && visible.all { slots.getOrNull(it - 1) != null }

    /** How many visible buckets are holding something, for the trash key's dimmed state. */
    fun filledCount(slots: List<String?>, visible: Set<Int>): Int =
        visible.count { slots.getOrNull(it - 1) != null }

    /**
     * Serialised with a control character, so captured text may contain commas, quotes, braces and
     * newlines and come back exactly as it was. This text is going to be pasted into somebody's
     * document; it has to survive the round trip character for character.
     *
     * Every bucket is written, empty ones included, because position is the whole meaning here. A
     * format that dropped empties would shuffle C7 into C3 the moment an earlier bucket was poured
     * out, which is the sliding this design exists to prevent.
     */
    fun serialize(slots: List<String?>): String =
        (0 until CAPACITY).joinToString(SEP.toString()) { slots.getOrNull(it) ?: "" }

    fun parse(raw: String): List<String?> {
        if (raw.isBlank()) return Empty
        val parts = raw.split(SEP)
        return (0 until CAPACITY).map { parts.getOrNull(it)?.takeIf { p -> p.isNotEmpty() } }
    }

    /**
     * What a C key says out loud.
     *
     * The key itself shows only its number: ten text previews across one row would be a few
     * characters wide each and unreadable. Spoken, it is the only way to answer "what is in C4"
     * without pasting it somewhere to find out, which matters more here than it usually would.
     */
    fun describeSlot(text: String?, slot: Int): String = when {
        text == null -> "Copy bucket $slot, empty"
        else -> {
            val short = text.replace('\n', ' ').trim().take(60)
            if (text.length > 60) "Copy bucket $slot, $short\u2026" else "Copy bucket $slot, $short"
        }
    }
}


/**
 * Undo, for the buckets.
 *
 * ### Why the buckets need their own undo
 *
 * Ctrl+Z sends a key event to the field and the field decides what to do with it. The buckets are
 * not in a field — they are this keyboard's own state — so nothing the editor undoes ever touches
 * them. A code block collected from the wrong place used to be unrecoverable except by emptying
 * every bucket and starting the ladder again.
 *
 * ### How one key means both
 *
 * Undo reverses the last thing that happened. That is what it has always meant, so the rule is that
 * and nothing cleverer: **if the newest bucket change is newer than the last text this keyboard
 * wrote, undo puts the buckets back; otherwise the key does exactly what it did before.**
 *
 * This is a rule about ORDER, not about timing. It does not matter how long ago either happened or
 * how long the key is held — the same sequence of actions always gives the same answer. A key whose
 * meaning depends on how long you press it has to be predicted; a key that undoes whatever you did
 * last does not.
 *
 * Every text commit passes through `EditorInstance.commitText`, which stamps the clock here. So
 * typing, dictating or pasting all hand the undo key back to the field, in one place, with no list
 * of call sites to keep current.
 *
 * ### What is undoable, and what is deliberately not
 *
 * A capture and the bin are undoable: both change what the buckets hold and nothing else.
 *
 * **Pouring a bucket into a field is not**, and that is a decision rather than an omission. That
 * press did two things — filled the field and emptied the bucket — and undoing only the second would
 * leave the text in the document AND back in the bucket, which is a state he never asked for. The
 * paste stamped the text clock on its way through, so undo after a paste goes to the field, where
 * the visible half of that action lives.
 *
 * ### Once text is the newest thing, undo stays with the field
 *
 * This is worth stating because it is easy to assume the opposite. After any text is written, the
 * undo key belongs to the field until a bucket changes again — it does not walk back into the
 * buckets once the field runs out, because we cannot tell when the field has run out. The field
 * keeps its own multi-level history that this keyboard cannot see, so a second press that jumped to
 * the buckets would pull a collected block out from under somebody who was still undoing sentences.
 *
 * A first draft of this file claimed in a comment that the buckets were still reachable underneath.
 * They are not, the walked test caught it, and the rule is the better of the two: **undo is for what
 * you just did.** A copy taken from the wrong place is undone before typing resumes, which is the
 * moment it is noticed anyway.
 */
object MaBucketUndo {

    /** One reversible change: what the buckets held, and where the ladder was, before it. */
    data class Step(val slots: List<String?>, val atMs: Long)

    /**
     * Twenty deep. Deeper costs nothing in memory but stops meaning anything: past twenty presses
     * nobody remembers the state being returned to, and an undo that lands somewhere unrecognisable
     * is worse than a stop.
     */
    private const val DEPTH = 20

    private val stack = ArrayDeque<Step>()

    /**
     * What undo has taken off, waiting to be put back.
     *
     * Cleared by every new bucket change: a redo only means something while the world it would be
     * returned into is the one it left. `push` empties it, which is where that rule lives.
     */
    private val redoStack = ArrayDeque<Step>()

    private var lastTextAtMs = 0L

    /**
     * A step armed by the A key BEFORE it presses anything.
     *
     * The A key advances the ladder as soon as its press lands, while the copy reaches the clipboard
     * a moment later through the system. So by the time the capture records a step, the rank has
     * already moved on and a step recorded there would put the ladder back one rung short.
     *
     * Arming solves it without a timer: the A key hands over the state as it was before the press,
     * and the next capture uses that instead of reading the rank itself. **A flag consumed by the
     * next event is exact; a window measured in milliseconds is a guess that is usually right.**
     */
    private var armed: Step? = null

    /** Called by the A key before it presses a copy button. */
    fun armAuto(slots: List<String?>) {
        armed = Step(slots, android.os.SystemClock.elapsedRealtime())
    }

    /** Called when the A key's press did not land, so nothing will arrive to consume the arming. */
    fun disarm() {
        armed = null
    }

    /**
     * Records the state before a change.
     *
     * Consumes an arming if one is waiting, whether or not this call pushes anything — an armed step
     * that nobody used would otherwise be spent by an unrelated copy much later, rewinding a ladder
     * that had moved on for its own reasons.
     */
    fun push(slots: List<String?>) {
        val step = armed ?: Step(slots, android.os.SystemClock.elapsedRealtime())
        armed = null
        stack.addLast(step.copy(atMs = android.os.SystemClock.elapsedRealtime()))
        while (stack.size > DEPTH) stack.removeFirst()
        // A new change makes every pending redo meaningless. Keeping them would let a redo put a
        // bucket back over something collected since, which is a bucket holding what nobody put in
        // it — the exact failure the whole bucket design exists to avoid.
        redoStack.clear()
    }

    /** Consumes an arming without recording anything, when a copy changed no bucket at all. */
    fun dropArming() {
        armed = null
    }

    /** Stamped by every text commit, so undo knows which happened last. */
    fun noteText() {
        lastTextAtMs = android.os.SystemClock.elapsedRealtime()
    }

    /**
     * The step to reverse, or null when the field should have the key instead.
     *
     * Removes it, so pressing undo repeatedly walks back through the bucket changes and then falls
     * through to the field once they run out. What is removed goes onto the redo stack.
     *
     * [nowSlots] is what the buckets hold at this moment, so redo has somewhere to return to.
     */
    fun takeIfNewest(nowSlots: List<String?>): Step? {
        val step = stack.lastOrNull() ?: return null
        if (step.atMs < lastTextAtMs) return null
        stack.removeLast()
        redoStack.addLast(Step(nowSlots, android.os.SystemClock.elapsedRealtime()))
        while (redoStack.size > DEPTH) redoStack.removeFirst()
        return step
    }

    /**
     * Redo, the other half of the same key pair.
     *
     * Marko asked for undo and redo as keys on the row, and a redo that reversed text but not
     * buckets would be the asymmetry that makes a pair of keys untrustworthy: undo the collection,
     * press redo, and get half of it back.
     *
     * The rule is the mirror of undo's and just as plain: **a redo is available only while nothing
     * has happened since the undo.** Anything at all — a copy, another A press, a word typed —
     * throws the redo stack away, because at that point the state redo would return to is not a
     * state that follows from what is on screen now.
     *
     * That is stricter than undo's rule, and deliberately. Undo reverses a thing that definitely
     * happened; redo restores a thing that was already decided against, and restoring it into a
     * changed world is how a bucket ends up holding something nobody put there.
     */
    fun takeRedo(): Step? {
        val step = redoStack.lastOrNull() ?: return null
        if (step.atMs < lastTextAtMs) return null
        redoStack.removeLast()
        return step
    }
}
