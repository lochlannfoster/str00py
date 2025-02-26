package com.example.strooplocker

import android.content.Context
import android.util.Log
import com.example.strooplocker.data.LockedAppDatabase
import com.example.strooplocker.data.LockedAppsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object LockManager {
    private const val TAG = "LockManager_DEBUG"

    /**
     * Adds an app to the locked apps list
     */
    suspend fun addLockedApp(context: Context, packageName: String) {
        Log.d(TAG, "Attempting to add locked app: $packageName")
        try {
            val repository = getRepository(context)
            repository.addLockedApp(packageName)

            // Verify addition
            val lockedApps = repository.getAllLockedApps()
            Log.d(TAG, "Locked apps after adding $packageName: $lockedApps")
        } catch (e: Exception) {
            Log.e(TAG, "Error adding locked app: $packageName", e)
        }
    }

    /**
     * Removes an app from the locked apps list
     */
    suspend fun removeLockedApp(context: Context, packageName: String) {
        Log.d(TAG, "Attempting to remove locked app: $packageName")
        try {
            val repository = getRepository(context)
            repository.removeLockedApp(packageName)

            // Verify removal
            val lockedApps = repository.getAllLockedApps()
            Log.d(TAG, "Locked apps after removing $packageName: $lockedApps")
        } catch (e: Exception) {
            Log.e(TAG, "Error removing locked app: $packageName", e)
        }
    }

    /**
     * Gets all currently locked apps
     */
    suspend fun getLockedApps(context: Context): List<String> {
        val repository = getRepository(context)
        val lockedApps = repository.getAllLockedApps()
        Log.d(TAG, "Retrieved locked apps: $lockedApps")
        return lockedApps
    }

    /**
     * Checks if a specific app is locked
     */
    suspend fun isAppLocked(context: Context, packageName: String): Boolean {
        return withContext(Dispatchers.IO) {
            val lockedApps = getLockedApps(context)
            val isLocked = lockedApps.contains(packageName)
            Log.d(TAG, "Is $packageName locked? $isLocked")
            isLocked
        }
    }

    /**
     * Helper to get repository instance
     */
    private fun getRepository(context: Context): LockedAppsRepository {
        val db = LockedAppDatabase.getInstance(context)
        val dao = db.lockedAppDao()
        return LockedAppsRepository(dao)
    }
}