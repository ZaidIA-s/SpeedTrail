package com.zaid.speedtrail.data.model

/** Hasil agregasi ringan untuk kartu di daftar history (tanpa memuat semua titik). */
data class TripPointCount(
    val tripId: Long,
    val count: Int,
)
