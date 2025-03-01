package com.example.strooplocker.data

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.example.strooplocker.utils.LoggingUtil

class LockedAppsRepository(private val dao: LockedAppDao) {
    companion object {
        private const val TAG = "LockedAppsRepository_DEBUG"
    }

    suspend fun getAllLockedApps(): List<String> = withContext(Dispatchers.IO) {
        val lockedApps = dao.getAllLockedApps().map { it.packageName }
        LoggingUtil.debug(TAG, "getAllLockedApps", "Retrieved ${lockedApps.size} locked apps")
        lockedApps
    }

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