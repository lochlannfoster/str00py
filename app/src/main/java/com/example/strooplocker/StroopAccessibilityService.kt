package com.example.strooplocker

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.os.SystemClock
import android.util.Log
import android.view.accessibility.AccessibilityEvent
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
        private val completedChallenges = mutableSetOf<String>()
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d(TAG, "onServiceConnected: Configuring accessibility service")
        serviceInfo = serviceInfo.apply {
            feedbackType = AccessibilityServiceInfo.FEEDBACK_ALL_MASK
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
                    AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
            flags = AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                    AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
        }
        Log.d(TAG, "Service connected and configured successfully!")

        // Reset challenge state on service connection
        challengeInProgress = false
        pendingLockedPackage = null
        completedChallenges.clear()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        val eventType = event.eventType
        val packageName = event.packageName?.toString() ?: return
        val className = event.className?.toString() ?: ""

        Log.d(TAG, "Accessibility event: type=$eventType, package=$packageName, class=$className")

        // Skip events from our own app
        if (packageName == "com.example.strooplocker") {
            Log.d(TAG, "Skipping our own app events")
            return
        }

        // Check if the package is in the locked apps list
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val repository = LockedAppsRepository(
                    LockedAppDatabase.getInstance(this@StroopAccessibilityService).lockedAppDao()
                )

                val lockedApps = repository.getAllLockedApps()
                val isLocked = lockedApps.contains(packageName)
                val isCompleted = completedChallenges.contains(packageName)

                Log.d(TAG, "Package: $packageName, Locked: $isLocked, Challenge completed: $isCompleted")

                // Only launch challenge if app is locked and not recently completed
                if (isLocked && !isCompleted && !challengeInProgress) {
                    // Set challenge in progress to prevent multiple launches
                    challengeInProgress = true
                    pendingLockedPackage = packageName

                    withContext(Dispatchers.Main) {
                        Log.d(TAG, "Launching challenge for locked app: $packageName")
                        val lockIntent = Intent(this@StroopAccessibilityService, StroopLockActivity::class.java).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                            putExtra(EXTRA_LOCKED_PACKAGE, packageName)
                        }
                        startActivity(lockIntent)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in accessibility event handling", e)
                challengeInProgress = false
            }
        }
    }
    
    // Remove the direct method call
    fun markChallengeCompleted(packageName: String) {
        completedChallenges.add(packageName)
        challengeInProgress = false
        pendingLockedPackage = null
        Log.d(TAG, "Challenge completed for: $packageName")
    }

    // Method to reset challenge for a specific package
    fun resetChallenge(packageName: String) {
        completedChallenges.remove(packageName)
        Log.d(TAG, "Challenge reset for: $packageName")
    }

    override fun onInterrupt() {
        Log.w(TAG, "Accessibility service interrupted!")
        challengeInProgress = false
        pendingLockedPackage = null
        completedChallenges.clear()
    }
}