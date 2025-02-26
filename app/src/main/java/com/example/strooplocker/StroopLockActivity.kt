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

    // The puzzle’s expected answer
    private var expectedAnswer = ""

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
    private val availablePool: List<String> = colorMap.keys.toList()

    @Volatile
    private var buttonCooldownActive = false

    // The package we want to launch after a correct puzzle
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

        // Create a 3x3 grid of puzzle answer buttons
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

        // Make buttons square after the layout pass
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
     * Checks if our Accessibility Service is enabled.
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
     * Styles a button with background, stroke, text color, etc.
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
     * Opens the Accessibility Settings screen.
     */
    private fun openAccessibilitySettings() {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        startActivity(intent)
    }

    /**
     * Generates a single Stroop puzzle challenge.
     */
    private fun applyChallenge() {
        Log.d(TAG, "applyChallenge: Generating a new puzzle.")
        try {
            var word: String
            var inkColorName: String
            do {
                word = availablePool.random()
                inkColorName = availablePool.random()
            } while (word == inkColorName)

            // 50/50 chance: pick the Word vs. pick the Ink color
            val isInverse = (0..1).random() == 1
            expectedAnswer = if (isInverse) word else inkColorName

            runOnUiThread {
                challengeText.text = word
                challengeText.textSize = 72f
                challengeText.typeface = ResourcesCompat.getFont(
                    this,
                    R.font.open_sans_extrabold
                )
                val inkColor = Color.parseColor(colorMap[inkColorName] ?: "#000000")
                challengeText.setTextColor(inkColor)
            }

            generateChallengeButtons()
        } catch (e: Exception) {
            Log.e(TAG, "applyChallenge: Error generating puzzle", e)
        }
    }

    /**
     * Fills the 9 puzzle buttons with color labels, each label having a shifted text color.
     */
    private fun generateChallengeButtons() {
        CoroutineScope(Dispatchers.Default).launch {
            try {
                val selectedColors = availablePool.shuffled().toMutableList()
                if (!selectedColors.contains(expectedAnswer)) {
                    selectedColors[0] = expectedAnswer
                    selectedColors.shuffle()
                }
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

                        val assignedHex = colorMap[textColorAssignment[index]] ?: "#000000"
                        val textColor = Color.parseColor(assignedHex)
                        button.setTextColor(textColor)

                        Log.d(
                            TAG,
                            "generateChallengeButtons: Button $index => label=$labelColor, textColor=$assignedHex"
                        )

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
     * Called when user taps a puzzle button.
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

        // Visual feedback: flash
        bgDrawable.setColor(originalTextColor)
        button.setTextColor(Color.parseColor("#212121"))

        Handler(Looper.getMainLooper()).postDelayed({
            // Revert
            bgDrawable.setColor(buttonBackgroundColor)
            button.setTextColor(originalTextColor)

            if (selectedColor == expectedAnswer) {
                Log.i(TAG, "handleButtonClick: Correct answer. Launching locked app => $lockedAppPackage")
                Toast.makeText(this, "Correct! Launching locked app...", Toast.LENGTH_SHORT).show()
                launchLockedApp()
            } else {
                Log.d(TAG, "handleButtonClick: Incorrect answer, regenerating puzzle.")
                Toast.makeText(this, "Incorrect! Try again.", Toast.LENGTH_SHORT).show()
                applyChallenge()
            }

            buttonCooldownActive = false
        }, 500)
    }

    /**
     * Launches the chosen locked app (if any).
     */
    private fun launchLockedApp() {
        if (lockedAppPackage == null) {
            Log.w(TAG, "launchLockedApp: No locked app selected, finishing puzzle screen.")
            Toast.makeText(this, "No locked app selected!", Toast.LENGTH_SHORT).show()
            return
        }
        try {
            val launchIntent = packageManager.getLaunchIntentForPackage(lockedAppPackage!!)
            if (launchIntent != null) {
                Log.d(TAG, "launchLockedApp: Starting activity for $lockedAppPackage")
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
     * Lets the user pick an app from the system's "Pick Activity" UI.
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

                // 1) Retrieve existing locked apps
                val prefs = getSharedPreferences("strooplocker_prefs", MODE_PRIVATE)
                val lockedApps = prefs.getStringSet("locked_apps", emptySet())?.toMutableSet()
                    ?: mutableSetOf()

                // 2) Add this newly chosen package
                lockedApps.add(chosenPackage)

                // 3) Write back
                prefs.edit().putStringSet("locked_apps", lockedApps).apply()

                // 4) Debug log
                Log.d("SelectAppFlow", "Saved locked apps: $lockedApps")
            }
        }
    }

    /**
     * Shift a list by 1 position. (Ensures label != text color.)
     */
    private fun <T> simpleCyclicDerangement(list: List<T>): List<T> {
        return if (list.size <= 1) list else list.drop(1) + list.first()
    }

    /**
     * Adjust font size based on word length.
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
