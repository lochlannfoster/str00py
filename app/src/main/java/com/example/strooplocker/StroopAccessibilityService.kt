package com.example.strooplocker

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.widget.Toast
import com.example.strooplocker.data.LockedAppDatabase
import com.example.strooplocker.data.LockedAppsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class StroopAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "StroopAccessibilityService_DEBUG"
        const val EXTRA_LOCKED_PACKAGE = "TARGET_PACKAGE"
        private const val CHALLENGE_TIMEOUT_MS = 30000L // 30 seconds

        @Volatile
        var challengeInProgress: Boolean = false

        @Volatile
        var pendingLockedPackage: String? = null

        @Volatile
        private var lastChallengeTime: Long = 0

        // Persistent tracking of completed challenges
        val completedChallenges = mutableSetOf<String>()
    }

    // Track the current foreground package
    private var currentForegroundPackage: String? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d(TAG, "onServiceConnected: Configuring accessibility service")

        try {
            serviceInfo = serviceInfo.apply {
                feedbackType = AccessibilityServiceInfo.FEEDBACK_ALL_MASK
                eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
                        AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
                flags = AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                        AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
            }
            Log.d(TAG, "Service connected and configured successfully!")

            // Show a toast to confirm service is running
            CoroutineScope(Dispatchers.Main).launch {
                Toast.makeText(applicationContext, "Stroop Lock Service activated", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error configuring accessibility service", e)
        }

        // Reset challenge state on service connection
        challengeInProgress = false
        pendingLockedPackage = null
        completedChallenges.clear()
    }

    // Helper method to detect home screen
    private fun isHomeScreen(packageName: String): Boolean {
        // Common launcher package names
        return packageName.contains("launcher") ||
                packageName.contains("home") ||
                packageName == "com.google.android.apps.nexuslauncher" ||
                packageName == "com.android.launcher"
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        val eventType = event.eventType
        val packageName = event.packageName?.toString() ?: return
        val className = event.className?.toString() ?: ""

        // Only log important events
        if (eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            Log.d(TAG, "WINDOW CHANGE: package=$packageName, class=$className")

            // Track app switching to detect session endings
            if (isHomeScreen(packageName) && currentForegroundPackage != null &&
                completedChallenges.contains(currentForegroundPackage)) {
                Log.d(TAG, "HOME SCREEN: Ending session for: $currentForegroundPackage")
                completedChallenges.remove(currentForegroundPackage)
            }
            // If we're switching from a locked app to something else
            else if (currentForegroundPackage != packageName &&
                currentForegroundPackage != null &&
                completedChallenges.contains(currentForegroundPackage)) {
                Log.d(TAG, "APP SWITCH: Session ended for: $currentForegroundPackage")
                completedChallenges.remove(currentForegroundPackage)
            }

            // Update current foreground package
            currentForegroundPackage = packageName
        }

        // Skip events from our own app
        if (packageName == "com.example.strooplocker") {
            Log.d(TAG, "SKIPPING: Our own app events")
            return
        }

        // Check if the package is in the locked apps list
        if (eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            checkAndLockApp(packageName)
        }
    }

    private fun checkAndLockApp(packageName: String) {
        Log.d(TAG, "CHECKING: Is $packageName locked? (challenge in progress: $challengeInProgress)")

        // Skip if a challenge is already in progress
        if (challengeInProgress) {
            Log.d(TAG, "SKIPPING: Challenge already in progress")
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val repository = LockedAppsRepository(
                    LockedAppDatabase.getInstance(this@StroopAccessibilityService).lockedAppDao()
                )

                val lockedApps = repository.getAllLockedApps()
                Log.d(TAG, "LOCKED APPS: ${lockedApps.joinToString()}")

                val isLocked = lockedApps.contains(packageName)
                val isCompleted = completedChallenges.contains(packageName)

                Log.d(TAG, "APP STATUS: $packageName | Locked: $isLocked | Completed: $isCompleted")

                // Only launch challenge if app is locked and not recently completed
                if (isLocked && !isCompleted) {
                    // Set challenge in progress to prevent multiple launches
                    challengeInProgress = true
                    pendingLockedPackage = packageName

                    withContext(Dispatchers.Main) {
                        Log.d(TAG, "LAUNCHING CHALLENGE for: $packageName")

                        // Show toast for debugging
                        Toast.makeText(
                            applicationContext,
                            "Stroop challenge for: $packageName",
                            Toast.LENGTH_SHORT
                        ).show()

                        val lockIntent = Intent(this@StroopAccessibilityService, StroopLockActivity::class.java).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                            putExtra(EXTRA_LOCKED_PACKAGE, packageName)
                        }
                        startActivity(lockIntent)
                    }
                } else if (isCompleted) {
                    Log.d(TAG, "SKIPPING: Challenge already completed for $packageName")
                } else if (!isLocked) {
                    Log.d(TAG, "SKIPPING: $packageName is not locked")
                }
            } catch (e: Exception) {
                Log.e(TAG, "ERROR checking locked status: ${e.message}")
                challengeInProgress = false
            }
        }
    }

    // Method to mark a challenge as completed
    fun markChallengeCompleted(packageName: String) {
        completedChallenges.add(packageName)
        challengeInProgress = false
        pendingLockedPackage = null
        Log.d(TAG, "COMPLETED: Challenge for $packageName")
    }

    // Method to reset challenge for a specific package
    fun resetChallenge(packageName: String) {
        completedChallenges.remove(packageName)
        Log.d(TAG, "RESET: Challenge for $packageName")
    }

    override fun onInterrupt() {
        Log.w(TAG, "INTERRUPTED: Accessibility service interrupted!")
        challengeInProgress = false
        pendingLockedPackage = null
    }
}