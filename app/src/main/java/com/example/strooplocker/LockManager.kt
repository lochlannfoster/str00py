package com.example.strooplocker

import android.content.Context
import android.content.SharedPreferences

object LockManager {
    private const val PREFS_NAME = "StroopLockPrefs"
    private const val LOCKED_APPS_KEY = "LockedAppsSet"

    fun addLockedApp(context: Context, packageName: String) {
        val prefs = getPrefs(context)
        val lockedApps = getLockedApps(context).toMutableSet()
        lockedApps.add(packageName)
        prefs.edit().putStringSet(LOCKED_APPS_KEY, lockedApps).apply()
    }

    fun removeLockedApp(context: Context, packageName: String) {
        val prefs = getPrefs(context)
        val lockedApps = getLockedApps(context).toMutableSet()
        lockedApps.remove(packageName)
        prefs.edit().putStringSet(LOCKED_APPS_KEY, lockedApps).apply()
    }

    fun getLockedApps(context: Context): Set<String> {
        val prefs = getPrefs(context)
        return prefs.getStringSet(LOCKED_APPS_KEY, emptySet()) ?: emptySet()
    }

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
}
