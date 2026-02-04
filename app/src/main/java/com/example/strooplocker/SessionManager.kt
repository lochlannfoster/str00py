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

    // Track completed challenge packages
    private val completedChallenges = mutableSetOf<String>()

    // Track session start times for timeout support
    private val sessionStartTimes = mutableMapOf<String, Long>()

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

        // Add to completed challenges set
        completedChallenges.add(packageName)

        // Record session start time for timeout support
        sessionStartTimes[packageName] = System.currentTimeMillis()

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
     * Checks if a challenge has been completed for the given package.
     *
     * @param packageName The package name to check
     * @return true if the challenge has been completed for this package
     */
    @Synchronized
    fun isChallengeCompleted(packageName: String): Boolean {
        return completedChallenges.contains(packageName)
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
        sessionStartTimes.remove(packageName)
    }

    /**
     * Ends all active sessions.
     */
    @Synchronized
    fun endAllSessions() {
        Log.d(TAG, "Ending all sessions")
        completedChallenges.clear()
        sessionStartTimes.clear()
        resetChallenge()
    }

    /**
     * Handles app switch events to manage sessions.
     *
     * @param fromPackage Previous package
     * @param toPackage New package
     */
    @Synchronized
    fun handleAppSwitch(fromPackage: String?, toPackage: String) {
        Log.d(TAG, "App switch: $fromPackage -> $toPackage")

        // End session for previous app if it's different
        if (fromPackage != null && fromPackage != toPackage) {
            // Always end the session for the app we're leaving
            endSession(fromPackage)

            // Important: If we're switching from one locked app to another,
            // we want to enforce a new challenge
            if (completedChallenges.contains(toPackage)) {
                Log.d(TAG, "Detected switch between locked apps - forcing new challenge")
                completedChallenges.remove(toPackage)
            }
        }
    }

    /**
     * Retrieves list of completed challenges.
     *
     * @return List of package names with completed challenges
     */
    @Synchronized
    fun getCompletedChallenges(): List<String> = completedChallenges.toList()

    /**
     * Gets the session start time for a package.
     *
     * @param packageName The package name to check
     * @return The timestamp when the session started, or null if no session exists
     */
    @Synchronized
    fun getSessionStartTime(packageName: String): Long? = sessionStartTimes[packageName]

    /**
     * Checks if a session has expired based on the given timeout.
     *
     * @param packageName The package name to check
     * @param timeoutMs The timeout in milliseconds (0 means no timeout)
     * @return true if the session has expired, false otherwise
     */
    @Synchronized
    fun isSessionExpired(packageName: String, timeoutMs: Long): Boolean {
        if (timeoutMs <= 0) return false // 0 means no timeout
        val startTime = sessionStartTimes[packageName] ?: return true
        return System.currentTimeMillis() - startTime > timeoutMs
    }

    /**
     * Testing method to set a custom session start time for a package.
     * This allows tests to simulate expired sessions without waiting.
     */
    @Suppress("TestOnly")
    @Synchronized
    fun setSessionStartTimeForTesting(packageName: String, startTime: Long) {
        sessionStartTimes[packageName] = startTime
    }
}