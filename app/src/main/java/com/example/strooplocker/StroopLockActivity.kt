package com.example.strooplocker

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.GridLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * StroopLockActivity is the main entry point of the app.
 * It displays a cognitive challenge (Stroop test) that must be passed
 * before the user can access a locked app.
 */
class StroopLockActivity : AppCompatActivity() {

    companion object {
        const val TAG = "StroopLockActivity"
        const val EXTRA_LOCKED_PACKAGE = "extra_locked_package"
        const val REQUEST_CODE_PICK_APP = 1001
        // Exposed so other classes can read the set if needed
        val completedChallenges = mutableSetOf<String>()
    }

    // Challenge UI elements
    private lateinit var challengeText: TextView
    private lateinit var answerGrid: GridLayout

    // Maps color names to their RGB values
    private val colorMap = mapOf(
        "Red" to Color.rgb(255, 0, 0),
        "Green" to Color.rgb(0, 255, 0),
        "Blue" to Color.rgb(0, 0, 255),
        "Yellow" to Color.rgb(255, 255, 0),
        "Purple" to Color.rgb(128, 0, 128),
        "Orange" to Color.rgb(255, 165, 0),
        "Pink" to Color.rgb(255, 192, 203),
        "Brown" to Color.rgb(165, 42, 42),
        "Cyan" to Color.rgb(0, 255, 255)
    )

    // Challenge state variables
    private var correctColor: String? = null
    private var packageToLaunch: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_stroop_lock)

        // Initialize UI components and logic
        initUI()
        handleIntent(intent)
    }

    private fun initUI() {
        Log.d(TAG, "Initializing UI components")

        // Find views
        challengeText = findViewById(R.id.challengeText)
        answerGrid = findViewById(R.id.answerGrid)

        // Setup buttons
        findViewById<Button>(R.id.exitButton)?.setOnClickListener {
            Log.d(TAG, "Exit button clicked")
            finish()
        }

        findViewById<Button>(R.id.selectAppButton)?.setOnClickListener {
            Log.d(TAG, "Select app button clicked")
            val intent = Intent(this, SelectAppsActivity::class.java)
            startActivity(intent)
        }

        findViewById<Button>(R.id.enableAccessibilityButton)?.setOnClickListener {
            Log.d(TAG, "Enable accessibility button clicked")
            val intent = Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS)
            startActivity(intent)
        }
    }

    private fun handleIntent(intent: Intent) {
        Log.d(TAG, "Handling intent: $intent")
        val lockedPackage = intent.getStringExtra(EXTRA_LOCKED_PACKAGE)
        if (lockedPackage != null) {
            packageToLaunch = lockedPackage
            handleLockedApp(lockedPackage)
        } else {
            Log.d(TAG, "No locked package provided in the intent.")
            generateChallenge() // Generate a sample challenge for testing
        }
    }

    private fun handleLockedApp(packageName: String) {
        Log.d(TAG, "Handling locked app for package: $packageName")

        // Launch locked app if challenge already completed, otherwise start challenge
        if (checkChallengeCompletion(packageName)) {
            Log.d(TAG, "Challenge already completed for $packageName, launching app")
            launchLockedApp(packageName)
        } else {
            Log.d(TAG, "Starting challenge for $packageName")
            ChallengeManager.startChallenge(packageName)
            generateChallenge()
        }
    }

    /**
     * Generates a new Stroop challenge with random colors.
     * The word and ink color will always be different to create the Stroop effect.
     */
    private fun generateChallenge() {
        Log.d(TAG, "Generating new challenge")

        // Clear existing buttons
        answerGrid.removeAllViews()

        // Get available colors
        val colorNames = colorMap.keys.toList()

        // Choose a random word (color name)
        val wordColorName = colorNames.random()

        // Choose a different color for the ink
        var inkColorName: String
        do {
            inkColorName = colorNames.random()
        } while (inkColorName == wordColorName)

        // Set the correct answer to the ink color
        correctColor = inkColorName

        // Set the challenge text with proper colors
        challengeText.text = wordColorName
        challengeText.setTextColor(colorMap[inkColorName] ?: Color.BLACK)

        // Log the challenge details for debugging
        Log.d(TAG, "Challenge - Word: $wordColorName, Ink: $inkColorName (correct answer)")

        // Create the buttons for color selection
        createColorButtons(colorNames)
    }

    /**
     * Creates the grid of color buttons for the user to select from.
     * Each button shows a color name in a different color to maintain the Stroop effect.
     */
    private fun createColorButtons(colorNames: List<String>) {
        // Create a shuffled list of colors for buttons
        val shuffledColors = colorNames.shuffled()

        // Make sure we don't use more colors than we have available
        val numButtons = Math.min(9, shuffledColors.size)

        // For each color, create a button
        for (i in 0 until numButtons) {
            val colorName = shuffledColors[i]
            val button = Button(this)
            button.text = colorName

            // Set a different text color than the button text (maintaining Stroop effect)
            val textColorIndex = (i + 1) % numButtons
            val textColorName = shuffledColors[textColorIndex]
            button.setTextColor(colorMap[textColorName] ?: Color.BLACK)

            // Make buttons evenly distributed
            val params = GridLayout.LayoutParams()
            params.width = 0
            params.height = GridLayout.LayoutParams.WRAP_CONTENT
            params.columnSpec = GridLayout.spec(i % 3, 1f)
            params.rowSpec = GridLayout.spec(i / 3, 1f)
            button.layoutParams = params

            // Set click listener
            button.setOnClickListener { onColorSelected(colorName) }

            // Add to grid
            answerGrid.addView(button)
        }
    }

    /**
     * Handles user selection of a color.
     * If correct, launches the locked app.
     * If incorrect, generates a new challenge.
     */
    private fun onColorSelected(selectedColor: String) {
        if (selectedColor == correctColor) {
            Log.d(TAG, "Correct answer selected: $selectedColor")
            Toast.makeText(this, getString(R.string.correct_answer), Toast.LENGTH_SHORT).show()

            // Mark challenge as completed and launch the app
            packageToLaunch?.let { pkg ->
                completedChallenges.add(pkg)
                ChallengeManager.completeChallenge(true)
                launchLockedApp(pkg)
            }
        } else {
            Log.d(TAG, "Incorrect answer: $selectedColor, expected: $correctColor")
            Toast.makeText(this, getString(R.string.incorrect_answer), Toast.LENGTH_SHORT).show()

            // Generate a new challenge
            generateChallenge()
        }
    }

    private fun checkChallengeCompletion(packageName: String): Boolean {
        // Check if the challenge for this package has been completed recently
        return completedChallenges.contains(packageName)
    }

    /**
     * Launches the locked app by retrieving its launch intent.
     */
    private fun launchLockedApp(packageName: String) {
        Log.d(TAG, "Launching locked app: $packageName")
        try {
            val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
            if (launchIntent != null) {
                startActivity(launchIntent)
                finish() // Close StroopLockActivity after launching the app
            } else {
                Log.e(TAG, "No launch intent found for package: $packageName")
                Toast.makeText(
                    this,
                    getString(R.string.unable_to_launch, packageName),
                    Toast.LENGTH_LONG
                ).show()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error launching app: $packageName", e)
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Shifts the first element of the list to the end, simulating a cyclic derangement.
     */
    private fun <T> simpleCyclicDerangement(list: List<T>): List<T> {
        if (list.size <= 1) return list
        val mutableList = list.toMutableList()
        val first = mutableList.removeAt(0)
        mutableList.add(first)
        return mutableList
    }

    /**
     * Calculates a font size based on the length of the provided word.
     */
    private fun calculateFontSizeForWord(word: String): Float {
        return when {
            word.length > 10 -> 18f
            word.length > 5  -> 24f
            else             -> 30f
        }
    }
}