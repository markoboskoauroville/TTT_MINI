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

import dev.patrickgold.florisboard.ime.keyboard.FlorisImeSizing
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Box
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CoroutineScope
import dev.patrickgold.florisboard.app.FlorisPreferenceStore
import dev.patrickgold.florisboard.ime.window.LocalWindowController
import dev.patrickgold.florisboard.ime.window.ImeWindowController
import androidx.compose.runtime.CompositionLocalProvider
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

    /**
     * Its own window controller, because the theme insists on one and the IME may not be running.
     *
     * Built from preferences and this class's own scope. Nothing here drives a real window — the
     * theme only reads a font scale from it — so a controller of its own is honest rather than a
     * stand-in for something missing.
     */
    private val overlayPrefs by FlorisPreferenceStore
    private val windowController by lazy {
        ImeWindowController(overlayPrefs, CoroutineScope(Dispatchers.Main.immediate + SupervisorJob()))
    }
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
                    // THE LOCAL THE THEME NEEDS, WHICH THE IME NORMALLY PROVIDES.
                    //
                    // `FlorisImeTheme` reads `LocalWindowController`, and that local's default is
                    // `error("only available within an IME view")` — a hard throw, not a fallback.
                    // Without this line the bar crashes the instant it is shown, which is exactly
                    // what "it does not survive without the keyboard" was.
                    //
                    // It is constructed from preferences and a scope; it needs no IME. All the theme
                    // wants from it is the font scale.
                    CompositionLocalProvider(
                        LocalWindowController provides windowController,
                    ) {
                    FlorisImeTheme {
                        // THE REAL BAR. Not a copy of it.
                        //
                        // `DictateSmartbarUi` takes only the state, so once the window can host
                        // Compose there is nothing to reimplement: the same function draws the same
                        // controls, the same icons, the same meter, in the same theme. Anything that
                        // changes on the keyboard changes here, because it is the same code.
                        val state by DictateController.state.collectAsState()
                        // GIVE IT A HEIGHT. This is why the bar recorded but never appeared.
                        //
                        // `DictateSmartbarUi` sizes itself with `fillMaxSize()`, which is right
                        // inside the keyboard: the smartbar slot there has a fixed height and the
                        // bar fills it. This window is WRAP_CONTENT, so the parent's height is
                        // whatever the child asks for — and a child asking to fill its parent, in a
                        // parent sized by its child, resolves to **zero**.
                        //
                        // The window was added, the composition ran, the recording worked. It was
                        // simply nought pixels tall. Nothing threw, so nothing said so.
                        //
                        // `smartbarHeight` is the same number the keyboard gives it, so the bar is
                        // the size it is at home rather than a size invented here.
                        Box(modifier = Modifier.height(FlorisImeSizing.smartbarHeight)) {
                            DictateSmartbarUi(state = state)
                        }
                    }
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
            // The bar failing must never take the service with it.
            //
            // This window hosts a composition that reads the keyboard's theme, and the accessibility
            // service also owns the magic finger and the reader. An exception thrown here would kill
            // all three at once — he would lose the finger and the reader because a strip of UI could
            // not be drawn, which is a wildly disproportionate way to fail.
            //
            // The recording itself is unaffected either way: the microphone does not run through
            // this window. Worst case he records without seeing the bar, which is where this feature
            // started.
            MaLog.add("keys", "recording bar could not be shown: ${it.javaClass.simpleName}")
            host.detach()
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
