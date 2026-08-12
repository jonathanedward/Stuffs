package com.stampbook.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [TripEntity::class, StampEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class StampbookDatabase : RoomDatabase() {

    abstract fun tripDao(): TripDao
    abstract fun stampDao(): StampDao

    companion object {
        @Volatile
        private var instance: StampbookDatabase? = null

        fun get(context: Context): StampbookDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                StampbookDatabase::class.java,
                "stampbook.db",
            ).build().also { instance = it }
        }
    }
}
