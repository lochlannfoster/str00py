package com.example.strooplocker.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Room database for storing locked packages.
 *
 * This is the main database class for the str00py app. It uses the Room
 * persistence library to manage SQLite database operations. The database
 * contains a single table for storing locked app package names.
 *
 * The class implements the singleton pattern to ensure only one instance
 * of the database is created.
 */
@Database(
    entities = [LockedApp::class],
    version = 1,
    exportSchema = false
)
abstract class LockedAppDatabase : RoomDatabase() {

    /**
     * Provides access to the LockedAppDao.
     *
     * @return A DAO instance for accessing the locked_apps table
     */
    abstract fun lockedAppDao(): LockedAppDao

    companion object {
        @Volatile
        private var INSTANCE: LockedAppDatabase? = null

        /**
         * Gets the singleton instance of the database.
         * Creates it if it doesn't exist.
         *
         * @param context The context used to create the database
         * @return The singleton database instance
         */
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