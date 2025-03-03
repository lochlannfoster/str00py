package com.example.strooplocker

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * UI tests for [StroopLockActivity]
 *
 * These tests verify that the Stroop lock UI works as expected.
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class StroopLockActivityTest {

    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()

        // Set first launch flag to false to skip welcome dialogs
        val prefs = context.getSharedPreferences("stroop_prefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("is_first_launch", false).apply()

        // Reset any session state to ensure tests start from a clean state
        SessionManager.endAllSessions()
    }

    @Test
    fun mainActivity_launches_successfully() {
        // Launch the main activity with test flags to bypass dialogs
        val intent = Intent(context, StroopLockActivity::class.java)
        intent.putExtra("test_mode", true) // We'll add handling for this in the activity

        ActivityScenario.launch<StroopLockActivity>(intent).use { scenario ->
            // Verify that key UI elements are displayed
            onView(withId(R.id.challengeText))
                .check(matches(isDisplayed()))

            onView(withId(R.id.exitButton))
                .check(matches(isDisplayed()))

            onView(withId(R.id.selectAppButton))
                .check(matches(isDisplayed()))
        }
    }
}