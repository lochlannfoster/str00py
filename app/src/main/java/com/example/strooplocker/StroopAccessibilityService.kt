package com.example.strooplocker

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.content.SharedPreferences
import android.util.Log
import android.view.accessibility.AccessibilityEvent

/**
 * Accessibility service that re-launches the Stroop puzzle
 * whenever a locked app's window comes to the foreground (focus).
 */
class StroopAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "StroopAccessibility"
        /** Key for passing the locked package name into StroopLockActivity */
        const val EXTRA_LOCKED_PACKAGE = "TARGET_PACKAGE"
    }

    override fun onServiceConnected() {
        super.onServiceConnected()

        // Set up the service to watch for window state changes (and optionally content changes).
        serviceInfo = serviceInfo.apply {
            feedbackType = AccessibilityServiceInfo.FEEDBACK_ALL_MASK
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
                    AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
            flags = AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS
        }
        Log.d(TAG, "Service connected!")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        val eventType = event.eventType
        // We mainly care about window state changes (app in foreground).
        if (eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED ||
            eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {

            val packageName = event.packageName?.toString() ?: return
            val lockedApps = getLockedAppsSet()

            val isLocked = lockedApps.contains(packageName)
            Log.d(TAG, "TYPE_WINDOW_STATE_CHANGED: packageName=$packageName, isLocked=$isLocked")

            if (isLocked) {
                Log.i(TAG, "Launching StroopLockActivity for locked package: $packageName")

                val lockIntent = Intent(this, StroopLockActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    putExtra(EXTRA_LOCKED_PACKAGE, packageName)
                }
                startActivity(lockIntent)
            }
        }
    }

    override fun onInterrupt() {
        Log.w(TAG, "onInterrupt: Accessibility service interrupted.")
    }

    /**
     * Loads the locked packages from SharedPreferences
     */
    private fun getLockedAppsSet(): Set<String> {
        val prefs: SharedPreferences = getSharedPreferences("strooplocker_prefs", MODE_PRIVATE)
        return prefs.getStringSet("locked_apps", emptySet()) ?: emptySet()
    }
}
