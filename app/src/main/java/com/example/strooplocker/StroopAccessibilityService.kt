package com.example.strooplocker

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
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
import kotlinx.coroutines.delay
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
 * - Handles device lock/unlock events
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

    // Track if device is currently in locked state
    private var isDeviceLocked = false

    // Broadcast receiver for screen events
    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_OFF -> {
                    LoggingUtil.debug(TAG, "onReceive", "SCREEN OFF detected")
                    isDeviceLocked = true
                }
                Intent.ACTION_SCREEN_ON -> {
                    LoggingUtil.debug(TAG, "onReceive", "SCREEN ON detected")
                    // Just turning screen on doesn't necessarily unlock the device
                }
                Intent.ACTION_USER_PRESENT -> {
                    LoggingUtil.debug(TAG, "onReceive", "USER PRESENT detected - device unlocked")
                    isDeviceLocked = false
                    // Reset all sessions when user unlocks the device
                    // This ensures challenges will be required when returning to apps
                    SessionManager.endAllSessions()
                    currentForegroundPackage = null
                }
            }
        }
    }

    /**
     * Called when the service is connected and ready.
     * Configures the service to monitor app launches and switches.
     */
    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d(TAG, "onServiceConnected: ACCESSIBILITY SERVICE STARTING")

        try {
            serviceInfo = serviceInfo.apply {
                feedbackType = AccessibilityServiceInfo.FEEDBACK_ALL_MASK
                eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
                        AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
                flags = AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                        AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
            }
            Log.d(TAG, "ACCESSIBILITY SERVICE CONNECTED AND ACTIVE!")
            isServiceActive = true

            // Reset all session and challenge states
            SessionManager.endAllSessions()
            Log.d(TAG, "All sessions cleared")

            // Initialize locked apps cache
            CoroutineScope(Dispatchers.IO).launch {
                refreshLockedAppsCache()
                Log.d(TAG, "Initial locked apps: ${cachedLockedApps.joinToString()}")
            }

            // Register screen event receiver
            val filter = IntentFilter().apply {
                addAction(Intent.ACTION_SCREEN_ON)
                addAction(Intent.ACTION_SCREEN_OFF)
                addAction(Intent.ACTION_USER_PRESENT)
            }
            registerReceiver(screenReceiver, filter)

            // Show a toast to confirm service is running
            Handler(Looper.getMainLooper()).post {
                Toast.makeText(applicationContext, "STROOP LOCK SERVICE ACTIVATED", Toast.LENGTH_LONG).show()
                Log.d(TAG, "Service activation toast shown")
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

            // If we detect device was locked and now an app is opening
            // (meaning the device must be unlocked), clear all sessions
            if (isDeviceLocked) {
                LoggingUtil.debug(TAG, "onAccessibilityEvent", "Device appears to be unlocking - clearing all sessions")
                isDeviceLocked = false
                SessionManager.endAllSessions()
                currentForegroundPackage = null
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
        Log.d(TAG, "LOCK_CHECK: Starting for $packageName (previous: $previousPackage)")

        // Debug: Log accessibility service status
        Log.d(TAG, "Service active: $isServiceActive")

        // Skip if a challenge is already in progress
        if (SessionManager.isChallengeInProgress()) {
            Log.d(TAG, "SKIPPING LOCK: Challenge already in progress")
            return
        }

        // Skip our own app
        if (packageName == "com.example.strooplocker") {
            Log.d(TAG, "SKIPPING LOCK: This is our own app")
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Force refresh the locked apps cache to ensure it's up-to-date
                Log.d(TAG, "Refreshing locked apps cache (old cache: ${cachedLockedApps.joinToString()})")
                refreshLockedAppsCache()
                Log.d(TAG, "Cache refreshed: ${cachedLockedApps.joinToString()}")

                // Check if this app is in the locked list
                val isLocked = cachedLockedApps.contains(packageName)
                Log.d(TAG, "Is $packageName in locked list? $isLocked")

                // Check if a challenge has been completed for this app
                val hasCompletedChallenge = SessionManager.isChallengeCompleted(packageName)
                Log.d(TAG, "Has $packageName completed a challenge? $hasCompletedChallenge")

                // Handle app switch with locked apps information if this is from an app switch
                if (previousPackage != null) {
                    // This will ensure proper session management during app switches
                    SessionManager.handleAppSwitch(previousPackage, packageName, cachedLockedApps)
                }

                // Force clearing any completion status for debugging
                if (isLocked) {
                    Log.d(TAG, "DEBUG FORCING: Clearing any existing completion status for $packageName")
                    SessionManager.endSession(packageName)
                    // Double-check it worked
                    val nowHasCompletedChallenge = SessionManager.isChallengeCompleted(packageName)
                    Log.d(TAG, "After forcing: Challenge completed status is now: $nowHasCompletedChallenge")
                }

                // After the switch is handled, check again if this app needs a challenge
                val shouldShowLock = isLocked && !SessionManager.isChallengeCompleted(packageName)

                Log.d(TAG, "LOCK_DECISION: Package=$packageName, isLocked=$isLocked, hasCompletedChallenge=$hasCompletedChallenge, shouldShowLock=$shouldShowLock")

                // Only launch challenge if app is locked and not recently completed
                if (shouldShowLock) {
                    // Start the challenge in SessionManager
                    val challengeStarted = SessionManager.startChallenge(packageName)
                    Log.d(TAG, "Attempt to start challenge: $challengeStarted")

                    if (challengeStarted) {
                        withContext(Dispatchers.Main) {
                            Log.d(TAG, "LAUNCHING LOCK CHALLENGE for: $packageName")

                            // Show toast for debugging
                            Toast.makeText(
                                applicationContext,
                                "LAUNCHING LOCK for: $packageName",
                                Toast.LENGTH_LONG
                            ).show()

                            // Force short delay to ensure toast is visible
                            try {
                                delay(500)
                            } catch (e: Exception) {
                                Log.e(TAG, "Delay error", e)
                            }

                            val lockIntent = Intent(this@StroopAccessibilityService, StroopLockActivity::class.java).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                                putExtra(EXTRA_LOCKED_PACKAGE, packageName)
                            }
                            startActivity(lockIntent)

                            // Debug: confirm intent was sent
                            Log.d(TAG, "Lock intent for $packageName sent successfully")
                        }
                    } else {
                        Log.d(TAG, "ERROR: Could not start challenge for $packageName")
                    }
                } else {
                    val reason = when {
                        !isLocked -> "not locked"
                        SessionManager.isChallengeCompleted(packageName) -> "challenge already completed"
                        else -> "unknown reason"
                    }
                    Log.d(TAG, "NOT SHOWING LOCK: $packageName ($reason)")
                }
            } catch (e: Exception) {
                Log.e(TAG, "ERROR checking locked status for $packageName", e)
                e.printStackTrace()
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

        // Unregister receivers
        try {
            unregisterReceiver(screenReceiver)
        } catch (e: Exception) {
            // Already unregistered, ignore
        }

        SessionManager.endAllSessions()
    }
}