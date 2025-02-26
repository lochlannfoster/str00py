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

        // Exit => go home
        exitButton.setOnClickListener {
            Log.d(TAG, "exitButton clicked: returning home + finishing activity.")
            val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivity(homeIntent)
            finishAffinity()
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
        if (buttonCooldownActive) return
        buttonCooldownActive = true

        Log.d(TAG, "handleButtonClick: user selected=$selectedColor expectedAnswer=$expectedAnswer")

        val buttonBgColor = Color.parseColor("#CCCCCC")
        val bgDrawable = button.background as GradientDrawable
        val originalTextColor = button.currentTextColor

        // flash
        bgDrawable.setColor(originalTextColor)
        button.setTextColor(Color.parseColor("#212121"))

        Handler(Looper.getMainLooper()).postDelayed({
            // revert
            bgDrawable.setColor(buttonBgColor)
            button.setTextColor(originalTextColor)

            if (selectedColor == expectedAnswer) {
                Log.i(TAG, "handleButtonClick: CORRECT. The user found the color=$selectedColor")
                Toast.makeText(this, "Correct!", Toast.LENGTH_SHORT).show()

                // Mark challenge as complete
                StroopAccessibilityService.challengeInProgress = false
                StroopAccessibilityService.pendingLockedPackage = null

                launchLockedApp(lockedPackageToLaunch)
            } else {
                Log.d(TAG, "handleButtonClick: INCORRECT. Regenerating puzzle.")
                Toast.makeText(this, "Incorrect! Try again.", Toast.LENGTH_SHORT).show()
                applyChallenge()
            }

            buttonCooldownActive = false
        }, 500)
    }

    private fun launchLockedApp(packageName: String?) {
        if (packageName == null) {
            Log.w(TAG, "launchLockedApp: No locked app selected; puzzle finishes here.")
            Toast.makeText(this, "No locked app selected!", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            // 1) Attempt the standard approach
            val directIntent = packageManager.getLaunchIntentForPackage(packageName)
            if (directIntent != null) {
                Log.d(
                    TAG,
                    "launchLockedApp: Starting activity via getLaunchIntentForPackage => $packageName"
                )
                startActivity(directIntent)
                finish() // optional
                return
            }

            // 2) Fallback approach with ACTION_MAIN + CATEGORY_LAUNCHER
            val fallbackIntent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
                this.`package` = packageName
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            val resolveInfo = packageManager.resolveActivity(fallbackIntent, 0)
            if (resolveInfo != null) {
                Log.d(TAG, "launchLockedApp: Starting fallback activity => $packageName")
                startActivity(fallbackIntent)
                finish() // optional
                return
            }

            // If both attempts fail:
            Log.w(TAG, "launchLockedApp: Unable to launch $packageName")
            Toast.makeText(this, "Unable to launch $packageName", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e(TAG, "launchLockedApp: Error launching $packageName", e)
            Toast.makeText(this, "Error launching $packageName", Toast.LENGTH_SHORT).show()
        }
    }

    private fun pickAppFromLauncher() {
        val intent = Intent(Intent.ACTION_PICK_ACTIVITY).apply {
            putExtra(Intent.EXTRA_INTENT,
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
                Toast.makeText(this, "Locked app set to: $chosenPackage", Toast.LENGTH_SHORT).show()

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