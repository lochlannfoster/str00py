package com.example.strooplocker

import android.os.Handler
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.MockitoJUnitRunner
import org.junit.Assert.*

/**
 * Tests for session timeout scenarios that simulate real user behaviors
 *
 * These tests verify that the session timeout behaves correctly in various
 * app switching and timing scenarios.
 */
@RunWith(MockitoJUnitRunner::class)
class SessionTimeoutScenarioTest {

    private val INSTAGRAM_PACKAGE = "com.instagram.android"
    private val FACEBOOK_PACKAGE = "com.facebook.katana"
    private val HOME_PACKAGE = "com.android.launcher"

    @Mock
    private lateinit var mockHandler: Handler

    // Store the timeout value for testing
    private var sessionTimeoutMs: Long = 0

    @Before
    fun setup() {
        // Mock handler behaviors
        `when`(mockHandler.postDelayed(any(), anyLong())).thenReturn(true)
        doNothing().`when`(mockHandler).removeCallbacksAndMessages(null)

        // Replace handler in SessionManager
        SessionManager.replaceHandlerForTesting(mockHandler)

        // Reset SessionManager state
        SessionManager.endAllSessions()

        // Get the SESSION_TIMEOUT_MS value via reflection for testing
        val timeoutField = SessionManager::class.java.getDeclaredField("SESSION_TIMEOUT_MS")
        timeoutField.isAccessible = true
        sessionTimeoutMs = timeoutField.get(null) as Long
    }

    /**
     * Helper to get direct access to completed challenges map
     */
    private fun getCompletedChallengesMap(): MutableMap<String, Long> {
        val completedChallengesField = SessionManager::class.java.getDeclaredField("completedChallenges")
        completedChallengesField.isAccessible = true
        return completedChallengesField.get(SessionManager) as MutableMap<String, Long>
    }

    /**
     * Helper to manipulate the system time for a specific package
     */
    private fun setCompletionTime(packageName: String, timeOffsetMs: Long) {
        val map = getCompletedChallengesMap()
        map[packageName] = System.currentTimeMillis() + timeOffsetMs
    }

    @Test
    fun returningToAppQuickly_ShouldNotRequireNewChallenge() {
        // Scenario: Open Instagram, go home, quickly return to Instagram

        // 1. Complete a challenge for Instagram
        SessionManager.startChallenge(INSTAGRAM_PACKAGE)
        SessionManager.completeChallenge(INSTAGRAM_PACKAGE)

        // 2. Simulate going to home screen and returning quickly (2 seconds later)
        setCompletionTime(INSTAGRAM_PACKAGE, -2000)  // 2 seconds ago

        // 3. Check if Instagram needs a new challenge
        assertTrue("App should still be unlocked when returning quickly",
            SessionManager.isChallengeCompleted(INSTAGRAM_PACKAGE))
    }

    @Test
    fun returningToAppAfterTimeout_ShouldRequireNewChallenge() {
        // Scenario: Open Instagram, go home, return after timeout (>5 seconds)

        // 1. Complete a challenge for Instagram
        SessionManager.startChallenge(INSTAGRAM_PACKAGE)
        SessionManager.completeChallenge(INSTAGRAM_PACKAGE)

        // 2. Simulate going to home screen and returning after timeout
        setCompletionTime(INSTAGRAM_PACKAGE, -(sessionTimeoutMs + 1000))  // 6 seconds ago

        // 3. Check if Instagram needs a new challenge
        assertFalse("App should require new challenge after timeout",
            SessionManager.isChallengeCompleted(INSTAGRAM_PACKAGE))
    }

    @Test
    fun switchingBetweenLockedApps_ShouldRequireNewChallengeForEach() {
        // Scenario: Complete Instagram challenge, then open Facebook

        // 1. Complete a challenge for Instagram
        SessionManager.startChallenge(INSTAGRAM_PACKAGE)
        SessionManager.completeChallenge(INSTAGRAM_PACKAGE)

        // 2. Verify Instagram challenge is completed
        assertTrue("Instagram should be unlocked",
            SessionManager.isChallengeCompleted(INSTAGRAM_PACKAGE))

        // 3. Simulate app switch from Instagram to Facebook
        SessionManager.handleAppSwitch(INSTAGRAM_PACKAGE, FACEBOOK_PACKAGE)

        // 4. Verify Facebook requires a challenge
        assertFalse("Switching to Facebook should require a new challenge",
            SessionManager.isChallengeCompleted(FACEBOOK_PACKAGE))

        // 5. Verify Instagram is now locked again due to switching
        assertFalse("Instagram should be locked again after switching away",
            SessionManager.isChallengeCompleted(INSTAGRAM_PACKAGE))
    }

    @Test
    fun appSwitchingWithHomeInBetween_ShouldEndSessions() {
        // Scenario: Instagram → Home → Facebook

        // 1. Complete a challenge for Instagram
        SessionManager.startChallenge(INSTAGRAM_PACKAGE)
        SessionManager.completeChallenge(INSTAGRAM_PACKAGE)

        // 2. Go to home screen
        SessionManager.handleAppSwitch(INSTAGRAM_PACKAGE, HOME_PACKAGE)

        // 3. Check that Instagram session was ended
        assertFalse("Going to home should end Instagram session",
            SessionManager.isChallengeCompleted(INSTAGRAM_PACKAGE))

        // 4. Go to Facebook - should need a challenge
        assertFalse("Facebook should need a challenge",
            SessionManager.isChallengeCompleted(FACEBOOK_PACKAGE))
    }

    @Test
    fun backgroundingApp_ShouldRespectTimeout() {
        // Scenario: Open Instagram, background app, return within timeout

        // 1. Complete a challenge for Instagram
        SessionManager.startChallenge(INSTAGRAM_PACKAGE)
        SessionManager.completeChallenge(INSTAGRAM_PACKAGE)

        // 2. Simulate backgrounding app but not switching (set time 3 seconds ago)
        setCompletionTime(INSTAGRAM_PACKAGE, -3000)

        // 3. Check if still unlocked
        assertTrue("App should stay unlocked when backgrounded within timeout",
            SessionManager.isChallengeCompleted(INSTAGRAM_PACKAGE))

        // 4. Now simulate timeout passing (set time 6 seconds ago)
        setCompletionTime(INSTAGRAM_PACKAGE, -(sessionTimeoutMs + 1000))

        // 5. Check that it's now locked
        assertFalse("App should lock after timeout even when just backgrounded",
            SessionManager.isChallengeCompleted(INSTAGRAM_PACKAGE))
    }

    @Test
    fun multipleRapidAppSwitches_ShouldWorkCorrectly() {
        // Scenario: Rapidly switch between multiple apps

        // 1. Instagram → completed challenge
        SessionManager.startChallenge(INSTAGRAM_PACKAGE)
        SessionManager.completeChallenge(INSTAGRAM_PACKAGE)

        // 2. Instagram → Facebook
        SessionManager.handleAppSwitch(INSTAGRAM_PACKAGE, FACEBOOK_PACKAGE)

        // 3. Facebook → completed challenge
        SessionManager.startChallenge(FACEBOOK_PACKAGE)
        SessionManager.completeChallenge(FACEBOOK_PACKAGE)

        // 4. Facebook → Instagram
        SessionManager.handleAppSwitch(FACEBOOK_PACKAGE, INSTAGRAM_PACKAGE)

        // 5. Check Instagram needs challenge
        assertFalse("Instagram should need a new challenge",
            SessionManager.isChallengeCompleted(INSTAGRAM_PACKAGE))

        // 6. Instagram → completed challenge
        SessionManager.startChallenge(INSTAGRAM_PACKAGE)
        SessionManager.completeChallenge(INSTAGRAM_PACKAGE)

        // 7. Quick return to Instagram (within 5s)
        setCompletionTime(INSTAGRAM_PACKAGE, -2000)
        assertTrue("Quick return to Instagram should not need a challenge",
            SessionManager.isChallengeCompleted(INSTAGRAM_PACKAGE))
    }

    @Test
    fun edgeCaseZeroTimeSwitch_ShouldWorkCorrectly() {
        // Testing edge case: extremely fast app switching

        // 1. Complete Instagram challenge
        SessionManager.startChallenge(INSTAGRAM_PACKAGE)
        SessionManager.completeChallenge(INSTAGRAM_PACKAGE)

        // 2. Instant app switch Instagram → Home → Instagram
        SessionManager.handleAppSwitch(INSTAGRAM_PACKAGE, HOME_PACKAGE)
        SessionManager.handleAppSwitch(HOME_PACKAGE, INSTAGRAM_PACKAGE)

        // Should require new challenge as handleAppSwitch removes completed status
        assertFalse("Instagram should need challenge after Home→Instagram switch",
            SessionManager.isChallengeCompleted(INSTAGRAM_PACKAGE))
    }
}