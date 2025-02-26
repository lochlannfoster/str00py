package com.example.strooplocker

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.util.TypedValue
import android.view.accessibility.AccessibilityManager
import android.widget.Button
import android.widget.GridLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.res.ResourcesCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class StroopLockActivity : AppCompatActivity() {

    // UI Components
    private lateinit var challengeText: TextView
    private lateinit var answerGrid: GridLayout
    private lateinit var rootLayout: ConstraintLayout
    private lateinit var exitButton: Button
    private lateinit var selectAppButton: Button

    // Dynamically created answer buttons
    private lateinit var answerButtons: MutableList<Button>

    // A map of color name -> hex code
    private val colorMap = mapOf(
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

    // Pool of available color names
    private val availablePool: List<String> = colorMap.keys.toList()

    // We store what the user must choose to succeed
    private var expectedAnswer = ""

    // To avoid rapid clicks
    @Volatile
    private var buttonCooldownActive = false

    // The app package we want to "lock" and launch after success
    private var lockedAppPackage: String? = null

    companion object {
        private const val REQUEST_CODE_PICK_APP = 2001
    }


    /**
     * Helper function to check if our StroopAccessibilityService is enabled.
     */
    private fun isAccessibilityServiceEnabled(
        context: Context,
        serviceClass: Class<*>
    ): Boolean {
        val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        val enabledServices = am.getEnabledAccessibilityServiceList(
            android.accessibilityservice.AccessibilityServiceInfo.FEEDBACK_ALL_MASK
        )

        for (enabledService in enabledServices) {
            val enabledServiceId = enabledService.id
            if (enabledServiceId.contains(context.packageName + "/" + serviceClass.name)) {
                return true
            }
        }
        return false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_stroop_lock)

        // Root layout
        rootLayout = findViewById(R.id.rootLayout)
        rootLayout.setBackgroundColor(Color.parseColor("#F0F0F0"))

        // Text + Grid
        challengeText = findViewById(R.id.challengeText)
        answerGrid = findViewById(R.id.answerGrid)

        // Buttons
        exitButton = findViewById(R.id.exitButton)
        styleMenuButton(exitButton, Color.parseColor("#CCCCCC"), Color.parseColor("#212121"))
        exitButton.setOnClickListener { finishAffinity() }

        selectAppButton = findViewById(R.id.selectAppButton)
        styleMenuButton(selectAppButton, Color.parseColor("#CCCCCC"), Color.parseColor("#212121"))
        selectAppButton.setOnClickListener { pickAppFromLauncher() }

        // Create a 3x3 grid of answer buttons
        answerButtons = mutableListOf()
        answerGrid.columnCount = 3
        answerGrid.rowCount = 3
        for (i in 0 until 9) {
            val btn = Button(this)
            val params = GridLayout.LayoutParams().apply {
                rowSpec = GridLayout.spec(i / 3, 1f)
                columnSpec = GridLayout.spec(i % 3, 1f)
                setMargins(8, 8, 8, 8)
            }
            btn.layoutParams = params
            answerGrid.addView(btn)
            answerButtons.add(btn)
        }

        // Ensure buttons are square after layout
        answerGrid.post {
            answerButtons.forEach { button ->
                val size = button.width
                val params = button.layoutParams
                params.height = size
                button.layoutParams = params
            }
        }

        // Set click listeners
        answerButtons.forEachIndexed { index, button ->
            button.setOnClickListener { handleButtonClick(button, button.text.toString(), index) }
        }

        // Generate the initial single challenge
        applyChallenge()
    }

    /**
     * We check here if our Accessibility Service is active; if not, open settings.
     */
    override fun onResume() {
        super.onResume()

        // If your accessibility service is NOT enabled, open Accessibility Settings
        if (!isAccessibilityServiceEnabled(this, StroopAccessibilityService::class.java)) {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            startActivity(intent)
        }
    }

    /**
     * Simple helper to style a menu button with a background, stroke, etc.
     */
    private fun styleMenuButton(button: Button, backgroundColor: Int, textColor: Int) {
        val drawable = GradientDrawable().apply {
            setColor(backgroundColor)
            cornerRadius = 8f
            setStroke(4, Color.parseColor("#757575"))
        }
        button.background = drawable
        button.setTextColor(textColor)
    }

    /**
     * Generate a single challenge. We randomly decide if it's "primary" or "inverse".
     * If primary, user must pick the INK color name.
     * If inverse, user must pick the WORD itself.
     */
    private fun applyChallenge() {
        try {
            // 1) Choose random distinct word & color
            var word: String
            var inkColorName: String
            do {
                word = availablePool.random()
                inkColorName = availablePool.random()
            } while (word == inkColorName)

            // 2) Randomly decide: is this a "primary" or "inverse" challenge?
            val isInverse = (0..1).random() == 1  // 50% chance

            if (isInverse) {
                // Inverse: user must pick the word itself
                expectedAnswer = word
            } else {
                // Primary: user must pick the ink color
                expectedAnswer = inkColorName
            }

            // Update the UI
            runOnUiThread {
                challengeText.text = word
                challengeText.textSize = 72f
                challengeText.typeface =
                    ResourcesCompat.getFont(this, R.font.open_sans_extrabold)

                val inkColor = Color.parseColor(colorMap[inkColorName] ?: "#000000")
                challengeText.setTextColor(inkColor)
            }

            // 3) Populate the 9 answer buttons
            generateChallengeButtons()

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Populate the 9 answer buttons with random color labels, each label with a text color from a shifted list.
     */
    private fun generateChallengeButtons() {
        CoroutineScope(Dispatchers.Default).launch {
            try {
                // Create a shuffled list for the labels
                val selectedColors = availablePool.shuffled().toMutableList()

                // Ensure the expectedAnswer is included
                if (!selectedColors.contains(expectedAnswer)) {
                    selectedColors[0] = expectedAnswer
                    selectedColors.shuffle()
                }

                // A simple shift so label i has text color i+1
                val textColorAssignment = simpleCyclicDerangement(selectedColors)

                withContext(Dispatchers.Main) {
                    val buttonBgColor = Color.parseColor("#CCCCCC")
                    answerButtons.forEachIndexed { index, button ->
                        val labelColor = selectedColors[index]
                        button.text = labelColor
                        button.setTextSize(
                            TypedValue.COMPLEX_UNIT_SP,
                            calculateFontSizeForWord(labelColor)
                        )
                        button.typeface = ResourcesCompat.getFont(
                            this@StroopLockActivity,
                            R.font.open_sans_extrabold
                        )

                        // Look up the hex for the assigned text color
                        val assignedHex = colorMap[textColorAssignment[index]] ?: "#000000"
                        val textColor = Color.parseColor(assignedHex)
                        button.setTextColor(textColor)

                        // Debug log
                        Log.d(
                            "ColorDebug",
                            "Button $index: label=$labelColor, intendedTextColor=$assignedHex, computedTextColor=${
                                hexStringOfColor(textColor)
                            }"
                        )

                        // Set background
                        val drawable = GradientDrawable().apply {
                            setColor(buttonBgColor)
                            cornerRadius = 8f
                            setStroke(4, Color.parseColor("#757575"))
                        }
                        button.background = drawable
                        button.invalidate()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Called when an answer button is tapped. If correct, launch locked app. Otherwise, re-generate challenge.
     */
    private fun handleButtonClick(button: Button, selectedColor: String, buttonIndex: Int) {
        if (buttonCooldownActive) return
        buttonCooldownActive = true

        val buttonBackgroundColor = Color.parseColor("#CCCCCC")
        val bgDrawable = button.background as GradientDrawable
        val originalTextColor = button.currentTextColor

        // Quick feedback: flash button
        bgDrawable.setColor(originalTextColor)
        button.setTextColor(Color.parseColor("#212121"))

        Handler(Looper.getMainLooper()).postDelayed({
            // Revert
            bgDrawable.setColor(buttonBackgroundColor)
            button.setTextColor(originalTextColor)

            if (selectedColor == expectedAnswer) {
                // Correct: launch locked app
                Toast.makeText(
                    this,
                    "Correct! Launching locked app...",
                    Toast.LENGTH_SHORT
                ).show()
                launchLockedApp()
            } else {
                Toast.makeText(this, "Incorrect! Try again.", Toast.LENGTH_SHORT).show()
                applyChallenge()
            }

            buttonCooldownActive = false
        }, 500)
    }

    /**
     * Launch the app chosen in pickAppFromLauncher(), if any.
     */
    private fun launchLockedApp() {
        if (lockedAppPackage == null) {
            Toast.makeText(this, "No locked app selected!", Toast.LENGTH_SHORT).show()
            return
        }
        try {
            val launchIntent = packageManager.getLaunchIntentForPackage(lockedAppPackage!!)
            if (launchIntent != null) {
                startActivity(launchIntent)
            } else {
                Toast.makeText(this, "Unable to launch $lockedAppPackage", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error launching $lockedAppPackage", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Let user pick a launchable app from the system's built-in "Pick Activity" UI.
     */
    private fun pickAppFromLauncher() {
        val intent = Intent(Intent.ACTION_PICK_ACTIVITY).apply {
            putExtra(
                Intent.EXTRA_INTENT,
                Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
            )
        }
        startActivityForResult(intent, REQUEST_CODE_PICK_APP)
    }

    /**
     * Receive the result of picking an app.
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_CODE_PICK_APP && resultCode == Activity.RESULT_OK) {
            // The chosen component
            val component = data?.component
            if (component != null) {
                lockedAppPackage = component.packageName
                Toast.makeText(this, "Locked app set to: $lockedAppPackage", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Convert an int color to #RRGGBB string for debugging logs.
     */
    private fun hexStringOfColor(color: Int): String {
        return String.format("#%06X", 0xFFFFFF and color)
    }

    /**
     * A trivial "shift everything by 1" function so label i doesn't get color i.
     */
    private fun <T> simpleCyclicDerangement(list: List<T>): List<T> {
        return if (list.size <= 1) list else list.drop(1) + list.first()
    }

    /**
     * Adjust the font size based on word length, so longer color names appear smaller.
     */
    private fun calculateFontSizeForWord(word: String): Float {
        val maxSize = 26f
        val minSize = 16f
        val baselineLength = 3
        val reductionPerExtraChar = 1.5f
        val extraChars = (word.length - baselineLength).coerceAtLeast(0)
        val calculatedSize = maxSize - extraChars * reductionPerExtraChar
        return calculatedSize.coerceAtLeast(minSize)
    }
}
