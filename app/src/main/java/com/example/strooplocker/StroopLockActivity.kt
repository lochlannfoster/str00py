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

    private lateinit var challengeText: TextView
    private lateinit var answerGrid: GridLayout
    private lateinit var rootLayout: ConstraintLayout
    private lateinit var exitButton: Button
    private lateinit var selectAppButton: Button
    private lateinit var enableAccessibilityButton: Button

    private lateinit var answerButtons: MutableList<Button>

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

    private var expectedAnswer = ""

    @Volatile
    private var buttonCooldownActive = false

    companion object {
        private const val REQUEST_CODE_PICK_APP = 2001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // NOTE: singleTask or singleTop in the Manifest helps ensure we get launched in a new Task.
        setContentView(R.layout.activity_stroop_lock)

        rootLayout = findViewById(R.id.rootLayout)
        rootLayout.setBackgroundColor(Color.parseColor("#F0F0F0"))

        challengeText = findViewById(R.id.challengeText)
        answerGrid = findViewById(R.id.answerGrid)

        exitButton = findViewById(R.id.exitButton)
        styleMenuButton(exitButton, Color.parseColor("#CCCCCC"), Color.parseColor("#212121"))
        exitButton.setOnClickListener {
            // Mark that the puzzle is incomplete and go Home
            StroopAccessibilityService.challengeInProgress = false
            StroopAccessibilityService.pendingLockedPackage = null

            val startMain = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivity(startMain)
            finish()
        }

        selectAppButton = findViewById(R.id.selectAppButton)
        styleMenuButton(selectAppButton, Color.parseColor("#CCCCCC"), Color.parseColor("#212121"))
        selectAppButton.setOnClickListener {
            pickAppFromLauncher()
        }

        enableAccessibilityButton = findViewById(R.id.enableAccessibilityButton)
        styleMenuButton(enableAccessibilityButton, Color.parseColor("#CCCCCC"), Color.parseColor("#212121"))
        enableAccessibilityButton.setOnClickListener {
            if (!isAccessibilityServiceEnabled()) {
                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                startActivity(intent)
            } else {
                Toast.makeText(this, "Accessibility is already enabled!", Toast.LENGTH_SHORT).show()
            }
        }

        // Create the 3x3 answer buttons
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

        // Make them square
        answerGrid.post {
            answerButtons.forEach { button ->
                val size = button.width
                val params = button.layoutParams
                params.height = size
                button.layoutParams = params
            }
        }

        answerButtons.forEachIndexed { index, button ->
            button.setOnClickListener { handleButtonClick(button, button.text.toString(), index) }
        }

        applyChallenge()
    }

    /**
     * If we come back from the background or something, we can re-apply the puzzle
     */
    override fun onResume() {
        super.onResume()

        // If for some reason we want to re-generate puzzle each time
        if (StroopAccessibilityService.challengeInProgress) {
            applyChallenge()
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_PICK_APP && resultCode == Activity.RESULT_OK) {
            val component = data?.component
            if (component != null) {
                val newLockedApp = component.packageName
                LockManager.addLockedApp(this, newLockedApp)
                Toast.makeText(this, "Locked app added: $newLockedApp", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val manager = getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        val enabledServices = manager.getEnabledAccessibilityServiceList(
            android.accessibilityservice.AccessibilityServiceInfo.FEEDBACK_ALL_MASK
        )
        for (serviceInfo in enabledServices) {
            val enabledService = serviceInfo.resolveInfo.serviceInfo
            if (
                enabledService.packageName == packageName &&
                enabledService.name == "com.example.strooplocker.StroopAccessibilityService"
            ) {
                return true
            }
        }
        return false
    }

    private fun applyChallenge() {
        try {
            var word: String
            var inkColorName: String
            do {
                word = availablePool.random()
                inkColorName = availablePool.random()
            } while (word == inkColorName)

            val isInverse = (0..1).random() == 1
            expectedAnswer = if (isInverse) word else inkColorName

            runOnUiThread {
                challengeText.text = word
                challengeText.textSize = 72f
                challengeText.typeface = ResourcesCompat.getFont(this, R.font.open_sans_extrabold)
                val inkColor = Color.parseColor(colorMap[inkColorName] ?: "#000000")
                challengeText.setTextColor(inkColor)
            }

            generateChallengeButtons()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

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

    private fun handleButtonClick(button: Button, selectedColor: String, buttonIndex: Int) {
        if (buttonCooldownActive) return
        buttonCooldownActive = true

        val buttonBackgroundColor = Color.parseColor("#CCCCCC")
        val bgDrawable = button.background as GradientDrawable
        val originalTextColor = button.currentTextColor

        bgDrawable.setColor(originalTextColor)
        button.setTextColor(Color.parseColor("#212121"))

        Handler(Looper.getMainLooper()).postDelayed({
            bgDrawable.setColor(buttonBackgroundColor)
            button.setTextColor(originalTextColor)

            if (selectedColor == expectedAnswer) {
                Toast.makeText(this, "Correct! Launching locked app...", Toast.LENGTH_SHORT).show()

                // Mark puzzle done
                StroopAccessibilityService.challengeInProgress = false

                // Attempt to relaunch the locked package
                val lockedPackage = StroopAccessibilityService.pendingLockedPackage
                if (!lockedPackage.isNullOrEmpty()) {
                    try {
                        val launchIntent = packageManager.getLaunchIntentForPackage(lockedPackage)
                        if (launchIntent != null) {
                            startActivity(launchIntent)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        Toast.makeText(this, "Unable to launch $lockedPackage", Toast.LENGTH_SHORT).show()
                    } finally {
                        StroopAccessibilityService.pendingLockedPackage = null
                    }
                }
                finish()
            } else {
                Toast.makeText(this, "Incorrect! Try again.", Toast.LENGTH_SHORT).show()
                applyChallenge()
            }

            buttonCooldownActive = false
        }, 500)
    }

    private fun styleMenuButton(button: Button, backgroundColor: Int, textColor: Int) {
        val drawable = GradientDrawable().apply {
            setColor(backgroundColor)
            cornerRadius = 8f
            setStroke(4, Color.parseColor("#757575"))
        }
        button.background = drawable
        button.setTextColor(textColor)
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
