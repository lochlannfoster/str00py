package com.example.strooplocker

import android.content.Context
import com.example.strooplocker.data.LockedAppDatabase
import com.example.strooplocker.data.LockedAppsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Centralized manager for app locking operations.
 * Provides a single source of truth for locked apps.
 */
object LockManager {
    /**
     * Adds an app to the locked apps list
     */
    suspend fun addLockedApp(context: Context, packageName: String) {
        val repository = getRepository(context)
        repository.addLockedApp(packageName)
    }

    /**
     * Removes an app from the locked apps list
     */
    suspend fun removeLockedApp(context: Context, packageName: String) {
        val repository = getRepository(context)
        repository.removeLockedApp(packageName)
    }

    /**
     * Gets all currently locked apps
     */
    suspend fun getLockedApps(context: Context): List<String> {
        val repository = getRepository(context)
        return repository.getAllLockedApps()
    }

    /**
     * Checks if a specific app is locked
     */
    suspend fun isAppLocked(context: Context, packageName: String): Boolean {
        return withContext(Dispatchers.IO) {
            val lockedApps = getLockedApps(context)
            lockedApps.contains(packageName)
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