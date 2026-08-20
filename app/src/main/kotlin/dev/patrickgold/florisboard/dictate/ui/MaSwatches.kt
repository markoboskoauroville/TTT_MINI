/*
 * Copyright (C) 2026 Marko Bosko, Mantra Productions
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package dev.patrickgold.florisboard.dictate.ui

/**
 * The swatches, shared by the colour picker and the reader dashboard.
 *
 * ### Why a grid exists beside a wheel
 *
 * Adobe puts swatches in every one of its programs and keeps the picker behind them, and the reason
 * is that the two answer different questions. **A wheel answers "which colour"; a grid answers
 * "which of the ones I keep using".** Nine times out of ten the second question is the one being
 * asked, and answering it on a wheel means hunting for a colour he has already chosen before.
 *
 * ### The order, which is the whole design
 *
 * **Black and white first**, because they are the most used and the hardest to hit on a wheel — pure
 * white is a single point at the centre of the brightness axis and pure black is another, so on a
 * wheel they are the two colours a finger is least likely to land on exactly. A grid makes them one
 * tap.
 *
 * Then the greys, then the hues in spectrum order, each in a light and a strong version. Spectrum
 * order rather than by taste, because that is the order every professional palette uses and the eye
 * finds a hue by position without reading.
 */
object MaSwatches {

    /**
     * The full grid: greyscale first, then hues.
     *
     * Deliberately not exhaustive. Thirty swatches is a palette; a hundred is a wheel with worse
     * resolution, and anyone wanting the hundred should be on the wheel instead.
     */
    val ALL: List<String> = listOf(
        // Greyscale, dark to light. These are the ones a wheel is worst at.
        "#000000", "#1A1A1A", "#3A3A3C", "#6E6E73", "#AEAEB2", "#E5E5EA", "#FFFFFF",
        // The app's own, so the thing he is theming can be set to match itself.
        "#E8A64B", "#E8B15C", "#F2DDB4", "#0B0D10",
        // Reds and warm
        "#FF3B30", "#EF4444", "#FF6B6B", "#FF9500", "#FBBF24",
        // Greens
        "#34C759", "#6FA85A", "#A7F3D0",
        // Blues and cool
        "#0A84FF", "#5B8DEF", "#6FE0EE", "#A5B4FC",
        // Purples and pinks
        "#AF52DE", "#C084FC", "#FF2D95", "#F9A8D4",
    )

    /**
     * The four on the dashboard, where there is room for four and no more.
     *
     * White first: it is what he reads best on black, and the reason the reader's page is white at
     * all. Then the app's amber, then a cool and a warm alternative for when a passage needs to look
     * different from the last one.
     */
    /**
     * Seven greys, dark to white, for the reader highlight.
     *
     * Greys only: a highlight over a white page needs to be BRIGHTER than the page, not a different
     * colour. Seven because a row of seven is read at a glance and chosen without aiming, where a
     * slider needs a precise finger and hands back a number nobody asked for.
     *
     * **The dark end is not black.** `#000000` on a near-black background is a square that appears
     * to do nothing, and a highlight set to it disappears into the page — a setting that defeats the
     * control offering it. `#6E6E73` is the darkest that still reads as a mark, and the row climbs
     * from there in even steps to white.
     */
    val GREYS: List<String> = listOf(
        "#6E6E73", "#8A8A8E", "#A5A5AA", "#C0C0C4", "#D6D6DA", "#EDEDF0", "#FFFFFF",
    )

    val QUICK: List<String> = listOf("#FFFFFF", "#E8B15C", "#6FE0EE", "#FF6B6B")
}
