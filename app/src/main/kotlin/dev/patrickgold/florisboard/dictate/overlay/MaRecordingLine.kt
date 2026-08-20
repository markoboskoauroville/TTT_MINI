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
import android.content.Context
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.LayoutDirection
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.dictate.DictateController
import dev.patrickgold.florisboard.dictate.MaLog
import dev.patrickgold.florisboard.dictate.ui.DictateSmartbarUi
import dev.patrickgold.florisboard.ime.theme.FlorisImeTheme
import org.florisboard.lib.compose.ProvideLocalizedResources
/**
 * The recording bar, shown over everything while recording with no keyboard in sight.
 *
 * ### It is the same bar, not a copy of it
 *
 * `DictateSmartbarUi` takes only the recorder's state, so this window calls that function directly:
 * the same controls, the same icons, the same VU meter, in the same theme. Anything that changes on
 * the keyboard changes here, because it is the same code.
 *
 * It was rebuilt by hand three times before this, each version a near-miss — the wrong bin icon, no
 * level meter, different spacing — because a window added by an accessibility service has no
 * Activity behind it, and Compose refuses to run without a lifecycle owner, a saved-state registry
 * and a view-model store. [MaOverlayHost] supplies all three in eighty lines.
 *
 * **When reuse is blocked by plumbing, build the plumbing.** Three approximations cost more than the
 * owners did, and a copy of a living thing needs maintaining forever and drifts the first time the
 * original changes.
 *
 * ### Why it exists at all
 *
 * The volume keys record whether or not the keyboard is on screen, and that is the feature. But a
 * recording nobody can see is one he does not know he started, and the first he would learn of it is
 * a transcript arriving from a conversation he had with somebody else in the room. **Anything that
 * captures a microphone must be visible while it does.**
 *
 * ### Only when the keyboard is hidden
 *
 * With the keyboard up this same bar is already on screen in its usual place. Two of them would be
 * two things saying the same thing.
 */
class MaRecordingLine(private val service: AccessibilityService) {

    private val windowManager =
        service.getSystemService(Context.WINDOW_SERVICE) as WindowManager

    private var view: View? = null
    private var lifecycleHost: MaOverlayHost? = null
    private var added = false

    /**
     * Shows or hides the line. Safe to call repeatedly with the same value.
     *
     * Idempotent on purpose: it is driven from a state flow that emits on every change to the
     * recorder, and adding a window that is already added throws.
     */
    fun show(visible: Boolean) {
        if (visible == added) return
        if (visible) add() else remove()
    }


    private fun add() {
        val host = MaOverlayHost(service)
        val compose = ComposeView(service).apply {
            setContent {
                ProvideLocalizedResources(
                    resourcesContext = service,
                    appName = R.string.app_name,
                    forceLayoutDirection = LayoutDirection.Ltr,
                ) {
                    FlorisImeTheme {
                        // THE REAL BAR. Not a copy of it.
                        //
                        // `DictateSmartbarUi` takes only the state, so once the window can host
                        // Compose there is nothing to reimplement: the same function draws the same
                        // controls, the same icons, the same meter, in the same theme. Anything that
                        // changes on the keyboard changes here, because it is the same code.
                        val state by DictateController.state.collectAsState()
                        DictateSmartbarUi(state = state)
                    }
                }
            }
        }
        host.attach(compose)

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            // Touchable, because every control on the bar is a control. Never focusable: it must not
            // take the cursor from the field he is dictating into.
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT,
        ).apply { gravity = Gravity.BOTTOM or Gravity.START }

        runCatching {
            windowManager.addView(compose, params)
            view = compose
            lifecycleHost = host
            added = true
        }.onFailure {
            MaLog.add("keys", "recording bar could not be shown: ${it.javaClass.simpleName}")
        }
    }

    private fun remove() {
        val v = view ?: return
        // Wrapped, because the window can already be gone if the service was torn down under it, and
        // an exception here would take the accessibility service with it — costing him the finger,
        // the reader and the bar at once.
        runCatching { windowManager.removeView(v) }
        lifecycleHost?.detach()
        lifecycleHost = null
        view = null
        added = false
    }
}
