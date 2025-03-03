package com.example.strooplocker

import android.os.Handler
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.MockitoJUnitRunner
import org.junit.Assert.*
import org.junit.After
import java.lang.reflect.Field

/**
 * Targeted tests for SessionManager to diagnose issues with the locking behavior.
 *
 * These tests focus on app switching scenarios and session expiration
 * to identify why locks aren't engaging as expected.
 */
@RunWith(MockitoJUnitRunner::class)
class SessionManagerBehaviorTest {

    // Common app package names for testing
    private val APP1 = "com.example.app1"
    private val APP2 = "com.example.app2"
    private val HOME_SCREEN = "com.android.launcher"

    // Mocked handler for controlling timing in tests
    @Mock
    private lateinit var mockHandler: Handler

    // Fields for accessing internal SessionManager state
    private lateinit var completedChallengesField: Field
    private lateinit var testingModeField: Field
    private lateinit var timeoutHandlerField: Field

    @Before
    fun setup() {
        // Initialize reflection fields for SessionManager
        completedChallengesField = SessionManager::class.java.getDeclaredField("completedChallenges")
        completedChallengesField.isAccessible = true

        testingModeField = SessionManager::class.java.getDeclaredField("testingMode")
        testingModeField.isAccessible = true

        timeoutHandlerField = SessionManager::class.java.getDeclaredField("timeoutHandler")
        timeoutHandlerField.isAccessible = true

        // Configure mock handler
        `when`(mockHandler.postDelayed(any(), anyLong())).thenReturn(true)
        doNothing().`when`(mockHandler).removeCallbacksAndMessages(null)

        // Replace handler in SessionManager
        SessionManager.replaceHandlerForTesting(mockHandler)

        // Clean state before each test
        SessionManager.endAllSessions()
    }

    @After
    fun tearDown() {
        // Clean up after each test
        SessionManager.endAllSessions()
    }

    /**
     * Helper method to directly access completed challenges map
     */
    private fun getCompletedChallenges(): MutableMap<String, Long> {
        return completedChallengesField.get(SessionManager) as MutableMap<String, Long>
    }

    /**
     * Helper to force a specific timestamp for a completed challenge
     */
    private fun setCompletionTime(packageName: String, timeOffsetMs: Long) {
        val map = getCompletedChallenges()
        map[packageName] = System.currentTimeMillis() + timeOffsetMs
    }

    /**
     * Test the exact scenario described in the issue:
     * - Initial challenge works
     * - But subsequent challenges for other locked apps don't appear
     */
    @Test
    fun switchingBetweenLockedApps_ShouldRequireNewChallenges() {
        // Setup: Both apps are locked
        val lockedApps = listOf(APP1, APP2)

        // Step 1: Complete challenge for first app
        SessionManager.startChallenge(APP1)
        SessionManager.completeChallenge(APP1)

        // Verify first app is now unlocked
        assertTrue("First app should be unlocked after completing challenge",
            SessionManager.isChallengeCompleted(APP1))

        // Step 2: Switch to second app
        SessionManager.handleAppSwitch(APP1, APP2, lockedApps)

        // Verify second app needs a challenge
        assertFalse("Second app should require a challenge",
            SessionManager.isChallengeCompleted(APP2))

        // Step 3: Complete challenge for second app
        SessionManager.startChallenge(APP2)
        SessionManager.completeChallenge(APP2)

        // Verify second app is now unlocked
        assertTrue("Second app should be unlocked after completing challenge",
            SessionManager.isChallengeCompleted(APP2))

        // Step 4: Switch back to first app
        SessionManager.handleAppSwitch(APP2, APP1, lockedApps)

        // IMPORTANT: Verify first app requires a new challenge
        assertFalse("First app should require a new challenge after switching back",
            SessionManager.isChallengeCompleted(APP1))
    }

    /**
     * Test reopening a locked app after closing it
     */
    @Test
    fun reopeningLockedApp_ShouldRequireNewChallenge() {
        // Setup: App1 is locked
        val lockedApps = listOf(APP1)

        // Step 1: Complete challenge for the app
        SessionManager.startChallenge(APP1)
        SessionManager.completeChallenge(APP1)

        // Verify app is unlocked
        assertTrue("App should be unlocked after completing challenge",
            SessionManager.isChallengeCompleted(APP1))

        // Step 2: Go to home screen (simulates closing the app)
        SessionManager.handleAppSwitch(APP1, HOME_SCREEN, lockedApps)

        // Step 3: Return to the app
        SessionManager.handleAppSwitch(HOME_SCREEN, APP1, lockedApps)

        // Verify app requires a new challenge
        assertFalse("App should require a new challenge after reopening",
            SessionManager.isChallengeCompleted(APP1))
    }

    /**
     * Test the aggressive auto-expiry mechanism
     */
    @Test
    fun autoExpiry_ExpiresSessionsTooQuickly() {
        // Turn off testing mode to enable real-world auto-expiry
        testingModeField.set(SessionManager, false)

        try {
            // Get access to the real-world timeout value
            val realTimeoutField = SessionManager::class.java.getDeclaredField("REAL_WORLD_TIMEOUT_MS")
            realTimeoutField.isAccessible = true
            val timeout = realTimeoutField.getLong(null)

            println("REAL WORLD TIMEOUT VALUE: $timeout ms")

            // Complete a challenge
            SessionManager.startChallenge(APP1)
            SessionManager.completeChallenge(APP1)

            // Verify auto-expiry is scheduled with the correct timeout
            verify(mockHandler).postDelayed(any(), eq(timeout))

            // We can't easily access the Runnable directly in a unit test
            // So instead, just verify the session is set up correctly, then
            // manually end the session as if the timeout occurred
            assertTrue("Session should be active before timeout",
                SessionManager.isChallengeCompleted(APP1))

            // Manually end the session to simulate the timeout
            SessionManager.endSession(APP1)

            // Verify the session is now expired
            assertFalse("Session should be expired after manual session end",
                SessionManager.isChallengeCompleted(APP1))

            // Print debug info about what's in the completed challenges map
            println("Completed challenges after simulated auto-expiry: ${getCompletedChallenges()}")
        } finally {
            // Restore testing mode
            testingModeField.set(SessionManager, true)
        }
    }

    /**
     * Test that the timeout from SessionManager is applied correctly
     */
    @Test
    fun sessionTimeout_ExpiresAfterTimeoutPeriod() {
        // Step 1: Complete a challenge
        SessionManager.startChallenge(APP1)
        SessionManager.completeChallenge(APP1)

        // Step 2: Set completion time to just before timeout
        val timeoutField = SessionManager::class.java.getDeclaredField("SESSION_TIMEOUT_MS")
        timeoutField.isAccessible = true
        val timeout = timeoutField.getLong(null)

        setCompletionTime(APP1, -(timeout - 100)) // Just within timeout

        // Verify still valid
        assertTrue("Session should still be valid just before timeout",
            SessionManager.isChallengeCompleted(APP1))

        // Step 3: Set completion time to just after timeout
        setCompletionTime(APP1, -(timeout + 100)) // Just past timeout

        // Verify expired
        assertFalse("Session should expire after timeout period",
            SessionManager.isChallengeCompleted(APP1))
    }

    /**
     * Test the real world scenario where the issue was observed
     */
    @Test
    fun realWorldScenario_SwitchingBetweenMultipleLockedApps() {
        // Setup: Create realistic app names
        val instagram = "com.instagram.android"
        val facebook = "com.facebook.katana"
        val tiktok = "com.zhiliaoapp.musically"

        val lockedApps = listOf(instagram, facebook, tiktok)

        // 1. Complete challenge for Instagram
        SessionManager.startChallenge(instagram)
        SessionManager.completeChallenge(instagram)
        assertTrue(SessionManager.isChallengeCompleted(instagram))

        // 2. Switch to Facebook
        SessionManager.handleAppSwitch(instagram, facebook, lockedApps)
        assertFalse("Facebook should require a challenge",
            SessionManager.isChallengeCompleted(facebook))

        // 3. Complete Facebook challenge
        SessionManager.startChallenge(facebook)
        SessionManager.completeChallenge(facebook)
        assertTrue(SessionManager.isChallengeCompleted(facebook))

        // 4. Switch back to Instagram - CRITICAL TEST
        SessionManager.handleAppSwitch(facebook, instagram, lockedApps)
        assertFalse("Instagram should require a new challenge",
            SessionManager.isChallengeCompleted(instagram))

        // 5. Switch to Home then reopen TikTok
        SessionManager.handleAppSwitch(instagram, HOME_SCREEN)
        SessionManager.handleAppSwitch(HOME_SCREEN, tiktok, lockedApps)
        assertFalse("TikTok should require a challenge",
            SessionManager.isChallengeCompleted(tiktok))
    }
}