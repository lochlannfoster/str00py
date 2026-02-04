package com.example.strooplocker

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.SwitchCompat

/**
 * SettingsActivity provides a UI for configuring app preferences.
 *
 * All settings are auto-saved when changed using TextWatcher and
 * OnCheckedChangeListener. Theme changes apply immediately.
 *
 * Settings available:
 * - Challenge timeout (seconds, 0 = infinite)
 * - Session duration (seconds, 0 = until you leave)
 * - Challenges required (minimum 1)
 * - Sound on wrong answer (on/off)
 * - Vibration on wrong answer (on/off)
 * - Theme (System Default, Light, Dark)
 * - Master disable (temporarily disable all locking)
 */
class SettingsActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "SettingsActivity"
    }

    private lateinit var settingsManager: SettingsManager

    // UI elements
    private lateinit var challengeTimeoutInput: EditText
    private lateinit var sessionDurationInput: EditText
    private lateinit var challengesRequiredInput: EditText
    private lateinit var soundSwitch: SwitchCompat
    private lateinit var vibrationSwitch: SwitchCompat
    private lateinit var themeSpinner: Spinner
    private lateinit var masterDisableSwitch: SwitchCompat

    // Track if we're loading initial values to avoid triggering saves
    private var isLoadingValues = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        Log.d(TAG, "onCreate: Setting up settings activity")

        settingsManager = SettingsManager.getInstance(this)

        initViews()
        loadCurrentSettings()
        setupListeners()
    }

    /**
     * Initializes view references from the layout.
     */
    private fun initViews() {
        challengeTimeoutInput = findViewById(R.id.challengeTimeoutInput)
        sessionDurationInput = findViewById(R.id.sessionDurationInput)
        challengesRequiredInput = findViewById(R.id.challengesRequiredInput)
        soundSwitch = findViewById(R.id.soundSwitch)
        vibrationSwitch = findViewById(R.id.vibrationSwitch)
        themeSpinner = findViewById(R.id.themeSpinner)
        masterDisableSwitch = findViewById(R.id.masterDisableSwitch)
    }

    /**
     * Loads current settings values into the UI.
     */
    private fun loadCurrentSettings() {
        isLoadingValues = true

        // Number inputs
        challengeTimeoutInput.setText(settingsManager.challengeTimeout.toString())
        sessionDurationInput.setText(settingsManager.sessionDuration.toString())
        challengesRequiredInput.setText(settingsManager.challengesRequired.toString())

        // Switches
        soundSwitch.isChecked = settingsManager.soundEnabled
        vibrationSwitch.isChecked = settingsManager.vibrationEnabled
        masterDisableSwitch.isChecked = settingsManager.masterDisable

        // Theme spinner - map theme value to position
        val themeValues = resources.getStringArray(R.array.theme_values)
        val currentTheme = settingsManager.theme
        val themePosition = themeValues.indexOf(currentTheme).takeIf { it >= 0 } ?: 0
        themeSpinner.setSelection(themePosition)

        Log.d(TAG, "Loaded settings - timeout: ${settingsManager.challengeTimeout}, " +
                "session: ${settingsManager.sessionDuration}, " +
                "challenges: ${settingsManager.challengesRequired}, " +
                "sound: ${settingsManager.soundEnabled}, " +
                "vibration: ${settingsManager.vibrationEnabled}, " +
                "theme: $currentTheme, " +
                "masterDisable: ${settingsManager.masterDisable}")

        isLoadingValues = false
    }

    /**
     * Sets up change listeners for auto-save functionality.
     */
    private fun setupListeners() {
        // Challenge timeout listener
        challengeTimeoutInput.addTextChangedListener(createTextWatcher { text ->
            val value = text.toIntOrNull() ?: 0
            settingsManager.challengeTimeout = value
            Log.d(TAG, "Challenge timeout saved: $value")
        })

        // Session duration listener
        sessionDurationInput.addTextChangedListener(createTextWatcher { text ->
            val value = text.toIntOrNull() ?: 0
            settingsManager.sessionDuration = value
            Log.d(TAG, "Session duration saved: $value")
        })

        // Challenges required listener
        challengesRequiredInput.addTextChangedListener(createTextWatcher { text ->
            val value = text.toIntOrNull()?.coerceAtLeast(1) ?: 1
            // Update display if coerced
            if (text.isNotEmpty() && value != text.toIntOrNull()) {
                challengesRequiredInput.setText(value.toString())
                challengesRequiredInput.setSelection(challengesRequiredInput.text.length)
            }
            settingsManager.challengesRequired = value
            Log.d(TAG, "Challenges required saved: $value")
        })

        // Sound switch listener
        soundSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (!isLoadingValues) {
                settingsManager.soundEnabled = isChecked
                Log.d(TAG, "Sound enabled saved: $isChecked")
            }
        }

        // Vibration switch listener
        vibrationSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (!isLoadingValues) {
                settingsManager.vibrationEnabled = isChecked
                Log.d(TAG, "Vibration enabled saved: $isChecked")
            }
        }

        // Theme spinner listener
        themeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (!isLoadingValues) {
                    val themeValues = resources.getStringArray(R.array.theme_values)
                    val selectedTheme = themeValues.getOrNull(position) ?: "system"
                    settingsManager.theme = selectedTheme
                    applyTheme(selectedTheme)
                    Log.d(TAG, "Theme saved and applied: $selectedTheme")
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Do nothing
            }
        }

        // Master disable switch listener
        masterDisableSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (!isLoadingValues) {
                settingsManager.masterDisable = isChecked
                Log.d(TAG, "Master disable saved: $isChecked")
            }
        }
    }

    /**
     * Creates a TextWatcher that calls the save function after text changes.
     *
     * @param onSave Function to call with the new text value
     */
    private fun createTextWatcher(onSave: (String) -> Unit): TextWatcher {
        return object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                if (!isLoadingValues) {
                    onSave(s?.toString() ?: "")
                }
            }
        }
    }

    /**
     * Applies the selected theme using AppCompatDelegate.
     *
     * @param theme Theme value: "system", "light", or "dark"
     */
    private fun applyTheme(theme: String) {
        val mode = when (theme) {
            "light" -> AppCompatDelegate.MODE_NIGHT_NO
            "dark" -> AppCompatDelegate.MODE_NIGHT_YES
            else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }
        AppCompatDelegate.setDefaultNightMode(mode)
        Log.d(TAG, "Applied theme mode: $mode")
    }
}
