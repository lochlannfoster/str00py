package com.example.strooplocker

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent

class StroopAccessibilityService : AccessibilityService() {

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Called when an accessibility event occurs.
        // Implement your logic here if you want to monitor windows/apps.
    }

    override fun onInterrupt() {
        // Called when the service is interrupted.
    }
}
