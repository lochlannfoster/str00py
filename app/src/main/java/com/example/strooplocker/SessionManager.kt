// app/src/main/java/com/example/strooplocker/SessionManager.kt
package com.example.strooplocker

import android.os.Handler
import android.os.Looper
import android.util.Log

/**
 * SessionManager is a singleton that manages app sessions and challenge states across the application.
 *
 * It serves as the single source of truth for tracking which apps have been unlocked and
 * ensures consistent behavior when determining if an app should be locked or allowed to run
 * without a challenge. This manager coordinates the state between different components of the
 * application to provide a seamless experience.
 *
 * Key features:
 * - Tracks completed challenges for apps
 * - Manages challenge state for in-progress challenges
 * - Handles session timeout and resets
 * - Provides app switching detection and handling
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
     * Starts a challenge for the given package.
     *
     * This method initiates a new challenge state and sets a timeout to automatically
     * reset the challenge if it's not completed within a certain time period.
     *
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
     * Marks a challenge as completed, allowing access to the app.
     *
     * Once a challenge is completed, the app will be accessible without
     * facing another challenge until the session ends.
     *
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
     * Checks if a package has completed its challenge in this session.
     *
     * @param packageName The package to check
     * @return true if the package has completed its challenge, false otherwise
     */
    fun isChallengeCompleted(packageName: String): Boolean {
        val isCompleted = completedChallenges.contains(packageName)
        Log.d(TAG, "Check if $packageName challenge is completed: $isCompleted")
        return isCompleted
    }

    /**
     * Checks if a challenge is currently in progress.
     *
     * @return true if a challenge is in progress, false otherwise
     */
    fun isChallengeInProgress(): Boolean {
        return challengeInProgress
    }

    /**
     * Gets the package name that's currently being challenged, if any.
     *
     * @return The package name or null if no challenge is in progress
     */
    fun getCurrentChallengePackage(): String? {
        return currentChallengePackage
    }

    /**
     * Ends a session for a package, requiring it to complete a new challenge next time.
     *
     * This is typically called when the user switches away from an app or returns to
     * the home screen.
     *
     * @param packageName The package to end the session for
     */
    @Synchronized
    fun endSession(packageName: String) {
        Log.d(TAG, "Ending session for $packageName")
        completedChallenges.remove(packageName)
    }

    /**
     * Resets the current challenge state without completing it.
     *
     * This is used when a challenge needs to be canceled, such as when it times out
     * or when the user force-closes the challenge.
     */
    @Synchronized
    fun resetChallenge() {
        Log.d(TAG, "Resetting current challenge state")
        challengeInProgress = false
        currentChallengePackage = null
        timeoutHandler.removeCallbacksAndMessages(null)
    }

    /**
     * Ends all active sessions, requiring all apps to complete new challenges.
     *
     * This is typically called when the service is started or stopped, or when
     * the device is rebooted.
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
     * When the user switches from one app to another, this method ensures that
     * the session for the previous app is ended properly.
     *
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
     * Gets a debug list of all completed challenges.
     *
     * This is useful for debugging and logging purposes.
     *
     * @return A list of all packages that have completed challenges
     */
    fun getCompletedChallenges(): List<String> {
        return completedChallenges.toList()
    }
}