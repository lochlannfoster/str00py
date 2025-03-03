// app/src/main/java/com/example/strooplocker/SessionManager.kt
package com.example.strooplocker

import android.os.Handler
import android.os.Looper
import android.util.Log
import com.example.strooplocker.utils.SessionDebugger


/**
 * SessionManager manages app session states and challenge tracking.
 *
 * This singleton provides a centralized way to track app challenge completions,
 * manage session lifecycles, and handle app switching scenarios.
 */
object SessionManager {
    private const val TAG = "SessionManager"

    // Session timeout for determining when a completed challenge is no longer valid
    const val SESSION_TIMEOUT_MS = 5000L  // 5 seconds for test compatibility

    // For real usage, use a moderate timeout for better lock engagement
    // Previously this was only 500ms which was likely too short and causing issues
    private const val REAL_WORLD_TIMEOUT_MS = 3000L // 3 seconds for real-world behavior

    // Track completed challenges with timestamps
    private val completedChallenges = mutableMapOf<String, Long>()

    // Only track if a challenge is currently in progress to prevent overlapping challenges
    @Volatile
    private var challengeInProgress = false

    @Volatile
    private var currentChallengePackage: String? = null

    // Handler for timeouts (needed for testing)
    private var timeoutHandler: Handler = createDefaultHandler()

    // Flag to indicate if we're in testing mode (to avoid auto-expiry)
    private var testingMode = false

    // Last time sessions were cleared (to force periodic clearing)
    private var lastClearTime = System.currentTimeMillis()
    private const val FORCE_CLEAR_INTERVAL = 60000L // 1 minute

    /**
     * Creates a default Handler using the main Looper.
     */
    private fun createDefaultHandler(): Handler {
        return try {
            Handler(Looper.getMainLooper())
        } catch (e: Exception) {
            Handler()
        }
    }

    /**
     * Testing method to replace the handler for unit testing.
     * Also activates testing mode which disables auto-expiry.
     */
    @Suppress("TestOnly")
    fun replaceHandlerForTesting(handler: Handler) {
        timeoutHandler = handler
        testingMode = true
    }

    /**
     * Starts a challenge for a specific package.
     *
     * @param packageName Package to challenge
     * @return true if challenge started successfully, false if already in progress
     */
    @Synchronized
    fun startChallenge(packageName: String): Boolean {
        // Check if we should force a periodic clearing of challenges
        checkForPeriodicClearing()

        if (challengeInProgress) {
            Log.d(TAG, "Cannot start challenge for $packageName: already in progress")
            SessionDebugger.logChallenge(packageName, "START_FAILED", "already in progress")
            return false
        }

        Log.d(TAG, "Starting challenge for $packageName")
        SessionDebugger.logChallenge(packageName, "STARTED")
        challengeInProgress = true
        currentChallengePackage = packageName

        // Set a timeout to reset the challenge state if it doesn't complete
        timeoutHandler.removeCallbacksAndMessages(null)
        timeoutHandler.postDelayed({
            if (challengeInProgress && currentChallengePackage == packageName) {
                Log.w(TAG, "Challenge timed out for $packageName")
                resetChallenge()
            }
        }, 30000L)

        return true
    }

    /**
     * Completes a challenge for a specific package.
     *
     * @param packageName Package that completed the challenge
     */
    @Synchronized
    fun completeChallenge(packageName: String) {
        Log.d(TAG, "Completing challenge for $packageName")
        SessionDebugger.logChallenge(packageName, "COMPLETED")
        challengeInProgress = false
        currentChallengePackage = null

        // Add to completed challenges with the current timestamp
        completedChallenges[packageName] = System.currentTimeMillis()

        timeoutHandler.removeCallbacksAndMessages(null)

        // Only set auto-expiry in real usage, not during tests
        if (!testingMode) {
            // Set a timeout to expire this completion
            timeoutHandler.postDelayed({
                Log.d(TAG, "Auto-expiring session for $packageName")
                SessionDebugger.logSessionStatus(packageName, false, "auto-expiry")
                endSession(packageName)
            }, REAL_WORLD_TIMEOUT_MS)
        }
    }

    /**
     * Checks if a challenge is currently in progress.
     *
     * @return true if a challenge is in progress
     */
    fun isChallengeInProgress(): Boolean = challengeInProgress

    /**
     * Gets the current challenge package name.
     *
     * @return Package name or null if no challenge is in progress
     */
    fun getCurrentChallengePackage(): String? = currentChallengePackage

    /**
     * Checks if a challenge has been completed for the given package
     * and is still valid (within the timeout period).
     *
     * IMPORTANT: This also periodically forces clearing of all challenges
     * to ensure the app remains aggressive about re-locking.
     *
     * @param packageName The package name to check
     * @return true if the challenge has been completed for this package and is still valid
     */

// app/src/main/java/com/example/strooplocker/SessionManager.kt

    fun isChallengeCompleted(packageName: String): Boolean {
        // Debug logging
        Log.d(TAG, "CHECK_LOCK: Checking if challenge for $packageName is completed")

        // Check if we should force a periodic clearing of challenges
        checkForPeriodicClearing()

        val timestamp = completedChallenges[packageName] ?: return false

        // Check if the timestamp is still valid (within timeout period)
        val now = System.currentTimeMillis()
        val timeoutToUse = if (testingMode) SESSION_TIMEOUT_MS else REAL_WORLD_TIMEOUT_MS
        val isValid = (now - timestamp) < timeoutToUse

        // If expired, remove it and return false
        if (!isValid) {
            Log.d(TAG, "Challenge for $packageName has expired (age: ${now - timestamp}ms, timeout: ${timeoutToUse}ms)")
            SessionDebugger.logSessionStatus(packageName, false, "timeout: ${now - timestamp}ms > ${timeoutToUse}ms")
            completedChallenges.remove(packageName)
            return false
        }

        SessionDebugger.logSessionStatus(packageName, true, "within timeout: ${now - timestamp}ms < ${timeoutToUse}ms")
        return true
    }

    /**
     * Periodically clears all challenges to ensure the app doesn't stay
     * unlocked for too long, even if the user keeps interacting with it.
     */
    private fun checkForPeriodicClearing() {
        if (testingMode) return

        val now = System.currentTimeMillis()
        if (now - lastClearTime > FORCE_CLEAR_INTERVAL) {
            Log.d(TAG, "Performing periodic clearing of all sessions")
            completedChallenges.clear()
            lastClearTime = now
        }
    }

    /**
     * Resets the current challenge state.
     */
    @Synchronized
    fun resetChallenge() {
        Log.d(TAG, "Resetting current challenge state")
        challengeInProgress = false
        currentChallengePackage = null
        timeoutHandler.removeCallbacksAndMessages(null)
    }

    /**
     * Ends a session for the given package.
     * This removes the package from the completed challenges list.
     *
     * @param packageName The package name to end session for
     */
    @Synchronized
    fun endSession(packageName: String) {
        Log.d(TAG, "Ending session for: $packageName")
        SessionDebugger.logSessionStatus(packageName, false, "session ended explicitly")
        completedChallenges.remove(packageName)
    }

    /**
     * Ends all active sessions.
     */
    @Synchronized
    fun endAllSessions() {
        Log.d(TAG, "Ending all sessions")
        SessionDebugger.logEvent("GLOBAL", "ALL_SESSIONS_ENDED",
            "cleared ${completedChallenges.size} sessions")
        completedChallenges.clear()
        resetChallenge()
        lastClearTime = System.currentTimeMillis()
    }

    /**
     * Handles app switch events to manage sessions.
     *
     * @param fromPackage Previous package
     * @param toPackage New package
     * @param lockedApps List of currently locked app package names
     */

    @Synchronized
    fun handleAppSwitch(fromPackage: String?, toPackage: String, lockedApps: List<String> = emptyList()) {
        Log.d(TAG, "APP_SWITCH: From $fromPackage -> To $toPackage")
        SessionDebugger.logAppSwitch(fromPackage, toPackage)

        // Check if we should force a periodic clearing of challenges
        checkForPeriodicClearing()

        // Skip processing if switching to the same app
        if (fromPackage == toPackage) {
            Log.d(TAG, "Same app switch - no action needed")
            return
        }

        // Special home screen handling - end sessions when going home
        if (isHomeScreen(toPackage)) {
            Log.d(TAG, "Going to home screen - ending all active sessions")
            // When going to home screen, end all active app sessions
            completedChallenges.clear()
            lastClearTime = System.currentTimeMillis()
            return
        }

        // End session for previous app
        if (fromPackage != null) {
            Log.d(TAG, "Ending session for app we're leaving: $fromPackage")
            endSession(fromPackage)
        }

        // IMPORTANT FIX: If the destination app is locked, ALWAYS require a new challenge
        if (lockedApps.contains(toPackage)) {
            Log.d(TAG, "Ensuring a fresh challenge for locked app: $toPackage")
            endSession(toPackage)
        }
    }

    // Add a helper method to identify home screen packages
    private fun isHomeScreen(packageName: String): Boolean {
        return packageName.contains("launcher") ||
                packageName.contains("home") ||
                packageName == "com.google.android.apps.nexuslauncher" ||
                packageName == "com.android.launcher"
    }
    /**
     * Retrieves list of completed challenges.
     *
     * @return List of package names with completed challenges
     */
    fun getCompletedChallenges(): List<String> = completedChallenges.keys.toList()
}