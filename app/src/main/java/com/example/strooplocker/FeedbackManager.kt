package com.example.strooplocker

import android.content.Context
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.View
import android.view.animation.AnimationUtils

/**
 * FeedbackManager handles feedback for wrong answers in the Stroop challenge.
 *
 * Feedback types:
 * - Screen shake animation (always plays when view is provided)
 * - Vibration (only if settingsManager.vibrationEnabled is true)
 * - Sound (only if settingsManager.soundEnabled is true)
 *
 * @param context Application context for accessing system services and resources
 * @param settingsManager Settings manager for checking vibration/sound preferences
 */
class FeedbackManager(
    private val context: Context,
    private val settingsManager: SettingsManager
) {
    private val vibrator: Vibrator? = getVibrator()

    private fun getVibrator(): Vibrator? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    /**
     * Plays wrong answer feedback based on current settings.
     *
     * - Screen shake always plays if view is provided
     * - Vibration only plays if vibrationEnabled setting is true
     * - Sound only plays if soundEnabled setting is true
     *
     * @param view The view to apply shake animation to, or null to skip animation
     */
    fun playWrongAnswerFeedback(view: View?) {
        // Screen shake - always plays if view is provided
        view?.let { playShakeAnimation(it) }

        // Vibration - only if enabled in settings
        if (settingsManager.vibrationEnabled) {
            playVibration()
        }

        // Sound - only if enabled in settings
        if (settingsManager.soundEnabled) {
            playSound()
        }
    }

    private fun playShakeAnimation(view: View) {
        try {
            val shakeAnimation = AnimationUtils.loadAnimation(context, R.anim.shake)
            view.startAnimation(shakeAnimation)
        } catch (e: Exception) {
            // Animation resource not found or other error - fail silently
        }
    }

    private fun playVibration() {
        vibrator?.let { vib ->
            try {
                if (!vib.hasVibrator()) return

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val vibrationEffect = VibrationEffect.createOneShot(
                        VIBRATION_DURATION_MS,
                        VibrationEffect.DEFAULT_AMPLITUDE
                    )
                    vib.vibrate(vibrationEffect)
                } else {
                    @Suppress("DEPRECATION")
                    vib.vibrate(VIBRATION_DURATION_MS)
                }
            } catch (e: Exception) {
                // Vibration failed - fail silently
            }
        }
    }

    private fun playSound() {
        var mediaPlayer: MediaPlayer? = null
        try {
            val notificationUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            mediaPlayer = MediaPlayer.create(context, notificationUri)
            mediaPlayer?.apply {
                setOnCompletionListener { release() }
                start()
            }
        } catch (e: Exception) {
            mediaPlayer?.release()
        }
    }

    companion object {
        private const val VIBRATION_DURATION_MS = 200L

        @Volatile
        private var instance: FeedbackManager? = null

        /**
         * Gets the singleton instance of FeedbackManager.
         *
         * @param context Application context
         * @param settingsManager Settings manager instance
         * @return FeedbackManager instance
         */
        fun getInstance(context: Context, settingsManager: SettingsManager): FeedbackManager {
            synchronized(this) {
                return instance ?: FeedbackManager(
                    context.applicationContext,
                    settingsManager
                ).also { instance = it }
            }
        }

        /**
         * Resets the singleton instance. For testing purposes only.
         */
        @Suppress("TestOnly")
        fun resetInstance() {
            instance = null
        }
    }
}
