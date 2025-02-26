package com.example.strooplocker.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LockedAppsRepository(private val dao: LockedAppDao) {

    // Now a suspending function
    suspend fun getAllLockedApps(): List<String> = withContext(Dispatchers.IO) {
        dao.getAllLockedApps().map { it.packageName }
    }

    // Also a suspending function
    suspend fun addLockedApp(packageName: String) = withContext(Dispatchers.IO) {
        dao.insertLockedApp(LockedApp(packageName))
    }

    suspend fun removeLockedApp(packageName: String) = withContext(Dispatchers.IO) {
        dao.deleteLockedApp(LockedApp(packageName))
    }
}
