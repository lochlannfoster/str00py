// app/src/main/java/com/example/strooplocker/StroopLockViewModel.kt

package com.example.strooplocker

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.strooplocker.data.LockedAppDatabase
import com.example.strooplocker.data.LockedAppsRepository
import kotlinx.coroutines.launch
import com.example.strooplocker.utils.LoggingUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
@Suppress("UNCHECKED_CAST")

/**
 * ViewModel for the str00py lock functionality.
 *
 * This ViewModel manages and preserves UI-related data across configuration changes.
 * It handles the challenge generation, validation, and locked apps management.
 *
 * The ViewModel separates the UI from the business logic, making the code more
 * maintainable and testable. It uses LiveData for UI updates and coroutines for
 * asynchronous operations.
 */
class StroopLockViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "StroopLockViewModel_DEBUG"
    }

    // Repository for database operations
    private val repository = LockedAppsRepository(
        LockedAppDatabase.getInstance(application).lockedAppDao()
    )

    // Flag to track if locked apps have been loaded
    private var _lockedAppsLoaded = false

    // Color data for the Stroop test
    private val _colorMap = MutableLiveData<Map<String, String>>()

    /**
     * Map of color names to their hex color codes.
     * Used for displaying the Stroop challenge colors.
     */
    val colorMap: LiveData<Map<String, String>> = _colorMap

    // Challenge word and color
    private val _challengeWord = MutableLiveData<String>()

    /**
     * The word displayed in the Stroop challenge.
     * This is the color name that acts as a distractor.
     */
    val challengeWord: LiveData<String> = _challengeWord

    private val _inkColor = MutableLiveData<String>()

    /**
     * The ink color used to display the challenge word.
     * This is the correct answer the user should identify.
     */
    val inkColor: LiveData<String> = _inkColor

    // Button colors and labels
    private val _buttonLabels = MutableLiveData<List<String>>()

    /**
     * The text labels for the color buttons in the challenge.
     */
    val buttonLabels: LiveData<List<String>> = _buttonLabels

    private val _buttonColors = MutableLiveData<List<String>>()

    /**
     * The colors used to display the button labels.
     * These are intentionally mismatched with the labels to create the Stroop effect.
     */
    val buttonColors: LiveData<List<String>> = _buttonColors

    // Expected answer
    private val _expectedAnswer = MutableLiveData<String>()

    /**
     * The correct answer for the current challenge.
     * This is the ink color of the challenge word.
     */
    val expectedAnswer: LiveData<String> = _expectedAnswer

    // Locked apps
    private val _lockedApps = MutableLiveData<List<String>>()

    /**
     * List of package names for apps that are locked.
     */
    val lockedApps: LiveData<List<String>> = _lockedApps

    // Currently selected app to launch
    private val _appToLaunch = MutableLiveData<String?>()

    /**
     * The package name of the app to launch after a successful challenge.
     */
    val appToLaunch: LiveData<String?> = _appToLaunch

    // State Flow for locked apps (alternative to LiveData for modern UI updates)
    private val _lockedAppsFlow = MutableStateFlow<List<String>>(emptyList())

    /**
     * StateFlow of locked app package names.
     * Provides a reactive stream of the locked apps list.
     */
    val lockedAppsFlow: StateFlow<List<String>> = _lockedAppsFlow.asStateFlow()

    /**
     * Initializes the ViewModel by setting up the color map and loading locked apps.
     */
    init {
        LoggingUtil.debug(TAG, "init", "Initializing ViewModel")
        // Initialize color map
        _colorMap.value = mapOf(
            "Red" to "#FF0000",
            "Green" to "#00FF00",
            "Blue" to "#3366FF",
            "Yellow" to "#CCFF33",
            "Pink" to "#FF66FF",
            "Orange" to "#FF6600",
            "Brown" to "#FF8000",
            "Cyan" to "#00FFFF",
            "Purple" to "#8A00E6"
        )

        // Load locked apps initially
        loadLockedApps()
    }

    /**
     * Loads the list of locked apps from the repository.
     * Updates both LiveData and StateFlow for observers.
     */
    fun loadLockedApps() {
        LoggingUtil.debug(TAG, "loadLockedApps", "Loading locked apps from repository")
        viewModelScope.launch {
            try {
                val apps = repository.getAllLockedApps()
                _lockedApps.value = apps
                _lockedAppsFlow.value = apps
                _lockedAppsLoaded = true
                LoggingUtil.debug(TAG, "loadLockedApps", "Loaded ${apps.size} locked apps")
            } catch (e: Exception) {
                LoggingUtil.error(TAG, "loadLockedApps", "Error loading locked apps", e)
                _lockedApps.value = emptyList()
                _lockedAppsFlow.value = emptyList()
            }
        }
    }

    /**
     * Adds a new app to the locked apps list.
     *
     * @param packageName The package name of the app to lock
     */
    fun addLockedApp(packageName: String) {
        LoggingUtil.debug(TAG, "addLockedApp", "Adding locked app: $packageName")
        viewModelScope.launch {
            try {
                repository.addLockedApp(packageName)
                loadLockedApps() // Refresh the list
            } catch (e: Exception) {
                LoggingUtil.error(TAG, "addLockedApp", "Error adding locked app", e)
            }
        }
    }

    /**
     * Removes an app from the locked apps list.
     *
     * @param packageName The package name of the app to unlock
     */
    fun removeLockedApp(packageName: String) {
        LoggingUtil.debug(TAG, "removeLockedApp", "Removing locked app: $packageName")
        viewModelScope.launch {
            try {
                repository.removeLockedApp(packageName)
                loadLockedApps() // Refresh the list
            } catch (e: Exception) {
                LoggingUtil.error(TAG, "removeLockedApp", "Error removing locked app", e)
            }
        }
    }

    /**
     * Generates a new Stroop challenge.
     * Creates a word in one color and mismatched button colors.
     */
    fun generateChallenge() {
        LoggingUtil.debug(TAG, "generateChallenge", "Generating new challenge")
        val colorMap = _colorMap.value ?: return
        val availableColors = colorMap.keys.toList()

        // 1) random color word
        val randomWord = availableColors.random()
        _challengeWord.value = randomWord

        // 2) random ink color (different from word)
        var inkColorName: String
        do {
            inkColorName = availableColors.random()
        } while (inkColorName == randomWord)

        _inkColor.value = inkColorName
        _expectedAnswer.value = inkColorName

        // Generate button options
        generateButtonOptions(availableColors, inkColorName)

        LoggingUtil.debug(TAG, "generateChallenge", "Generated challenge: Word=$randomWord, Ink=$inkColorName")
    }

    /**
     * Generates the button labels and colors for the challenge.
     * Ensures the correct answer is included and colors are mismatched.
     *
     * @param availableColors List of available color names
     * @param correctAnswer The correct answer (ink color name)
     */
    private fun generateButtonOptions(availableColors: List<String>, correctAnswer: String) {
        // Create shuffled list of colors, ensuring correct answer is included
        val selectedColors = availableColors.shuffled().toMutableList()
        if (!selectedColors.contains(correctAnswer)) {
            selectedColors[0] = correctAnswer
            selectedColors.shuffle()
        }

        // For text colors, use cyclic derangement (easy way to ensure no color matches its label)
        val textColorAssignment = if (selectedColors.size <= 1)
            selectedColors
        else
            selectedColors.drop(1) + selectedColors.first()

        _buttonLabels.value = selectedColors.take(9)
        _buttonColors.value = textColorAssignment.take(9)
    }

    /**
     * Checks if the selected answer is correct.
     *
     * @param selectedColor The color name selected by the user
     * @return true if the answer is correct, false otherwise
     */
    fun checkAnswer(selectedColor: String): Boolean {
        val isCorrect = selectedColor == _expectedAnswer.value
        LoggingUtil.debug(TAG, "checkAnswer", "Checking answer: $selectedColor (expected: ${_expectedAnswer.value}) -> $isCorrect")
        return isCorrect
    }

    /**
     * Sets the package name of the app to launch after a successful challenge.
     *
     * @param packageName The package name to launch
     */
    fun setAppToLaunch(packageName: String?) {
        _appToLaunch.value = packageName
        LoggingUtil.debug(TAG, "setAppToLaunch", "Set app to launch: $packageName")
    }

    /**
     * Calculates an appropriate font size for a color word based on its length.
     * Reduces the font size for longer words to ensure they fit properly.
     *
     * @param word The word to calculate font size for
     * @return The calculated font size in sp
     */
    fun calculateFontSizeForWord(word: String): Float {
        val maxSize = 26f
        val minSize = 16f
        val baselineLength = 3
        val reductionPerExtraChar = 1.5f
        val extraChars = (word.length - baselineLength).coerceAtLeast(0)
        val calculatedSize = maxSize - extraChars * reductionPerExtraChar
        return calculatedSize.coerceAtLeast(minSize)
    }
}