package com.zaid.speedtrail.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

/** Helper konversi & format untuk ditampilkan di UI. */
object Formatters {

    fun mpsToKmh(mps: Double): Double = mps * 3.6
    fun mpsToKmh(mps: Float): Double = mps * 3.6

    fun speedKmh(mps: Float): String = "${(mps * 3.6).roundToInt()} km/j"

    fun distance(meters: Double): String =
        if (meters >= 1000) String.format(Locale.US, "%.2f km", meters / 1000.0)
        else "${meters.roundToInt()} m"

    fun duration(seconds: Long): String {
        val h = seconds / 3600
        val m = (seconds % 3600) / 60
        val s = seconds % 60
        return if (h > 0) String.format(Locale.US, "%d:%02d:%02d", h, m, s)
        else String.format(Locale.US, "%02d:%02d", m, s)
    }

    private val dateFmt = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("id", "ID"))
    fun dateTime(epochMillis: Long): String = dateFmt.format(Date(epochMillis))
}
