---
name: session-debugging
description: Use when debugging session management, challenge timing, or lock behavior issues
---

# Session Debugging Skill

## The Session Problem

str00py has known issues with session management:
- Lock engages randomly
- Re-locks during active session
- Challenge timeout behavior inconsistent

## Key Components to Check

### SessionManager.kt
- `startChallenge(packageName)` - Begins challenge tracking
- `completeChallenge(packageName)` - Marks challenge done
- `isChallengeCompleted(packageName)` - Checks if app is unlocked
- `endSession(packageName)` - Clears single app session
- `endAllSessions()` - Clears all sessions

### StroopAccessibilityService.kt
- `onAccessibilityEvent()` - Detects app switches
- Home screen detection triggers session end
- Race conditions possible between events

## Debugging Steps

1. **Enable verbose logging**
   Check `LoggingUtil.kt` for log levels

2. **Watch logcat for session events**
   ```bash
   adb logcat | grep -E "(SessionManager|StroopAccessibility)"
   ```

3. **Check challenge timeout (30 seconds)**
   In SessionManager, timeout is 30000ms

4. **Trace the flow**
   ```
   App launch detected → isAppLocked? → startChallenge() →
   User completes → completeChallenge() → isChallengeCompleted() = true
   ```

## Common Issues

| Symptom | Likely Cause | Check |
|---------|--------------|-------|
| Locks immediately after unlock | Session not stored | `completedChallenges` map |
| Never locks | Accessibility service disabled | Settings > Accessibility |
| Random locks | Multiple accessibility events | Event deduplication |

## Test Commands

```bash
# Run session manager tests
./gradlew test --tests "*SessionManager*"

# Run timeout tests
./gradlew test --tests "*SessionManagerTimeoutTest*"
```
