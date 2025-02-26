package com.example.strooplocker

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.example.strooplocker.data.LockedAppDatabase
import com.example.strooplocker.data.LockedAppsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Accessibility service that re-launches the Stroop puzzle
 * whenever a locked app's window comes to the foreground (focus).
 */
class StroopAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "StroopAccessibility"
        /** Key for passing the locked package name into StroopLockActivity */
        const val EXTRA_LOCKED_PACKAGE = "TARGET_PACKAGE"

        @Volatile
        var challengeInProgress: Boolean = false

        @Volatile
        var pendingLockedPackage: String? = null
    }

    override fun onServiceConnected() {
        super.onServiceConnected()

        // Set up the service to watch for window state changes (and optionally content changes).
        serviceInfo = serviceInfo.apply {
            feedbackType = AccessibilityServiceInfo.FEEDBACK_ALL_MASK
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
                    AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
            flags = AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                    AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
        }
        Log.d(TAG, "Service connected!")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        val eventType = event.eventType
        // We mainly care about window state changes (app in foreground).
        if (eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val packageName = event.packageName?.toString() ?: return

            // Skip processing if we're already showing a challenge
            if (challengeInProgress) {
                Log.d(TAG, "Challenge already in progress, ignoring $packageName")
                return
            }

            // Get locked apps from database
            CoroutineScope(Dispatchers.IO).launch {
                val repository = LockedAppsRepository(
                    LockedAppDatabase.getInstance(this@StroopAccessibilityService).lockedAppDao()
                )

                val lockedApps = repository.getAllLockedApps()
                val isLocked = lockedApps.contains(packageName)
                Log.d(TAG, "TYPE_WINDOW_STATE_CHANGED: packageName=$packageName, isLocked=$isLocked")

                if (isLocked) {
                    Log.i(TAG, "Launching StroopLockActivity for locked package: $packageName")

                    // Mark package as pending and challenge as active
                    pendingLockedPackage = packageName
                    challengeInProgress = true

                    withContext(Dispatchers.Main) {
                        val lockIntent = Intent(this@StroopAccessibilityService, StroopLockActivity::class.java).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or
                                    Intent.FLAG_ACTIVITY_CLEAR_TOP)
                            putExtra(EXTRA_LOCKED_PACKAGE, packageName)
                        }
                        startActivity(lockIntent)
                    }
                }
            }
        }
    }

    override fun onInterrupt() {
        Log.w(TAG, "onInterrupt: Accessibility service interrupted.")
    }
}