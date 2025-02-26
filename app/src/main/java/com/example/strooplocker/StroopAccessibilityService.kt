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
<<<<<<< Updated upstream
        private const val TAG = "StroopAccessibility"
        /** Key for passing the locked package name into StroopLockActivity */
        const val EXTRA_LOCKED_PACKAGE = "TARGET_PACKAGE"
=======
        @Volatile
        var challengeInProgress: Boolean = false

        @Volatile
        var pendingLockedPackage: String? = null

        private const val TAG = "StroopAccessibility"
>>>>>>> Stashed changes
    }

    override fun onServiceConnected() {
        super.onServiceConnected()

<<<<<<< Updated upstream
        // Set up the service to watch for window state changes (and optionally content changes).
        serviceInfo = serviceInfo.apply {
            feedbackType = AccessibilityServiceInfo.FEEDBACK_ALL_MASK
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
                    AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
            flags = AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS
        }
        Log.d(TAG, "Service connected!")
=======
        // Force event types to all, retrieve windows, etc.
        val updatedInfo = serviceInfo.apply {
            // Listen for any event type for debugging:
            eventTypes = AccessibilityEvent.TYPES_ALL_MASK

            // Any feedback type is fine if you only want to “listen”:
            feedbackType = AccessibilityServiceInfo.FEEDBACK_ALL_MASK

            // Let us see windows changes, windows content changes:
            flags = flags or AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS

            // If you want to limit to certain packages, you can set `packageNames = ...`
        }
        serviceInfo = updatedInfo

        Log.d(TAG, "onServiceConnected: Service is now configured with eventTypes=ALL, retrieving windows.")
>>>>>>> Stashed changes
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

<<<<<<< Updated upstream
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
=======
        // Convert eventType -> human-readable name, e.g. TYPE_WINDOW_STATE_CHANGED, etc.
        val eventTypeName = when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> "TYPE_WINDOW_STATE_CHANGED"
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> "TYPE_WINDOW_CONTENT_CHANGED"
            AccessibilityEvent.TYPE_VIEW_CLICKED -> "TYPE_VIEW_CLICKED"
            AccessibilityEvent.TYPE_VIEW_FOCUSED -> "TYPE_VIEW_FOCUSED"
            else -> "OTHER-${event.eventType}"
        }

        // ALWAYS log this line, to confirm we do see events at all:
        Log.d(TAG, "onAccessibilityEvent => type=$eventTypeName pkg=${event.packageName} class=${event.className}")

        // Our typical logic is triggered for window-state changes (i.e. "I see a new window"):
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val pkgName = event.packageName?.toString() ?: return

            // Debug: log what apps are locked right now
            val lockedApps = LockManager.getLockedApps(this)
            Log.d(TAG, "Locked apps are currently: $lockedApps")
            Log.d(TAG, "Checking if current pkg=$pkgName is locked...")

            if (lockedApps.contains(pkgName)) {
                Log.d(TAG, "YES - $pkgName is locked. Checking challengeInProgress=$challengeInProgress")
                if (!challengeInProgress) {
                    // Start puzzle
                    challengeInProgress = true
                    pendingLockedPackage = pkgName

                    Log.d(TAG, "Launching StroopLockActivity for $pkgName ...")

                    // Launch the lock screen
                    val lockIntent = Intent(this, StroopLockActivity::class.java).apply {
                        addFlags(
                            Intent.FLAG_ACTIVITY_NEW_TASK or
                                    Intent.FLAG_ACTIVITY_CLEAR_TASK or
                                    Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
                        )
                    }
                    startActivity(lockIntent)
                } else {
                    Log.d(TAG, "Already in puzzle => ignoring additional triggers.")
                }
            } else {
                // Not in locked list, do nothing
>>>>>>> Stashed changes
            }
        }
    }

    override fun onInterrupt() {
<<<<<<< Updated upstream
        Log.w(TAG, "onInterrupt: Accessibility service interrupted.")
    }

    /**
     * Loads the locked packages from SharedPreferences
     */
    private fun getLockedAppsSet(): Set<String> {
        val prefs: SharedPreferences = getSharedPreferences("strooplocker_prefs", MODE_PRIVATE)
        return prefs.getStringSet("locked_apps", emptySet()) ?: emptySet()
=======
        // We generally have nothing special to do here
        Log.d(TAG, "onInterrupt called – service is being interrupted.")
>>>>>>> Stashed changes
    }
}
