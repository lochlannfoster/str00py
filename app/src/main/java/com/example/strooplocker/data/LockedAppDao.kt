package com.example.strooplocker.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface LockedAppDao {

    @Query("SELECT * FROM locked_apps")
    fun getAllLockedApps(): List<LockedApp>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertLockedApp(lockedApp: LockedApp)

    @Delete
    fun deleteLockedApp(lockedApp: LockedApp)
}
