package com.example.strooplocker

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.content.SharedPreferences
import android.util.Log
import android.view.accessibility.AccessibilityEvent

/**
 * Accessibility service that monitors window changes.
 * If a newly opened package is in the locked list, we show StroopLockActivity.
 */
class StroopAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "StroopAccessibility"
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        serviceInfo = serviceInfo.apply {
            feedbackType = AccessibilityServiceInfo.FEEDBACK_ALL_MASK
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
            flags = AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS
        }
        Log.d(TAG, "Service connected!")
    }


    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) {
            Log.w(TAG, "onAccessibilityEvent: event is null, ignoring.")
            return
        }

        val eventType = event.eventType
        val eventTypeName = when (eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> "TYPE_WINDOW_STATE_CHANGED"
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> "TYPE_WINDOW_CONTENT_CHANGED"
            else -> "OTHER($eventType)"
        }
        Log.d(TAG, "onAccessibilityEvent: type=$eventTypeName")

        if (eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val packageName = event.packageName?.toString() ?: ""
            Log.d(TAG, "TYPE_WINDOW_STATE_CHANGED: packageName=$packageName")

            // Load locked apps (only do this once)
            val lockedApps = getLockedAppsSet()
            Log.d(TAG, "Currently locked apps: $lockedApps")

            val isLocked = lockedApps.contains(packageName)
            Log.d(TAG, "isLocked=$isLocked for packageName=$packageName")

            if (isLocked) {
                Log.i(TAG, "Launching StroopLockActivity for locked package: $packageName")
                val lockIntent = Intent(this, StroopLockActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                startActivity(lockIntent)
            }
        }
    }

    override fun onInterrupt() {
        Log.w(TAG, "onInterrupt: Accessibility service interrupted.")
    }

    /**
     * Loads the locked packages set from SharedPreferences.
     * Adjust the file name/key to match your usage.
     */
    private fun getLockedAppsSet(): Set<String> {
        val prefs: SharedPreferences = getSharedPreferences("strooplocker_prefs", MODE_PRIVATE)
        return prefs.getStringSet("locked_apps", emptySet()) ?: emptySet()
    }
}
