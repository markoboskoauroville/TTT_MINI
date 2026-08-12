/*
 * Copyright (C) 2026 Marko Bosko, Mantra Productions
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package dev.patrickgold.florisboard.dictate.overlay

import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo
import java.util.Locale

/**
 * Presses the send button of whatever chat app is in front.
 *
 * Marko types a prompt, then has to leave the keyboard, find the send button and tap it. On a
 * workflow that is otherwise one key after another — bucket, bucket, tab, send — that one reach is
 * the thing that breaks the rhythm. This key removes it.
 *
 * ### Why it searches the screen instead of knowing about Claude
 *
 * Hard-coding one app's send button would work until that app was updated, and it would work in
 * nothing else. Instead this walks the window the user is looking at and finds a control that says
 * it sends, using the same information a screen reader would announce. That makes the key work in
 * any app that labels its send button — which is most of them, because an unlabelled button is
 * unusable to a blind user and app stores check for it.
 *
 * It also means the key fails honestly. If nothing on screen claims to be a send button, nothing is
 * pressed, rather than a guess landing on whatever happens to sit in the bottom-right corner.
 *
 * ### Why not the editor's own send action
 *
 * `performEditorAction(IME_ACTION_SEND)` is the tidy answer and does not work here. A chat box that
 * accepts several lines declares no send action at all — its enter key has to make a new line — so
 * the call goes nowhere in exactly the apps this key is for.
 */
object MaSendButton {

    /**
     * Labels that mean "send this".
     *
     * English and Croatian, since those are the two languages Marko works in, and matched as a
     * prefix so "Send message" and "Pošalji poruku" both count. Kept short deliberately: a longer
     * list is a wider net, and the cost of matching something that is not a send button is a key
     * that presses the wrong thing.
     */
    private val SEND_WORDS = listOf(
        "send",
        "pošalji",
        "posalji",
        "salji",
        "šalji",
    )

    /**
     * Words that look like sending and are not.
     *
     * "Send feedback" and "Send to" open menus. Checked before the positive list, because a control
     * whose label contains both is more likely to be the trap than the target.
     */
    private val NOT_SEND = listOf(
        "send feedback",
        "send to",
        "resend",
        "send later",
    )

    /**
     * Finds the send control in [root] and clicks it. Returns false when there is nothing to click.
     *
     * Nodes are recycled as the walk goes, except the one being returned, because an accessibility
     * walk that leaks nodes degrades the whole system's accessibility over a session rather than
     * just this app's.
     */
    fun pressIn(root: AccessibilityNodeInfo?): Boolean {
        val node = root ?: return false
        val candidates = mutableListOf<AccessibilityNodeInfo>()
        collect(node, candidates, depth = 0)
        if (candidates.isEmpty()) return false

        // The lowest one on screen wins when several match. A chat screen can carry a send button in
        // the composer and the word "send" somewhere in the conversation above it; the composer is
        // always the lower.
        val best = candidates.maxByOrNull { n ->
            val r = Rect().also { n.getBoundsInScreen(it) }
            r.bottom
        }
        var clicked = false
        if (best != null) {
            // The label may sit on an icon inside the button rather than on the button itself, so
            // walk up until something is actually clickable. Bounded, because an unbounded walk ends
            // at the window root, and clicking the root does whatever the app does on a stray tap.
            var target: AccessibilityNodeInfo? = best
            var hops = 0
            while (target != null && !target.isClickable && hops < 4) {
                target = target.parent
                hops++
            }
            if (target != null && target.isClickable && target.isEnabled) {
                clicked = target.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            }
        }
        candidates.forEach { runCatching { it.recycle() } }
        return clicked
    }

    private fun collect(
        node: AccessibilityNodeInfo,
        out: MutableList<AccessibilityNodeInfo>,
        depth: Int,
    ) {
        // A chat screen's tree is deep but a send button is never far from the composer. The bound
        // keeps a pathological layout from turning one key press into thousands of IPC calls.
        if (depth > 24) return
        if (looksLikeSend(node)) {
            out.add(AccessibilityNodeInfo.obtain(node))
        }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            collect(child, out, depth + 1)
            runCatching { child.recycle() }
        }
    }

    private fun looksLikeSend(node: AccessibilityNodeInfo): Boolean {
        if (!node.isVisibleToUser) return false
        val label = buildString {
            node.contentDescription?.let { append(it).append(' ') }
            node.text?.let { append(it).append(' ') }
            node.viewIdResourceName?.let { append(it) }
        }.lowercase(Locale.ROOT)
        if (label.isBlank()) return false
        if (NOT_SEND.any { label.contains(it) }) return false
        // Whole word, not substring: "sender", "resend" and an id like "sendbird" all contain the
        // letters and none of them are the button.
        return SEND_WORDS.any { word ->
            label.split(' ', '_', '/', ':', '.', '-').any { it == word }
        }
    }
}
