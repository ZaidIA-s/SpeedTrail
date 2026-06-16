package com.zaid.speedtrail.util

import androidx.compose.ui.graphics.Color

/**
 * Memetakan kecepatan ke warna untuk pewarnaan jalur.
 *
 * Dua mode:
 *  - ABSOLUTE: ambang tetap (km/jam), cocok untuk perbandingan antar trip.
 *  - RELATIVE: relatif terhadap kecepatan rata-rata trip, cocok untuk menyorot
 *    perlambatan secara kontekstual (mis. jalan tol pelan tetap "merah").
 */
object SpeedColorMapper {

    // Gradien: merah (lambat) -> kuning -> hijau (cepat)
    private val stops = listOf(
        0.0 to Color(0xFFB71C1C),   // merah tua  - berhenti/macet
        0.2 to Color(0xFFF44336),   // merah
        0.4 to Color(0xFFFF9800),   // oranye
        0.6 to Color(0xFFFFEB3B),   // kuning
        0.8 to Color(0xFF8BC34A),   // hijau muda
        1.0 to Color(0xFF2E7D32),   // hijau tua  - cepat
    )

    /** km/jam -> m/detik */
    private const val KMH_TO_MPS = 1000.0 / 3600.0

    /**
     * Warna berdasarkan ambang absolut.
     * @param speedMps kecepatan titik (m/detik)
     * @param maxKmh kecepatan yang dianggap "penuh hijau" (default 60 km/jam)
     */
    fun absolute(speedMps: Float, maxKmh: Double = 60.0): Color {
        val t = (speedMps / (maxKmh * KMH_TO_MPS)).coerceIn(0.0, 1.0)
        return lerpStops(t)
    }

    /**
     * Warna relatif terhadap rata-rata trip.
     * speed == avg -> tengah gradien; speed >= 2*avg -> hijau penuh.
     */
    fun relative(speedMps: Float, avgSpeedMps: Double): Color {
        if (avgSpeedMps <= 0.0) return lerpStops(0.5)
        val t = (speedMps / (2.0 * avgSpeedMps)).coerceIn(0.0, 1.0)
        return lerpStops(t)
    }

    /** Interpolasi linear di antara stop warna. t dalam [0,1]. */
    private fun lerpStops(t: Double): Color {
        for (i in 0 until stops.size - 1) {
            val (p0, c0) = stops[i]
            val (p1, c1) = stops[i + 1]
            if (t in p0..p1) {
                val local = if (p1 == p0) 0.0 else (t - p0) / (p1 - p0)
                return lerp(c0, c1, local.toFloat())
            }
        }
        return stops.last().second
    }

    private fun lerp(a: Color, b: Color, f: Float): Color = Color(
        red = a.red + (b.red - a.red) * f,
        green = a.green + (b.green - a.green) * f,
        blue = a.blue + (b.blue - a.blue) * f,
        alpha = 1f,
    )

    /** Versi integer ARGB untuk osmdroid (Paint). */
    fun absoluteArgb(speedMps: Float, maxKmh: Double = 60.0): Int =
        absolute(speedMps, maxKmh).toArgb()

    fun relativeArgb(speedMps: Float, avgSpeedMps: Double): Int =
        relative(speedMps, avgSpeedMps).toArgb()

    private fun Color.toArgb(): Int {
        val a = (alpha * 255).toInt() and 0xFF
        val r = (red * 255).toInt() and 0xFF
        val g = (green * 255).toInt() and 0xFF
        val b = (blue * 255).toInt() and 0xFF
        return (a shl 24) or (r shl 16) or (g shl 8) or b
    }
}
