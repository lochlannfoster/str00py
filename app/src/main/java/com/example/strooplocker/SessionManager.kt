// app/src/main/java/com/example/strooplocker/SessionManager.kt
package com.example.strooplocker

import android.os.Handler
import android.os.Looper
import android.util.Log

/**
 * SessionManager manages app session states and challenge tracking.
 *
 * This singleton provides a centralized way to track app challenge completions,
 * manage session lifecycles, and handle app switching scenarios.
 */
object SessionManager {
    private const val TAG = "SessionManager"

    // Add the missing session timeout constant - 5 seconds
    const val SESSION_TIMEOUT_MS = 5000L

    // Change from a Set to a Map with timestamps to support timeout functionality
    private val completedChallenges = mutableMapOf<String, Long>()

    // Only track if a challenge is currently in progress to prevent overlapping challenges
    @Volatile
    private var challengeInProgress = false

    @Volatile
    private var currentChallengePackage: String? = null

    // Handler for timeouts (needed for testing)
    private var timeoutHandler: Handler = createDefaultHandler()

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
     */
    @Suppress("TestOnly")
    fun replaceHandlerForTesting(handler: Handler) {
        timeoutHandler = handler
    }

    /**
     * Starts a challenge for a specific package.
     *
     * @param packageName Package to challenge
     * @return true if challenge started successfully, false if already in progress
     */
    @Synchronized
    fun startChallenge(packageName: String): Boolean {
        if (challengeInProgress) {
            Log.d(TAG, "Cannot start challenge for $packageName: already in progress")
            return false
        }

        Log.d(TAG, "Starting challenge for $packageName")
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
        challengeInProgress = false
        currentChallengePackage = null

        // Add to completed challenges with the current timestamp
        completedChallenges[packageName] = System.currentTimeMillis()

        timeoutHandler.removeCallbacksAndMessages(null)
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
     * @param packageName The package name to check
     * @return true if the challenge has been completed for this package and is still valid
     */
    fun isChallengeCompleted(packageName: String): Boolean {
        val timestamp = completedChallenges[packageName] ?: return false

        // Check if the timestamp is still valid (within timeout period)
        val now = System.currentTimeMillis()
        val isValid = (now - timestamp) < SESSION_TIMEOUT_MS

        // If expired, remove it and return false
        if (!isValid) {
            completedChallenges.remove(packageName)
            return false
        }

        return true
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
        completedChallenges.remove(packageName)
    }

    /**
     * Ends all active sessions.
     */
    @Synchronized
    fun endAllSessions() {
        Log.d(TAG, "Ending all sessions")
        completedChallenges.clear()
        resetChallenge()
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
        Log.d(TAG, "App switch: $fromPackage -> $toPackage")

        // End session for previous app if it's different
        if (fromPackage != null && fromPackage != toPackage) {
            // Always end the session for the app we're leaving
            endSession(fromPackage)

            // If we're switching directly between two locked apps (not via home),
            // force a new challenge for the destination app
            if (lockedApps.contains(fromPackage) && lockedApps.contains(toPackage)) {
                Log.d(TAG, "Detected direct switch between locked apps - forcing new challenge")
                completedChallenges.remove(toPackage)
            }
        }
    }

    /**
     * Retrieves list of completed challenges.
     *
     * @return List of package names with completed challenges
     */
    fun getCompletedChallenges(): List<String> = completedChallenges.keys.toList()
}