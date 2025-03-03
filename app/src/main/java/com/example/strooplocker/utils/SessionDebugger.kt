package com.example.strooplocker.utils

import android.util.Log
import com.example.strooplocker.SessionManager
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * Utility class for debugging session management behavior.
 * Maintains a concise log of important session events for debugging purposes.
 */
object SessionDebugger {

    private const val TAG = "SessionDebug"
    private const val MAX_ENTRIES = 100

    // Queue to store the most recent debug events
    private val eventLog = ConcurrentLinkedQueue<DebugEvent>()
    private val dateFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)

    data class DebugEvent(
        val timestamp: Long = System.currentTimeMillis(),
        val type: String,
        val details: String,
        val extraInfo: String = ""
    ) {
        override fun toString(): String {
            val time = dateFormat.format(Date(timestamp))
            return "[$time] $type: $details ${if (extraInfo.isNotEmpty()) "($extraInfo)" else ""}"
        }
    }

    /**
     * Logs a session event with details
     */
    fun logEvent(type: String, details: String, extraInfo: String = "") {
        val event = DebugEvent(
            type = type,
            details = details,
            extraInfo = extraInfo
        )

        // Add to queue and trim if needed
        eventLog.add(event)
        while (eventLog.size > MAX_ENTRIES) {
            eventLog.poll()
        }

        // Also log to system for immediate visibility
        Log.d(TAG, event.toString())
    }

    /**
     * Logs app switch events
     */
    fun logAppSwitch(fromApp: String?, toApp: String) {
        logEvent(
            "APP_SWITCH",
            "${fromApp ?: "null"} → $toApp",
            "Completed: ${SessionManager.getCompletedChallenges().joinToString()}"
        )
    }

    /**
     * Logs session status changes
     */
    fun logSessionStatus(packageName: String, isCompleted: Boolean, reason: String) {
        logEvent(
            "SESSION",
            "$packageName ${if (isCompleted) "UNLOCKED" else "LOCKED"}",
            reason
        )
    }

    /**
     * Logs challenge events
     */
    fun logChallenge(packageName: String, action: String, details: String = "") {
        logEvent("CHALLENGE", "$action: $packageName", details)
    }

    /**
     * Gets the complete event log as formatted text
     */
    fun getEventLog(): String {
        return if (eventLog.isEmpty()) {
            "No session events logged yet."
        } else {
            eventLog.joinToString(separator = "\n")
        }
    }

    /**
     * Gets a comprehensive but concise debug report of current session state
     */
    fun getDebugReport(): String {
        val currentTime = dateFormat.format(Date())
        val inProgress = SessionManager.isChallengeInProgress()
        val currentPkg = SessionManager.getCurrentChallengePackage()
        val completed = SessionManager.getCompletedChallenges()

        val sb = StringBuilder()
        sb.appendLine("=== SESSION DEBUG REPORT [$currentTime] ===")
        sb.appendLine("Challenge in progress: $inProgress")
        sb.appendLine("Current challenge pkg: $currentPkg")
        sb.appendLine("Completed challenges: ${completed.joinToString()}")
        sb.appendLine("--- Recent Events (newest first) ---")

        // Add the most recent events (limited number)
        val recentEvents = eventLog.toList().reversed().take(20)
        recentEvents.forEach { sb.appendLine(it.toString()) }

        return sb.toString()
    }

    /**
     * Clears the event log
     */
    fun clearLog() {
        eventLog.clear()
        Log.d(TAG, "Event log cleared")
    }
}