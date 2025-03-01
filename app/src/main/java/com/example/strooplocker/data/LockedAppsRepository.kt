package com.example.strooplocker.data

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LockedAppsRepository(private val dao: LockedAppDao) {
    companion object {
        private const val TAG = "LockedAppsRepository_DEBUG"
    }

    /**
     * Retrieve all locked app package names
     */
    suspend fun getAllLockedApps(): List<String> = withContext(Dispatchers.IO) {
        val lockedApps = dao.getAllLockedApps().map { it.packageName }
        Log.d(TAG, "Retrieved all locked apps: $lockedApps")
        lockedApps
    }

    /**
     * Add a package name to locked apps
     */
    suspend fun addLockedApp(packageName: String) {
        val lockedApp = LockedApp(packageName)
        withContext(Dispatchers.IO) {
            Log.d(TAG, "Adding locked app: $packageName")
            dao.insertLockedApp(lockedApp)
            Log.d(TAG, "Successfully added locked app: $packageName")
        }
    }

    /**
     * Remove a package name from locked apps
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