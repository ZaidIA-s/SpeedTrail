package com.zaid.speedtrail.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.zaid.speedtrail.data.model.TrackPoint
import com.zaid.speedtrail.data.model.Trip
import kotlinx.coroutines.flow.Flow

@Dao
interface TripDao {

    // ---- Trip ----
    @Insert
    suspend fun insertTrip(trip: Trip): Long

    @Update
    suspend fun updateTrip(trip: Trip)

    @Query("SELECT * FROM trips ORDER BY startTime DESC")
    fun observeTrips(): Flow<List<Trip>>

    @Query("SELECT * FROM trips WHERE id = :tripId")
    suspend fun getTrip(tripId: Long): Trip?

    @Query("SELECT * FROM trips WHERE id = :tripId")
    fun observeTrip(tripId: Long): Flow<Trip?>

    @Query("DELETE FROM trips WHERE id = :tripId")
    suspend fun deleteTrip(tripId: Long)

    // ---- TrackPoint ----
    @Insert
    suspend fun insertPoint(point: TrackPoint): Long

    @Query("SELECT * FROM track_points WHERE tripId = :tripId ORDER BY timestamp ASC")
    suspend fun getPoints(tripId: Long): List<TrackPoint>

    @Query("SELECT * FROM track_points WHERE tripId = :tripId ORDER BY timestamp ASC")
    fun observePoints(tripId: Long): Flow<List<TrackPoint>>

    @Query("SELECT COUNT(*) FROM track_points WHERE tripId = :tripId")
    suspend fun countPoints(tripId: Long): Int
}
