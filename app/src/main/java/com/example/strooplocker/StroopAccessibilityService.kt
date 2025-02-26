package com.example.strooplocker

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.view.accessibility.AccessibilityEvent
import android.content.Intent
import android.util.Log

class StroopAccessibilityService : AccessibilityService() {

    companion object {
        // Set by the lock screen to let us know user is currently solving a puzzle
        @Volatile
        var challengeInProgress: Boolean = false

        // The package that triggered the lock screen
        @Volatile
        var pendingLockedPackage: String? = null
    }

    override fun onInterrupt() {
        // You can leave this empty or log something if needed.
        // For most basic services, an empty body is fine.
    }
    override fun onServiceConnected() {
        super.onServiceConnected()
        // Basic config
        serviceInfo = serviceInfo.apply {
            flags = flags or AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
        }
        Log.d("StroopAccessibility", "Service connected.")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val fgPackage = event.packageName?.toString() ?: return

            Log.d("StroopAccessibility", "Window changed: $fgPackage")

            // Check if this foreground package is one of our locked apps
            val lockedApps = LockManager.getLockedApps(this)
            if (lockedApps.contains(fgPackage)) {
                // If not already showing puzzle, launch puzzle
                if (!challengeInProgress) {
                    challengeInProgress = true
                    pendingLockedPackage = fgPackage

                    Log.d("StroopAccessibility", "Launching StroopLock for $fgPackage")

                    // Launch the lock screen
                    val lockIntent = Intent(this, StroopLockActivity::class.java).apply {
                        addFlags(
                            Intent.FLAG_ACTIVITY_NEW_TASK or
                                    Intent.FLAG_ACTIVITY_CLEAR_TASK or
                                    Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
                        )
                    }
                    startActivity(lockIntent)
                }
            }
        }
    }

}
