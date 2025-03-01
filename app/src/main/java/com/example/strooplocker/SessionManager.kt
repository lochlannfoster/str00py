// app/src/main/java/com/example/strooplocker/SessionManager.kt
package com.example.strooplocker

import android.util.Log

/**
 * Manages app sessions and challenge states across the application.
 * Acts as a single source of truth for tracking which apps have been unlocked.
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
    }

    /**
     * Check if a package has completed its challenge in this session.
     * @param packageName The package to check
     * @return true if the package has completed its challenge
     */
    fun isChallengeCompleted(packageName: String): Boolean {
        return completedChallenges.contains(packageName)
    }

    /**
     * Check if a challenge is currently in progress.
     * @return true if a challenge is in progress
     */
    fun isChallengeInProgress(): Boolean {
        return challengeInProgress
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
     * End all active sessions, requiring all apps to complete new challenges.
     */
    @Synchronized
    fun endAllSessions() {
        Log.d(TAG, "Ending all sessions")
        completedChallenges.clear()
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
}