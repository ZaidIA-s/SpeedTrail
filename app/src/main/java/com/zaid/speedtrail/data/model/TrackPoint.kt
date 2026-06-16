package com.zaid.speedtrail.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Satu sampel GPS dalam sebuah trip. Inti dari pewarnaan jalur & grafik kecepatan.
 * Dihapus otomatis (CASCADE) jika trip induknya dihapus.
 */
@Entity(
    tableName = "track_points",
    foreignKeys = [
        ForeignKey(
            entity = Trip::class,
            parentColumns = ["id"],
            childColumns = ["tripId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("tripId")]
)
data class TrackPoint(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val tripId: Long,

    val latitude: Double,
    val longitude: Double,

    val speedMps: Float,        // kecepatan dari GPS (m/detik)
    val timestamp: Long,        // epoch millis
    val accuracyM: Float,       // akurasi horizontal (meter)
    val altitudeM: Double = 0.0,
    val bearingDeg: Float = 0f,
)
