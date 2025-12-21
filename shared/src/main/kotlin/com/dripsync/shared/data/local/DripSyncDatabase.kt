package com.dripsync.shared.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.dripsync.shared.data.model.HydrationRecord

@Database(
    entities = [HydrationRecord::class],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class DripSyncDatabase : RoomDatabase() {
    abstract fun hydrationDao(): HydrationDao

    companion object {
        const val DATABASE_NAME = "dripsync.db"
    }
}
