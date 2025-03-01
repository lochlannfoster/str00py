package com.example.strooplocker

import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

/**
 * Unit tests for [SessionManager]
 *
 * These tests verify that the SessionManager correctly manages app sessions,
 * including starting and completing challenges, tracking session state,
 * and handling timeouts.
 */
class SessionManagerTest {

    private val testPackage1 = "com.example.app1"
    private val testPackage2 = "com.example.app2"

    @Before
    fun setup() {
        // Reset the SessionManager state before each test
        SessionManager.endAllSessions()
    }

    @Test
    fun startChallenge_whenNoActiveChallenges_returnsTrue() {
        // Act
        val result = SessionManager.startChallenge(testPackage1)

        // Assert
        assertTrue("Should successfully start a challenge when none is active", result)
        assertTrue("Challenge should be marked as in progress", SessionManager.isChallengeInProgress())
        assertEquals("Current challenge package should match the started one",
            testPackage1, SessionManager.getCurrentChallengePackage())
    }

    @Test
    fun startChallenge_whenChallengeAlreadyInProgress_returnsFalse() {
        // Arrange
        SessionManager.startChallenge(testPackage1)

        // Act
        val result = SessionManager.startChallenge(testPackage2)

        // Assert
        assertFalse("Should not start a new challenge when one is already in progress", result)
        assertEquals("Current challenge package should remain unchanged",
            testPackage1, SessionManager.getCurrentChallengePackage())
    }

    @Test
    fun completeChallenge_addsPackageToCompletedList() {
        // Arrange
        SessionManager.startChallenge(testPackage1)

        // Act
        SessionManager.completeChallenge(testPackage1)

        // Assert
        assertTrue("Package should be marked as completed after challenge",
            SessionManager.isChallengeCompleted(testPackage1))
        assertFalse("Challenge should no longer be in progress",
            SessionManager.isChallengeInProgress())
        assertNull("Current challenge package should be null",
            SessionManager.getCurrentChallengePackage())
    }

    @Test
    fun endSession_removesPackageFromCompletedList() {
        // Arrange
        SessionManager.startChallenge(testPackage1)
        SessionManager.completeChallenge(testPackage1)
        assertTrue(SessionManager.isChallengeCompleted(testPackage1))

        // Act
        SessionManager.endSession(testPackage1)

        // Assert
        assertFalse("Package should no longer be marked as completed",
            SessionManager.isChallengeCompleted(testPackage1))
    }

    @Test
    fun resetChallenge_clearsCurrentChallengeState() {
        // Arrange
        SessionManager.startChallenge(testPackage1)
        assertTrue(SessionManager.isChallengeInProgress())

        // Act
        SessionManager.resetChallenge()

        // Assert
        assertFalse("Challenge should no longer be in progress",
            SessionManager.isChallengeInProgress())
        assertNull("Current challenge package should be null",
            SessionManager.getCurrentChallengePackage())
    }

    @Test
    fun endAllSessions_clearsAllCompletedChallenges() {
        // Arrange
        SessionManager.startChallenge(testPackage1)
        SessionManager.completeChallenge(testPackage1)
        SessionManager.startChallenge(testPackage2)
        SessionManager.completeChallenge(testPackage2)
        assertTrue(SessionManager.isChallengeCompleted(testPackage1))
        assertTrue(SessionManager.isChallengeCompleted(testPackage2))

        // Act
        SessionManager.endAllSessions()

        // Assert
        assertFalse("First package should no longer be marked as completed",
            SessionManager.isChallengeCompleted(testPackage1))
        assertFalse("Second package should no longer be marked as completed",
            SessionManager.isChallengeCompleted(testPackage2))
        assertEquals("Completed challenges list should be empty",
            0, SessionManager.getCompletedChallenges().size)
    }

    @Test
    fun handleAppSwitch_endsSessionForPreviousApp() {
        // Arrange
        SessionManager.startChallenge(testPackage1)
        SessionManager.completeChallenge(testPackage1)
        assertTrue(SessionManager.isChallengeCompleted(testPackage1))

        // Act
        SessionManager.handleAppSwitch(testPackage1, testPackage2)

        // Assert
        assertFalse("Previous app session should be ended",
            SessionManager.isChallengeCompleted(testPackage1))
    }

    @Test
    fun handleAppSwitch_doesNotEndSessionForSameApp() {
        // Arrange
        SessionManager.startChallenge(testPackage1)
        SessionManager.completeChallenge(testPackage1)
        assertTrue(SessionManager.isChallengeCompleted(testPackage1))

        // Act - switching to same app
        SessionManager.handleAppSwitch(testPackage1, testPackage1)

        // Assert
        assertTrue("Session should not end when switching to same app",
            SessionManager.isChallengeCompleted(testPackage1))
    }

    @Test
    fun getCompletedChallenges_returnsCorrectList() {
        // Arrange
        SessionManager.startChallenge(testPackage1)
        SessionManager.completeChallenge(testPackage1)
        SessionManager.startChallenge(testPackage2)
        SessionManager.completeChallenge(testPackage2)

        // Act
        val completedChallenges = SessionManager.getCompletedChallenges()

        // Assert
        assertEquals("Should return 2 completed challenges",
            2, completedChallenges.size)
        assertTrue("List should contain first package",
            completedChallenges.contains(testPackage1))
        assertTrue("List should contain second package",
            completedChallenges.contains(testPackage2))
    }
}