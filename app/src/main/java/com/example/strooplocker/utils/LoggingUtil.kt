package com.example.strooplocker.utils

import android.util.Log

/**
 * Utility class for consistent logging throughout the application.
 *
 * This singleton provides standardized methods for logging different types
 * of messages (debug, error, warning) with a consistent format. The format
 * includes the calling method name to make logs easier to trace.
 *
 * Using this util instead of direct Log calls allows for:
 * - Consistent log formatting across the app
 * - Easy enabling/disabling of logs for release builds
 * - Centralized control of logging behavior
 */
object LoggingUtil {

    /**
     * Logs a debug message with the specified tag, method, and message.
     *
     * @param tag The log tag, typically the class name
     * @param method The method name where the log is called from
     * @param message The message to log
     */
    fun debug(tag: String, method: String, message: String) {
        Log.d(tag, "[$method] $message")
    }

    /**
     * Logs an error message with the specified tag, method, message, and optional throwable.
     *
     * @param tag The log tag, typically the class name
     * @param method The method name where the log is called from
     * @param message The message to log
     * @param throwable Optional exception to include in the log
     */
    fun error(tag: String, method: String, message: String, throwable: Throwable? = null) {
        if (throwable != null) {
            Log.e(tag, "[$method] $message", throwable)
        } else {
            Log.e(tag, "[$method] $message")
        }
    }

    /**
     * Logs a warning message with the specified tag, method, and message.
     *
     * @param tag The log tag, typically the class name
     * @param method The method name where the log is called from
     * @param message The message to log
     */
    fun warning(tag: String, method: String, message: String) {
        Log.w(tag, "[$method] $message")
    }
}