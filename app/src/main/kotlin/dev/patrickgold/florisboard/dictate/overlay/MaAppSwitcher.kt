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

import android.content.pm.PackageManager
import android.accessibilityservice.AccessibilityService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import android.content.Context
import android.content.Intent

/**
 * Which two apps were last in front, so the TAB key can flip between them.
 *
 * Alt+Tab, on a phone. Marko copies in one app and pastes in another, and the buckets made the
 * carrying easy while the switching stayed as slow as it ever was: recents, find the card, tap it.
 * This makes it one key.
 *
 * ### Why a package list and not the recents screen
 *
 * Android gives an accessibility service `GLOBAL_ACTION_RECENTS`, which opens the recents overlay
 * and stops there — the user still has to find the card and tap it, which is the work being removed.
 * Firing it twice does toggle on some builds and does nothing on others; it depends on the launcher,
 * so it is not something to build a key on.
 *
 * There is no API that says "go back to the previous app". So this watches instead. The accessibility
 * service already receives `TYPE_WINDOW_STATE_CHANGED` for the bubble, every event carries the
 * package that came forward, and two of those remembered in order is the whole switcher.
 *
 * Switching is then a launch intent, which brings the existing task forward rather than starting the
 * app again: the user lands back where they were, mid-scroll, mid-message.
 */
object MaAppSwitcher {

    /** The pause between the two recents presses. See switchViaRecents. */
    private const val RECENTS_GAP_MS = 180L

    /**
     * Not apps, and none of them should ever become "the app you were just in".
     *
     * The keyboard itself is the obvious one — it is in front constantly and would make every switch
     * a switch to the keyboard. The launcher and the system shade are the subtle ones: passing
     * through the home screen on the way somewhere else is not a destination, and a switcher that
     * counted it would send the user home instead of back.
     */
    /**
     * Not apps, and none of them should ever become "the app you were just in".
     *
     * The keyboard itself is the obvious one. The system shade is the next. The launcher is the one
     * that actually broke: this was a hard-coded list of stock launcher packages, Marko runs Nova,
     * and Nova is not on any such list — so the home screen counted as an app and TAB took him to
     * the desktop and back. A list of launcher package names can only ever be a list of the
     * launchers somebody thought of.
     *
     * So the launcher is asked for by name instead, from the system, at the moment it is needed.
     * Whatever he is running is the right answer, including one released tomorrow.
     */
    private val IGNORED_PREFIXES = listOf(
        "com.android.systemui",
    )

    /**
     * The package that handles HOME, or null if that cannot be resolved.
     *
     * Resolved per call rather than cached: the default launcher can be changed while the keyboard
     * is running, and a cached answer would leave the old one counting as an app.
     */
    private fun launcherPackage(context: Context): String? = runCatching {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        context.packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
            ?.activityInfo?.packageName
    }.getOrNull()

    @Volatile
    private var current: String? = null

    @Volatile
    private var previous: String? = null

    /** The package the TAB key would switch to, or null when nothing has been seen yet. */
    val previousApp: String? get() = previous

    /**
     * Records that [pkg] came to the front.
     *
     * Repeats are ignored rather than shuffled. An app raises many window events while it is being
     * used — a dialog, a keyboard opening, a fragment swap — and treating each as a new arrival
     * would push the genuinely previous app out after a few seconds of ordinary use, leaving TAB
     * flipping between one app and itself.
     */
    fun onWindowPackage(context: Context, ownPackage: String, pkg: String?) {
        val name = pkg?.takeIf { it.isNotBlank() } ?: return
        if (name == ownPackage) return
        if (IGNORED_PREFIXES.any { name.startsWith(it) }) return
        // Passing through the home screen on the way somewhere else is not a destination. Counting
        // it is what sent Marko to the desktop instead of back to his last app.
        if (name == launcherPackage(context)) return
        if (name == current) return
        previous = current
        current = name
    }

    /**
     * Brings the previously used app forward. Returns false when there is nowhere to go.
     *
     * The launch intent rather than a fresh Activity: `ACTION_MAIN`/`CATEGORY_LAUNCHER` resumes the
     * app's existing task if there is one, so the user arrives back where they left rather than at
     * its front door. `NEW_TASK` is required because this is started from a service context.
     *
     * ### Why current and previous are swapped here rather than left to the event
     *
     * The window event will arrive and do the same thing a moment later, but not before the user can
     * press TAB again. Two quick presses would otherwise both read the same stale pair and switch to
     * the same app twice, which reads as the key having missed. Swapping now makes the second press
     * correct even if it lands before the system has caught up, and the event that follows finds the
     * state already right and changes nothing.
     */
    /**
     * Switches by opening the recents list twice, which is what Marko does with his thumb.
     *
     * Pressing the square and swiping is the gesture he already trusts, and on stock Android
     * opening recents twice in quick succession is exactly that: the second open returns to the app
     * behind the current one. It is better than a launch intent for two reasons — it uses the
     * system's own idea of task order rather than ours, so it cannot be confused by what this app
     * happened to observe, and it restores the task exactly as the user left it.
     *
     * Returns false when the service is not running, so the caller can fall back.
     */
    fun switchViaRecents(scope: CoroutineScope): Boolean {
        val service = DictateAccessibilityService.gestureService() ?: return false
        scope.launch {
            service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_RECENTS)
            // Long enough for the recents animation to have started, short enough that the two
            // presses are still read as one gesture. Too short and the second is swallowed by the
            // first; too long and the list simply stays open.
            delay(RECENTS_GAP_MS)
            service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_RECENTS)
        }
        return true
    }

    fun switchToPrevious(context: Context): Boolean {
        val target = previous ?: return false
        val intent = context.packageManager.getLaunchIntentForPackage(target)
            ?: run {
                // The app has been uninstalled or hidden since it was seen. Forget it rather than
                // keep failing on it, so the next switch has a chance of working.
                previous = null
                return false
            }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
        return runCatching {
            context.startActivity(intent)
            val wasCurrent = current
            current = target
            previous = wasCurrent
            true
        }.getOrDefault(false)
    }

    /** True when the TAB key has somewhere to go, for its dimmed state. */
    fun hasTarget(): Boolean = previous != null
}
