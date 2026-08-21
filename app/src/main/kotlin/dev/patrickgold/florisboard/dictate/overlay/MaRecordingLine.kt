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

import android.widget.FrameLayout
import android.graphics.Typeface
import android.graphics.Paint
import android.graphics.Canvas
import dev.patrickgold.florisboard.dictate.MaLanguage
import dev.patrickgold.florisboard.dictate.DictateController
import android.os.SystemClock
import android.os.Looper
import android.os.Handler
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

    private var timer: TextView? = null
    private var ticker: Runnable? = null
    private val handler = Handler(Looper.getMainLooper())

    private fun add() {
        val d = service.resources.displayMetrics.density
        fun px(v: Int) = (d * v).toInt()

        // A FRAME, so the clock is at the true centre of the SCREEN.
        //
        // It was a row with weighted spacers, and that centres the middle group only when the things
        // either side happen to be the same width — ENG plus a bin on the left against one send arrow
        // on the right, which they are not. The numbers sat slightly off, and off-centre is the kind
        // of wrong the eye reports without being able to name.
        //
        // A frame places each group against its own edge and the clock in the middle regardless of
        // what the others weigh.
        val frame = FrameLayout(service).apply {
            // Fully opaque. Nothing of the status bar shows through: this sits at the top of the
            // screen and a translucent strip over the clock and the battery is two layers of
            // information competing in one place.
            setBackgroundColor(0xFF000000.toInt())
            setPadding(px(8), px(6), px(8), px(6))
        }

        // LEFT — the language and the bin, in that order, as on the keyboard.
        val left = LinearLayout(service).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        left.addView(
            TextView(service).apply {
                text = MaLanguage.badge()
                setTextColor(MA_INK)
                textSize = 15f
                typeface = Typeface.DEFAULT_BOLD
                setPadding(px(14), px(6), px(14), px(6))
                setOnClickListener {
                    MaLanguage.cycleMode(service)
                    text = MaLanguage.badge()
                }
            },
        )
        left.addView(glyph("\uD83D\uDDD1", px(12)) { DictateController.cancelRecording() })
        frame.addView(
            left,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.START or Gravity.CENTER_VERTICAL,
            ),
        )

        // CENTRE — the meter above, then the dot and the clock on one line.
        //
        // The meter goes above because this bar lives at the top of the screen: putting it under the
        // numbers pushes it toward the middle of his view, and a level he glances at belongs at the
        // edge with everything else that is only glanced at.
        val centre = LinearLayout(service).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
        }
        centre.addView(
            MaVuView(service),
            LinearLayout.LayoutParams(px(170), px(4)).apply { bottomMargin = px(4) },
        )
        val line = LinearLayout(service).apply {
            orientation = LinearLayout.HORIZONTAL
            // CENTER_VERTICAL, so the dot sits on the middle of the digits rather than on their
            // baseline. It was aligned to the bottom of the row and read as having slipped.
            gravity = Gravity.CENTER_VERTICAL
        }
        line.addView(
            View(service).apply {
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(MA_REC_RED)
                }
            },
            LinearLayout.LayoutParams(px(11), px(11)).apply { rightMargin = px(10) },
        )
        val clock = TextView(service).apply {
            text = "0:00"
            setTextColor(0xFFFFFFFF.toInt())
            textSize = 24f
            // The same face the keyboard's clock uses, so the two are one design rather than two.
            typeface = Typeface.create("sans-serif", Typeface.BOLD)
            setOnClickListener {
                runCatching {
                    (service.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager)
                        ?.showSoftInput(null, InputMethodManager.SHOW_FORCED)
                }
                MaLog.add("keys", "overlay clock tapped, asking for the keyboard")
            }
        }
        timer = clock
        line.addView(clock)
        centre.addView(line)
        frame.addView(
            centre,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER,
            ),
        )

        // RIGHT — send, larger and amber.
        //
        // No cog: settings are not something he reaches for mid-recording, and removing it gives the
        // send arrow the room to be the biggest control on the bar, which is what it deserves to be.
        //
        // Amber, and the same amber the meter uses, because on this bar send means **into the
        // archive** rather than into a text field. The colour is the difference between the two
        // recorders, and it is the only difference he needs to see.
        frame.addView(
            glyph("\u27A4", px(10)) { DictateController.onMicClick(service) }.apply {
                textSize = 26f
                setTextColor(MA_AMBER)
            },
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.END or Gravity.CENTER_VERTICAL,
            ),
        )

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT,
        ).apply { gravity = Gravity.TOP or Gravity.START }

        runCatching {
            windowManager.addView(frame, params)
            view = frame
            added = true
            startTicking()
        }
    }

    /** One glyph as a button, sized for a thumb rather than for its own ink. */
    private fun glyph(text: String, pad: Int, onClick: () -> Unit) = TextView(service).apply {
        this.text = text
        setTextColor(0xFFF2DDB4.toInt())
        textSize = 18f
        setPadding(pad, pad / 2, pad, pad / 2)
        setOnClickListener { onClick() }
    }

    /**
     * The clock, once a second.
     *
     * Read from the recorder's own state rather than counted here, so a pause is honoured and the
     * two views can never disagree about how long he has been speaking.
     */
    private fun startTicking() {
        val r = object : Runnable {
            override fun run() {
                val st = DictateController.state.value
                if (st is DictateController.UiState.Recording) {
                    val ms = if (st.paused) {
                        st.accumulatedMs
                    } else {
                        st.accumulatedMs + (SystemClock.elapsedRealtime() - st.startedAtMs)
                    }
                    val total = ms / 1000
                    timer?.text = "%d:%02d".format(total / 60, total % 60)
                }
                handler.postDelayed(this, 1000)
            }
        }
        ticker = r
        handler.post(r)
    }

    private fun remove() {
        val v = view ?: return
        // Wrapped, because a window can already be gone if the service was torn down under it, and
        // an exception here would take the accessibility service with it — which would cost him the
        // finger, the reader and the line all at once.
        ticker?.let { handler.removeCallbacks(it) }
        ticker = null
        timer = null
        runCatching { windowManager.removeView(v) }
        view = null
        added = false
    }
}

/**
 * The VU meter, on the overlay, reading the same numbers as the one on the keyboard.
 *
 * ### It is the module, not a lookalike
 *
 * The level comes from `DictateController.audioLevel` — the same StateFlow the keyboard's meter
 * collects. The dB conversion, the floor, the peak decay and the three colours are lifted from
 * `MaRecordMeter` unchanged, so the two cannot disagree about how loud he is: **one source of
 * numbers, drawn twice, rather than two meters that happen to look similar.**
 *
 * It is drawn with a Canvas rather than composed because this window has no lifecycle owner for
 * Compose to attach to. That is a difference in the brush, not in the picture.
 *
 * ### Peak hold
 *
 * A bar alone tells you the current instant, which at speech rates is a flicker. The peak mark falls
 * at 0.6 dB per frame — slow enough to read, fast enough to follow a sentence — and it is what makes
 * the thing a meter rather than a light.
 */
private class MaVuView(context: Context) : View(context) {

    private val bar = Paint(Paint.ANTI_ALIAS_FLAG)
    private val track = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF2A2A2E.toInt() }
    private val peakPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFF2DDB4.toInt() }

    private var smoothed = FLOOR_DB
    private var peakDb = FLOOR_DB

    private val tick = object : Runnable {
        override fun run() {
            val db = toDb(DictateController.audioLevel.value)
            // Fast to rise, slow to fall: an attack that lags makes the meter feel dead, a release
            // that snaps makes it feel nervous. The same asymmetry every hardware meter has.
            smoothed = if (db > smoothed) db else smoothed + (db - smoothed) * 0.3f
            peakDb = if (db > peakDb) db else (peakDb - 0.6f).coerceAtLeast(FLOOR_DB)
            invalidate()
            postDelayed(this, 40L)
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        post(tick)
    }

    override fun onDetachedFromWindow() {
        removeCallbacks(tick)
        super.onDetachedFromWindow()
    }

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        canvas.drawRoundRect(0f, 0f, w, h, h / 2, h / 2, track)
        val n = norm(smoothed)
        if (n > 0f) {
            bar.color = colourFor(smoothed)
            canvas.drawRoundRect(0f, 0f, w * n, h, h / 2, h / 2, bar)
        }
        val p = norm(peakDb)
        if (p > 0f) {
            val x = (w * p).coerceIn(2f, w - 2f)
            canvas.drawRect(x - 1.5f, 0f, x + 1.5f, h, peakPaint)
        }
    }

    private companion object {
        const val FLOOR_DB = -54f

        fun toDb(level: Float): Float {
            val v = kotlin.math.abs(level)
            if (v <= 0.0005f) return FLOOR_DB
            return (20.0 * kotlin.math.log10(v.toDouble())).toFloat().coerceIn(FLOOR_DB, 0f)
        }

        fun norm(db: Float): Float = ((db - FLOOR_DB) / (0f - FLOOR_DB)).coerceIn(0f, 1f)

        // The same three, from MaRecordMeter: green while there is headroom, amber approaching, and
        // the app's recording red at the top.
        fun colourFor(db: Float): Int = when {
            db > -3f -> 0xFF9B3B33.toInt()
            db > -12f -> 0xFFF0883E.toInt()
            else -> 0xFF56D364.toInt()
        }
    }
}

/** The sand this app writes in. */
private const val MA_INK = 0xFFF2DDB4.toInt()

/** The amber of the meter's headroom, reused on send so the two agree. */
private const val MA_AMBER = 0xFFF0883E.toInt()

/** Recording red, the same one the keyboard's dot uses. */
private const val MA_REC_RED = 0xFFEF4444.toInt()
