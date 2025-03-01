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
            if (isHomeScreen(packageName)) {
                // Returning to home screen should end all sessions
                Log.d(TAG, "HOME SCREEN: Ending all sessions")
                val previousPackage = currentForegroundPackage
                if (previousPackage != null) {
                    Log.d(TAG, "Removing completed challenge for: $previousPackage")
                    completedChallenges.remove(previousPackage)
                }
                currentForegroundPackage = packageName
            }
            // If we're switching from one app to another
            else if (currentForegroundPackage != packageName && currentForegroundPackage != null) {
                Log.d(TAG, "APP SWITCH: From $currentForegroundPackage to $packageName")

                // End session for previous app
                if (completedChallenges.contains(currentForegroundPackage)) {
                    Log.d(TAG, "Removing completed challenge for: $currentForegroundPackage")
                    completedChallenges.remove(currentForegroundPackage)
                }

                currentForegroundPackage = packageName

                // Check and lock the new app
                if (packageName != "com.example.strooplocker") {
                    checkAndLockApp(packageName)
                }
            }
            // First app launch or same app (new activity)
            else if (currentForegroundPackage == null || currentForegroundPackage == packageName) {
                currentForegroundPackage = packageName

                // Check and lock on first launch or new activity
                if (packageName != "com.example.strooplocker") {
                    checkAndLockApp(packageName)
                }
            }
        }
    }

    private fun checkAndLockApp(packageName: String) {
        // Add current timestamp to help track timing issues
        val currentTime = System.currentTimeMillis()
        Log.d(TAG, "CHECKING[$currentTime]: Is $packageName locked? (challenge in progress: $challengeInProgress)")

        // Skip if a challenge is already in progress
        if (challengeInProgress) {
            Log.d(TAG, "SKIPPING[$currentTime]: Challenge already in progress")
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val repository = LockedAppsRepository(
                    LockedAppDatabase.getInstance(this@StroopAccessibilityService).lockedAppDao()
                )

                val lockedApps = repository.getAllLockedApps()
                Log.d(TAG, "LOCKED APPS[$currentTime]: ${lockedApps.joinToString()}")
                Log.d(TAG, "COMPLETED CHALLENGES[$currentTime]: ${completedChallenges.joinToString()}")

                val isLocked = lockedApps.contains(packageName)
                val isCompleted = completedChallenges.contains(packageName)

                Log.d(TAG, "APP STATUS[$currentTime]: $packageName | Locked: $isLocked | Completed: $isCompleted")

                // Only launch challenge if app is locked and not recently completed
                if (isLocked && !isCompleted) {
                    // Set challenge in progress to prevent multiple launches
                    // Use synchronized block to avoid race conditions
                    synchronized(this@StroopAccessibilityService) {
                        if (challengeInProgress) {
                            Log.d(TAG, "SKIPPING[$currentTime]: Challenge has been started by another thread")
                            return@synchronized
                        }
                        challengeInProgress = true
                        pendingLockedPackage = packageName

                        // Set a timeout in case the challenge doesn't complete
                        Handler(Looper.getMainLooper()).postDelayed({
                            if (challengeInProgress && pendingLockedPackage == packageName) {
                                Log.w(TAG, "TIMEOUT[$currentTime]: Challenge timed out for $packageName")
                                challengeInProgress = false
                                pendingLockedPackage = null
                            }
                        }, 30000) // 30-second timeout
                    }

                    withContext(Dispatchers.Main) {
                        Log.d(TAG, "LAUNCHING CHALLENGE[$currentTime] for: $packageName")

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
                    Log.d(TAG, "SKIPPING[$currentTime]: Challenge already completed for $packageName")
                } else if (!isLocked) {
                    Log.d(TAG, "SKIPPING[$currentTime]: $packageName is not locked")
                }
            } catch (e: Exception) {
                Log.e(TAG, "ERROR[$currentTime] checking locked status: ${e.message}")
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

    fun resetAllChallenges() {
        synchronized(this) {
            challengeInProgress = false
            pendingLockedPackage = null
            completedChallenges.clear()
            Log.d(TAG, "RESET: All challenges and state reset")
        }
    }

    override fun onInterrupt() {
        Log.w(TAG, "INTERRUPTED: Accessibility service interrupted!")
        challengeInProgress = false
        pendingLockedPackage = null
    }
}