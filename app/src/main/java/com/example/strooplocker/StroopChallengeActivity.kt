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
import androidx.core.content.ContextCompat
import kotlin.random.Random

/**
 * StroopChallengeActivity implements the Stroop color-word test as a cognitive challenge
 * before allowing access to a locked app.
 *
 * The challenge presents a color word (e.g., "Red") displayed in a different ink color (e.g., blue).
 * The user must identify the ink color (not the word) by selecting the correct color from a grid.
 */
class StroopChallengeActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "StroopChallenge"
    }

    // The color word displayed to the user
    private lateinit var challengeText: TextView

    // Grid of color options
    private lateinit var colorGrid: GridLayout

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

    // The correct color answer (the ink color, not the word)
    private lateinit var correctColor: String

    // The app to launch after successful challenge
    private var packageToLaunch: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_stroop_challenge)

        // Get the package to launch from intent
        packageToLaunch = intent.getStringExtra(StroopLockActivity.EXTRA_LOCKED_PACKAGE)
        if (packageToLaunch == null) {
            Log.e(TAG, "No package to launch provided")
            finish()
            return
        }

        // Initialize UI components
        challengeText = findViewById(R.id.challengeText)
        colorGrid = findViewById(R.id.colorGrid)

        // Generate and display a challenge
        generateChallenge()
    }

    /**
     * Generates a new Stroop challenge with random colors.
     * The word and ink color will always be different to create the Stroop effect.
     */
    private fun generateChallenge() {
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
        // Clear existing buttons
        colorGrid.removeAllViews()

        // Create a shuffled list of colors for buttons
        val shuffledColors = colorNames.shuffled()

        // For each color, create a button
        for (colorName in shuffledColors) {
            val button = Button(this)
            button.text = colorName

            // Set a different text color than the button text (maintaining Stroop effect)
            val textColorName = shuffledColors.find { it != colorName } ?: shuffledColors.first()
            button.setTextColor(colorMap[textColorName] ?: Color.BLACK)

            // Make buttons evenly distributed
            val params = GridLayout.LayoutParams()
            params.width = 0
            params.height = GridLayout.LayoutParams.WRAP_CONTENT
            params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
            button.layoutParams = params

            // Set click listener
            button.setOnClickListener { onColorSelected(colorName) }

            // Add to grid
            colorGrid.addView(button)
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
            StroopLockActivity.completedChallenges.add(packageToLaunch ?: "")
            launchLockedApp()
        } else {
            Log.d(TAG, "Incorrect answer: $selectedColor, expected: $correctColor")
            Toast.makeText(this, getString(R.string.incorrect_answer), Toast.LENGTH_SHORT).show()

            // Generate a new challenge
            generateChallenge()
        }
    }

    /**
     * Launches the previously locked app.
     */
    private fun launchLockedApp() {
        packageToLaunch?.let { packageName ->
            try {
                val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
                if (launchIntent != null) {
                    startActivity(launchIntent)
                    finish()
                } else {
                    Log.e(TAG, "Unable to find launch intent for $packageName")
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
    }
}