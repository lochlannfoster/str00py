package com.example.strooplocker.utils

import android.util.Log

object LoggingUtil {
    fun debug(tag: String, method: String, message: String) {
        Log.d(tag, "[$method] $message")
    }

    fun error(tag: String, method: String, message: String, throwable: Throwable? = null) {
        if (throwable != null) {
            Log.e(tag, "[$method] $message", throwable)
        } else {
            Log.e(tag, "[$method] $message")
        }
    }

    fun warning(tag: String, method: String, message: String) {
        Log.w(tag, "[$method] $message")
    }
}