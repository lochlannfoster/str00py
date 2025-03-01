package com.example.strooplocker.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

/**
 * Data Access Object (DAO) for the locked_apps table.
 *
 * This interface defines the database operations that can be performed
 * on the locked_apps table. Room will automatically implement this
 * interface at compile time.
 */
@Dao
interface LockedAppDao {

    /**
     * Retrieves all locked apps from the database.
     *
     * @return A list of all LockedApp entities
     */
    @Query("SELECT * FROM locked_apps")
    fun getAllLockedApps(): List<LockedApp>

    /**
     * Inserts a new locked app into the database.
     * If the app already exists, the insertion is ignored.
     *
     * @param lockedApp The LockedApp entity to insert
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertLockedApp(lockedApp: LockedApp)

    /**
     * Removes a locked app from the database.
     *
     * @param lockedApp The LockedApp entity to delete
     */
    @Delete
    fun deleteLockedApp(lockedApp: LockedApp)
}