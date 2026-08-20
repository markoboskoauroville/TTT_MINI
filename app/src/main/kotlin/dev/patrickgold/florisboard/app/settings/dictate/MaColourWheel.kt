/*
 * Copyright (C) 2026 Marko Bosko, Mantra Productions
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package dev.patrickgold.florisboard.app.settings.dictate

import dev.patrickgold.florisboard.dictate.ui.MaSwatches
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.layout
import androidx.compose.foundation.Canvas
import androidx.core.graphics.ColorUtils
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * A colour wheel, full screen, with a loupe you drag.
 *
 * ### Why a wheel and not a row of swatches
 *
 * He is a DJ, an editor and a musician, and he asked for the thing his other tools give him: the
 * wheel from DaVinci and Photoshop, where colour is a **place** rather than a list. A list of six
 * chips answers "which of these" — a wheel answers "which colour", and those are different
 * questions. He knows the second one by feel and cannot express it in the first.
 *
 * ### How it is laid out
 *
 * Hue goes round the rim; saturation goes from white at the centre to full colour at the edge; and
 * brightness is a bar underneath, because it is the one axis a disc cannot carry. That is the same
 * arrangement every professional tool uses, and using a different one would cost him the muscle
 * memory he already has.
 *
 * ### The loupe
 *
 * A ring under the finger showing the colour it is over, lifted **above** the touch point so the
 * finger is not covering the thing being chosen. That offset is the whole reason a loupe exists;
 * without it a picker is a guess made with a thumb in the way.
 *
 * ### Live, and cancellable
 *
 * The colour applies as it is dragged, so it is judged against the thing it is for rather than
 * against a swatch. Cancel puts back what it was — which is what makes dragging freely safe.
 */
@Composable
fun MaColourWheel(
    initial: Color,
    onDismiss: () -> Unit,
    onPick: (Color) -> Unit,
) {
    // Held as HSV rather than as a Color, because the wheel IS these three numbers. Converting back
    // and forth would lose the hue of a fully desaturated colour — drag to the centre and back out
    // and the hue would have been forgotten on the way.
    val start = remember(initial) { FloatArray(3).also { ColorUtils.colorToHSL(initial.toArgb(), it) } }
    var hue by remember { mutableFloatStateOf(start[0]) }
    var sat by remember { mutableFloatStateOf(start[1]) }
    var light by remember { mutableFloatStateOf(start[2]) }
    var loupe by remember { mutableStateOf<Offset?>(null) }
    // Swatches first: nine times in ten the colour wanted is one already used.
    var swatches by remember { mutableStateOf(true) }

    val picked = Color(ColorUtils.HSLToColor(floatArrayOf(hue, sat, light)))

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0B0D10))
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            // What is being chosen, big enough to judge. A swatch the size of a fingernail cannot
            // be compared with anything.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(picked),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = picked.hex(),
                    color = if (light > 0.55f) Color.Black else Color.White,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                )
            }

            Spacer(Modifier.height(16.dp))

            // Two views, one picker. Swatches first because it answers the commoner question.
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                for ((value, label) in listOf(true to "Swatches", false to "Wheel")) {
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 6.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (swatches == value) Color(0xFFE8B15C) else Color(0xFF1E1E20))
                            .clickable { swatches = value }
                            .padding(horizontal = 20.dp, vertical = 10.dp),
                    ) {
                        Text(
                            text = label,
                            color = if (swatches == value) Color.Black else Color(0xFFF2DDB4),
                            fontWeight = if (swatches == value) FontWeight.Bold else FontWeight.Normal,
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            if (swatches) {
                // Seven across: wide enough for the greyscale run to read as one row, narrow enough
                // that a swatch stays bigger than a fingertip.
                LazyVerticalGrid(
                    columns = GridCells.Fixed(7),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    items(MaSwatches.ALL) { sw ->
                        val c = swatchOf(sw)
                        Box(
                            modifier = Modifier
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(c)
                                .clickable {
                                    val hsl = FloatArray(3)
                                    ColorUtils.colorToHSL(c.toArgb(), hsl)
                                    hue = hsl[0]; sat = hsl[1]; light = hsl[2]
                                },
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
            } else Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .pointerInput(Unit) {
                        fun apply(pos: Offset) {
                            val r = size.width / 2f
                            val dx = pos.x - r
                            val dy = pos.y - r
                            val dist = hypot(dx, dy)
                            // Outside the disc still counts, clamped to the rim. A finger that
                            // slides past the edge should hold the colour it had, not drop it.
                            val clamped = dist.coerceAtMost(r)
                            hue = ((atan2(dy, dx) * 180f / Math.PI.toFloat()) + 360f) % 360f
                            sat = (clamped / r).coerceIn(0f, 1f)
                            loupe = Offset(r + dx / (dist.coerceAtLeast(0.001f)) * clamped,
                                           r + dy / (dist.coerceAtLeast(0.001f)) * clamped)
                        }
                        detectDragGestures(
                            onDragStart = { apply(it) },
                            onDragEnd = { loupe = null },
                            onDragCancel = { loupe = null },
                        ) { change, _ -> apply(change.position) }
                    }
                    .pointerInput(Unit) {
                        detectTapGestures { pos ->
                            val r = size.width / 2f
                            val dx = pos.x - r
                            val dy = pos.y - r
                            val dist = hypot(dx, dy).coerceAtMost(r)
                            hue = ((atan2(dy, dx) * 180f / Math.PI.toFloat()) + 360f) % 360f
                            sat = (dist / r).coerceIn(0f, 1f)
                        }
                    },
            ) {
                Canvas(Modifier.fillMaxSize()) {
                    val r = size.minDimension / 2f
                    val centre = Offset(size.width / 2f, size.height / 2f)
                    // Hue as a sweep, saturation as white bleeding out of the middle. Two gradients
                    // rather than a bitmap: it scales to any screen and costs nothing to draw.
                    drawCircle(
                        brush = Brush.sweepGradient(
                            colors = (0..360 step 30).map {
                                Color(ColorUtils.HSLToColor(floatArrayOf(it.toFloat() % 360f, 1f, 0.5f)))
                            },
                            center = centre,
                        ),
                        radius = r,
                        center = centre,
                    )
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color.White, Color.Transparent),
                            center = centre,
                            radius = r,
                        ),
                        radius = r,
                        center = centre,
                    )
                    // The current position, always drawn — the loupe only appears under a finger,
                    // but where the colour came from should be visible when nothing is touching it.
                    val a = hue * Math.PI.toFloat() / 180f
                    val p = Offset(centre.x + cos(a) * sat * r, centre.y + sin(a) * sat * r)
                    drawCircle(Color.White, radius = 14f, center = p, style = Stroke(width = 4f))
                    drawCircle(Color.Black, radius = 18f, center = p, style = Stroke(width = 2f))
                }

                // The loupe: lifted above the finger, showing the colour full size.
                loupe?.let { pos ->
                    Box(
                        modifier = Modifier
                            .offsetPx(pos.x - 44f, pos.y - 150f)
                            .size(88.dp)
                            .clip(RoundedCornerShape(44.dp))
                            .background(picked),
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            // Brightness, the axis a disc cannot carry.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                Color.Black,
                                Color(ColorUtils.HSLToColor(floatArrayOf(hue, sat, 0.5f))),
                                Color.White,
                            ),
                        ),
                    )
                    .pointerInput(Unit) {
                        detectDragGestures { change, _ ->
                            light = (change.position.x / size.width).coerceIn(0f, 1f)
                        }
                    }
                    .pointerInput(Unit) {
                        detectTapGestures { light = (it.x / size.width).coerceIn(0f, 1f) }
                    },
            ) {
                Box(
                    modifier = Modifier
                        .offsetFraction(light)
                        .size(width = 4.dp, height = 48.dp)
                        .background(Color.White),
                )
            }

            Spacer(Modifier.height(28.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                TextButton(onClick = onDismiss) { Text("Cancel") }
                TextButton(onClick = { onPick(picked) }) { Text("Use this colour") }
            }
        }
    }
}

/** Places a child at an absolute pixel offset inside its parent. */
private fun Modifier.offsetPx(x: Float, y: Float) = this.then(
    Modifier.layout { measurable, constraints ->
        val placeable = measurable.measure(constraints)
        layout(placeable.width, placeable.height) {
            placeable.place(x.roundToInt(), y.roundToInt())
        }
    },
)

/** Places the brightness marker along the bar. */
private fun Modifier.offsetFraction(fraction: Float) = this.then(
    Modifier.layout { measurable, constraints ->
        val placeable = measurable.measure(constraints)
        val x = ((constraints.maxWidth - placeable.width) * fraction).roundToInt()
        layout(constraints.maxWidth, placeable.height) { placeable.place(x, 0) }
    },
)

/** `#RRGGBB`, because a colour he can read is a colour he can write down. */
private fun Color.hex(): String {
    val argb = toArgb()
    return "#%06X".format(argb and 0xFFFFFF)
}

/** A swatch string to a colour. */
private fun swatchOf(hex: String): Color {
    val h = hex.removePrefix("#")
    val v = h.toLongOrNull(16) ?: return Color.Gray
    return Color(0xFF000000L or v)
}
