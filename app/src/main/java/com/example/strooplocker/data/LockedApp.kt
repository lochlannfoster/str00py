package com.example.strooplocker.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity class representing a locked app in the database.
 *
 * This class maps to a row in the 'locked_apps' table. Each instance
 * represents a single app that the user has chosen to lock behind
 * the Stroop challenge.
 *
 * The package name is used as the primary key since it uniquely
 * identifies each app on the device.
 *
 * @property packageName The package name of the locked app
 */
@Entity(tableName = "locked_apps")
data class LockedApp(
    @PrimaryKey
    val packageName: String
)