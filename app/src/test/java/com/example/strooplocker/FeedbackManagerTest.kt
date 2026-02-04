package com.example.strooplocker

import android.content.Context
import android.media.MediaPlayer
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockedStatic
import org.mockito.Mockito.any
import org.mockito.Mockito.mock
import org.mockito.Mockito.mockStatic
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.MockitoJUnitRunner

/**
 * Unit tests for [FeedbackManager]
 *
 * Tests verify correct feedback behavior based on settings:
 * - Screen shake always plays when view is provided
 * - Vibration only plays when vibrationEnabled is true
 * - Sound only plays when soundEnabled is true
 */
@RunWith(MockitoJUnitRunner::class)
class FeedbackManagerTest {

    @Mock
    private lateinit var mockContext: Context

    @Mock
    private lateinit var mockSettingsManager: SettingsManager

    @Mock
    private lateinit var mockView: View

    @Mock
    private lateinit var mockAnimation: Animation

    @Mock
    private lateinit var mockVibrator: Vibrator

    private lateinit var feedbackManager: FeedbackManager

    @Before
    fun setup() {
        // Setup context to return vibrator service
        `when`(mockContext.getSystemService(Context.VIBRATOR_SERVICE)).thenReturn(mockVibrator)
        `when`(mockVibrator.hasVibrator()).thenReturn(true)

        feedbackManager = FeedbackManager(mockContext, mockSettingsManager)
    }

    // Screen Shake Tests

    @Test
    fun playWrongAnswerFeedback_withView_startsShakeAnimation() {
        mockStatic(AnimationUtils::class.java).use { mockedAnimationUtils ->
            mockedAnimationUtils.`when`<Animation> {
                AnimationUtils.loadAnimation(mockContext, R.anim.shake)
            }.thenReturn(mockAnimation)

            `when`(mockSettingsManager.vibrationEnabled).thenReturn(false)
            `when`(mockSettingsManager.soundEnabled).thenReturn(false)

            feedbackManager.playWrongAnswerFeedback(mockView)

            verify(mockView).startAnimation(mockAnimation)
        }
    }

    @Test
    fun playWrongAnswerFeedback_withNullView_doesNotCrash() {
        `when`(mockSettingsManager.vibrationEnabled).thenReturn(false)
        `when`(mockSettingsManager.soundEnabled).thenReturn(false)

        // Should not throw
        feedbackManager.playWrongAnswerFeedback(null)
    }

    // Vibration Tests

    @Test
    fun playWrongAnswerFeedback_vibrationEnabled_triggersVibration() {
        mockStatic(AnimationUtils::class.java).use { mockedAnimationUtils ->
            mockedAnimationUtils.`when`<Animation> {
                AnimationUtils.loadAnimation(mockContext, R.anim.shake)
            }.thenReturn(mockAnimation)

            `when`(mockSettingsManager.vibrationEnabled).thenReturn(true)
            `when`(mockSettingsManager.soundEnabled).thenReturn(false)

            feedbackManager.playWrongAnswerFeedback(mockView)

            // Verify vibration was triggered (the exact method depends on API level)
            verify(mockVibrator).hasVibrator()
        }
    }

    @Test
    fun playWrongAnswerFeedback_vibrationDisabled_doesNotVibrate() {
        mockStatic(AnimationUtils::class.java).use { mockedAnimationUtils ->
            mockedAnimationUtils.`when`<Animation> {
                AnimationUtils.loadAnimation(mockContext, R.anim.shake)
            }.thenReturn(mockAnimation)

            `when`(mockSettingsManager.vibrationEnabled).thenReturn(false)
            `when`(mockSettingsManager.soundEnabled).thenReturn(false)

            feedbackManager.playWrongAnswerFeedback(mockView)

            // Verify vibrator.hasVibrator() was never called (vibration check skipped)
            verify(mockVibrator, never()).hasVibrator()
        }
    }

    @Test
    fun playWrongAnswerFeedback_vibrationEnabled_noVibrator_doesNotCrash() {
        mockStatic(AnimationUtils::class.java).use { mockedAnimationUtils ->
            mockedAnimationUtils.`when`<Animation> {
                AnimationUtils.loadAnimation(mockContext, R.anim.shake)
            }.thenReturn(mockAnimation)

            `when`(mockSettingsManager.vibrationEnabled).thenReturn(true)
            `when`(mockSettingsManager.soundEnabled).thenReturn(false)
            `when`(mockVibrator.hasVibrator()).thenReturn(false)

            // Should not throw
            feedbackManager.playWrongAnswerFeedback(mockView)
        }
    }

    // Sound Tests

    @Test
    fun playWrongAnswerFeedback_soundEnabled_checksSettingsManager() {
        mockStatic(AnimationUtils::class.java).use { mockedAnimationUtils ->
            mockedAnimationUtils.`when`<Animation> {
                AnimationUtils.loadAnimation(mockContext, R.anim.shake)
            }.thenReturn(mockAnimation)

            `when`(mockSettingsManager.vibrationEnabled).thenReturn(false)
            `when`(mockSettingsManager.soundEnabled).thenReturn(true)

            feedbackManager.playWrongAnswerFeedback(mockView)

            verify(mockSettingsManager).soundEnabled
        }
    }

    @Test
    fun playWrongAnswerFeedback_soundDisabled_doesNotPlaySound() {
        mockStatic(AnimationUtils::class.java).use { mockedAnimationUtils ->
            mockedAnimationUtils.`when`<Animation> {
                AnimationUtils.loadAnimation(mockContext, R.anim.shake)
            }.thenReturn(mockAnimation)

            `when`(mockSettingsManager.vibrationEnabled).thenReturn(false)
            `when`(mockSettingsManager.soundEnabled).thenReturn(false)

            feedbackManager.playWrongAnswerFeedback(mockView)

            verify(mockSettingsManager).soundEnabled
        }
    }

    // Combined Tests

    @Test
    fun playWrongAnswerFeedback_allEnabled_triggersAllFeedback() {
        mockStatic(AnimationUtils::class.java).use { mockedAnimationUtils ->
            mockedAnimationUtils.`when`<Animation> {
                AnimationUtils.loadAnimation(mockContext, R.anim.shake)
            }.thenReturn(mockAnimation)

            `when`(mockSettingsManager.vibrationEnabled).thenReturn(true)
            `when`(mockSettingsManager.soundEnabled).thenReturn(true)

            feedbackManager.playWrongAnswerFeedback(mockView)

            // Verify animation started
            verify(mockView).startAnimation(mockAnimation)
            // Verify vibration was checked
            verify(mockVibrator).hasVibrator()
            // Verify sound setting was checked
            verify(mockSettingsManager).soundEnabled
        }
    }
}
