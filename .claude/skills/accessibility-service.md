---
name: accessibility-service
description: Use when modifying StroopAccessibilityService or debugging app detection
---

# Accessibility Service Skill

## Overview

`StroopAccessibilityService` monitors app launches to trigger Stroop challenges for locked apps.

## Key File
`app/src/main/java/com/example/strooplocker/StroopAccessibilityService.kt`

## How It Works

1. Service registers for `TYPE_WINDOW_STATE_CHANGED` events
2. When foreground app changes, `onAccessibilityEvent()` fires
3. Service checks if package is in locked list
4. If locked and no active session, launches `StroopLockActivity`

## Configuration

`app/src/main/res/xml/accessibility_service_config.xml`:
```xml
<accessibility-service
    android:accessibilityEventTypes="typeWindowStateChanged"
    android:accessibilityFeedbackType="feedbackGeneric"
    android:canRetrieveWindowContent="false"
    android:notificationTimeout="100" />
```

## Testing Considerations

- **Cannot unit test** - Requires Android framework
- **Emulator limitations** - Behavior differs from real devices
- **Permission requirements** - User must enable in Settings

## Manual Testing Steps

1. Install app on device
2. Go to Settings > Accessibility > str00py
3. Enable the service
4. Grant overlay permission when prompted
5. Select apps to lock
6. Switch to a locked app
7. Verify challenge appears

## Common Modifications

### Adding new event types
```kotlin
override fun onAccessibilityEvent(event: AccessibilityEvent?) {
    when (event?.eventType) {
        AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> handleWindowChange(event)
        AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> handleContentChange(event)
    }
}
```

### Detecting home screen
```kotlin
private fun isHomeScreen(packageName: String): Boolean {
    val homePackages = listOf(
        "com.google.android.apps.nexuslauncher",
        "com.android.launcher3",
        // Add more launchers as needed
    )
    return packageName in homePackages
}
```

## Debugging

```bash
# Watch accessibility events
adb logcat | grep "StroopAccessibility"

# Check service status
adb shell dumpsys accessibility | grep stroop
```
