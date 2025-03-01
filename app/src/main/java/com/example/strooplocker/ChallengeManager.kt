package com.example.strooplocker

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * ChallengeManager is a singleton that manages the state of Stroop challenges throughout the app.
 *
 * It provides functionality to:
 * - Start a new challenge for a specific package
 * - Check if a challenge is in progress
 * - Complete a challenge and track its success/failure
 *
 * This manager is essential to ensure consistent challenge state across different
 * components of the application.
 */
object ChallengeManager {

    /**
     * Data class representing a single Stroop challenge instance.
     * Contains the package name of the locked app and the time the challenge started.
     *
     * @property lockedPackage The package name of the app being challenged
     * @property startTime Timestamp when the challenge was initiated
     */
    data class Challenge(
        val lockedPackage: String,
        val startTime: Long = System.currentTimeMillis()
    )

    // StateFlow to maintain and observe the current challenge state
    private val _currentChallenge = MutableStateFlow<Challenge?>(null)

    /**
     * Public observable state of the current challenge.
     * Observers can react to changes in challenge state.
     */
    val currentChallenge: StateFlow<Challenge?> = _currentChallenge.asStateFlow()

    /**
     * Starts a new challenge for the specified package.
     *
     * @param packageName The package name of the app to be locked behind a challenge
     * @return The newly created Challenge object
     */
    fun startChallenge(packageName: String): Challenge {
        val challenge = Challenge(packageName)
        _currentChallenge.value = challenge
        return challenge
    }

    /**
     * Gets the current challenge if one exists.
     *
     * @return The current Challenge object or null if no challenge is in progress
     */
    fun getCurrentChallenge(): Challenge? = _currentChallenge.value

    /**
     * Marks the current challenge as complete and clears the current challenge state.
     *
     * @param success Whether the challenge was completed successfully
     * @return The package name of the completed challenge if successful, null otherwise
     */
    fun completeChallenge(success: Boolean): String? {
        val pkg = _currentChallenge.value?.lockedPackage
        _currentChallenge.value = null
        return if (success) pkg else null
    }

    /**
     * Checks if a challenge is currently in progress.
     *
     * @return true if a challenge is in progress, false otherwise
     */
    fun isChallengeInProgress(): Boolean {
        return _currentChallenge.value != null
    }
}