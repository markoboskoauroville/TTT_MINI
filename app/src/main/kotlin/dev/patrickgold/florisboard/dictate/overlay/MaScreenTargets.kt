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

import android.accessibilityservice.AccessibilityService
import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityWindowInfo
import java.util.Locale

/**
 * Finds a control on screen by what it says, and presses it.
 *
 * ### Why this rather than recognising the picture of a button
 *
 * Marko asked for template matching: crop the orange send button, look for that shape on screen,
 * tap it. That would work, and it is the harder and weaker of the two ways.
 *
 * Reading pixels needs `MediaProjection`, which throws a system consent dialog every session and
 * cannot be held open by a keyboard. Then the match has to survive a theme change, a font change, a
 * different screen density, the button being half scrolled off, and the same button drawn a shade
 * darker while it is pressed. Every one of those is a miss, and a miss looks exactly like the key
 * being broken.
 *
 * Android already hands us something better for free. Every control carries the label a screen
 * reader would announce, plus its exact rectangle on screen. `Add URL`, `Generate Images` and a send
 * arrow are all findable by that label with no pixels at all, and the answer is right whatever the
 * theme, the density or the scroll position. This is the same information a blind user's phone uses
 * to describe the screen, so it is maintained by the app author rather than guessed at by us.
 *
 * ### One key, several buttons
 *
 * The targets are a list and the first one found on screen wins. Marko's three buttons are never on
 * screen at the same time, so one key presses whichever is showing: the magic wand.
 */
object MaScreenTargets {

    /** Labels that look like a target and are not. Checked first. */
    private val NEVER = listOf(
        "send feedback",
        "send to",
        "resend",
        "send later",
        "add urls to",
    )

    /**
     * Everything on screen that can be pressed, with the label that would find it.
     *
     * The answer to "how would I know its name". Claude's send button carries no visible text — it
     * is an orange circle with an arrow — so there is nothing to type into the term list, and until
     * now the only way to find its label was to guess. This reads the labels straight out of the
     * screen and lets him pick one.
     *
     * Only genuinely pressable things are listed. A screen holds hundreds of nodes and almost all of
     * them are layout: offering those would bury the four buttons that matter under a wall of
     * containers, and picking a container adds a term that matches something nobody can press.
     *
     * Deduplicated, because one button is often several nested nodes carrying the same label, and
     * three identical rows in the picker look like a fault rather than a choice.
     */
    fun scanClickable(service: AccessibilityService): List<String> {
        val out = LinkedHashSet<String>()
        for (root in appWindowRoots(service)) {
            try {
                collectClickable(root, out, 0)
            } finally {
                runCatching { root.recycle() }
            }
        }
        return out.toList()
    }

    private fun collectClickable(
        node: AccessibilityNodeInfo,
        out: MutableSet<String>,
        depth: Int,
    ) {
        if (depth > 28) return
        // Only things that are themselves pressable. This used to accept any node sitting inside a
        // clickable parent, which on a chat screen means every line of the conversation: Marko's
        // list came back holding "Claude is AI and can make mistakes" and the whole message body.
        // A screen is mostly text, and a picker full of text is a picker nobody can find a button in.
        if (node.isVisibleToUser && node.isClickable && node.isEnabled) {
            // The label may be on the button or on the icon inside it, so look down as well as at
            // the node itself — but only for the button's own label, not for everything under it.
            (labelOf(node) ?: childLabel(node))?.let { out.add(it) }
        }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            collectClickable(child, out, depth + 1)
            runCatching { child.recycle() }
        }
    }

    /** The first short label among a button's immediate children, for an icon button. */
    private fun childLabel(node: AccessibilityNodeInfo): String? {
        for (i in 0 until minOf(node.childCount, 6)) {
            val child = node.getChild(i) ?: continue
            val label = labelOf(child)
            runCatching { child.recycle() }
            if (label != null) return label
        }
        return null
    }

    /**
     * A short, typeable label for a node, or null when it has nothing worth matching.
     *
     * Long text is dropped rather than truncated. A paragraph that happens to be tappable is not a
     * button anybody names, and a truncated term would match the wrong thing later.
     */
    private fun labelOf(node: AccessibilityNodeInfo): String? {
        val raw = node.contentDescription?.toString()?.trim()
            ?: node.text?.toString()?.trim()
            ?: node.viewIdResourceName?.substringAfterLast('/')?.replace('_', ' ')
        val label = raw?.replace('\n', ' ')?.trim().orEmpty()
        // Short enough to be a button's name. A tappable paragraph is not a button anybody
        // names, and a truncated term would match the wrong control later.
        if (label.isBlank() || label.length > 32) return null
        return label
    }

    /**
     * Presses the first target found. Returns the label pressed, or null when nothing matched.
     *
     * The label is returned rather than a boolean so the key can say which button it pressed. On a
     * screen with several possible targets that is the difference between trusting the key and
     * wondering what it just did.
     */
    fun pressFirstMatch(service: AccessibilityService, targets: List<String>): String? {
        for (root in appWindowRoots(service)) {
            try {
                val hit = findIn(root, targets)
                if (hit != null) return hit
            } finally {
                runCatching { root.recycle() }
            }
        }
        return null
    }

    /**
     * The roots of the application windows, front-most first, never the keyboard's own.
     *
     * `rootInActiveWindow` alone is not enough and is the likeliest reason a press finds nothing:
     * while the keyboard is up, the active window can be the input method's, so the search walks the
     * keyboard's own tree — where there is no send button, because the send button is in the app
     * underneath.
     *
     * Windows are ordered by layer, so the highest is the dialog on top rather than the page behind
     * it. That matters for `Add URL`: the dialog and the page beneath it are both present, and the
     * button worth pressing is the one the user can see.
     */
    private fun appWindowRoots(service: AccessibilityService): List<AccessibilityNodeInfo> {
        val own = service.packageName
        val windows = runCatching { service.windows }.getOrNull().orEmpty()
        val roots = windows
            .filter { it.type == AccessibilityWindowInfo.TYPE_APPLICATION }
            .sortedByDescending { it.layer }
            .mapNotNull { w -> runCatching { w.root }.getOrNull() }
            .filter { it.packageName?.toString() != own }
        // The active window as a fallback, for the case where the window list is empty because no
        // service in the system has requested interactive windows yet.
        return roots.ifEmpty { listOfNotNull(service.rootInActiveWindow) }
    }

    private fun findIn(root: AccessibilityNodeInfo, targets: List<String>): String? {
        val found = mutableListOf<Pair<AccessibilityNodeInfo, String>>()
        collect(root, targets, found, 0)
        if (found.isEmpty()) return null

        // Lowest on screen wins. A page can carry the word "send" in its text and a send button in
        // the composer below it; the control is always the lower of the two.
        val best = found.maxByOrNull { (node, _) ->
            Rect().also { node.getBoundsInScreen(it) }.bottom
        }
        var pressed: String? = null
        if (best != null) {
            // The label often sits on an icon inside the button rather than on the button, so walk
            // up until something is clickable. Bounded: an unbounded walk ends at the window root,
            // and clicking that does whatever the app does on a stray tap.
            var target: AccessibilityNodeInfo? = best.first
            var hops = 0
            while (target != null && !(target.isClickable && target.isEnabled) && hops < 5) {
                target = target.parent
                hops++
            }
            if (target != null && target.isClickable && target.isEnabled) {
                if (target.performAction(AccessibilityNodeInfo.ACTION_CLICK)) pressed = best.second
            }
        }
        found.forEach { runCatching { it.first.recycle() } }
        return pressed
    }

    private fun collect(
        node: AccessibilityNodeInfo,
        targets: List<String>,
        out: MutableList<Pair<AccessibilityNodeInfo, String>>,
        depth: Int,
    ) {
        // Deep enough for a real screen, bounded so a pathological layout cannot turn one key press
        // into thousands of calls across a process boundary.
        if (depth > 28) return
        matchOf(node, targets)?.let { out.add(AccessibilityNodeInfo.obtain(node) to it) }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            collect(child, targets, out, depth + 1)
            runCatching { child.recycle() }
        }
    }

    private fun matchOf(node: AccessibilityNodeInfo, targets: List<String>): String? {
        if (!node.isVisibleToUser) return null
        // Description, text and the view's id all count. A button drawn as an icon carries only a
        // description; one drawn as a word carries only text; and an unlabelled button sometimes
        // still has a telling id such as send_button.
        val label = buildString {
            node.contentDescription?.let { append(it).append(' ') }
            node.text?.let { append(it).append(' ') }
            node.viewIdResourceName?.let { append(it.substringAfterLast('/')) }
        }.lowercase(Locale.ROOT).replace('_', ' ')
        if (label.isBlank()) return null
        if (NEVER.any { label.contains(it) }) return null
        return targets.firstOrNull { t ->
            val target = t.lowercase(Locale.ROOT)
            if (target.contains(' ')) {
                // A phrase matches as a phrase, so "generate image" still finds "Generate Images 2"
                // however the coin count beside it changes.
                label.contains(target)
            } else {
                // A single word matches whole, so "send" does not fire on "resend" or "sendbird".
                label.split(' ', '/', ':', '.', '-', ',').any { it == target }
            }
        }
    }
}
