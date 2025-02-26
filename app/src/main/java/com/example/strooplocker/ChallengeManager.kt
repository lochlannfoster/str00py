package com.example.strooplocker

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Manages the state of Stroop challenges throughout the app.
 */
object ChallengeManager {

    data class Challenge(
        val lockedPackage: String,
        val startTime: Long = System.currentTimeMillis()
    )

    private val _currentChallenge = MutableStateFlow<Challenge?>(null)
    val currentChallenge: StateFlow<Challenge?> = _currentChallenge.asStateFlow()

    /**
     * Start a new challenge for the given package
     */
    fun startChallenge(packageName: String): Challenge {
        val challenge = Challenge(packageName)
        _currentChallenge.value = challenge
        return challenge
    }

    /**
     * Get the current challenge (if any)
     */
    fun getCurrentChallenge(): Challenge? = _currentChallenge.value

    /**
     * Mark the current challenge as complete
     * @param success Whether the challenge was completed successfully
     * @return The package name if successful, null otherwise
     */
    fun completeChallenge(success: Boolean): String? {
        val pkg = _currentChallenge.value?.lockedPackage
        _currentChallenge.value = null
        return if (success) pkg else null
    }

    /**
     * Check if a challenge is currently in progress
     */
    fun isChallengeInProgress(): Boolean {
        return _currentChallenge.value != null
    }
}