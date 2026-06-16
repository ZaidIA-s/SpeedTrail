package com.zaid.speedtrail.service

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Status perekaman live yang dibagikan dari Service ke UI. */
enum class TrackingStatus { IDLE, RECORDING, PAUSED }

data class LiveStats(
    val status: TrackingStatus = TrackingStatus.IDLE,
    val tripId: Long? = null,
    val currentSpeedMps: Float = 0f,
    val maxSpeedMps: Float = 0f,
    val avgSpeedMps: Double = 0.0,
    val distanceMeters: Double = 0.0,
    val durationSec: Long = 0,
    val lastLat: Double? = null,
    val lastLon: Double? = null,
    val accuracyM: Float = 0f,
    val pointCount: Int = 0,
)

/**
 * Singleton penghubung Service <-> UI. Service yang menulis, UI hanya membaca.
 * Sengaja sederhana (tanpa DI) agar fondasi ringkas; bisa diganti Hilt nanti.
 */
object TrackingState {
    private val _stats = MutableStateFlow(LiveStats())
    val stats: StateFlow<LiveStats> = _stats.asStateFlow()

    internal fun update(transform: (LiveStats) -> LiveStats) {
        _stats.value = transform(_stats.value)
    }

    internal fun reset() {
        _stats.value = LiveStats()
    }
}
