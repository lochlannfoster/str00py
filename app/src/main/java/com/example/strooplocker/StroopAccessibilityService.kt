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
import com.example.strooplocker.utils.LoggingUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class StroopAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "StroopAccessibility_DEBUG"
        const val EXTRA_LOCKED_PACKAGE = "TARGET_PACKAGE"
    }

    // Track the current foreground package
    private var currentForegroundPackage: String? = null

    // Flag to track if service is currently active
    private var isServiceActive = false

    override fun onServiceConnected() {
        super.onServiceConnected()
        LoggingUtil.debug(TAG, "onServiceConnected", "Configuring accessibility service")

        try {
            serviceInfo = serviceInfo.apply {
                feedbackType = AccessibilityServiceInfo.FEEDBACK_ALL_MASK
                eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
                        AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
                flags = AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                        AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
            }
            LoggingUtil.debug(TAG, "onServiceConnected", "Service connected and configured successfully!")
            isServiceActive = true

            // Reset all session and challenge states
            SessionManager.endAllSessions()

            // Show a toast to confirm service is running
            Handler(Looper.getMainLooper()).post {
                Toast.makeText(applicationContext, "Stroop Lock Service activated", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            LoggingUtil.error(TAG, "onServiceConnected", "Error configuring accessibility service", e)
            isServiceActive = false
        }
    }

    // Helper method to detect home screen
    private fun isHomeScreen(packageName: String): Boolean {
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
            LoggingUtil.debug(TAG, "onAccessibilityEvent", "WINDOW CHANGE: package=$packageName, class=$className")

            // Skip events from our own app
            if (packageName == "com.example.strooplocker") {
                LoggingUtil.debug(TAG, "onAccessibilityEvent", "SKIPPING: Our own app events")
                return
            }

            // Track app switching to detect session endings
            if (isHomeScreen(packageName)) {
                // Returning to home screen should end all sessions
                LoggingUtil.debug(TAG, "onAccessibilityEvent", "HOME SCREEN: Ending all sessions")
                val previousPackage = currentForegroundPackage
                if (previousPackage != null && previousPackage != packageName) {
                    LoggingUtil.debug(TAG, "onAccessibilityEvent", "Ending session for: $previousPackage")
                    SessionManager.endSession(previousPackage)
                }
                currentForegroundPackage = packageName
            }
            // App switch
            else if (currentForegroundPackage != packageName) {
                LoggingUtil.debug(TAG, "onAccessibilityEvent", "APP SWITCH: From ${currentForegroundPackage ?: "null"} to $packageName")

                // End session for previous app
                if (currentForegroundPackage != null) {
                    SessionManager.handleAppSwitch(currentForegroundPackage, packageName)
                }

                // Update current package
                currentForegroundPackage = packageName

                // Check and lock the new app
                performCheckAndLockApp(packageName)
            }
            // Same app (new activity)
            else {
                LoggingUtil.debug(TAG, "onAccessibilityEvent", "SAME APP: New activity in $packageName")
                // Don't end the session, but still check if it's locked
                // in case this is the initial launch
                if (!SessionManager.isChallengeCompleted(packageName)) {
                    performCheckAndLockApp(packageName)
                }
            }
        }
    }

    /**
     * Checks if the current app is locked and shows the challenge if needed.
     * @param packageName The package name to check
     */
    private fun performCheckAndLockApp(packageName: String) {
        LoggingUtil.debug(TAG, "performCheckAndLockApp", "CHECKING: Is $packageName locked?")
        // Skip if a challenge is already in progress
        if (SessionManager.isChallengeInProgress()) {
            LoggingUtil.debug(TAG, "performCheckAndLockApp", "SKIPPING: Challenge already in progress")
            return
        }

        // Skip if this app has already completed a challenge
        if (SessionManager.isChallengeCompleted(packageName)) {
            LoggingUtil.debug(TAG, "performCheckAndLockApp", "SKIPPING: Challenge already completed for $packageName")
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val repository = LockedAppsRepository(
                    LockedAppDatabase.getInstance(this@StroopAccessibilityService).lockedAppDao()
                )

                val lockedApps = repository.getAllLockedApps()
                LoggingUtil.debug(TAG, "performCheckAndLockApp", "LOCKED APPS: ${lockedApps.joinToString()}")
                LoggingUtil.debug(TAG, "performCheckAndLockApp", "COMPLETED CHALLENGES: ${SessionManager.getCompletedChallenges().joinToString()}")

                val isLocked = lockedApps.contains(packageName)

                LoggingUtil.debug(TAG, "performCheckAndLockApp", "APP STATUS: $packageName | Locked: $isLocked")

                // Only launch challenge if app is locked and not recently completed
                if (isLocked) {
                    // Start the challenge in SessionManager
                    val challengeStarted = SessionManager.startChallenge(packageName)

                    if (challengeStarted) {
                        withContext(Dispatchers.Main) {
                            LoggingUtil.debug(TAG, "performCheckAndLockApp", "LAUNCHING CHALLENGE for: $packageName")

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
                        LoggingUtil.debug(TAG, "performCheckAndLockApp", "SKIPPING: Could not start challenge for $packageName")
                    }
                } else {
                    LoggingUtil.debug(TAG, "performCheckAndLockApp", "SKIPPING: $packageName is not locked")
                }
            } catch (e: Exception) {
                LoggingUtil.error(TAG, "performCheckAndLockApp", "ERROR checking locked status", e)
                SessionManager.resetChallenge()
            }
        }
    }

    override fun onInterrupt() {
        LoggingUtil.warning(TAG, "onInterrupt", "Accessibility service interrupted!")
        isServiceActive = false
        SessionManager.resetChallenge()
    }

    override fun onDestroy() {
        super.onDestroy()
        LoggingUtil.debug(TAG, "onDestroy", "Service destroyed")
        isServiceActive = false
        SessionManager.endAllSessions()
    }

}