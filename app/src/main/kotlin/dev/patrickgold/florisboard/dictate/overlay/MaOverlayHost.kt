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

import android.content.Context
import android.view.View
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner

/**
 * The three owners a window needs before Compose will run in it.
 *
 * ### Why this file exists
 *
 * The recording bar was rebuilt by hand three times for the overlay, each version a near-miss — the
 * wrong bin icon, no level meter, different spacing — because a `WindowManager` window added by an
 * accessibility service has no `Activity` behind it, and `ComposeView` refuses to compose without a
 * **lifecycle owner**, a **saved-state registry** and a **view-model store**.
 *
 * An Activity supplies all three and nobody notices they exist. A raw window supplies none, and the
 * failure is a blank view rather than an error, which is why "just reuse the composable" kept
 * turning into "reimplement the composable".
 *
 * **They are about eighty lines. Reimplementing the bar was three attempts and still wrong.** That
 * trade should have been obvious two builds earlier: when reuse is blocked by plumbing, build the
 * plumbing — a copy of a living thing needs maintaining forever and drifts the first time the
 * original changes.
 *
 * ### The lifecycle is honest
 *
 * It goes RESUMED when the view is attached and DESTROYED when it is removed, which is exactly what
 * a window's life is. A registry left at CREATED would let the composition run but never animate,
 * and the meter would sit still — a failure that looks like a broken meter rather than a missing
 * lifecycle.
 */
class MaOverlayHost(context: Context) : LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {

    private val registry = LifecycleRegistry(this)
    private val savedState = SavedStateRegistryController.create(this)
    private val store = ViewModelStore()

    override val lifecycle: Lifecycle get() = registry
    override val viewModelStore: ViewModelStore get() = store
    override val savedStateRegistry: SavedStateRegistry get() = savedState.savedStateRegistry

    init {
        // Restored before the lifecycle moves, because the registry refuses a restore afterwards.
        savedState.performRestore(null)
    }

    /** Gives [view] the three owners and starts the lifecycle. */
    fun attach(view: View) {
        view.setViewTreeLifecycleOwner(this)
        view.setViewTreeViewModelStoreOwner(this)
        view.setViewTreeSavedStateRegistryOwner(this)
        registry.currentState = Lifecycle.State.RESUMED
    }

    /** Ends it. Called when the window is removed, so the composition is disposed with it. */
    fun detach() {
        runCatching { registry.currentState = Lifecycle.State.DESTROYED }
        store.clear()
    }
}
