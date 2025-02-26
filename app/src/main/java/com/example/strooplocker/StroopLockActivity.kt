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
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.res.ResourcesCompat
import com.example.strooplocker.data.LockedAppDatabase
import com.example.strooplocker.data.LockedAppsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class StroopLockActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "StroopLockActivity"
        private const val REQUEST_CODE_PICK_APP = 2001
    }

    // Repository for DB calls
    private lateinit var repository: LockedAppsRepository

    // UI references
    private lateinit var challengeText: TextView
    private lateinit var answerGrid: GridLayout
    private lateinit var rootLayout: ConstraintLayout
    private lateinit var exitButton: Button
    private lateinit var selectAppButton: Button
    private lateinit var enableAccessibilityButton: Button
    private lateinit var answerButtons: MutableList<Button>

    // The puzzle's correct color to pick
    private var expectedAnswer = ""

    // Map color name -> hex
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
    private val availablePool = colorMap.keys.toList()

    @Volatile
    private var buttonCooldownActive = false

    // The package we want to open after puzzle success
    private var lockedPackageToLaunch: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate: Entered.")
        setContentView(R.layout.activity_stroop_lock)

        // Disable back button
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                Toast.makeText(
                    this@StroopLockActivity,
                    "Complete the Stroop challenge to unlock",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })


        // Initialize DB + repository
        val db = LockedAppDatabase.getInstance(this)
        val dao = db.lockedAppDao()
        repository = LockedAppsRepository(dao)

        // Check if AccessibilityService gave us a locked package
        lockedPackageToLaunch =
            intent?.getStringExtra(StroopAccessibilityService.EXTRA_LOCKED_PACKAGE)
        Log.d(TAG, "onCreate: lockedPackageToLaunch=$lockedPackageToLaunch")

        // Optionally, fallback if user launched from home screen and we have exactly one locked app
        if (lockedPackageToLaunch == null) {
            CoroutineScope(Dispatchers.IO).launch {
                val lockedApps = repository.getAllLockedApps()
                if (lockedApps.size == 1) {
                    lockedPackageToLaunch = lockedApps.first()
                    Log.d(TAG, "Fallback to single locked app => $lockedPackageToLaunch")
                }
            }
        }

        // UI references
        rootLayout = findViewById(R.id.rootLayout)
        challengeText = findViewById(R.id.challengeText)
        answerGrid = findViewById(R.id.answerGrid)
        exitButton = findViewById(R.id.exitButton)
        selectAppButton = findViewById(R.id.selectAppButton)
        enableAccessibilityButton = findViewById(R.id.enableAccessibilityButton)

        // Style
        rootLayout.setBackgroundColor(Color.parseColor("#F0F0F0"))
        styleMenuButton(exitButton, Color.parseColor("#CCCCCC"), Color.parseColor("#212121"))
        styleMenuButton(selectAppButton, Color.parseColor("#CCCCCC"), Color.parseColor("#212121"))
        styleMenuButton(
            enableAccessibilityButton,
            Color.parseColor("#CCCCCC"),
            Color.parseColor("#212121")
        )

// Exit button behavior
        exitButton.setOnClickListener {
            val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivity(homeIntent)
            finishAffinity() // This will close all activities in the app
        }

        // Let user pick an app to lock
        selectAppButton.setOnClickListener {
            Log.d(TAG, "selectAppButton clicked: picking an app from launcher.")
            pickAppFromLauncher()
        }

        // Accessibility
        enableAccessibilityButton.setOnClickListener {
            Log.d(TAG, "enableAccessibilityButton clicked: opening Accessibility Settings.")
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            startActivity(intent)
        }

        // Build puzzle buttons (3x3)
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

        // Ensure squares after layout
        answerGrid.post {
            answerButtons.forEach { button ->
                val size = button.width
                val lp = button.layoutParams
                lp.height = size
                button.layoutParams = lp
            }
        }

        // Puzzle button clicks
        answerButtons.forEach { button ->
            button.setOnClickListener {
                val chosenColor = button.text.toString()
                handleButtonClick(button, chosenColor)
            }
        }

        // Generate puzzle
        applyChallenge()
    }

    // Prevent system-level app switching
    override fun onUserLeaveHint() {
        Toast.makeText(
            this,
            "Complete the Stroop challenge to unlock",
            Toast.LENGTH_SHORT
        ).show()


        // Let user pick an app to lock
        selectAppButton.setOnClickListener {
            Log.d(TAG, "selectAppButton clicked: picking an app from launcher.")
            pickAppFromLauncher()
        }

        // Accessibility
        enableAccessibilityButton.setOnClickListener {
            Log.d(TAG, "enableAccessibilityButton clicked: opening Accessibility Settings.")
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            startActivity(intent)
        }

        // Build puzzle buttons (3x3)
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

        // Ensure squares after layout
        answerGrid.post {
            answerButtons.forEach { button ->
                val size = button.width
                val lp = button.layoutParams
                lp.height = size
                button.layoutParams = lp
            }
        }

        // Puzzle button clicks
        answerButtons.forEach { button ->
            button.setOnClickListener {
                val chosenColor = button.text.toString()
                handleButtonClick(button, chosenColor)
            }
        }

        // Generate puzzle
        applyChallenge()
    }

    override fun onPause() {
        super.onPause()
        Log.d(TAG, "onPause: Reapplying lock")

        // If the challenge wasn't successfully completed, restart the challenge
        if (lockedPackageToLaunch != null) {
            applyChallenge()

            Toast.makeText(
                this,
                "App is still locked. Complete the Stroop challenge to unlock.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume: Checking if Accessibility is enabled.")
        if (!isAccessibilityServiceEnabled(this, StroopAccessibilityService::class.java)) {
            Log.w(TAG, "onResume: Stroop Accessibility Service is NOT enabled.")
            Toast.makeText(
                this,
                "Stroop Accessibility Service is NOT enabled. Tap 'Enable Service' if you wish to activate it.",
                Toast.LENGTH_LONG
            ).show()
        } else {
            Log.d(TAG, "onResume: Accessibility service is enabled.")

            // Check if there's a challenge in progress
            if (StroopAccessibilityService.challengeInProgress) {
                // Regenerate the challenge
                applyChallenge()
            }
        }
    }

    private fun isAccessibilityServiceEnabled(
        context: Context,
        serviceClass: Class<*>
    ): Boolean {
        val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        val expected = ComponentName(context, serviceClass)
        val enabledServices =
            am.getEnabledAccessibilityServiceList(android.accessibilityservice.AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
        for (serviceInfo in enabledServices) {
            val enabledService = serviceInfo.resolveInfo.serviceInfo
            if (enabledService.packageName == expected.packageName &&
                enabledService.name == expected.className
            ) {
                return true
            }
        }
        return false
    }

    private fun styleMenuButton(button: Button, backgroundColor: Int, textColor: Int) {
        val drawable = GradientDrawable().apply {
            setColor(backgroundColor)
            cornerRadius = 8f
            setStroke(4, Color.parseColor("#757575"))
        }
        button.background = drawable
        button.setTextColor(textColor)

        // Also remove any Material tint from the button
        button.backgroundTintList = null
        button.backgroundTintMode = null
    }

    private fun applyChallenge() {
        Log.d(TAG, "applyChallenge: Generating puzzle.")
        try {
            // 1) random color word
            val randomWord = availablePool.random()
            // 2) random ink color
            var inkColorName: String
            do {
                inkColorName = availablePool.random()
            } while (inkColorName == randomWord)

            // user must pick the ink color
            expectedAnswer = inkColorName

            runOnUiThread {
                challengeText.text = randomWord
                challengeText.textSize = 72f
                challengeText.typeface = ResourcesCompat.getFont(
                    this,
                    R.font.open_sans_extrabold
                )
                val inkColor = Color.parseColor(colorMap[inkColorName] ?: "#000000")
                challengeText.setTextColor(inkColor)
            }

            generateChallengeButtons()

            // Mark challenge as in progress for the service
            StroopAccessibilityService.challengeInProgress = true
        } catch (e: Exception) {
            Log.e(TAG, "applyChallenge: Error generating puzzle", e)
        }
    }

    private fun generateChallengeButtons() {
        try {
            val selectedColors = availablePool.shuffled().toMutableList()
            if (!selectedColors.contains(expectedAnswer)) {
                selectedColors[0] = expectedAnswer
                selectedColors.shuffle()
            }
            val textColorAssignment = simpleCyclicDerangement(selectedColors)

            runOnUiThread {
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
                        "generateButtons: index=$index label=$labelColor textColor=$assignedHex"
                    )

                    val drawable = GradientDrawable().apply {
                        setColor(buttonBgColor)
                        cornerRadius = 8f
                        setStroke(4, Color.parseColor("#757575"))
                    }
                    button.background = drawable

                    // Also remove any Material tint
                    button.backgroundTintList = null
                    button.backgroundTintMode = null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "generateChallengeButtons: Error populating puzzle buttons", e)
        }
    }

    private fun handleButtonClick(button: Button, selectedColor: String) {
        if (selectedColor == expectedAnswer) {
            // Mark challenge completed
            val packageToLaunch =
                StroopAccessibilityService.pendingLockedPackage ?: lockedPackageToLaunch
            if (packageToLaunch != null) {
                StroopAccessibilityService.completedChallenges.add(packageToLaunch)
            }

            val buttonBgColor = Color.parseColor("#CCCCCC")
            val bgDrawable = button.background as GradientDrawable
            val originalTextColor = button.currentTextColor

            // Flash animation
            bgDrawable.setColor(originalTextColor)
            button.setTextColor(Color.parseColor("#212121"))

            Handler(Looper.getMainLooper()).postDelayed({
                // Revert animation
                bgDrawable.setColor(buttonBgColor)
                button.setTextColor(originalTextColor)

                if (selectedColor == expectedAnswer) {
// Mark the challenge as completed before launching
                    (this as? StroopAccessibilityService)?.let {
                        it.markChallengeCompleted(lockedPackageToLaunch ?: packageToLaunch)
                    }

                    // Get the package to launch - check both sources
                    val packageToLaunch =
                        StroopAccessibilityService.pendingLockedPackage ?: lockedPackageToLaunch
                    Log.d(TAG, "Launching app: $packageToLaunch")

                    // Clear the challenge state
                    StroopAccessibilityService.challengeInProgress = false
                    StroopAccessibilityService.pendingLockedPackage = null

                    // Launch the app
                    if (packageToLaunch != null) {
                        launchLockedApp(packageToLaunch)
                    } else {
                        Log.w(TAG, "No package to launch after successful challenge")
                        finish() // Just finish the activity
                    }
                } else {
                    Log.d(TAG, "handleButtonClick: INCORRECT. Regenerating puzzle.")
                    Toast.makeText(this, "Incorrect! Try again.", Toast.LENGTH_SHORT).show()
                    applyChallenge()
                }

                buttonCooldownActive = false
            }, 500)
        }

        private fun launchLockedApp(packageName: String) {
            Log.d(TAG, "launchLockedApp: Attempting to launch $packageName")

            try {
                // Clear any existing tasks for this package
                val clearIntent = Intent(Intent.ACTION_MAIN)
                clearIntent.`package` = packageName
                clearIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)

                // Attempt to launch the app
                val launchIntent = packageManager.getLaunchIntentForPackage(packageName)

                if (launchIntent != null) {
                    Log.d(TAG, "Launching with custom flags")
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)

                    startActivity(launchIntent)
                } else {
                    Log.e(TAG, "No launch intent found for $packageName")
                    Toast.makeText(this, "Could not launch app", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to launch app $packageName", e)
                Toast.makeText(this, "Could not launch app: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                // Always finish the Stroop Lock Activity
                finish()
            }
        }

        private fun pickAppFromLauncher() {
            val intent = Intent(Intent.ACTION_PICK_ACTIVITY).apply {
                putExtra(
                    Intent.EXTRA_INTENT,
                    Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
                )
            }
            startActivityForResult(intent, REQUEST_CODE_PICK_APP)
        }

        @Deprecated("Deprecated in Java")
        override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
            super.onActivityResult(requestCode, resultCode, data)
            if (requestCode == REQUEST_CODE_PICK_APP && resultCode == Activity.RESULT_OK) {
                val component = data?.component
                if (component != null) {
                    val chosenPackage = component.packageName
                    Log.d(TAG, "Locked app set to: $chosenPackage")
                    Toast.makeText(this, "Locked app set to: $chosenPackage", Toast.LENGTH_SHORT)
                        .show()

                    // Insert into DB on background thread:
                    CoroutineScope(Dispatchers.IO).launch {
                        repository.addLockedApp(chosenPackage)

                        // Also set lockedPackageToLaunch so puzzle can open it immediately if user solves
                        lockedPackageToLaunch = chosenPackage

                        // Optionally read back for debugging:
                        val allLocked = repository.getAllLockedApps()
                        Log.d(TAG, "onActivityResult: lockedApps now = $allLocked")
                    }
                }
            }
        }

        private fun <T> simpleCyclicDerangement(list: List<T>): List<T> {
            return if (list.size <= 1) list else list.drop(1) + list.first()
        }

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
}