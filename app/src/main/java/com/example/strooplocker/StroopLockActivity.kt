// app/src/main/java/com/example/strooplocker/StroopLockActivity.kt

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
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity

/**
 * Main activity for the str00py app.
 *
 * This activity serves several purposes:
 * 1. Acts as the main entry point for the application
 * 2. Handles the Stroop challenge when a locked app is launched
 * 3. Manages permission requests and app setup
 * 4. Provides UI for users to select apps to lock
 *
 * The Stroop challenge presents a color word displayed in a different color ink,
 * requiring the user to identify the ink color, not the word itself.
 */
class StroopLockActivity : AppCompatActivity() {

    companion object {
        const val TAG = "StroopLockActivity"
        const val EXTRA_LOCKED_PACKAGE = "extra_locked_package"

        // Set of packages that have completed challenges
        // Exposed so other classes can access this information
        val completedChallenges = mutableSetOf<String>()
    }

    // UI elements for the challenge
    private lateinit var challengeText: TextView
    private lateinit var answerGrid: GridLayout
    private lateinit var enableAccessibilityButton: Button

    // Maps color names to their RGB values for the Stroop challenge
    private lateinit var colorMap: Map<String, Int>

    // Activity Result launchers for handling permission flows
    private lateinit var accessibilitySettingsLauncher: ActivityResultLauncher<Intent>
    private lateinit var overlayPermissionLauncher: ActivityResultLauncher<Intent>
    private lateinit var selectAppLauncher: ActivityResultLauncher<Intent>

    // Challenge state variables
    private var correctColor: String? = null
    private var packageToLaunch: String? = null

    /**
     * Initializes the activity, sets up UI components and event handlers,
     * and checks necessary permissions.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_stroop_lock)

        // Register for Activity Results to handle permission request callbacks
        registerActivityResultLaunchers()

        // Initialize UI components and logic
        initUI()

        // Check permissions and setup status
        checkAndRequestPermissions()

        // Handle intent if one was provided
        handleIntent(intent)

        // Initialize color map for the Stroop challenge
        initializeColorMap()
    }

    /**
     * Registers Activity Result launchers to handle callbacks from
     * permission requests and app selection.
     */
    private fun registerActivityResultLaunchers() {
        // Handle accessibility settings result
        accessibilitySettingsLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) {
            // Check if accessibility service is now enabled
            if (isAccessibilityServiceEnabled()) {
                Toast.makeText(this, getString(R.string.toast_accessibility_enabled), Toast.LENGTH_SHORT).show()
                // Check if we need overlay permission next
                if (!Settings.canDrawOverlays(this)) {
                    requestOverlayPermission()
                }
            } else {
                Toast.makeText(this, getString(R.string.toast_accessibility_disabled), Toast.LENGTH_LONG).show()
            }
            // Update permission status UI
            updatePermissionStatus()
        }

        // Handle overlay permission result
        overlayPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) {
            // Check if overlay permission is now granted
            if (Settings.canDrawOverlays(this)) {
                Toast.makeText(this, getString(R.string.toast_overlay_granted), Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, getString(R.string.toast_overlay_needed), Toast.LENGTH_LONG).show()
            }
            // Update permission status UI
            updatePermissionStatus()
        }

        // Handle app selection result
        selectAppLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) {
            // Refresh after returning from app selection
            updatePermissionStatus()
        }
    }

    /**
     * Updates permission status when the activity resumes.
     * This ensures the UI reflects the current permission state.
     */
    override fun onResume() {
        super.onResume()
        // Refresh permission status whenever the activity resumes
        updatePermissionStatus()
    }

    /**
     * Initializes UI components and sets up event handlers.
     */
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

        // Setup app selection button
        findViewById<Button>(R.id.selectAppButton)?.setOnClickListener {
            Log.d(TAG, "Select app button clicked")
            val intent = Intent(this, SelectAppsActivity::class.java)
            selectAppLauncher.launch(intent)
        }

        // Setup accessibility button
        enableAccessibilityButton = findViewById(R.id.enableAccessibilityButton)
        enableAccessibilityButton.setOnClickListener {
            Log.d(TAG, "Enable accessibility button clicked")
            showPermissionsGuide()
        }
    }

    /**
     * Initializes the color map for the Stroop challenge.
     * Maps color names to their RGB values.
     */
    private fun initializeColorMap() {
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

    /**
     * Checks if required permissions are granted and requests them if needed.
     * Shows first-time setup dialog on first launch.
     */
    private fun checkAndRequestPermissions() {
        Log.d(TAG, "Checking app permissions and setup status")

        // Skip permission checks in test mode
        if (intent.getBooleanExtra("test_mode", false)) {
            Log.d(TAG, "Test mode detected, skipping permission checks")
            updatePermissionStatus()
            generateChallenge() // Generate a sample challenge for testing
            return
        }

        // Rest of the method remains the same
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

    /**
     * Shows welcome dialog on first launch to explain the app's purpose
     * and guide the user through the setup process.
     */
    private fun showWelcomeDialog() {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.dialog_welcome_title))
            .setMessage(getString(R.string.dialog_welcome_message))
            .setPositiveButton(getString(R.string.dialog_get_started)) { _, _ ->
                showPermissionsGuide()
            }
            .setCancelable(false)
            .show()
    }

    /**
     * Shows a guide explaining which permissions are needed and why.
     * Based on the missing permissions, presents appropriate guidance.
     */
    private fun showPermissionsGuide() {
        // Check which permissions are needed
        val accessibilityEnabled = isAccessibilityServiceEnabled()
        val overlayPermissionGranted = Settings.canDrawOverlays(this)

        // Build message based on missing permissions
        val missingPermissions = mutableListOf<String>()

        if (!accessibilityEnabled) {
            missingPermissions.add(getString(R.string.accessibility_permission_item))
        }

        if (!overlayPermissionGranted) {
            missingPermissions.add(getString(R.string.overlay_permission_item))
        }

        if (missingPermissions.isEmpty()) {
            // All permissions granted
            Toast.makeText(this, getString(R.string.toast_all_permissions_enabled), Toast.LENGTH_SHORT).show()
            return
        }

        // Show dialog about missing permissions
        val message = getString(R.string.dialog_permissions_message, missingPermissions.joinToString("\n\n"))

        AlertDialog.Builder(this)
            .setTitle(getString(R.string.dialog_permissions_title))
            .setMessage(message)
            .setPositiveButton(getString(R.string.dialog_continue)) { _, _ ->
                startPermissionFlow()
            }
            .setNegativeButton(getString(R.string.dialog_later)) { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    /**
     * Starts the permission request flow, guiding the user through
     * each required permission in sequence.
     */
    private fun startPermissionFlow() {
        // Start with accessibility permission if needed
        if (!isAccessibilityServiceEnabled()) {
            showAccessibilityInstructions()
        } else if (!Settings.canDrawOverlays(this)) {
            // Accessibility is enabled, check overlay permission
            requestOverlayPermission()
        }
    }

    /**
     * Shows instructions for enabling the accessibility service.
     */
    private fun showAccessibilityInstructions() {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.dialog_enable_accessibility_title))
            .setMessage(getString(R.string.dialog_enable_accessibility_message))
            .setPositiveButton(getString(R.string.dialog_open_settings)) { _, _ ->
                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                accessibilitySettingsLauncher.launch(intent)
            }
            .setCancelable(false)
            .show()
    }

    /**
     * Requests permission to display over other apps, which is required
     * to show the challenge overlay.
     */
    private fun requestOverlayPermission() {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.dialog_overlay_title))
            .setMessage(getString(R.string.dialog_overlay_message))
            .setPositiveButton(getString(R.string.dialog_continue)) { _, _ ->
                // Request system overlay permission
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
                overlayPermissionLauncher.launch(intent)
            }
            .setCancelable(false)
            .show()
    }

    /**
     * Checks if the accessibility service is enabled.
     * @return true if the service is enabled, false otherwise
     */
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

    /**
     * Updates the UI based on the current permission status.
     */
    private fun updatePermissionStatus() {
        // Update UI based on permission status
        val accessibilityEnabled = isAccessibilityServiceEnabled()
        val overlayPermissionGranted = Settings.canDrawOverlays(this)

        // Update button text
        if (accessibilityEnabled && overlayPermissionGranted) {
            enableAccessibilityButton.text = "All Permissions OK"
        } else {
            enableAccessibilityButton.text = getString(R.string.enable_accessibility)
        }
    }

    /**
     * Handles the intent that started this activity, processing any locked
     * package information to either show a challenge or launch the app.
     *
     * @param intent The intent that started this activity
     */
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

    /**
     * Handles a locked app by either launching it (if the challenge is already completed)
     * or starting a new challenge.
     *
     * @param packageName The package name of the locked app
     */
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
     * Creates a word displayed in a different ink color and
     * presents a grid of options for the user to select from.
     */
    private fun generateChallenge() {
        Log.d(TAG, "Generating new challenge - BEGIN")

        try {
            // Validate that colorMap is initialized
            if (!::colorMap.isInitialized) {
                Log.e(TAG, "Color map not initialized! Initializing now...")
                initializeColorMap()
            }

            if (colorMap.isEmpty()) {
                Log.e(TAG, "Color map is empty after initialization!")
                Toast.makeText(this, getString(R.string.toast_error_colors), Toast.LENGTH_SHORT).show()
                return
            }

            Log.d(TAG, "Color map has ${colorMap.size} colors: ${colorMap.keys.joinToString()}")

            // Clear existing buttons
            answerGrid.removeAllViews()

            // Get available colors
            val colorNames = colorMap.keys.toList()

            // Ensure we have enough colors
            if (colorNames.size < 2) {
                Log.e(TAG, "Not enough colors for Stroop challenge, only have: ${colorNames.size}")
                Toast.makeText(this, getString(R.string.toast_error_not_enough_colors), Toast.LENGTH_SHORT).show()
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
            val inkColor = colorMap[inkColorName]
            if (inkColor != null) {
                challengeText.setTextColor(inkColor)
                Log.d(TAG, "Set challenge text to '$wordColorName' with ink color '$inkColorName' (${inkColor.toString()})")
            } else {
                Log.e(TAG, "Failed to get color value for '$inkColorName'")
                challengeText.setTextColor(Color.RED) // Fallback
            }

            // Create the buttons for color selection
            createColorButtons(colorNames)

            Log.d(TAG, "Challenge generation complete")
        } catch (e: Exception) {
            Log.e(TAG, "Error generating challenge", e)
            Toast.makeText(this, getString(R.string.toast_error_initialize, e.message), Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Creates the grid of color buttons for the user to select from.
     * Each button displays a color name in a different ink color,
     * maintaining the Stroop effect.
     *
     * @param colorNames List of available color names
     */
    private fun createColorButtons(colorNames: List<String>) {
        try {
            Log.d(TAG, "Creating color buttons with ${colorNames.size} available colors")

            // Clear existing buttons
            answerGrid.removeAllViews()

            // Explicitly set column and row count
            answerGrid.columnCount = 3
            answerGrid.rowCount = 3

            // Create a shuffled list of colors for buttons
            val shuffledColors = colorNames.shuffled()

            // Make sure we don't use more colors than we have available
            val numButtons = Math.min(9, shuffledColors.size)

            Log.d(TAG, "Will create $numButtons buttons")

            // For each color, create a button
            for (i in 0 until numButtons) {
                val colorName = shuffledColors[i]
                val button = Button(this)
                button.text = colorName

                // Make buttons more visible
                button.textSize = 18f
                button.setPadding(8, 16, 8, 16)

                // Set a different text color than the button text (maintaining Stroop effect)
                // Safely get a different color for the text
                val textColorIndex = if (numButtons > 1) {
                    (i + 1) % numButtons
                } else {
                    0 // Fallback if we only have one color
                }

                val textColorName = shuffledColors[textColorIndex]
                button.setTextColor(colorMap[textColorName] ?: Color.BLACK)

                // Fixed layout parameters for consistent button sizes
                val params = GridLayout.LayoutParams()
                params.width = GridLayout.LayoutParams.WRAP_CONTENT
                params.height = GridLayout.LayoutParams.WRAP_CONTENT
                params.setMargins(8, 8, 8, 8)
                params.columnSpec = GridLayout.spec(i % 3, 1f)
                params.rowSpec = GridLayout.spec(i / 3, 1f)
                button.layoutParams = params

                // Set click listener
                button.setOnClickListener {
                    Log.d(TAG, "Color button clicked: $colorName")
                    onColorSelected(colorName)
                }

                // Add to grid
                answerGrid.addView(button)
                Log.d(TAG, "Added button for color: $colorName")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error creating color buttons", e)
            Toast.makeText(this, getString(R.string.toast_error_buttons, e.message), Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Handles the selection of a color button.
     * If the selected color matches the correct answer (the ink color),
     * completes the challenge and launches the locked app.
     * Otherwise, generates a new challenge.
     *
     * @param selectedColor The color name selected by the user
     */
    private fun onColorSelected(selectedColor: String) {
        if (selectedColor == correctColor) {
            Log.d(TAG, "Correct answer selected: $selectedColor")
            Toast.makeText(this, getString(R.string.correct_answer), Toast.LENGTH_SHORT).show()

            // Mark challenge as completed
            packageToLaunch?.let { pkg ->
                Log.d(TAG, "Completing challenge for: $pkg")
                SessionManager.completeChallenge(pkg)

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
            // Incorrect answer
            Log.d(TAG, "Incorrect answer: $selectedColor, expected: $correctColor")
            Toast.makeText(this, getString(R.string.incorrect_answer), Toast.LENGTH_SHORT).show()

            // Generate a new challenge as per the user stories
            generateChallenge()
        }
    }

    /**
     * Checks if a challenge for the given package has been completed recently.
     *
     * @param packageName The package name to check
     * @return true if the challenge has been completed, false otherwise
     */
    private fun checkChallengeCompletion(packageName: String): Boolean {
        // Check if the challenge for this package has been completed recently
        return completedChallenges.contains(packageName) || SessionManager.isChallengeCompleted(packageName)
    }

    /**
     * Launches the locked app after a successful challenge completion.
     *
     * @param packageName The package name of the app to launch
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
            Toast.makeText(this, getString(R.string.toast_error_generic, e.message), Toast.LENGTH_LONG).show()
        }
    }
}