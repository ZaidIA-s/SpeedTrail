package com.zaid.speedtrail.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Satu perjalanan (sesi berkendara) dari Start sampai Stop.
 * Nilai ringkasan dihitung & disimpan saat trip berakhir agar daftar history cepat dimuat.
 */
@Entity(tableName = "trips")
data class Trip(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val startTime: Long,                 // epoch millis
    val endTime: Long? = null,           // null = masih berjalan

    val distanceMeters: Double = 0.0,
    val durationSec: Long = 0,

    val avgSpeedMps: Double = 0.0,       // meter/detik
    val maxSpeedMps: Double = 0.0,

    val title: String? = null,           // mis. "Perjalanan pagi"
)
