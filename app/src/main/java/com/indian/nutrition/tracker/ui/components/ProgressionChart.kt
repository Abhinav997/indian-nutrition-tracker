package com.indian.nutrition.tracker.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.indian.nutrition.tracker.ui.screens.progress.ChartMetric
import com.indian.nutrition.tracker.util.NumberUtils
import com.indian.nutrition.tracker.ui.screens.progress.ChartSeries
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

// Palette (web parity).
private val GRID = Color(0xFFE2E8F0)
private val GRID_LABEL = Color(0xFF94A3B8)
private val AXIS_LABEL = Color(0xFF64748B)
private val TARGET_LINE = Color(0xFFF43F5E)
private val WEIGHT_COLOR = Color(0xFF0D9488)
private val CALORIES_COLOR = Color(0xFF0D9488)
private val PROTEIN_COLOR = Color(0xFF059669)
private val WATER_COLOR = Color(0xFF0284C7)
private val ZERO_BAR = Color(0xFFCBD5E1)
private val OVER_BAR = Color(0xFFF59E0B)
private val HIT_BAR = Color(0xFF10B981)
private val DOT_FILL = Color.White

// Canvas geometry constants (web's 600x220 viewBox proportions).
private const val VIEW_W = 600f
private const val VIEW_H = 220f
private const val PAD_LEFT = 45f
private const val PAD_RIGHT = 20f
private const val PAD_TOP = 25f
private const val PAD_BOTTOM = 35f

/**
 * Custom Canvas chart with touch points (the web used SVG hover only;
 * phones need tap + drag). Weight = line + gradient area + dots; calories/
 * protein/water = bars with target-hit/over colors. Geometry matches the
 * web's 600×220 viewBox, scaled to the available width.
 */
@Composable
fun ProgressionChart(
    series: ChartSeries,
    modifier: Modifier = Modifier,
) {
    var selectedIndex by remember(series) { mutableStateOf<Int?>(null) }
    val unit = series.unit
    val xAxisFormatter = remember { DateTimeFormatter.ofPattern("MMM d", Locale.US) }

    Column(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth(),
        ) {
            val point = selectedIndex?.let { series.points.getOrNull(it) }
            Text(
                text = point?.let {
                    "${it.date}: ${NumberUtils.formatValue(it.value, unit)}" +
                        if (it.target > 0.0) " (Target: ${NumberUtils.formatValue(it.target, unit)})" else ""
                } ?: "Touch or drag over data points for exact values",
                style = MaterialTheme.typography.bodySmall,
                color = if (point != null) MaterialTheme.colorScheme.onSurface else
                    MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                LegendDot(seriesColor(series.metric), "Logged")
                LegendDash(TARGET_LINE, "Target (${NumberUtils.formatValue(series.target, unit)})")
            }
        }

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(VIEW_W / VIEW_H)
                .padding(top = 4.dp)
                .pointerInput(series) {
                    detectTapGestures { offset ->
                        selectedIndex = nearestIndex(offset.x, size.width.toFloat(), series)
                    }
                }
                .pointerInput(series) {
                    detectDragGestures { change, _ ->
                        selectedIndex = nearestIndex(change.position.x, size.width.toFloat(), series)
                    }
                },
        ) {
            val w = size.width
            val h = size.height
            val padL = w * PAD_LEFT / VIEW_W
            val padR = w * PAD_RIGHT / VIEW_W
            val padT = h * PAD_TOP / VIEW_H
            val padB = h * PAD_BOTTOM / VIEW_H
            val cw = w - padL - padR
            val ch = h - padT - padB
            val n = series.points.size
            val minV = series.minValue.toFloat()
            val maxV = series.maxValue.toFloat()

            fun x(i: Int): Float =
                if (n <= 1) padL + cw / 2f else padL + (i.toFloat() / (n - 1)) * cw

            fun y(value: Double): Float {
                val ratio = ((value - minV) / (maxV - minV)).toFloat().coerceIn(0f, 1f)
                return padT + ch - ratio * ch
            }

            val labelPaint = android.graphics.Paint().apply {
                isAntiAlias = true
                textAlign = android.graphics.Paint.Align.RIGHT
                textSize = 9.sp.toPx()
                color = GRID_LABEL.toArgb()
            }
            val xLabelPaint = android.graphics.Paint(labelPaint).apply {
                textAlign = android.graphics.Paint.Align.CENTER
                color = AXIS_LABEL.toArgb()
            }

            // Horizontal grid lines + y labels (web parity: 0/25/50/75/100%).
            for (i in 0..4) {
                val pct = i / 4f
                val gy = padT + ch * pct
                val yVal = (minV + (maxV - minV) * (1f - pct)).roundToInt()
                drawLine(
                    color = GRID,
                    start = Offset(padL, gy),
                    end = Offset(w - padR, gy),
                    strokeWidth = 1f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f)),
                )
                drawContext.canvas.nativeCanvas.drawText(
                    yVal.toString(), padL - 8f, gy + 3.5f, labelPaint,
                )
            }

            // Target reference line (dashed rose, with label).
            if (series.target > 0.0) {
                val ty = y(series.target)
                if (ty in padT..(padT + ch)) {
                    drawLine(
                        color = TARGET_LINE,
                        start = Offset(padL, ty),
                        end = Offset(w - padR, ty),
                        strokeWidth = 1.5f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f)),
                    )
                    labelPaint.color = TARGET_LINE.toArgb()
                    drawContext.canvas.nativeCanvas.drawText(
                        "Target ${NumberUtils.formatValue(series.target, unit)}",
                        w - padR, ty - 4f, labelPaint,
                    )
                }
            }

            if (series.isLine && n > 0) {
                val linePath = Path().apply {
                    moveTo(x(0), y(series.points[0].value))
                    for (i in 1 until n) lineTo(x(i), y(series.points[i].value))
                }
                if (n > 1) {
                    val area = Path().apply {
                        moveTo(x(0), padT + ch)
                        lineTo(x(0), y(series.points[0].value))
                        for (i in 1 until n) lineTo(x(i), y(series.points[i].value))
                        lineTo(x(n - 1), padT + ch)
                        close()
                    }
                    drawPath(
                        area,
                        brush = Brush.verticalGradient(
                            colors = listOf(WEIGHT_COLOR.copy(alpha = 0.25f), WEIGHT_COLOR.copy(alpha = 0f)),
                            startY = padT,
                            endY = padT + ch,
                        ),
                    )
                }
                drawPath(linePath, WEIGHT_COLOR, style = Stroke(width = 2.5f))

                val dotR = max(3.dp.toPx(), w * 4.5f / VIEW_W)
                series.points.forEachIndexed { i, p ->
                    val cx = x(i)
                    val cy = y(p.value)
                    if (selectedIndex == i) {
                        drawCircle(WEIGHT_COLOR.copy(alpha = 0.25f), radius = dotR * 2f, center = Offset(cx, cy))
                    }
                    drawCircle(DOT_FILL, radius = dotR, center = Offset(cx, cy))
                    drawCircle(WEIGHT_COLOR, radius = dotR, center = Offset(cx, cy), style = Stroke(width = 2f))
                }
            } else if (n > 0) {
                val barWidth = max(
                    w * 6f / VIEW_W,
                    min(w * 22f / VIEW_W, cw / n - w * 4f / VIEW_W),
                )
                series.points.forEachIndexed { i, p ->
                    val cx = x(i)
                    val cy = y(p.value)
                    val top = min(cy, padT + ch)
                    val bottom = padT + ch
                    val fill = when {
                        p.value <= 0.0 -> ZERO_BAR
                        series.metric == ChartMetric.WATER -> WATER_COLOR
                        p.value > series.target && series.target > 0.0 -> OVER_BAR
                        abs(p.value - series.target) <= series.target * 0.08 -> HIT_BAR
                        series.metric == ChartMetric.CALORIES -> CALORIES_COLOR
                        else -> PROTEIN_COLOR
                    }
                    val barSize = Size(barWidth, (bottom - top).coerceAtLeast(0f))
                    drawRect(
                        color = fill,
                        topLeft = Offset(cx - barWidth / 2f, top),
                        size = barSize,
                    )
                    if (selectedIndex == i) {
                        drawRect(
                            color = TARGET_LINE,
                            topLeft = Offset(cx - barWidth / 2f, top),
                            size = barSize,
                            style = Stroke(width = 2f),
                        )
                    }
                }
            }

            // X-axis dates with thinning for long ranges (web parity).
            if (n > 0) {
                val step = if (n > 20) 5 else if (n > 10) 2 else 1
                series.points.forEachIndexed { i, p ->
                    if (i % step != 0 && i != n - 1) return@forEachIndexed
                    drawContext.canvas.nativeCanvas.drawText(
                        p.date.format(xAxisFormatter), x(i), h - 10f, xLabelPaint,
                    )
                }
            }
        }
    }
}

/**
 * Map a touch x (px) to the nearest point index, using the same geometry as
 * the Canvas: padLeft 45/600 of the width, chart width = w - padL - padR.
 */
private fun nearestIndex(px: Float, width: Float, series: ChartSeries): Int? {
    val n = series.points.size
    if (n == 0) return null
    val padL = width * PAD_LEFT / VIEW_W
    val padR = width * PAD_RIGHT / VIEW_W
    val cw = width - padL - padR
    val idx = if (n <= 1) 0 else {
        ((px - padL) / cw * (n - 1)).roundToInt().coerceIn(0, n - 1)
    }
    return idx
}

private fun seriesColor(metric: ChartMetric): Color = when (metric) {
    ChartMetric.WEIGHT -> WEIGHT_COLOR
    ChartMetric.CALORIES -> CALORIES_COLOR
    ChartMetric.PROTEIN -> PROTEIN_COLOR
    ChartMetric.WATER -> WATER_COLOR
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Canvas(modifier = Modifier.size(10.dp).padding(1.dp)) {
            drawCircle(color)
        }
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun LegendDash(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Canvas(modifier = Modifier.size(14.dp).padding(vertical = 4.dp)) {
            drawLine(
                color = color,
                start = Offset(0f, size.height / 2f),
                end = Offset(size.width, size.height / 2f),
                strokeWidth = 2f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f)),
            )
        }
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}
