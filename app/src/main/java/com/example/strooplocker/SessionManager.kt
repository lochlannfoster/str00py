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
    private const val CHALLENGE_TIMEOUT_MS = 30000L // 30 seconds

    // Mutable set to track completed challenges
    private val completedChallenges = mutableSetOf<String>()

    // Flags to track challenge and session states
    @Volatile
    private var challengeInProgress = false

    @Volatile
    private var currentChallengePackage: String? = null

    // Lazy-initialized handler with fallback for testing
    private var timeoutHandler: Handler = createDefaultHandler()

    /**
     * Creates a default Handler using the main Looper.
     * Provides a fallback mechanism for testing scenarios.
     */
    private fun createDefaultHandler(): Handler {
        return try {
            Handler(Looper.getMainLooper())
        } catch (e: Exception) {
            // Fallback for testing environments
            Handler()
        }
    }

    /**
     * Testing method to replace the handler for unit testing purposes.
     * This allows mocking the Handler during test runs.
     *
     * @param handler Replacement Handler for testing
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
        }, CHALLENGE_TIMEOUT_MS)

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
        completedChallenges.add(packageName)
        challengeInProgress = false
        currentChallengePackage = null
        timeoutHandler.removeCallbacksAndMessages(null)

        // Add a longer expiration for completed challenges (e.g., 2 minutes)
        timeoutHandler.postDelayed({
            Log.d(TAG, "Challenge session expired for $packageName")
            completedChallenges.remove(packageName)
        }, 120000L) // 2 minutes
    }

    /**
     * Checks if a package has completed its challenge.
     *
     * @param packageName Package to check
     * @return true if challenge completed, false otherwise
     */
    fun isChallengeCompleted(packageName: String): Boolean {
        val isCompleted = completedChallenges.contains(packageName)
        Log.d(TAG, "Check if $packageName challenge is completed: $isCompleted")
        return isCompleted
    }

    /**
     * Checks if a challenge is currently in progress.
     *
     * @return true if challenge in progress, false otherwise
     */
    fun isChallengeInProgress(): Boolean = challengeInProgress

    /**
     * Gets the currently challenged package.
     *
     * @return Package name of current challenge, or null
     */
    fun getCurrentChallengePackage(): String? = currentChallengePackage

    /**
     * Ends a session for a specific package.
     *
     * @param packageName Package to end session for
     */
    @Synchronized
    fun endSession(packageName: String) {
        Log.d(TAG, "Ending session for $packageName")
        completedChallenges.remove(packageName)
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
    @Synchronized
    fun handleAppSwitch(fromPackage: String?, toPackage: String) {
        Log.d(TAG, "App switch: $fromPackage -> $toPackage")

        // End session for previous app if it's different
        if (fromPackage != null && fromPackage != toPackage) {
            endSession(fromPackage)
        }
    }

    /**
     * Retrieves list of completed challenges.
     *
     * @return List of package names with completed challenges
     */
    fun getCompletedChallenges(): List<String> = completedChallenges.toList()
}