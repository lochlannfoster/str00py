package com.example.strooplocker

import android.content.SharedPreferences
import android.os.Bundle
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.example.strooplocker.databinding.ActivitySettingsBinding

/**
 * SettingsActivity allows the user to customize preferences such as language and background color.
 *
 * Design choices:
 * - The activity follows the system theme.
 * - It uses SharedPreferences to load and save settings.
 * - View binding connects UI components declared in activity_settings.xml.
 * - Spinners are used to provide selectable options, and a save button commits the choices.
 */
class SettingsActivity : AppCompatActivity() {

    // View binding variable for accessing the layout views.
    private lateinit var binding: ActivitySettingsBinding
    // SharedPreferences instance for saving and loading settings.
    private lateinit var preferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        // Adopt the system theme for light/dark mode.
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        super.onCreate(savedInstanceState)

        // Inflate the layout using view binding.
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize SharedPreferences.
        preferences = getSharedPreferences("stroopLockerPrefs", MODE_PRIVATE)

        // Set up UI elements and load existing preferences.
        setupUI()
    }

    private fun setupUI() {
        // Set up a spinner for language selection.
        val languages = listOf("English", "Spanish", "French", "German")
        val languageAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, languages)
        languageAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.languageSpinner.adapter = languageAdapter

        // Load saved language (if any) and update the spinner.
        val savedLanguage = preferences.getString("language", "English")
        val languagePosition = languages.indexOf(savedLanguage)
        if (languagePosition >= 0) {
            binding.languageSpinner.setSelection(languagePosition)
        }

        // Set up a spinner for background color selection.
        val backgroundColors = listOf("Black", "White", "Gray", "Blue", "Red")
        val colorAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, backgroundColors)
        colorAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.backgroundSpinner.adapter = colorAdapter

        // Load saved background color (if any) and update the spinner.
        val savedColor = preferences.getString("background_color", "Black")
        val colorPosition = backgroundColors.indexOf(savedColor)
        if (colorPosition >= 0) {
            binding.backgroundSpinner.setSelection(colorPosition)
        }

        // Set up the save button to store preferences.
        binding.saveButton.setOnClickListener {
            savePreferences()
        }
    }

    private fun savePreferences() {
        val selectedLanguage = binding.languageSpinner.selectedItem as String
        val selectedBackground = binding.backgroundSpinner.selectedItem as String

        // Save the selected preferences.
        preferences.edit().apply {
            putString("language", selectedLanguage)
            putString("background_color", selectedBackground)
            apply()
        }

        // Provide feedback to the user.
        binding.statusTextView.text = "Preferences saved successfully!"
    }
}
