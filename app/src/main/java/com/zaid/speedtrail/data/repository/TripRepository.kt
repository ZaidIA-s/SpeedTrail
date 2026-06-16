package com.zaid.speedtrail.data.repository

import android.content.Context
import com.zaid.speedtrail.data.local.AppDatabase
import com.zaid.speedtrail.data.local.TripDao
import com.zaid.speedtrail.data.model.TrackPoint
import com.zaid.speedtrail.data.model.Trip
import kotlinx.coroutines.flow.Flow

/**
 * Pintu tunggal akses data. Service maupun UI memakai repository yang sama
 * sehingga titik yang direkam service langsung muncul di UI lewat Flow.
 */
class TripRepository private constructor(private val dao: TripDao) {

    fun observeTrips(): Flow<List<Trip>> = dao.observeTrips()

    fun observeTrip(tripId: Long): Flow<Trip?> = dao.observeTrip(tripId)

    fun observePoints(tripId: Long): Flow<List<TrackPoint>> = dao.observePoints(tripId)

    suspend fun getTrip(tripId: Long): Trip? = dao.getTrip(tripId)

    suspend fun getPoints(tripId: Long): List<TrackPoint> = dao.getPoints(tripId)

    suspend fun startTrip(startTime: Long): Long =
        dao.insertTrip(Trip(startTime = startTime))

    suspend fun addPoint(point: TrackPoint) = dao.insertPoint(point)

    suspend fun finishTrip(trip: Trip) = dao.updateTrip(trip)

    suspend fun deleteTrip(tripId: Long) = dao.deleteTrip(tripId)

    companion object {
        @Volatile
        private var INSTANCE: TripRepository? = null

        fun get(context: Context): TripRepository =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: TripRepository(AppDatabase.get(context).tripDao())
                    .also { INSTANCE = it }
            }
    }
}
