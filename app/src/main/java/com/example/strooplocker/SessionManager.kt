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
/**
 * Simplified SessionManager with straightforward logic.
 * Any locked app that's opened will require a challenge, regardless of history.
 */
object SessionManager {
    private const val TAG = "SessionManager"

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
     * Resets the current challenge state.
     */
    @Synchronized
    fun resetChallenge() {
        Log.d(TAG, "Resetting current challenge state")
        challengeInProgress = false
        currentChallengePackage = null
        timeoutHandler.removeCallbacksAndMessages(null)
    }
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
     */
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
    fun getCompletedChallenges(): List<String> = completedChallenges.toList()
}