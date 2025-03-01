// app/src/main/java/com/example/strooplocker/StroopLockViewModel.kt

package com.example.strooplocker

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.strooplocker.data.LockedAppDatabase
import com.example.strooplocker.data.LockedAppRepository
import kotlinx.coroutines.launch

class StroopLockViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = LockedAppRepository(
        LockedAppDatabase.getInstance(application).lockedAppDao()
    )

    // Color data for the test
    private val _colorMap = MutableLiveData<Map<String, String>>()
    val colorMap: LiveData<Map<String, String>> = _colorMap

    // Challenge word and color
    private val _challengeWord = MutableLiveData<String>()
    val challengeWord: LiveData<String> = _challengeWord

    private val _inkColor = MutableLiveData<String>()
    val inkColor: LiveData<String> = _inkColor

    // Button colors
    private val _buttonLabels = MutableLiveData<List<String>>()
    val buttonLabels: LiveData<List<String>> = _buttonLabels

    private val _buttonColors = MutableLiveData<List<String>>()
    val buttonColors: LiveData<List<String>> = _buttonColors

    // Expected answer
    private val _expectedAnswer = MutableLiveData<String>()
    val expectedAnswer: LiveData<String> = _expectedAnswer

    // Locked apps
    private val _lockedApps = MutableLiveData<List<String>>()
    val lockedApps: LiveData<List<String>> = _lockedApps

    // Currently selected app to launch
    private val _appToLaunch = MutableLiveData<String?>()
    val appToLaunch: LiveData<String?> = _appToLaunch

    init {
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

        loadLockedApps()
    }

    /**
     * Load locked apps from database
     */
    fun loadLockedApps() {
        viewModelScope.launch {
            val apps = repository.getAllLockedApps()
            _lockedApps.postValue(apps)
        }
    }

    /**
     * Add an app to locked list
     */
    fun addLockedApp(packageName: String) {
        viewModelScope.launch {
            repository.addLockedApp(packageName)
            _appToLaunch.value = packageName
            loadLockedApps()
        }
    }

    /**
     * Generate a new Stroop challenge
     */
    fun generateChallenge() {
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
    }

    /**
     * Generate button labels and colors
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
     * Check if the selected answer is correct
     */
    fun checkAnswer(selectedColor: String): Boolean {
        return selectedColor == _expectedAnswer.value
    }

    /**
     * Set app package to launch after successful challenge
     */
    fun setAppToLaunch(packageName: String?) {
        _appToLaunch.value = packageName
    }

    /**
     * Calculate appropriate font size for a color word
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