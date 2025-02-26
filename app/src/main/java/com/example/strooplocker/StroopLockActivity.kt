package com.example.strooplocker

import android.app.Activity
import android.content.ComponentName
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

    companion object {
        private const val TAG = "StroopLockActivity"
        private const val REQUEST_CODE_PICK_APP = 2001
    }

    // UI references
    private lateinit var challengeText: TextView
    private lateinit var answerGrid: GridLayout
    private lateinit var rootLayout: ConstraintLayout
    private lateinit var exitButton: Button
    private lateinit var selectAppButton: Button
    private lateinit var enableAccessibilityButton: Button

    // Dynamically created answer buttons
    private lateinit var answerButtons: MutableList<Button>

    // This will store the correct color name to pick
    private var expectedAnswer = ""

    // Each key is a color name, each value is the hex color
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
    private val availablePool: List<String> = colorMap.keys.toList()

    // For preventing spam taps
    @Volatile
    private var buttonCooldownActive = false

    // The user’s “locked” app, launched after correct answer
    private var lockedAppPackage: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, "onCreate: Entered.")
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_stroop_lock)

        // Hook up UI references
        rootLayout = findViewById(R.id.rootLayout)
        challengeText = findViewById(R.id.challengeText)
        answerGrid = findViewById(R.id.answerGrid)
        exitButton = findViewById(R.id.exitButton)
        selectAppButton = findViewById(R.id.selectAppButton)
        enableAccessibilityButton = findViewById(R.id.enableAccessibilityButton)

        // Style the layout background
        rootLayout.setBackgroundColor(Color.parseColor("#F0F0F0"))

        // Style the three main buttons
        styleMenuButton(exitButton, Color.parseColor("#CCCCCC"), Color.parseColor("#212121"))
        styleMenuButton(selectAppButton, Color.parseColor("#CCCCCC"), Color.parseColor("#212121"))
        styleMenuButton(enableAccessibilityButton, Color.parseColor("#CCCCCC"), Color.parseColor("#212121"))

        // Exit button: go home, then finish
        exitButton.setOnClickListener {
            Log.d(TAG, "exitButton clicked, returning to home + finishing activity.")
            val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivity(homeIntent)
            finishAffinity()
        }

        // Let user pick an app to lock
        selectAppButton.setOnClickListener {
            Log.d(TAG, "selectAppButton clicked, picking an app from launcher.")
            pickAppFromLauncher()
        }

        // Enable the Accessibility Service
        enableAccessibilityButton.setOnClickListener {
            Log.d(TAG, "enableAccessibilityButton clicked, opening Accessibility Settings.")
            openAccessibilitySettings()
        }

        // Create a 3×3 grid of puzzle answer buttons
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

        // Adjust button shapes after layout pass
        answerGrid.post {
            answerButtons.forEach { button ->
                val size = button.width
                val params = button.layoutParams
                params.height = size
                button.layoutParams = params
            }
        }

        // Set puzzle button click listeners
        answerButtons.forEachIndexed { index, button ->
            button.setOnClickListener {
                handleButtonClick(button, button.text.toString(), index)
            }
        }

        // Generate the initial puzzle
        applyChallenge()
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume: Checking if Accessibility is enabled.")
        if (!isAccessibilityServiceEnabled(this, StroopAccessibilityService::class.java)) {
            Log.w(TAG, "Accessibility NOT enabled. Showing toast to user.")
            Toast.makeText(
                this,
                "Stroop Accessibility Service is NOT enabled. Tap 'Enable Service' if you wish to activate it.",
                Toast.LENGTH_LONG
            ).show()
        } else {
            Log.d(TAG, "Accessibility service is enabled.")
        }
    }

    /**
     * A quick check whether our AccessibilityService is running.
     */
    private fun isAccessibilityServiceEnabled(
        context: Context,
        serviceClass: Class<*>
    ): Boolean {
        val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        val expectedComponentName = ComponentName(context, serviceClass)
        val enabledServices = am.getEnabledAccessibilityServiceList(
            android.accessibilityservice.AccessibilityServiceInfo.FEEDBACK_ALL_MASK
        )
        for (serviceInfo in enabledServices) {
            val enabledService = serviceInfo.resolveInfo.serviceInfo
            if (enabledService.packageName == expectedComponentName.packageName &&
                enabledService.name == expectedComponentName.className
            ) {
                return true
            }
        }
        return false
    }

    /**
     * Style a basic button with a background, stroke, and text color.
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
     * Jump to Accessibility Settings if user wants to enable the service.
     */
    private fun openAccessibilitySettings() {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        startActivity(intent)
    }

    /**
     * The main puzzle generator: picks a “decoyWord” to display, picks an “answer color” for the text,
     * and ensures the user must choose the *ink color* from the button grid.
     */
    private fun applyChallenge() {
        Log.d(TAG, "applyChallenge: Generating a new puzzle...")

        try {
            // 1) Pick two different color names
            var decoyWord: String
            var answerColorName: String
            do {
                decoyWord = availablePool.random()
                answerColorName = availablePool.random()
            } while (decoyWord == answerColorName)

            // 2) We'll set the challenge text to decoyWord, but colored in answerColorName
            expectedAnswer = answerColorName

            // 3) Update the puzzle text
            runOnUiThread {
                challengeText.text = decoyWord
                challengeText.textSize = 72f
                challengeText.typeface =
                    ResourcesCompat.getFont(this, R.font.open_sans_extrabold)

                val colorHex = colorMap[answerColorName] ?: "#000000"
                val puzzleInkColor = Color.parseColor(colorHex)
                challengeText.setTextColor(puzzleInkColor)

                Log.d(TAG, "applyChallenge: decoyWord='$decoyWord', answerColorName='$answerColorName' => puzzle text is '$decoyWord' in color=$colorHex")
            }

            // 4) Now populate the 9 answer buttons, ensuring we show answerColorName in the set
            generateChallengeButtons()

        } catch (e: Exception) {
            Log.e(TAG, "applyChallenge: Error generating puzzle", e)
        }
    }

    /**
     * Populate 9 buttons with color labels in a “shifted” text color scheme for the Stroop effect.
     */
    private fun generateChallengeButtons() {
        CoroutineScope(Dispatchers.Default).launch {
            try {
                // Shuffle a set of color labels
                val selectedColors = availablePool.shuffled().toMutableList()
                // Make sure our expectedAnswer is definitely among them
                if (!selectedColors.contains(expectedAnswer)) {
                    selectedColors[0] = expectedAnswer
                    selectedColors.shuffle()
                }

                // We do a simple “cyclic shift” so the label’s text color is offset
                val textColorAssignment = simpleCyclicDerangement(selectedColors)

                withContext(Dispatchers.Main) {
                    val buttonBgColor = Color.parseColor("#CCCCCC")

                    answerButtons.forEachIndexed { index, button ->
                        val labelText = selectedColors[index]
                        button.text = labelText
                        // Adjust font size if color name is longer
                        button.setTextSize(
                            TypedValue.COMPLEX_UNIT_SP,
                            calculateFontSizeForWord(labelText)
                        )
                        button.typeface = ResourcesCompat.getFont(
                            this@StroopLockActivity,
                            R.font.open_sans_extrabold
                        )

                        // The actual color used for the text is the next color in the derangement
                        val assignedHex = colorMap[textColorAssignment[index]] ?: "#000000"
                        val textColor = Color.parseColor(assignedHex)
                        button.setTextColor(textColor)

                        // Debug log
                        Log.d(TAG, "generateChallengeButtons: Button $index => label=$labelText, textColor=$assignedHex")

                        val drawable = GradientDrawable().apply {
                            setColor(buttonBgColor)
                            cornerRadius = 8f
                            setStroke(4, Color.parseColor("#757575"))
                        }
                        button.background = drawable
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "generateChallengeButtons: Error populating puzzle buttons", e)
            }
        }
    }

    /**
     * Handle user tapping on one of the color-labeled buttons.
     */
    private fun handleButtonClick(button: Button, selectedColor: String, buttonIndex: Int) {
        if (buttonCooldownActive) {
            Log.d(TAG, "handleButtonClick: buttonCooldownActive, ignoring click.")
            return
        }
        buttonCooldownActive = true

        Log.d(
            TAG,
            "handleButtonClick: user selectedColor=$selectedColor, expectedAnswer=$expectedAnswer"
        )

        val buttonBackgroundColor = Color.parseColor("#CCCCCC")
        val bgDrawable = button.background as GradientDrawable
        val originalTextColor = button.currentTextColor

        // Flash the button color
        bgDrawable.setColor(originalTextColor)
        button.setTextColor(Color.parseColor("#212121"))

        // Revert after 500ms
        Handler(Looper.getMainLooper()).postDelayed({
            bgDrawable.setColor(buttonBackgroundColor)
            button.setTextColor(originalTextColor)

            if (selectedColor == expectedAnswer) {
                Log.i(TAG, "handleButtonClick: CORRECT. The user found the color=$selectedColor.")
                Toast.makeText(
                    this,
                    "Correct! Launching locked app (if selected)...",
                    Toast.LENGTH_SHORT
                ).show()
                launchLockedApp()
            } else {
                Log.d(TAG, "handleButtonClick: INCORRECT. Re-generate puzzle.")
                Toast.makeText(this, "Incorrect! Try again.", Toast.LENGTH_SHORT).show()
                applyChallenge()
            }

            buttonCooldownActive = false
        }, 500)
    }

    /**
     * Launch the chosen locked app (if any).
     */
    private fun launchLockedApp() {
        if (lockedAppPackage == null) {
            Log.w(TAG, "launchLockedApp: No locked app selected; puzzle finishes here.")
            Toast.makeText(this, "No locked app selected!", Toast.LENGTH_SHORT).show()
            return
        }
        try {
            val launchIntent = packageManager.getLaunchIntentForPackage(lockedAppPackage!!)
            if (launchIntent != null) {
                Log.d(TAG, "launchLockedApp: Starting activity for lockedAppPackage=$lockedAppPackage")
                startActivity(launchIntent)
            } else {
                Log.w(TAG, "launchLockedApp: Unable to launch $lockedAppPackage")
                Toast.makeText(this, "Unable to launch $lockedAppPackage", Toast.LENGTH_SHORT)
                    .show()
            }
        } catch (e: Exception) {
            Log.e(TAG, "launchLockedApp: Error launching $lockedAppPackage", e)
            Toast.makeText(this, "Error launching $lockedAppPackage", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Let user pick an app from a standard "Pick Activity" system UI.
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_PICK_APP && resultCode == Activity.RESULT_OK) {
            val component = data?.component
            if (component != null) {
                val chosenPackage = component.packageName
                Toast.makeText(this, "Locked app set to: $chosenPackage", Toast.LENGTH_SHORT).show()

                // Save it in shared prefs in case we want to do more with “locked apps”
                val prefs = getSharedPreferences("strooplocker_prefs", MODE_PRIVATE)
                val lockedApps = prefs.getStringSet("locked_apps", emptySet())?.toMutableSet()
                    ?: mutableSetOf()
                lockedApps.add(chosenPackage)
                prefs.edit().putStringSet("locked_apps", lockedApps).apply()

                lockedAppPackage = chosenPackage
                Log.d(TAG, "onActivityResult: lockedApps now = $lockedApps")
            }
        }
    }

    /**
     * Simple shift so index i label uses color i+1, etc.
     */
    private fun <T> simpleCyclicDerangement(list: List<T>): List<T> {
        return if (list.size <= 1) list
        else list.drop(1) + list.first()
    }

    /**
     * Make longer color words slightly smaller so everything fits.
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
