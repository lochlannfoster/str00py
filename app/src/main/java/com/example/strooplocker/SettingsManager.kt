package com.example.strooplocker

import android.content.Context
import android.content.SharedPreferences

/**
 * SettingsManager manages app preferences using SharedPreferences.
 *
 * This class provides centralized access to all app settings with
 * sensible defaults. Settings are persisted across app restarts.
 *
 * Default values:
 * - challengeTimeout: 0 (infinite - no timeout)
 * - sessionDuration: 0 (until user leaves the app)
 * - soundEnabled: false
 * - vibrationEnabled: false
 * - challengesRequired: 1
 * - theme: "system"
 * - masterDisable: false
 */
class SettingsManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * Challenge timeout in seconds. 0 means infinite (no timeout).
     */
    var challengeTimeout: Int
        get() = prefs.getInt(KEY_CHALLENGE_TIMEOUT, DEFAULT_CHALLENGE_TIMEOUT)
        set(value) = prefs.edit().putInt(KEY_CHALLENGE_TIMEOUT, value).apply()

    /**
     * Session duration in seconds. 0 means session lasts until user leaves the app.
     */
    var sessionDuration: Int
        get() = prefs.getInt(KEY_SESSION_DURATION, DEFAULT_SESSION_DURATION)
        set(value) = prefs.edit().putInt(KEY_SESSION_DURATION, value).apply()

    /**
     * Whether sound feedback is enabled.
     */
    var soundEnabled: Boolean
        get() = prefs.getBoolean(KEY_SOUND_ENABLED, DEFAULT_SOUND_ENABLED)
        set(value) = prefs.edit().putBoolean(KEY_SOUND_ENABLED, value).apply()

    /**
     * Whether vibration feedback is enabled.
     */
    var vibrationEnabled: Boolean
        get() = prefs.getBoolean(KEY_VIBRATION_ENABLED, DEFAULT_VIBRATION_ENABLED)
        set(value) = prefs.edit().putBoolean(KEY_VIBRATION_ENABLED, value).apply()

    /**
     * Number of challenges required to unlock an app.
     * Must be at least 1.
     */
    var challengesRequired: Int
        get() = prefs.getInt(KEY_CHALLENGES_REQUIRED, DEFAULT_CHALLENGES_REQUIRED)
        set(value) {
            require(value >= 1) { "challengesRequired must be at least 1" }
            prefs.edit().putInt(KEY_CHALLENGES_REQUIRED, value).apply()
        }

    /**
     * App theme. Valid values: "system", "light", "dark".
     */
    var theme: String
        get() = prefs.getString(KEY_THEME, DEFAULT_THEME) ?: DEFAULT_THEME
        set(value) {
            require(value in VALID_THEMES) { "theme must be one of: ${VALID_THEMES.joinToString()}" }
            prefs.edit().putString(KEY_THEME, value).apply()
        }

    /**
     * Master disable switch. When true, all app locking is disabled.
     */
    var masterDisable: Boolean
        get() = prefs.getBoolean(KEY_MASTER_DISABLE, DEFAULT_MASTER_DISABLE)
        set(value) = prefs.edit().putBoolean(KEY_MASTER_DISABLE, value).apply()

    companion object {
        private const val PREFS_NAME = "str00py_settings"

        // Keys
        private const val KEY_CHALLENGE_TIMEOUT = "challenge_timeout"
        private const val KEY_SESSION_DURATION = "session_duration"
        private const val KEY_SOUND_ENABLED = "sound_enabled"
        private const val KEY_VIBRATION_ENABLED = "vibration_enabled"
        private const val KEY_CHALLENGES_REQUIRED = "challenges_required"
        private const val KEY_THEME = "theme"
        private const val KEY_MASTER_DISABLE = "master_disable"

        // Defaults
        private const val DEFAULT_CHALLENGE_TIMEOUT = 0
        private const val DEFAULT_SESSION_DURATION = 0
        private const val DEFAULT_SOUND_ENABLED = false
        private const val DEFAULT_VIBRATION_ENABLED = false
        private const val DEFAULT_CHALLENGES_REQUIRED = 1
        private const val DEFAULT_THEME = "system"
        private const val DEFAULT_MASTER_DISABLE = false

        // Valid theme values
        private val VALID_THEMES = setOf("system", "light", "dark")

        @Volatile
        private var instance: SettingsManager? = null

        /**
         * Gets the singleton instance of SettingsManager.
         *
         * @param context Application context
         * @return SettingsManager instance
         */
        fun getInstance(context: Context): SettingsManager {
            synchronized(this) {
                return instance ?: SettingsManager(context.applicationContext).also { instance = it }
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
