// app/src/test/java/com/example/strooplocker/ChallengeManagerTest.kt

package com.example.strooplocker

import org.junit.Test
import org.junit.Assert.*

/**
 * Unit tests for [ChallengeManager]
 *
 * These tests verify that challenge state is correctly managed,
 * including starting, completing, and checking challenges.
 */
class ChallengeManagerTest {

    @Test
    fun startChallenge_setsCurrentChallenge() {
        // Arrange
        val packageName = "com.example.app"

        // Act
        val challenge = ChallengeManager.startChallenge(packageName)

        // Assert
        assertEquals(packageName, challenge.lockedPackage)
        assertEquals(challenge, ChallengeManager.getCurrentChallenge())
        assertTrue(ChallengeManager.isChallengeInProgress())
    }

    @Test
    fun completeChallenge_success_returnsPackageName() {
        // Arrange
        val packageName = "com.example.app"
        ChallengeManager.startChallenge(packageName)

        // Act
        val result = ChallengeManager.completeChallenge(true)

        // Assert
        assertEquals(packageName, result)
        assertFalse(ChallengeManager.isChallengeInProgress())
        assertNull(ChallengeManager.getCurrentChallenge())
    }

    @Test
    fun completeChallenge_failure_returnsNull() {
        // Arrange
        val packageName = "com.example.app"
        ChallengeManager.startChallenge(packageName)

        // Act
        val result = ChallengeManager.completeChallenge(false)

        // Assert
        assertNull(result)
        assertFalse(ChallengeManager.isChallengeInProgress())
        assertNull(ChallengeManager.getCurrentChallenge())
    }

    @Test
    fun isChallengeInProgress_noChallengeStarted_returnsFalse() {
        // Arrange - ensure no challenge is in progress
        ChallengeManager.completeChallenge(false)

        // Act & Assert
        assertFalse(ChallengeManager.isChallengeInProgress())
    }
}