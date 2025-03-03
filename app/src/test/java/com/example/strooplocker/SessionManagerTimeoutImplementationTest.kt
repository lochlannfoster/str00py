package com.example.strooplocker

import android.os.Handler
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.MockitoJUnitRunner
import org.junit.Assert.*
import java.util.concurrent.atomic.AtomicReference

/**
 * Implementation tests for the session timeout mechanism in SessionManager
 *
 * These tests focus on the internal implementation details of the timeout
 * mechanism to ensure it works correctly.
 */
@RunWith(MockitoJUnitRunner::class)
class SessionManagerTimeoutImplementationTest {

    private val TEST_PACKAGE = "com.test.app"

    @Mock
    private lateinit var mockHandler: Handler

    @Before
    fun setup() {
        // Mock handler behavior
        `when`(mockHandler.postDelayed(any(), anyLong())).thenReturn(true)
        doNothing().`when`(mockHandler).removeCallbacksAndMessages(null)

        // Replace handler in SessionManager
        SessionManager.replaceHandlerForTesting(mockHandler)

        // Reset SessionManager state
        SessionManager.endAllSessions()
    }

    /**
     * Helper to get direct access to completed challenges map
     */
    private fun getCompletedChallengesMap(): MutableMap<String, Long> {
        val completedChallengesField = SessionManager::class.java.getDeclaredField("completedChallenges")
        completedChallengesField.isAccessible = true
        return completedChallengesField.get(SessionManager) as MutableMap<String, Long>
    }

    @Test
    fun completingChallenge_storesCurrentTimestamp() {
        // Arrange
        val before = System.currentTimeMillis()

        // Act
        SessionManager.startChallenge(TEST_PACKAGE)
        SessionManager.completeChallenge(TEST_PACKAGE)

        // Assert
        val after = System.currentTimeMillis()
        val map = getCompletedChallengesMap()
        val timestamp = map[TEST_PACKAGE]

        assertNotNull("Timestamp should be stored", timestamp)
        assertTrue("Timestamp should be current time",
            timestamp!! >= before && timestamp <= after)
    }

    @Test
    fun isChallengeCompleted_checksAgainstTimeout() {
        // Get the SESSION_TIMEOUT_MS value via reflection for testing
        val timeoutField = SessionManager::class.java.getDeclaredField("SESSION_TIMEOUT_MS")
        timeoutField.isAccessible = true
        val timeout = timeoutField.get(null) as Long

        // Arrange - first, complete a challenge
        SessionManager.startChallenge(TEST_PACKAGE)
        SessionManager.completeChallenge(TEST_PACKAGE)

        // Now manipulate the timestamp directly
        val map = getCompletedChallengesMap()

        // Test case 1: Just within timeout (timeout - a small buffer)
        map[TEST_PACKAGE] = System.currentTimeMillis() - (timeout - 500)
        assertTrue("Challenge just within timeout should be valid",
            SessionManager.isChallengeCompleted(TEST_PACKAGE))

        // Test case 2: Exactly at timeout
        map[TEST_PACKAGE] = System.currentTimeMillis() - timeout
        assertFalse("Challenge exactly at timeout should be invalid",
            SessionManager.isChallengeCompleted(TEST_PACKAGE))

        // Test case 3: Just beyond timeout
        map.clear() // Reset since previous check might have removed it
        map[TEST_PACKAGE] = System.currentTimeMillis() - (timeout + 100)
        assertFalse("Challenge just beyond timeout should be invalid",
            SessionManager.isChallengeCompleted(TEST_PACKAGE))
        assertEquals("Map should be empty after removing expired challenge",
            0, map.size)
    }

    @Test
    fun expiredChallenges_areRemovedFromMap() {
        // Arrange - complete challenge and set expired timestamp
        SessionManager.startChallenge(TEST_PACKAGE)
        SessionManager.completeChallenge(TEST_PACKAGE)

        val map = getCompletedChallengesMap()
        map[TEST_PACKAGE] = System.currentTimeMillis() - 10000 // 10 seconds ago (beyond 5s timeout)

        // Act - this should remove the expired challenge
        val isCompleted = SessionManager.isChallengeCompleted(TEST_PACKAGE)

        // Assert
        assertFalse("Expired challenge should return false", isCompleted)
        assertTrue("Expired challenge should be removed from map", map.isEmpty())
    }

    @Test
    fun multipleTimeouts_handleCorrectly() {
        // Arrange - complete multiple app challenges
        val apps = listOf("app1", "app2", "app3", "app4")

        // Complete challenges for all apps
        apps.forEach {
            SessionManager.startChallenge(it)
            SessionManager.completeChallenge(it)
        }

        // Set different timestamps
        val map = getCompletedChallengesMap()
        val now = System.currentTimeMillis()

        // app1: 2 seconds ago (valid)
        map["app1"] = now - 2000

        // app2: 7 seconds ago (expired)
        map["app2"] = now - 7000

        // app3: 3 seconds ago (valid)
        map["app3"] = now - 3000

        // app4: 8 seconds ago (expired)
        map["app4"] = now - 8000

        // Act & Assert

        // Check app1 (should be valid)
        assertTrue("app1 should be valid", SessionManager.isChallengeCompleted("app1"))

        // Check app2 (should be expired and removed)
        assertFalse("app2 should be expired", SessionManager.isChallengeCompleted("app2"))
        assertFalse("app2 should be removed from map", map.containsKey("app2"))

        // Check app3 (should be valid)
        assertTrue("app3 should be valid", SessionManager.isChallengeCompleted("app3"))

        // Check app4 (should be expired and removed)
        assertFalse("app4 should be expired", SessionManager.isChallengeCompleted("app4"))
        assertFalse("app4 should be removed from map", map.containsKey("app4"))

        // Map should only contain app1 and app3
        assertEquals("Map should only contain valid challenges", 2, map.size)
        assertTrue("Map should contain app1", map.containsKey("app1"))
        assertTrue("Map should contain app3", map.containsKey("app3"))
    }
}