// app/src/main/java/com/example/strooplocker/StroopAccessibilityService.kt

package com.example.strooplocker

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.widget.Toast
import com.example.strooplocker.data.LockedAppDatabase
import com.example.strooplocker.data.LockedAppsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Accessibility service that monitors app launches to prevent mindless scrolling.
 *
 * This service detects when the user tries to open a locked app and triggers
 * the Stroop challenge, which must be completed before granting access.
 */
class StroopAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "StroopAccessibility"
        const val EXTRA_LOCKED_PACKAGE = "TARGET_PACKAGE"
    }

    // Track the current foreground package
    private var currentForegroundPackage: String? = null

    // Flag to track if service is currently active
    private var isServiceActive = false

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d(TAG, "onServiceConnected: Configuring accessibility service")

        try {
            serviceInfo = serviceInfo.apply {
                feedbackType = AccessibilityServiceInfo.FEEDBACK_ALL_MASK
                eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
                        AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
                flags = AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                        AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
            }
            Log.d(TAG, "Service connected and configured successfully!")
            isServiceActive = true

            // Reset all session and challenge states
            SessionManager.endAllSessions()

            // Show a toast to confirm service is running
            Handler(Looper.getMainLooper()).post {
                Toast.makeText(applicationContext, "Stroop Lock Service activated", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error configuring accessibility service", e)
            isServiceActive = false
        }
    }

    // Helper method to detect home screen
    private fun isHomeScreen(packageName: String): Boolean {
        // Common launcher package names
        return packageName.contains("launcher") ||
                packageName.contains("home") ||
                packageName == "com.google.android.apps.nexuslauncher" ||
                packageName == "com.android.launcher"
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null || !isServiceActive) return

        val eventType = event.eventType
        val packageName = event.packageName?.toString() ?: return
        val className = event.className?.toString() ?: ""

        // Only process important events
        if (eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            Log.d(TAG, "WINDOW CHANGE: package=$packageName, class=$className")

            // Skip events from our own app
            if (packageName == "com.example.strooplocker") {
                Log.d(TAG, "SKIPPING: Our own app events")
                return
            }

            // Track app switching to detect session endings
            if (isHomeScreen(packageName)) {
                // Returning to home screen should end all sessions
                Log.d(TAG, "HOME SCREEN: Ending all sessions")
                val previousPackage = currentForegroundPackage
                if (previousPackage != null && previousPackage != packageName) {
                    Log.d(TAG, "Ending session for: $previousPackage")
                    SessionManager.endSession(previousPackage)
                }
                currentForegroundPackage = packageName
            }
            // App switch
            else if (currentForegroundPackage != packageName) {
                Log.d(TAG, "APP SWITCH: From ${currentForegroundPackage ?: "null"} to $packageName")

                // End session for previous app
                if (currentForegroundPackage != null) {
                    SessionManager.handleAppSwitch(currentForegroundPackage, packageName)
                }

                // Update current package
                currentForegroundPackage = packageName

                // Check and lock the new app
                checkAndLockApp(packageName)
            }
            // Same app (new activity)
            else {
                Log.d(TAG, "SAME APP: New activity in $packageName")
                // Don't end the session, but still check if it's locked
                // in case this is the initial launch
                if (!SessionManager.isChallengeCompleted(packageName)) {
                    checkAndLockApp(packageName)
                }
            }
        }
    }

    /**
     * Checks if the current app is locked and shows the challenge if needed.
     * @param packageName The package name to check
     */
    private fun checkAndLockApp(packageName: String) {
        // Add timestamp for debugging
        val timestamp = System.currentTimeMillis()
        Log.d(TAG, "CHECKING[$timestamp]: Is $packageName locked?")

        // Skip if a challenge is already in progress
        if (SessionManager.isChallengeInProgress()) {
            Log.d(TAG, "SKIPPING[$timestamp]: Challenge already in progress")
            return
        }

        // Skip if this app has already completed a challenge
        if (SessionManager.isChallengeCompleted(packageName)) {
            Log.d(TAG, "SKIPPING[$timestamp]: Challenge already completed for $packageName")
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val repository = LockedAppsRepository(
                    LockedAppDatabase.getInstance(this@StroopAccessibilityService).lockedAppDao()
                )

                val lockedApps = repository.getAllLockedApps()
                Log.d(TAG, "LOCKED APPS[$timestamp]: ${lockedApps.joinToString()}")
                Log.d(TAG, "COMPLETED CHALLENGES[$timestamp]: ${SessionManager.getCompletedChallenges().joinToString()}")

                val isLocked = lockedApps.contains(packageName)

                Log.d(TAG, "APP STATUS[$timestamp]: $packageName | Locked: $isLocked")

                // Only launch challenge if app is locked and not recently completed
                if (isLocked) {
                    // Start the challenge in SessionManager
                    val challengeStarted = SessionManager.startChallenge(packageName)

                    if (challengeStarted) {
                        withContext(Dispatchers.Main) {
                            Log.d(TAG, "LAUNCHING CHALLENGE[$timestamp] for: $packageName")

                            // Show toast for debugging
                            Toast.makeText(
                                applicationContext,
                                "Stroop challenge for: $packageName",
                                Toast.LENGTH_SHORT
                            ).show()

                            val lockIntent = Intent(this@StroopAccessibilityService, StroopLockActivity::class.java).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                                putExtra(EXTRA_LOCKED_PACKAGE, packageName)
                            }
                            startActivity(lockIntent)
                        }
                    } else {
                        Log.d(TAG, "SKIPPING[$timestamp]: Could not start challenge for $packageName")
                    }
                } else {
                    Log.d(TAG, "SKIPPING[$timestamp]: $packageName is not locked")
                }
            } catch (e: Exception) {
                Log.e(TAG, "ERROR[$timestamp] checking locked status: ${e.message}")
                SessionManager.resetChallenge()
            }
        }
    }

    override fun onInterrupt() {
        Log.w(TAG, "INTERRUPTED: Accessibility service interrupted!")
        isServiceActive = false
        SessionManager.resetChallenge()
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Service destroyed")
        isServiceActive = false
        SessionManager.endAllSessions()
    }
}