package com.zaid.speedtrail.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.zaid.speedtrail.data.model.TrackPoint
import com.zaid.speedtrail.data.model.Trip

@Database(
    entities = [Trip::class, TrackPoint::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun tripDao(): TripDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun get(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "speedtrail.db"
                ).build().also { INSTANCE = it }
            }
    }
}
