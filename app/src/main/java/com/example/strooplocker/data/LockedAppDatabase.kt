package com.example.strooplocker.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * The main Room database for storing locked packages.
 */
@Database(
    entities = [LockedApp::class],
    version = 1,
    exportSchema = false
)
abstract class LockedAppDatabase : RoomDatabase() {
    abstract fun lockedAppDao(): LockedAppDao

    companion object {
        @Volatile
        private var INSTANCE: LockedAppDatabase? = null

        fun getInstance(context: Context): LockedAppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    LockedAppDatabase::class.java,
                    "locked_apps_db"
                ).build()

                INSTANCE = instance
                instance
            }
        }
    }
}

