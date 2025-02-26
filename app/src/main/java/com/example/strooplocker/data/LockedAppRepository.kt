package com.example.strooplocker.data

class LockedAppsRepository(private val dao: LockedAppDao) {

    fun getAllLockedApps(): List<String> {
        return dao.getAllLockedApps().map { it.packageName }
    }

    fun addLockedApp(packageName: String) {
        dao.insertLockedApp(LockedApp(packageName))
    }

    fun removeLockedApp(packageName: String) {
        dao.deleteLockedApp(LockedApp(packageName))
    }
}
