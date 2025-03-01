package com.example.strooplocker

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate

/**
 * Custom Application class for the str00py app.
 *
 * This class initializes app-wide configurations and settings.
 * It's declared in the AndroidManifest.xml file to ensure it's
 * created when the app starts.
 */
class MyApplication : Application() {

    /**
     * Called when the application is starting.
     * Sets up global app configurations.
     */
    override fun onCreate() {
        super.onCreate()

        // Configure the app to follow the system theme (light or dark)
        // This ensures the app respects the user's device theme preferences
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
    }
}