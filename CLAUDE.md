# str00py - Claude Code Project Instructions

## Project Overview

**str00py** is an Android productivity app that encourages mindful phone usage by locking selected apps behind Stroop color-word cognitive challenges.

**Core Mechanism:**
1. User selects apps to lock via SelectAppsActivity
2. StroopAccessibilityService monitors app launches
3. When a locked app is focused, StroopLockActivity presents a challenge
4. User must identify the INK COLOR (not the word) to unlock
5. SessionManager tracks challenge completion with 30-second timeout

## Architecture

```
┌─────────────────────────────────────────────────────────┐
│                    Data Layer                           │
│  LockedAppDatabase → LockedAppDao → LockedAppsRepository│
└─────────────────────────────────────────────────────────┘
                           ↓
┌─────────────────────────────────────────────────────────┐
│                  Manager Layer                          │
│  LockManager    SessionManager    ChallengeManager      │
└─────────────────────────────────────────────────────────┘
                           ↓
┌─────────────────────────────────────────────────────────┐
│                    UI Layer                             │
│  StroopLockActivity  SelectAppsActivity  ViewModel      │
└─────────────────────────────────────────────────────────┘
                           ↓
┌─────────────────────────────────────────────────────────┐
│                  System Layer                           │
│          StroopAccessibilityService                     │
└─────────────────────────────────────────────────────────┘
```

## Key Files

| Component | Path | Purpose |
|-----------|------|---------|
| Main Activity | `app/src/main/java/.../StroopLockActivity.kt` | Challenge UI, permissions setup |
| App Selection | `app/src/main/java/.../SelectAppsActivity.kt` | Lock/unlock app selection |
| Session Logic | `app/src/main/java/.../SessionManager.kt` | Challenge state, timeouts |
| App Monitor | `app/src/main/java/.../StroopAccessibilityService.kt` | Detects locked app launches |
| Database | `app/src/main/java/.../data/LockedAppDatabase.kt` | Room database singleton |
| Unit Tests | `app/src/test/java/.../` | JUnit 4 + Mockito tests |

## Development Commands

```bash
# Build debug APK
./gradlew assembleDebug

# Run unit tests
./gradlew test

# Run instrumented tests (requires emulator/device)
./gradlew connectedAndroidTest

# Clean build
./gradlew clean

# Check for dependency updates
./gradlew dependencyUpdates
```

## Testing Strategy

- **Unit tests:** SessionManager, ChallengeManager, Repository, ViewModel
- **Instrumented tests:** LockManager (real DB), Activity UI tests (Espresso)
- **Manual testing:** Accessibility service behavior, overlay permissions

## Known Issues

1. **Erratic lock behavior** - Lock engages randomly
2. **Session inconsistency** - Re-locks during active session
3. **Button visibility** - Text hard to read on some color combinations

## Code Conventions

- Kotlin with coroutines for async operations
- Room for persistence
- Singleton pattern for managers (SessionManager, LockManager, ChallengeManager)
- MVVM for UI (LiveData, ViewModel)
- Android View Binding (not Data Binding)

## Dependencies

- AndroidX Core/AppCompat/Lifecycle
- Room Database
- Hilt (dependency injection)
- Material Design Components
- Kotlin Coroutines

## Context7 MCP Integration

Always use Context7 MCP when I need library/API documentation, code generation, setup or configuration steps without me having to explicitly ask.

### Project Library References

When working with these libraries, use Context7 with the specified library IDs:

| Library | Context7 ID | Use For |
|---------|-------------|---------|
| AndroidX/Jetpack | `/websites/developer_android_jetpack_androidx` | Room, Lifecycle, ViewModel, LiveData |
| Kotlin Coroutines | `/kotlin/kotlinx.coroutines` | Async operations, Flow, suspend functions |
| Hilt DI | `/websites/dagger_dev_hilt` | Dependency injection setup, modules, scopes |
| Material Components | `/material-components/material-components-android` | UI components, theming, Material Design |
| Gradle | `/websites/gradle_current_userguide` | Build configuration, dependencies, tasks |
| Mockito | `/mockito/mockito` | Unit testing, mocking, verification |
| Kotlin Language | `/websites/kotlinlang` | Kotlin syntax, stdlib, best practices |

**Example usage:** "Add Room database migration. Use library `/websites/developer_android_jetpack_androidx` for API and docs."

## When Working on This Project

1. **Before modifying SessionManager:** Read existing tests in `SessionManagerTest.kt`
2. **Before UI changes:** Check `res/values/colors.xml` for the 9-color palette
3. **Accessibility changes:** Test on real device - emulator behavior differs
4. **Database changes:** Update LockedApp entity, DAO, and repository together
