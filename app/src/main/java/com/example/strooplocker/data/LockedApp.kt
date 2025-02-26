package com.example.strooplocker.data

import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity(tableName = "locked_apps")
data class LockedApp(
    @PrimaryKey
    val packageName: String
)

