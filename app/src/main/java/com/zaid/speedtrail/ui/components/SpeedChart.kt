package com.zaid.speedtrail.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import com.zaid.speedtrail.data.model.TrackPoint
import com.zaid.speedtrail.ui.detail.ColorMode
import com.zaid.speedtrail.util.SpeedColorMapper

/**
 * Grafik kecepatan terhadap urutan titik (proksi jarak/waktu), digambar manual
 * di Canvas. Tiap segmen diwarnai sama dengan skema peta sehingga mudah dikorelasikan.
 */
@androidx.compose.runtime.Composable
fun SpeedChart(
    points: List<TrackPoint>,
    colorMode: ColorMode,
    avgSpeedMps: Double,
    modifier: Modifier = Modifier,
) {
    if (points.size < 2) return
    val maxSpeed = (points.maxOf { it.speedMps }).coerceAtLeast(1f)

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val dx = w / (points.size - 1)

        // garis baseline
        for (i in 0 until points.size - 1) {
            val p0 = points[i]
            val p1 = points[i + 1]
            val x0 = i * dx
            val x1 = (i + 1) * dx
            val y0 = h - (p0.speedMps / maxSpeed) * h
            val y1 = h - (p1.speedMps / maxSpeed) * h
            val color = when (colorMode) {
                ColorMode.ABSOLUTE -> SpeedColorMapper.absolute(p0.speedMps)
                ColorMode.RELATIVE -> SpeedColorMapper.relative(p0.speedMps, avgSpeedMps)
            }
            drawLine(
                color = color,
                start = Offset(x0, y0),
                end = Offset(x1, y1),
                strokeWidth = 4.dp.toPx(),
                cap = StrokeCap.Round,
            )
        }
    }
}
