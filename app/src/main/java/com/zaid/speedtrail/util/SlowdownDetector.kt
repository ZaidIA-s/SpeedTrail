package com.zaid.speedtrail.util

import com.zaid.speedtrail.data.model.TrackPoint

/** Satu zona perlambatan yang terdeteksi pada sebuah trip. */
data class SlowdownZone(
    val startIndex: Int,
    val endIndex: Int,
    val centerLat: Double,
    val centerLon: Double,
    val minSpeedMps: Float,
    val avgSpeedMps: Double,
    val startTime: Long,
    val endTime: Long,
) {
    val durationSec: Long get() = (endTime - startTime) / 1000
}

/**
 * Mendeteksi zona perlambatan: rentang titik berturut-turut yang kecepatannya
 * di bawah ambang relatif terhadap rata-rata trip.
 */
object SlowdownDetector {

    /**
     * @param points titik trip terurut waktu.
     * @param thresholdRatio titik dianggap "lambat" jika speed < ratio * avg (default 0.4).
     * @param minPoints jumlah titik minimal agar sebuah rentang dihitung sebagai zona.
     */
    fun detect(
        points: List<TrackPoint>,
        thresholdRatio: Double = 0.4,
        minPoints: Int = 3,
    ): List<SlowdownZone> {
        if (points.size < minPoints) return emptyList()

        val avg = points.map { it.speedMps.toDouble() }.average()
        if (avg <= 0.0) return emptyList()
        val threshold = avg * thresholdRatio

        val zones = mutableListOf<SlowdownZone>()
        var runStart = -1

        fun closeRun(endExclusive: Int) {
            if (runStart < 0) return
            val endIdx = endExclusive - 1
            if (endIdx - runStart + 1 >= minPoints) {
                val slice = points.subList(runStart, endExclusive)
                zones += SlowdownZone(
                    startIndex = runStart,
                    endIndex = endIdx,
                    centerLat = slice.map { it.latitude }.average(),
                    centerLon = slice.map { it.longitude }.average(),
                    minSpeedMps = slice.minOf { it.speedMps },
                    avgSpeedMps = slice.map { it.speedMps.toDouble() }.average(),
                    startTime = slice.first().timestamp,
                    endTime = slice.last().timestamp,
                )
            }
            runStart = -1
        }

        points.forEachIndexed { i, p ->
            if (p.speedMps < threshold) {
                if (runStart < 0) runStart = i
            } else {
                closeRun(i)
            }
        }
        closeRun(points.size)

        return zones
    }
}
