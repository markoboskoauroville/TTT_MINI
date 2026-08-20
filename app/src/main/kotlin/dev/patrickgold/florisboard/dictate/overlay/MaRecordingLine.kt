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

/**
 * One line across the bottom of the screen, while a recording is running with no keyboard in sight.
 *
 * ### Why it exists
 *
 * The volume keys record whether or not the keyboard is on screen, and that turned out to be the
 * feature — he can start dictating without opening anything. But a recording nobody can see is a
 * recording he does not know he started, and the first he learns of it is a transcript arriving from
 * a conversation he had with somebody else in the room.
 *
 * **Anything that captures a microphone must be visible while it does.** Not as a courtesy — as the
 * minimum honesty of a device that listens.
 *
 * ### Why a line and not a bubble
 *
 * There is already a floating button, and it is a control: it can be dragged, pressed, and it takes
 * a corner of the screen. This is not a control. It carries one bit of information — *this is
 * recording* — and the smallest shape that carries one bit is a line.
 *
 * Three device-pixels tall, at the very bottom, across the full width. Not touchable, not focusable,
 * and it never covers anything: at that height it sits in the gesture bar's own margin.
 *
 * ### Why only when the keyboard is hidden
 *
 * With the keyboard up he can already see the recorder — the timer, the waveform, the red dot. A
 * second indicator would be a second thing saying what the first one says.
 */
class MaRecordingLine(private val service: AccessibilityService) {

    private val windowManager =
        service.getSystemService(Context.WINDOW_SERVICE) as WindowManager

    private var view: View? = null
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
        val v = View(service).apply {
            // Red, and this is the one place in this app where red is right: it is the universal
            // sign for recording, and unlike a full container or a finished bucket, a live
            // microphone genuinely is something to be aware of.
            setBackgroundColor(0xFFEF4444.toInt())
        }
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            (service.resources.displayMetrics.density * 3).toInt().coerceAtLeast(2),
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            // Not focusable and not touchable: it must never take a press meant for the app
            // underneath. FLAG_LAYOUT_NO_LIMITS lets it sit past the gesture inset, at the true
            // bottom edge.
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT,
        ).apply { gravity = Gravity.BOTTOM or Gravity.START }

        runCatching {
            windowManager.addView(v, params)
            view = v
            added = true
        }
    }

    private fun remove() {
        val v = view ?: return
        // Wrapped, because a window can already be gone if the service was torn down under it, and
        // an exception here would take the accessibility service with it — which would cost him the
        // finger, the reader and the line all at once.
        runCatching { windowManager.removeView(v) }
        view = null
        added = false
    }
}
