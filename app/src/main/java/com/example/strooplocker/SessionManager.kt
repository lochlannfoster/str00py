// app/src/main/java/com/example/strooplocker/SessionManager.kt
package com.example.strooplocker

import android.os.Handler
import android.os.Looper
import android.util.Log

/**
 * Manages app sessions and challenge states across the application.
 * Acts as a single source of truth for tracking which apps have been unlocked.
 *
 * This class helps ensure consistent behavior when determining if an app
 * should be locked or allowed to run without a challenge.
 */
object SessionManager {
    private const val TAG = "SessionManager"

    // Set of packages that have completed challenges in this session
    private val completedChallenges = mutableSetOf<String>()

    // Flag to track if a challenge is currently in progress
    @Volatile
    private var challengeInProgress = false

    // The package that's currently being challenged
    @Volatile
    private var currentChallengePackage: String? = null

    // Timeout handler for challenges
    private val timeoutHandler = Handler(Looper.getMainLooper())
    private const val CHALLENGE_TIMEOUT_MS = 30000L // 30 seconds

    /**
     * Start a challenge for the given package.
     * @param packageName The package to challenge
     * @return true if the challenge was started, false if one is already in progress
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
        timeoutHandler.removeCallbacksAndMessages(null) // Remove any existing timeouts
        timeoutHandler.postDelayed({
            if (challengeInProgress && currentChallengePackage == packageName) {
                Log.w(TAG, "Challenge timed out for $packageName")
                resetChallenge()
            }
        }, CHALLENGE_TIMEOUT_MS)

        return true
    }

    /**
     * Mark a challenge as completed, allowing access to the app.
     * @param packageName The package that completed the challenge
     */
    @Synchronized
    fun completeChallenge(packageName: String) {
        Log.d(TAG, "Completing challenge for $packageName")
        completedChallenges.add(packageName)
        challengeInProgress = false
        currentChallengePackage = null
        timeoutHandler.removeCallbacksAndMessages(null) // Cancel timeout
    }

    /**
     * Check if a package has completed its challenge in this session.
     * @param packageName The package to check
     * @return true if the package has completed its challenge
     */
    fun isChallengeCompleted(packageName: String): Boolean {
        val isCompleted = completedChallenges.contains(packageName)
        Log.d(TAG, "Check if $packageName challenge is completed: $isCompleted")
        return isCompleted
    }

    /**
     * Check if a challenge is currently in progress.
     * @return true if a challenge is in progress
     */
    fun isChallengeInProgress(): Boolean {
        return challengeInProgress
    }

    /**
     * Get the package name that's currently being challenged, if any.
     * @return The package name or null if no challenge is in progress
     */
    fun getCurrentChallengePackage(): String? {
        return currentChallengePackage
    }

    /**
     * End a session for a package, requiring it to complete a new challenge next time.
     * @param packageName The package to end the session for
     */
    @Synchronized
    fun endSession(packageName: String) {
        Log.d(TAG, "Ending session for $packageName")
        completedChallenges.remove(packageName)
    }

    /**
     * Reset the current challenge state without completing it.
     */
    @Synchronized
    fun resetChallenge() {
        Log.d(TAG, "Resetting current challenge state")
        challengeInProgress = false
        currentChallengePackage = null
        timeoutHandler.removeCallbacksAndMessages(null)
    }

    /**
     * End all active sessions, requiring all apps to complete new challenges.
     */
    @Synchronized
    fun endAllSessions() {
        Log.d(TAG, "Ending all sessions")
        completedChallenges.clear()
        resetChallenge()
    }

    /**
     * Handle app switch events to manage sessions.
     * @param fromPackage The package being switched from
     * @param toPackage The package being switched to
     */
    @Synchronized
    fun handleAppSwitch(fromPackage: String?, toPackage: String) {
        Log.d(TAG, "App switch: $fromPackage -> $toPackage")

        // End session for previous app if it exists
        if (fromPackage != null && fromPackage != toPackage) {
            endSession(fromPackage)
        }
    }

    /**
     * Get a debug list of all completed challenges
     * @return A list of all packages that have completed challenges
     */
    fun getCompletedChallenges(): List<String> {
        return completedChallenges.toList()
    }
}