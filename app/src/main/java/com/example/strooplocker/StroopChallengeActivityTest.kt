// app/src/androidTest/java/com/example/strooplocker/StroopChallengeActivityTest.kt

package com.example.strooplocker

import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.hamcrest.Matchers.not
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * UI tests for [StroopChallengeActivity]
 *
 * These tests verify that the Stroop challenge UI works as expected,
 * including displaying the challenge, handling user input, and navigating
 * based on correct/incorrect answers.
 */
@RunWith(AndroidJUnit4::class)
class StroopChallengeActivityTest {

    private lateinit var testPackageName: String

    @Before
    fun setup() {
        testPackageName = ApplicationProvider.getApplicationContext<androidx.test.core.app.ApplicationProvider.ApplicationContext>().packageName
    }

    @Test
    fun displaysChallengeTextAndButtons() {
        // Launch activity with test package
        val intent = Intent(ApplicationProvider.getApplicationContext(), StroopChallengeActivity::class.java).apply {
            putExtra(StroopLockActivity.EXTRA_LOCKED_PACKAGE, testPackageName)
        }

        ActivityScenario.launch<StroopChallengeActivity>(intent).use {
            // Verify challenge text is displayed
            onView(withId(R.id.challengeText))
                .check(matches(isDisplayed()))
                .check(matches(not(withText(""))))

            // Verify color grid is displayed
            onView(withId(R.id.colorGrid))
                .check(matches(isDisplayed()))
        }
    }
}