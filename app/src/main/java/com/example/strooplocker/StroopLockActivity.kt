package com.example.strooplocker

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Shader
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.util.TypedValue
import android.view.View
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
    private lateinit var challengePrompt: TextView
    private lateinit var answerGrid: GridLayout
    private lateinit var rootLayout: ConstraintLayout

    // Top menu buttons – these share the same style as in Settings
    private lateinit var statsButton: Button
    private lateinit var settingsButton: Button
    private lateinit var exitButton: Button

    // List to hold answer buttons (generated dynamically)
    private lateinit var answerButtons: MutableList<Button>

    // SharedPreferences for saving stats and settings
    private lateinit var sharedPreferences: android.content.SharedPreferences

    // Mapping of color names to hex codes
    private val colorMap = mapOf(
        "Red" to "#FF0000",
        "Green" to "#00FF00",
        "Blue" to "#3366FF",
        "Yellow" to "#CCFF33",
        "Pink" to "#FF66FF",
        "Orange" to "#FF6600",
        "Brown" to "#FF8000",
        "Cyan" to "#00FFFF",
        "Black" to "#000000",
        "White" to "#FFFFFF",
        "Purple" to "#8A00E6"
    )
    private val colorNames = colorMap.keys.toList()

    // The expected answer (the ink color name)
    private var expectedAnswer = ""
    private var lastChallengeWord = ""
    private var lastInkColor = ""
    // To avoid reusing the same incorrect button
    private var lastIncorrectButtonIndex = -1

    // Cooldown flag so rapid clicks are ignored
    @Volatile
    private var buttonCooldownActive = false

    // Stats keys (stored in SharedPreferences)
    private val KEY_SUCCESSFUL = "stats_successful"
    private val KEY_UNSUCCESSFUL = "stats_unsuccessful"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_stroop_lock)

        // Hide status bar for full screen
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.hide(android.view.WindowInsets.Type.statusBars())
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN
        }

        // Initialize views from XML
        rootLayout = findViewById(R.id.rootLayout)
        challengeText = findViewById(R.id.challengeText)
        challengePrompt = findViewById(R.id.challengePrompt)
        challengePrompt.visibility = View.GONE
        answerGrid = findViewById(R.id.answerGrid)
        statsButton = findViewById(R.id.statsButton)
        settingsButton = findViewById(R.id.settingsButton)
        exitButton = findViewById(R.id.exitButton)

        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences("StroopLockerPrefs", Context.MODE_PRIVATE)

        // Programmatically create 9 answer buttons and add them to the GridLayout
        answerButtons = mutableListOf()
        answerGrid.columnCount = 3
        answerGrid.rowCount = 3
        for (i in 0 until 9) {
            val btn = Button(this)
            // Create layout parameters with equal weight for a square grid cell
            val params = GridLayout.LayoutParams()
            params.rowSpec = GridLayout.spec(i / 3, 1f)
            params.columnSpec = GridLayout.spec(i % 3, 1f)
            // Set margins as needed
            params.setMargins(8, 8, 8, 8)
            btn.layoutParams = params
            answerGrid.addView(btn)
            answerButtons.add(btn)
        }

        // Style top menu buttons using saved background color
        val savedColor = sharedPreferences.getString("background_color", "Black") ?: "Black"
        val mainBgHex = colorMap[savedColor] ?: "#000000"
        rootLayout.setBackgroundColor(Color.parseColor(mainBgHex))
        val baseButtonBackground = if (savedColor == "Black") Color.WHITE else Color.BLACK
        val baseButtonBorder = if (baseButtonBackground == Color.WHITE) Color.BLACK else Color.WHITE

        fun styleMenuButton(button: Button) {
            val drawable = GradientDrawable()
            drawable.setColor(baseButtonBackground)
            drawable.cornerRadius = 8f
            drawable.setStroke(4, baseButtonBorder)
            button.background = drawable
            button.setTextColor(baseButtonBorder)
        }
        statsButton.setOnClickListener {
            startActivity(Intent(this, StatsActivity::class.java))
        }
        settingsButton.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
        exitButton.setOnClickListener {
            finishAffinity()
        }
        styleMenuButton(statsButton)
        styleMenuButton(settingsButton)
        styleMenuButton(exitButton)

        // Set click listeners for the dynamically created answer buttons
        answerButtons.forEachIndexed { index, button ->
            button.setOnClickListener { handleButtonClick(button, button.text.toString(), index) }
        }

        // After layout pass, make sure buttons are perfectly square
        answerGrid.post {
            answerButtons.forEach { button ->
                val size = button.width
                val params = button.layoutParams
                params.height = size
                button.layoutParams = params
            }
        }

        // Generate the first challenge
        applyBackgroundColorAndGenerateText()
    }

    private fun applyBackgroundColorAndGenerateText() {
        Log.d("StroopLocker", "Applying background color and generating new challenge")
        try {
            // Re-read saved background color in case it changed
            val savedColor = sharedPreferences.getString("background_color", "Black") ?: "Black"
            val bgHex = colorMap[savedColor] ?: "#000000"
            rootLayout.setBackgroundColor(Color.parseColor(bgHex))
            Log.d("StroopLocker", "Background color set to $savedColor ($bgHex)")

            // Generate a new challenge: choose two distinct colors ensuring variety from previous challenge
            var challengeWord: String
            var inkColorName: String
            do {
                challengeWord = colorNames.random()
                inkColorName = colorNames.random()
            } while (
                challengeWord == inkColorName ||
                colorMap[inkColorName] == bgHex ||
                challengeWord == lastChallengeWord ||
                inkColorName == lastInkColor
            )
            lastChallengeWord = challengeWord
            lastInkColor = inkColorName
            expectedAnswer = inkColorName

            // Update challenge text appearance
            runOnUiThread {
                challengeText.text = challengeWord
                val inkColor = Color.parseColor(colorMap[inkColorName] ?: "#000000")
                challengeText.setTextColor(inkColor)
                challengeText.textSize = 72f
                challengeText.typeface = ResourcesCompat.getFont(this, R.font.open_sans_extrabold)
                challengeText.setShadowLayer(8f, 0f, 0f, if (isColorDark(inkColor)) Color.WHITE else Color.BLACK)
                Log.d("StroopLocker", "Challenge text: '$challengeWord' rendered in $inkColorName")
            }
            generateChallengeButtons()
        } catch (e: Exception) {
            Log.e("StroopLocker", "Error during applyBackgroundColorAndGenerateText: ${e.message}", e)
        }
    }

    private fun generateChallengeButtons() {
        CoroutineScope(Dispatchers.Default).launch {
            try {
                val correctColor = expectedAnswer
                val availableColors = colorNames.filter { it != correctColor && it != lastChallengeWord }.toMutableList()
                availableColors.shuffle()
                val selectedColors = (mutableListOf(correctColor) + availableColors.take(8)).toMutableList()
                selectedColors.shuffle()

                if (lastIncorrectButtonIndex != -1) {
                    val currentCorrectIndex = selectedColors.indexOf(expectedAnswer)
                    if (currentCorrectIndex == lastIncorrectButtonIndex) {
                        val swapIndex = if (lastIncorrectButtonIndex == 0) 1 else 0
                        val temp = selectedColors[swapIndex]
                        selectedColors[swapIndex] = selectedColors[lastIncorrectButtonIndex]
                        selectedColors[lastIncorrectButtonIndex] = temp
                        Log.d("StroopLocker", "Swapped correct answer from index $currentCorrectIndex to $swapIndex")
                    }
                }

                withContext(Dispatchers.Main) {
                    val savedColor = sharedPreferences.getString("background_color", "Black") ?: "Black"
                    val baseButtonBackground = if (savedColor == "Black") Color.WHITE else Color.BLACK
                    val baseButtonBorder = if (baseButtonBackground == Color.WHITE) Color.BLACK else Color.WHITE

                    val availableTextColors = colorNames.filter {
                        val hex = colorMap[it] ?: "#000000"
                        hex != String.format("#%06X", 0xFFFFFF and baseButtonBackground)
                    }.shuffled()
                    val textColorList = availableTextColors.take(answerButtons.size)

                    answerButtons.forEachIndexed { index, button ->
                        val labelColor = selectedColors[index]
                        button.text = labelColor
                        val dynamicTextSize = calculateFontSizeForWord(labelColor)
                        button.setTextSize(TypedValue.COMPLEX_UNIT_SP, dynamicTextSize)
                        button.typeface = ResourcesCompat.getFont(this@StroopLockActivity, R.font.open_sans_extrabold)
                        val textColor = Color.parseColor(colorMap[textColorList[index]])
                        button.setTextColor(textColor)

                        val drawable = GradientDrawable()
                        drawable.setColor(baseButtonBackground)
                        drawable.cornerRadius = 8f
                        drawable.setStroke(4, baseButtonBorder)
                        button.background = drawable

                        val contrast = getContrastRatio(textColor, baseButtonBackground)
                        if (contrast < 4.5) {
                            applyGradientOverlay(button, textColor)
                        } else {
                            button.paint.shader = null
                        }
                        Log.d("StroopLocker", "Button $index: label=$labelColor, text color=${textColorList[index]}, textSize=${dynamicTextSize}sp")
                    }
                    lastIncorrectButtonIndex = -1
                }
            } catch (e: Exception) {
                Log.e("StroopLocker", "Error during generateChallengeButtons: ${e.message}", e)
            }
        }
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

    private fun applyGradientOverlay(button: Button, textColor: Int) {
        button.post {
            val textHeight = button.paint.descent() - button.paint.ascent()
            val overlayIntensity = 0.5f
            val lighterColor = blendColors(textColor, Color.WHITE, overlayIntensity)
            val gradient = LinearGradient(
                0f, 0f, 0f, textHeight,
                intArrayOf(textColor, lighterColor),
                null,
                Shader.TileMode.CLAMP
            )
            button.paint.shader = gradient
            button.invalidate()
        }
    }

    private fun blendColors(color1: Int, color2: Int, ratio: Float): Int {
        val inverseRatio = 1f - ratio
        val a = (Color.alpha(color1) * inverseRatio + Color.alpha(color2) * ratio).toInt()
        val r = (Color.red(color1) * inverseRatio + Color.red(color2) * ratio).toInt()
        val g = (Color.green(color1) * inverseRatio + Color.green(color2) * ratio).toInt()
        val b = (Color.blue(color1) * inverseRatio + Color.blue(color2) * ratio).toInt()
        return Color.argb(a, r, g, b)
    }

    private fun getContrastRatio(color1: Int, color2: Int): Double {
        val l1 = getRelativeLuminance(color1)
        val l2 = getRelativeLuminance(color2)
        val lighter = Math.max(l1, l2)
        val darker = Math.min(l1, l2)
        return (lighter + 0.05) / (darker + 0.05)
    }

    private fun getRelativeLuminance(color: Int): Double {
        val r = Color.red(color) / 255.0
        val g = Color.green(color) / 255.0
        val b = Color.blue(color) / 255.0
        val rLin = if (r <= 0.03928) r / 12.92 else Math.pow((r + 0.055) / 1.055, 2.4)
        val gLin = if (g <= 0.03928) g / 12.92 else Math.pow((g + 0.055) / 1.055, 2.4)
        val bLin = if (b <= 0.03928) b / 12.92 else Math.pow((b + 0.055) / 1.055, 2.4)
        return 0.2126 * rLin + 0.7152 * gLin + 0.0722 * bLin
    }

    private fun isColorDark(color: Int): Boolean {
        val r = Color.red(color) / 255.0
        val g = Color.green(color) / 255.0
        val b = Color.blue(color) / 255.0
        val rLin = if (r <= 0.03928) r / 12.92 else Math.pow((r + 0.055) / 1.055, 2.4)
        val gLin = if (g <= 0.03928) g / 12.92 else Math.pow((g + 0.055) / 1.055, 2.4)
        val bLin = if (b <= 0.03928) b / 12.92 else Math.pow((b + 0.055) / 1.055, 2.4)
        val luminance = 0.2126 * rLin + 0.7152 * gLin + 0.0722 * bLin
        return luminance < 0.5
    }

    private fun incrementStat(key: String) {
        val current = sharedPreferences.getInt(key, 0)
        sharedPreferences.edit().putInt(key, current + 1).apply()
    }

    private fun handleButtonClick(button: Button, selectedColor: String, buttonIndex: Int) {
        Log.d("StroopLocker", "Button clicked: color=$selectedColor, index=$buttonIndex")
        if (buttonCooldownActive) {
            Log.d("StroopLocker", "Button click ignored due to cooldown")
            return
        }
        buttonCooldownActive = true

        val bgDrawable = button.background
        val savedColor = sharedPreferences.getString("background_color", "Black") ?: "Black"
        val baseButtonBackground = if (savedColor == "Black") Color.WHITE else Color.BLACK
        val originalTextColor = button.currentTextColor
        val originalShader = button.paint.shader

        val contrast = getContrastRatio(originalTextColor, baseButtonBackground)
        if (contrast < 4.5 && bgDrawable is GradientDrawable) {
            applyGradientOverlay(button, originalTextColor)
        } else if (bgDrawable is GradientDrawable) {
            bgDrawable.setColor(originalTextColor)
            button.setTextColor(baseButtonBackground)
        }

        Handler(Looper.getMainLooper()).postDelayed({
            if (bgDrawable is GradientDrawable) {
                bgDrawable.setColor(baseButtonBackground)
            }
            button.setTextColor(originalTextColor)
            button.paint.shader = originalShader

            if (selectedColor == expectedAnswer) {
                incrementStat(KEY_SUCCESSFUL)
                Toast.makeText(this, "Correct! Launching locked app...", Toast.LENGTH_SHORT).show()
                // TODO: Implement locked app launching logic here.
            } else {
                incrementStat(KEY_UNSUCCESSFUL)
                Log.d("StroopLocker", "Incorrect answer: expected=$expectedAnswer, selected=$selectedColor")
                lastIncorrectButtonIndex = buttonIndex
                Toast.makeText(this, "Incorrect! Try again.", Toast.LENGTH_SHORT).show()
            }
            applyBackgroundColorAndGenerateText()
            buttonCooldownActive = false
        }, 500)
    }
}
