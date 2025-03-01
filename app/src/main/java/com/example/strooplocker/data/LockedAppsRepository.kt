package com.example.strooplocker.data

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.example.strooplocker.utils.LoggingUtil

/**
 * Repository for managing locked app data in the database.
 *
 * This class provides a clean API over the DAO for interacting with the locked app database.
 * It abstracts away the implementation details of the database and provides coroutine-based
 * methods for database operations.
 *
 * @property dao The DAO for accessing the locked apps database
 */
class LockedAppsRepository(private val dao: LockedAppDao) {
    companion object {
        private const val TAG = "LockedAppsRepository_DEBUG"
    }

    /**
     * Retrieves all locked apps from the database.
     *
     * @return A list of package names for all locked apps
     */
    suspend fun getAllLockedApps(): List<String> = withContext(Dispatchers.IO) {
        val lockedApps = dao.getAllLockedApps().map { it.packageName }
        LoggingUtil.debug(TAG, "getAllLockedApps", "Retrieved ${lockedApps.size} locked apps")
        lockedApps
    }

    /**
     * Adds a new app to the locked apps list.
     *
     * @param packageName The package name of the app to lock
     */
    suspend fun addLockedApp(packageName: String) {
        withContext(Dispatchers.IO) {
            try {
                LoggingUtil.debug(TAG, "addLockedApp", "Adding locked app: $packageName")
                dao.insertLockedApp(LockedApp(packageName))
                LoggingUtil.debug(
                    TAG,
                    "addLockedApp",
                    "Successfully added locked app: $packageName"
                )
            } catch (e: Exception) {
                LoggingUtil.error(TAG, "addLockedApp", "Error adding locked app: $packageName", e)
            }
        }
    }

    /**
     * Removes a package from the locked apps list.
     *
     * @param packageName The package name of the app to unlock
     */
    suspend fun removeLockedApp(packageName: String) {
        val lockedApp = LockedApp(packageName)
        withContext(Dispatchers.IO) {
            Log.d(TAG, "Removing locked app: $packageName")
            dao.deleteLockedApp(lockedApp)
            Log.d(TAG, "Successfully removed locked app: $packageName")
        }
    }
}