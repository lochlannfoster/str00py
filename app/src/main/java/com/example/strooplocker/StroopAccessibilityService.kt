// File: app/src/main/java/com/example/strooplocker/StroopAccessibilityService.kt

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

/**
 * The AccessibilityService implementation for str00py.
 *
 * This service monitors app launches and detects when the user tries to open a locked app.
 * When a locked app is detected, it launches the challenge activity, requiring the user to
 * complete a cognitive challenge before accessing the app.
 *
 * Key features:
 * - Monitors app launches and switches
 * - Detects when locked apps are opened
 * - Launches challenge activities
 * - Manages app sessions
 */
class StroopAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "StroopAccessibility_DEBUG"
        const val EXTRA_LOCKED_PACKAGE = "TARGET_PACKAGE"
    }

    // Track the current foreground package
    private var currentForegroundPackage: String? = null

    // Flag to track if service is currently active
    private var isServiceActive = false

    // Cache of locked apps to reduce DB lookups
    private var cachedLockedApps: List<String> = emptyList()
    private var lastLockedAppsUpdateTime: Long = 0
    private val CACHE_VALIDITY_PERIOD = 30000L // 30 seconds

    /**
     * Called when the service is connected and ready.
     * Configures the service to monitor app launches and switches.
     */
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

            // Initialize locked apps cache
            CoroutineScope(Dispatchers.IO).launch {
                refreshLockedAppsCache()
            }

            // Show a toast to confirm service is running
            Handler(Looper.getMainLooper()).post {
                Toast.makeText(applicationContext, getString(R.string.toast_service_activated), Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            LoggingUtil.error(TAG, "onServiceConnected", "Error configuring accessibility service", e)
            isServiceActive = false
        }
    }

    /**
     * Detects if a package name represents a home screen launcher.
     * Used to identify when the user returns to the home screen.
     *
     * @param packageName The package name to check
     * @return true if the package is likely a home screen launcher
     */
    private fun isHomeScreen(packageName: String): Boolean {
        return packageName.contains("launcher") ||
                packageName.contains("home") ||
                packageName == "com.google.android.apps.nexuslauncher" ||
                packageName == "com.android.launcher"
    }

    /**
     * Called when an accessibility event is received.
     * Monitors app launches and switches to detect when locked apps are opened.
     *
     * @param event The accessibility event that was received
     */
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
                currentForegroundPackage = packageName

                // Process home screen transition
                if (previousPackage != null && previousPackage != packageName) {
                    CoroutineScope(Dispatchers.IO).launch {
                        ensureLockedAppsCacheValid()
                        SessionManager.handleAppSwitch(previousPackage, packageName, cachedLockedApps)
                    }
                }
            }
            // App switch
            else if (currentForegroundPackage != packageName) {
                LoggingUtil.debug(TAG, "onAccessibilityEvent", "APP SWITCH: From ${currentForegroundPackage ?: "null"} to $packageName")

                // Store the previous package
                val previousPackage = currentForegroundPackage

                // Update current package
                currentForegroundPackage = packageName

                // Check if the new app should be locked, and handle the app switch
                performCheckAndLockApp(packageName, previousPackage)
            }
        }
    }

    /**
     * Ensures the locked apps cache is valid and refreshes it if needed
     */
    private suspend fun ensureLockedAppsCacheValid() {
        val now = System.currentTimeMillis()
        if (now - lastLockedAppsUpdateTime > CACHE_VALIDITY_PERIOD) {
            refreshLockedAppsCache()
        }
    }

    /**
     * Refreshes the cached list of locked apps from the database
     */
    private suspend fun refreshLockedAppsCache() {
        try {
            val repository = LockedAppsRepository(
                LockedAppDatabase.getInstance(this@StroopAccessibilityService).lockedAppDao()
            )
            cachedLockedApps = repository.getAllLockedApps()
            lastLockedAppsUpdateTime = System.currentTimeMillis()
            LoggingUtil.debug(TAG, "refreshLockedAppsCache", "Updated locked apps cache: ${cachedLockedApps.joinToString()}")
        } catch (e: Exception) {
            LoggingUtil.error(TAG, "refreshLockedAppsCache", "Error refreshing locked apps cache", e)
        }
    }

    /**
     * Checks if the current app is locked and shows the challenge if needed.
     * This is the core logic that determines when to present a challenge.
     *
     * @param packageName The package name to check
     * @param previousPackage The previous package name, if this is an app switch
     */
    private fun performCheckAndLockApp(packageName: String, previousPackage: String? = null) {
        LoggingUtil.debug(TAG, "performCheckAndLockApp", "CHECKING: Is $packageName locked?")

        // Skip if a challenge is already in progress
        if (SessionManager.isChallengeInProgress()) {
            LoggingUtil.debug(TAG, "performCheckAndLockApp", "SKIPPING: Challenge already in progress")
            return
        }

        // Skip our own app
        if (packageName == "com.example.strooplocker") {
            LoggingUtil.debug(TAG, "performCheckAndLockApp", "SKIPPING: This is our own app")
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Ensure we have up-to-date locked apps list
                ensureLockedAppsCacheValid()

                // Log current state before making any decisions
                LoggingUtil.debug(TAG, "performCheckAndLockApp", "LOCKED APPS: ${cachedLockedApps.joinToString()}")
                LoggingUtil.debug(TAG, "performCheckAndLockApp", "COMPLETED CHALLENGES: ${SessionManager.getCompletedChallenges().joinToString()}")
                LoggingUtil.debug(TAG, "performCheckAndLockApp", "Is challenge for $packageName already completed? ${SessionManager.isChallengeCompleted(packageName)}")

                // Handle app switch with locked apps information if this is from an app switch
                if (previousPackage != null) {
                    // This will ensure proper session management during app switches
                    SessionManager.handleAppSwitch(previousPackage, packageName, cachedLockedApps)
                }

                // After the switch is handled, check again if this app needs a challenge
                val isLocked = cachedLockedApps.contains(packageName)
                val hasCompletedChallenge = SessionManager.isChallengeCompleted(packageName)

                LoggingUtil.debug(TAG, "performCheckAndLockApp", "FINAL APP STATUS: $packageName | Locked: $isLocked | Challenge completed: $hasCompletedChallenge")

                // Only launch challenge if app is locked and not recently completed
                if (isLocked && !hasCompletedChallenge) {
                    // Start the challenge in SessionManager
                    val challengeStarted = SessionManager.startChallenge(packageName)

                    if (challengeStarted) {
                        withContext(Dispatchers.Main) {
                            LoggingUtil.debug(TAG, "performCheckAndLockApp", "LAUNCHING CHALLENGE for: $packageName")

                            // Show toast for debugging
                            Toast.makeText(
                                applicationContext,
                                getString(R.string.toast_challenge_for, packageName),
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
                    val reason = when {
                        !isLocked -> "not locked"
                        hasCompletedChallenge -> "challenge already completed"
                        else -> "unknown reason"
                    }
                    LoggingUtil.debug(TAG, "performCheckAndLockApp", "SKIPPING: $packageName ($reason)")
                }
            } catch (e: Exception) {
                LoggingUtil.error(TAG, "performCheckAndLockApp", "ERROR checking locked status", e)
                SessionManager.resetChallenge()
            }
        }
    }

    /**
     * Called when the service is interrupted.
     * Resets the service state and any active challenges.
     */
    override fun onInterrupt() {
        LoggingUtil.warning(TAG, "onInterrupt", "Accessibility service interrupted!")
        isServiceActive = false
        SessionManager.resetChallenge()
    }

    /**
     * Called when the service is being destroyed.
     * Cleans up any active sessions and challenges.
     */
    override fun onDestroy() {
        super.onDestroy()
        LoggingUtil.debug(TAG, "onDestroy", "Service destroyed")
        isServiceActive = false
        SessionManager.endAllSessions()
    }
}