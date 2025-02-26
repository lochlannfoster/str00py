package com.example.strooplocker.data

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LockedAppsRepository(private val dao: LockedAppDao) {
    companion object {
        private const val TAG = "LockedAppsRepository_DEBUG"
    }

    // Now a suspending function
    suspend fun getAllLockedApps(): List<String> = withContext(Dispatchers.IO) {
        val lockedApps = dao.getAllLockedApps().map { it.packageName }
        Log.d(TAG, "Retrieved all locked apps: $lockedApps")
        lockedApps
    }

    // Also a suspending function
    suspend fun addLockedApp(packageName: String) = withContext(Dispatchers.IO) {
        Log.d(TAG, "Adding locked app: $packageName")
        try {
            dao.insertLockedApp(LockedApp(packageName))
            Log.d(TAG, "Successfully added locked app: $packageName")
        } catch (e: Exception) {
            Log.e(TAG, "Error adding locked app: $packageName", e)
            throw e
        }
    }

    suspend fun removeLockedApp(packageName: String) = withContext(Dispatchers.IO) {
        Log.d(TAG, "Removing locked app: $packageName")
        try {
            dao.deleteLockedApp(LockedApp(packageName))
            Log.d(TAG, "Successfully removed locked app: $packageName")
        } catch (e: Exception) {
            Log.e(TAG, "Error removing locked app: $packageName", e)
            throw e
        }
    }
}