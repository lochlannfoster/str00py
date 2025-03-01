package com.example.strooplocker

import android.accessibilityservice.AccessibilityServiceInfo
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.accessibility.AccessibilityManager
import android.widget.Button
import android.widget.GridLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class StroopLockActivity : AppCompatActivity() {

    companion object {
        const val TAG = "StroopLockActivity"
        const val EXTRA_LOCKED_PACKAGE = "extra_locked_package"
        const val REQUEST_CODE_PICK_APP = 1001
        const val REQUEST_CODE_ACCESSIBILITY = 1002
        const val REQUEST_CODE_OVERLAY = 1003

        // Exposed so other classes can read the set
        val completedChallenges = mutableSetOf<String>()
    }

    // Challenge UI elements
    private lateinit var challengeText: TextView
    private lateinit var answerGrid: GridLayout
    private lateinit var enableAccessibilityButton: Button
    private lateinit var colorMap: Map<String, Int>

    // Challenge state variables
    private var correctColor: String? = null
    private var packageToLaunch: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_stroop_lock)

        // Initialize UI components and logic
        initUI()

        // Check permissions and setup status
        checkAndRequestPermissions()

        // Handle intent if one was provided
        handleIntent(intent)

        colorMap = mapOf(
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
       Log.d(TAG, "Color map initialized with ${colorMap.size} colors")
    }

    override fun onResume() {
        super.onResume()
        // Refresh permission status whenever the activity resumes
        updatePermissionStatus()
    }

    private fun initUI() {
        Log.d(TAG, "Initializing UI components")

        // Find views
        challengeText = findViewById(R.id.challengeText)
        answerGrid = findViewById(R.id.answerGrid)

        // Setup exit button - just close the activity without unlocking
        findViewById<Button>(R.id.exitButton)?.setOnClickListener {
            Log.d(TAG, "Exit button clicked - returning to home")

            // Launch home screen instead of the locked app
            val homeIntent = Intent(Intent.ACTION_MAIN)
            homeIntent.addCategory(Intent.CATEGORY_HOME)
            homeIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(homeIntent)

            // Close this activity
            finish()
        }

        findViewById<Button>(R.id.selectAppButton)?.setOnClickListener {
            Log.d(TAG, "Select app button clicked")
            val intent = Intent(this, SelectAppsActivity::class.java)
            startActivity(intent)
        }

        enableAccessibilityButton = findViewById(R.id.enableAccessibilityButton)
        enableAccessibilityButton.setOnClickListener {
            Log.d(TAG, "Enable accessibility button clicked")
            showPermissionsGuide()
        }
    }

    private fun checkAndRequestPermissions() {
        Log.d(TAG, "Checking app permissions and setup status")

        // Check if this is the first launch
        val prefs = getSharedPreferences("stroop_prefs", Context.MODE_PRIVATE)
        val isFirstLaunch = prefs.getBoolean("is_first_launch", true)

        if (isFirstLaunch) {
            // Show initial setup dialog
            showWelcomeDialog()

            // Mark that first launch has been done
            prefs.edit().putBoolean("is_first_launch", false).apply()
        } else {
            // Just update UI based on current permission status
            updatePermissionStatus()
        }
    }

    private fun showWelcomeDialog() {
        AlertDialog.Builder(this)
            .setTitle("Welcome to StroopLocker")
            .setMessage("This app helps prevent mindless scrolling by requiring you to solve a quick cognitive challenge before using locked apps.\n\nTo get started, you'll need to grant a few permissions.")
            .setPositiveButton("Get Started") { _, _ ->
                showPermissionsGuide()
            }
            .setCancelable(false)
            .show()
    }

    private fun showPermissionsGuide() {
        // Check which permissions are needed
        val accessibilityEnabled = isAccessibilityServiceEnabled()
        val overlayPermissionGranted = Settings.canDrawOverlays(this)

        // Build message based on missing permissions
        val missingPermissions = mutableListOf<String>()

        if (!accessibilityEnabled) {
            missingPermissions.add("• Accessibility Service: Required to detect when you launch a locked app")
        }

        if (!overlayPermissionGranted) {
            missingPermissions.add("• Display Over Other Apps: Required to show challenges over locked apps")
        }

        if (missingPermissions.isEmpty()) {
            // All permissions granted
            Toast.makeText(this, "All permissions are enabled!", Toast.LENGTH_SHORT).show()
            return
        }

        // Show dialog about missing permissions
        val message = "Please enable the following permissions:\n\n" +
                missingPermissions.joinToString("\n\n") +
                "\n\nYou'll be guided through each permission."

        AlertDialog.Builder(this)
            .setTitle("Required Permissions")
            .setMessage(message)
            .setPositiveButton("Continue") { _, _ ->
                startPermissionFlow()
            }
            .setNegativeButton("Later") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun startPermissionFlow() {
        // Start with accessibility permission if needed
        if (!isAccessibilityServiceEnabled()) {
            showAccessibilityInstructions()
        } else if (!Settings.canDrawOverlays(this)) {
            // Accessibility is enabled, check overlay permission
            requestOverlayPermission()
        }
    }

    private fun showAccessibilityInstructions() {
        AlertDialog.Builder(this)
            .setTitle("Enable Accessibility Service")
            .setMessage("1. Find \"Stroop Lock Service\" in the list\n2. Toggle it ON\n3. Confirm in the dialog\n4. Return to this app")
            .setPositiveButton("Open Settings") { _, _ ->
                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                startActivityForResult(intent, REQUEST_CODE_ACCESSIBILITY)
            }
            .setCancelable(false)
            .show()
    }

    private fun requestOverlayPermission() {
        AlertDialog.Builder(this)
            .setTitle("Display Over Other Apps")
            .setMessage("StroopLocker needs permission to display challenges over other apps.")
            .setPositiveButton("Continue") { _, _ ->
                // Request system overlay permission
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
                startActivityForResult(intent, REQUEST_CODE_OVERLAY)
            }
            .setCancelable(false)
            .show()
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val am = getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        val enabledServices = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)

        for (service in enabledServices) {
            if (service.id.contains(packageName)) {
                return true
            }
        }
        return false
    }

    private fun updatePermissionStatus() {
        // Update UI based on permission status
        val accessibilityEnabled = isAccessibilityServiceEnabled()
        val overlayPermissionGranted = Settings.canDrawOverlays(this)

        // Update button text
        if (accessibilityEnabled && overlayPermissionGranted) {
            enableAccessibilityButton.text = "All Permissions OK"
        } else {
            enableAccessibilityButton.text = "Enable Permissions"
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        // Check permission status after returning from system settings
        when (requestCode) {
            REQUEST_CODE_ACCESSIBILITY -> {
                if (isAccessibilityServiceEnabled()) {
                    Toast.makeText(this, "Accessibility service enabled!", Toast.LENGTH_SHORT).show()
                    // Check if we need overlay permission next
                    if (!Settings.canDrawOverlays(this)) {
                        requestOverlayPermission()
                    }
                } else {
                    Toast.makeText(this, "Accessibility service not enabled", Toast.LENGTH_LONG).show()
                }
            }
            REQUEST_CODE_OVERLAY -> {
                if (Settings.canDrawOverlays(this)) {
                    Toast.makeText(this, "Overlay permission granted!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Overlay permission needed for full functionality", Toast.LENGTH_LONG).show()
                }
            }
        }

        // Update permission status UI
        updatePermissionStatus()
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
    /**
     * Generates a new Stroop challenge with random colors.
     * The word and ink color will always be different to create the Stroop effect.
     */
    private fun generateChallenge() {
        Log.d(TAG, "Generating new challenge")

        // Validate that colorMap is initialized
        if (!::colorMap.isInitialized || colorMap.isEmpty()) {
            Log.e(TAG, "Color map not initialized or empty!")
            Toast.makeText(this, "Error initializing challenge", Toast.LENGTH_SHORT).show()
            return
        }

        // Clear existing buttons
        answerGrid.removeAllViews()

        // Get available colors
        val colorNames = colorMap.keys.toList()
        Log.d(TAG, "Available colors for challenge: ${colorNames.joinToString()}")

        // Ensure we have enough colors
        if (colorNames.size < 2) {
            Log.e(TAG, "Not enough colors for Stroop challenge")
            Toast.makeText(this, "Not enough colors available", Toast.LENGTH_SHORT).show()
            return
        }

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
        // Log the start of the method for debugging
        Log.d(TAG, "Creating color buttons with ${colorNames.size} available colors")

        // Clear existing buttons
        answerGrid.removeAllViews()

        // Create a shuffled list of colors for buttons
        val shuffledColors = colorNames.shuffled()

        // Make sure we don't use more colors than we have available
        val numButtons = Math.min(9, shuffledColors.size)

        // Double-check we have at least 2 colors to work with
        if (numButtons < 2) {
            Log.e(TAG, "Not enough colors available for Stroop challenge, found: $numButtons")
            Toast.makeText(this, "Not enough colors available", Toast.LENGTH_SHORT).show()
            return
        }

        Log.d(TAG, "Creating $numButtons buttons with shuffled colors")

        // For each color, create a button
        for (i in 0 until numButtons) {
            val colorName = shuffledColors[i]
            val button = Button(this)
            button.text = colorName

            // Set a different text color than the button text (maintaining Stroop effect)
            // We mod by numButtons to ensure we stay within bounds
            val textColorIndex = (i + 1) % numButtons
            val textColorName = shuffledColors[textColorIndex]

            // Add context to our logs
            Log.d(TAG, "Button $i: text='$colorName', textColorIndex=$textColorIndex, textColor='$textColorName'")

            // Set text color
            button.setTextColor(colorMap[textColorName] ?: Color.BLACK)

            // Add shadow to create text stroke/outline effect
            button.setShadowLayer(1.0f, 1.0f, 1.0f, Color.BLACK)

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

        Log.d(TAG, "Successfully created and added $numButtons color buttons")
    }

    private fun onColorSelected(selectedColor: String) {
        if (selectedColor == correctColor) {
            Log.d(TAG, "Correct answer selected: $selectedColor")
            Toast.makeText(this, getString(R.string.correct_answer), Toast.LENGTH_SHORT).show()

            // Mark challenge as completed
            packageToLaunch?.let { pkg ->
                completedChallenges.add(pkg)

                // Notify the accessibility service that this package passed the challenge
                val accessibilityService = applicationContext
                    .getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager

                // Complete the challenge in the manager
                ChallengeManager.completeChallenge(true)

                // Add a short delay before launching app to allow UI feedback
                challengeText.postDelayed({
                    Log.d(TAG, "Launching locked app: $pkg")
                    launchLockedApp(pkg)
                }, 500) // 500ms delay for visual feedback
            } ?: run {
                // No package to launch, just finish
                finish()
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
     * This also closes the current activity.
     */
    private fun launchLockedApp(packageName: String) {
        Log.d(TAG, "Launching locked app: $packageName")
        try {
            val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
            if (launchIntent != null) {
                // Add flags to make sure it starts cleanly
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)

                // Start the app
                startActivity(launchIntent)

                // Explicitly finish this activity AFTER launching the app
                finish()

                // Log completion
                Log.d(TAG, "Successfully launched $packageName and finished StroopLockActivity")
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
    }}