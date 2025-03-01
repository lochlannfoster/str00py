package com.example.strooplocker

import android.content.Context
import android.util.Log
import com.example.strooplocker.data.LockedAppDatabase
import com.example.strooplocker.data.LockedAppsRepository
import com.example.strooplocker.utils.LoggingUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * LockManager handles the persistence of locked app information.
 *
 * This singleton is responsible for managing which apps are locked behind the
 * Stroop challenge. It interfaces with the Room database through the LockedAppsRepository
 * to store and retrieve this information.
 *
 * Key features:
 * - Add apps to the locked list
 * - Remove apps from the locked list
 * - Check if an app is locked
 * - Retrieve all locked apps
 */
object LockManager {
    private const val TAG = "LockManager_DEBUG"

    /**
     * Adds an app to the locked apps list.
     *
     * This persists the app in the database so it will be locked
     * across app restarts and device reboots.
     *
     * @param context Android context used to access the database
     * @param packageName The package name of the app to lock
     */
    suspend fun addLockedApp(context: Context, packageName: String) {
        withContext(Dispatchers.IO) {
            try {
                LoggingUtil.debug(TAG, "addLockedApp", "Attempting to add locked app: $packageName")

                val repository = getRepository(context)
                repository.addLockedApp(packageName)

                val lockedApps = repository.getAllLockedApps()
                LoggingUtil.debug(
                    TAG,
                    "addLockedApp",
                    "Locked apps after adding $packageName: $lockedApps"
                )
            } catch (e: Exception) {
                LoggingUtil.error(TAG, "addLockedApp", "Error adding locked app: $packageName", e)
            }
        }
    }

    /**
     * Removes an app from the locked apps list.
     *
     * This deletes the app from the database so it will no longer
     * be locked behind a challenge.
     *
     * @param context Android context used to access the database
     * @param packageName The package name of the app to unlock
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
     * Gets all currently locked apps.
     *
     * @param context Android context used to access the database
     * @return List of package names for all locked apps
     */
    suspend fun getLockedApps(context: Context): List<String> {
        val repository = getRepository(context)
        val lockedApps = repository.getAllLockedApps()
        Log.d(TAG, "Retrieved locked apps: $lockedApps")
        return lockedApps
    }

    /**
     * Checks if a specific app is locked.
     *
     * @param context Android context used to access the database
     * @param packageName The package name to check
     * @return true if the app is locked, false otherwise
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
     * Helper method to get a repository instance.
     *
     * @param context Android context used to access the database
     * @return A repository instance for accessing locked app data
     */
    private fun getRepository(context: Context): LockedAppsRepository {
        val db = LockedAppDatabase.getInstance(context)
        val dao = db.lockedAppDao()
        return LockedAppsRepository(dao)
    }
}