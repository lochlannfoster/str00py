// app/src/androidTest/java/com/example/strooplocker/LockManagerTest.kt

package com.example.strooplocker

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.strooplocker.data.LockedAppDao
import com.example.strooplocker.data.LockedAppDatabase
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

/**
 * Integration tests for [LockManager]
 *
 * These tests verify that the LockManager correctly interfaces with
 * the Room database for managing locked apps.
 */
@RunWith(AndroidJUnit4::class)
class LockManagerTest {

    private lateinit var db: LockedAppDatabase
    private lateinit var dao: LockedAppDao
    private lateinit var context: Context

    @Before
    fun createDb() {
        context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, LockedAppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.lockedAppDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    @Test
    fun addLockedApp_addsToDatabase() = runBlocking {
        // Arrange
        val packageName = "com.example.testapp"

        // Act
        LockManager.addLockedApp(context, packageName)

        // Assert
        val lockedApps = LockManager.getLockedApps(context)
        assert(lockedApps.contains(packageName))
    }

    @Test
    fun removeLockedApp_removesFromDatabase() = runBlocking {
        // Arrange
        val packageName = "com.example.testapp"
        LockManager.addLockedApp(context, packageName)

        // Act
        LockManager.removeLockedApp(context, packageName)

        // Assert
        val lockedApps = LockManager.getLockedApps(context)
        assert(!lockedApps.contains(packageName))
    }

    @Test
    fun isAppLocked_returnsCorrectStatus() = runBlocking {
        // Arrange
        val packageName = "com.example.testapp"
        val unlockedPackage = "com.example.anotherone"
        LockManager.addLockedApp(context, packageName)

        // Act & Assert
        assert(LockManager.isAppLocked(context, packageName))
        assert(!LockManager.isAppLocked(context, unlockedPackage))
    }
}