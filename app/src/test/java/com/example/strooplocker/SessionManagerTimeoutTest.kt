package com.example.strooplocker

import android.os.Handler
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.MockitoJUnitRunner
import org.junit.Assert.*

@RunWith(MockitoJUnitRunner::class)
class SessionManagerTimeoutTest {

    private val testPackage = "com.example.testapp"

    @Mock
    private lateinit var mockHandler: Handler

    @Before
    fun setup() {
        // Setup mock handler correctly for non-void methods
        // postDelayed returns boolean, so use when/thenReturn instead of doNothing
        `when`(mockHandler.postDelayed(any(), anyLong())).thenReturn(true)

        // For removeCallbacksAndMessages, which is void, we can use doNothing
        doNothing().`when`(mockHandler).removeCallbacksAndMessages(null)

        // Replace handler in SessionManager
        SessionManager.replaceHandlerForTesting(mockHandler)

        // Reset session manager state directly
        setSessionManagerState(false, null)
    }

    // Helper method to bypass SessionManager's normal methods and set state directly
    private fun setSessionManagerState(inProgress: Boolean, packageName: String?) {
        val challengeInProgressField = SessionManager::class.java.getDeclaredField("challengeInProgress")
        challengeInProgressField.isAccessible = true
        challengeInProgressField.set(SessionManager, inProgress)

        val currentChallengePackageField = SessionManager::class.java.getDeclaredField("currentChallengePackage")
        currentChallengePackageField.isAccessible = true
        currentChallengePackageField.set(SessionManager, packageName)

        val completedChallengesField = SessionManager::class.java.getDeclaredField("completedChallenges")
        completedChallengesField.isAccessible = true
        val completedChallenges = completedChallengesField.get(SessionManager) as MutableMap<String, Long>
        completedChallenges.clear()
    }

    @Test
    fun startChallenge_setsTimeoutHandler() {
        // Act
        SessionManager.startChallenge(testPackage)

        // Assert
        verify(mockHandler).removeCallbacksAndMessages(null)
        verify(mockHandler).postDelayed(any(), eq(30000L))
    }

    @Test
    fun completeChallenge_cancelsTimeout() {
        // Arrange
        SessionManager.startChallenge(testPackage)

        // Reset invocation count after start to get clean counts
        clearInvocations(mockHandler)

        // Act
        SessionManager.completeChallenge(testPackage)

        // Assert
        verify(mockHandler, times(1)).removeCallbacksAndMessages(null)
    }

    @Test
    fun resetChallenge_cancelsTimeout() {
        // Arrange
        SessionManager.startChallenge(testPackage)

        // Reset invocation count after start to get clean counts
        clearInvocations(mockHandler)

        // Act
        SessionManager.resetChallenge()

        // Assert
        verify(mockHandler, times(1)).removeCallbacksAndMessages(null)
    }

    @Test
    fun resetChallenge_clearsState() {
        // Arrange - set initial state
        setSessionManagerState(true, testPackage)

        // Act
        SessionManager.resetChallenge()

        // Assert
        assertFalse("Challenge should no longer be in progress after reset",
            SessionManager.isChallengeInProgress())
        assertNull("Current challenge package should be null after reset",
            SessionManager.getCurrentChallengePackage())
    }

    @Test
    fun sessionTimeout_expiresChallengeCompletion() {
        // Arrange
        // Get access to the SESSION_TIMEOUT_MS constant via reflection
        val timeoutField = SessionManager::class.java.getDeclaredField("SESSION_TIMEOUT_MS")
        timeoutField.isAccessible = true
        val timeout = timeoutField.get(null) as Long

        // Get access to the completedChallenges map
        val completedChallengesField = SessionManager::class.java.getDeclaredField("completedChallenges")
        completedChallengesField.isAccessible = true
        val completedChallenges = completedChallengesField.get(SessionManager) as MutableMap<String, Long>

        // Manually add a completed challenge with a timestamp just beyond the timeout
        val expiredTime = System.currentTimeMillis() - (timeout + 100)
        completedChallenges[testPackage] = expiredTime

        // Act & Assert
        assertFalse("Challenge should be expired after timeout",
            SessionManager.isChallengeCompleted(testPackage))

        // The map should now be empty as the expired entry should be removed
        assertTrue("Expired challenge should be removed from map",
            completedChallenges.isEmpty())
    }
}