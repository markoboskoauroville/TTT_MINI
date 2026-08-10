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
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.os.Build
import dev.patrickgold.florisboard.dictate.MaClicks
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Sends a pointer click, and plays a recorded sequence of them, into whichever app is in front.
 *
 * Everything here goes through `dispatchGesture`, which delivers a real touch, so the target app
 * reacts as it would to a finger: menus open, dialogs appear, lists scroll. That is what makes
 * recording against a live screen possible in the first place, and it is why the accessibility
 * service now declares `canPerformGestures`.
 *
 * The service has to be enabled by hand in the system accessibility settings. Nothing here can turn
 * it on, and every entry point says so rather than failing quietly, because a click that does
 * nothing looks identical to a click that landed somewhere harmless.
 */
object MaClickPlayer {

    /**
     * Fractions become pixels here, against the screen as it is at the moment of the click.
     *
     * Deliberately not measured once and cached. The window changes size when the keyboard opens or
     * the phone is turned, and a cached height would put every click of a long sequence at the wrong
     * place for the rest of it.
     */
    private fun AccessibilityService.screenPx(): Pair<Int, Int> {
        val metrics = resources.displayMetrics
        return metrics.widthPixels to metrics.heightPixels
    }

    private val _playing = MutableStateFlow(false)

    /** Whether a sequence is running, so the UI can offer to stop it and refuse to start a second. */
    val playing: StateFlow<Boolean> = _playing.asStateFlow()

    @Volatile
    private var cancelled = false

    /** Asks a running sequence to stop after its current step. */
    fun cancel() {
        cancelled = true
    }

    /**
     * Dispatches one gesture and waits for the framework to say it finished.
     *
     * Waiting matters. `dispatchGesture` returns as soon as the gesture is accepted, not when it has
     * been delivered, so firing the next step immediately stacks gestures on top of each other and
     * Android drops all but one — which shows up as a sequence where random steps did nothing.
     *
     * The timeout is a backstop for the case where the completion callback never arrives at all,
     * which happens if the service is disabled mid-sequence. Without it the sequence would hang
     * forever holding the playing flag, and the only way out would be force-stopping the app.
     */
    private suspend fun AccessibilityService.dispatchAndWait(
        description: GestureDescription,
        timeoutMs: Long,
    ): Boolean {
        val done = CompletableDeferred<Boolean>()
        val accepted = dispatchGesture(
            description,
            object : AccessibilityService.GestureResultCallback() {
                override fun onCompleted(gestureDescription: GestureDescription?) {
                    done.complete(true)
                }

                override fun onCancelled(gestureDescription: GestureDescription?) {
                    done.complete(false)
                }
            },
            null,
        )
        if (!accepted) return false
        return withTimeoutOrNull(timeoutMs) { done.await() } ?: false
    }

    private fun strokeOf(path: Path, startMs: Long, durationMs: Long): GestureDescription =
        GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, startMs, durationMs))
            .build()

    /** A tap where the pointer is. */
    suspend fun tap(service: AccessibilityService, xFraction: Float, yFraction: Float): Boolean {
        val (w, h) = service.screenPx()
        val path = Path().apply { moveTo(xFraction * w, yFraction * h) }
        // A tap is a stroke that does not move. Android needs a non-zero duration for it: a stroke
        // of zero length and zero time is rejected as malformed rather than treated as a tap.
        return service.dispatchAndWait(strokeOf(path, 0L, 50L), 2_000L)
    }

    suspend fun longPress(service: AccessibilityService, xFraction: Float, yFraction: Float): Boolean {
        val (w, h) = service.screenPx()
        val path = Path().apply { moveTo(xFraction * w, yFraction * h) }
        return service.dispatchAndWait(
            strokeOf(path, 0L, MaClicks.LONG_PRESS_MS),
            MaClicks.LONG_PRESS_MS + 2_000L,
        )
    }

    suspend fun swipe(
        service: AccessibilityService,
        fromX: Float,
        fromY: Float,
        toX: Float,
        toY: Float,
    ): Boolean {
        val (w, h) = service.screenPx()
        val path = Path().apply {
            moveTo(fromX * w, fromY * h)
            lineTo(toX * w, toY * h)
        }
        return service.dispatchAndWait(
            strokeOf(path, 0L, MaClicks.SWIPE_MS),
            MaClicks.SWIPE_MS + 2_000L,
        )
    }

    /**
     * Plays a whole slot.
     *
     * Returns the index of the step that failed, or null when everything went through. The index
     * rather than a bare false: a sequence of twenty clicks that stopped at the eleventh is a
     * different problem from one that never started, and only the number tells them apart.
     */
    suspend fun play(slot: MaClicks.Slot): Int? {
        val service = DictateAccessibilityService.gestureService() ?: return 0
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return 0
        if (_playing.value) return null
        _playing.value = true
        cancelled = false
        try {
            slot.steps.forEachIndexed { index, step ->
                if (cancelled) return index
                val ok = when (step.kind) {
                    MaClicks.Kind.TAP -> tap(service, step.x, step.y)
                    MaClicks.Kind.LONG_PRESS -> longPress(service, step.x, step.y)
                    MaClicks.Kind.SWIPE -> swipe(service, step.x, step.y, step.toX, step.toY)
                    MaClicks.Kind.WAIT -> true
                    MaClicks.Kind.BACK ->
                        service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK)
                }
                if (!ok) return index
                // The delay is his, set per step, and it is waited even after the last one. The app
                // usually has something left to finish, and the flag going false is what the UI uses
                // to say the sequence is done.
                if (step.delayAfterMs > 0) delay(step.delayAfterMs)
            }
            return null
        } finally {
            _playing.value = false
            cancelled = false
        }
    }
}
