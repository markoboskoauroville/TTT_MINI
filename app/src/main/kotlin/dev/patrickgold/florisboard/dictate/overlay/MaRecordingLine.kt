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

import dev.patrickgold.florisboard.dictate.MaLog
import android.widget.TextView
import android.widget.LinearLayout
import android.view.inputmethod.InputMethodManager
import android.graphics.drawable.GradientDrawable
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
        val d = service.resources.displayMetrics.density
        val v = LinearLayout(service).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            // Black at seventy per cent, so what is underneath stays readable. A solid strip would
            // hide a line of whatever he is looking at, and this exists to inform him, not to cost
            // him the thing he was reading.
            setBackgroundColor(0xB3000000.toInt())
            setPadding((d * 14).toInt(), (d * 6).toInt(), (d * 14).toInt(), (d * 6).toInt())

            // The red dot: the same sign the recording bar uses, so the two views say it the same
            // way rather than each inventing a language.
            addView(
                View(context).apply {
                    background = GradientDrawable().apply {
                        shape = GradientDrawable.OVAL
                        setColor(0xFFEF4444.toInt())
                    }
                },
                LinearLayout.LayoutParams((d * 10).toInt(), (d * 10).toInt()),
            )

            addView(
                TextView(context).apply {
                    text = "recording"
                    // Lowercase, as he asked for elsewhere: this is a state, not a headline.
                    setTextColor(0xFFF2DDB4.toInt())
                    textSize = 13f
                    setPadding((d * 10).toInt(), 0, 0, 0)
                },
            )

            // The whole strip opens the keyboard, because the strip IS the answer to "where is it".
            // Anything more specific would be a target to aim at while walking.
            setOnClickListener {
                runCatching {
                    (service.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager)
                        ?.showSoftInput(null, InputMethodManager.SHOW_FORCED)
                }
                MaLog.add("keys", "recording strip tapped, asking for the keyboard")
            }
        }
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            // Focusable is still off — it must never steal the cursor from the field he is dictating
            // into — but touchable is now ON, because tapping it is the way back to the keyboard.
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
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
