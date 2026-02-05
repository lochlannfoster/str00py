# str00py

Android app that locks selected apps behind Stroop color-word challenges (identify INK COLOR to unlock).

## Architecture
Data: Room DB → LockedAppsRepository
Managers: LockManager, SessionManager, ChallengeManager (singletons)
UI: StroopLockActivity (challenge), SelectAppsActivity (app picker)
System: StroopAccessibilityService (monitors app launches)

## Commands
```bash
./gradlew assembleDebug    # build
./gradlew test             # unit tests
./gradlew connectedAndroidTest  # instrumented (needs device)
```

## Known Issues
- Erratic lock behavior / random engagement
- Session inconsistency / re-locks during active session
- Button text visibility on some colors

## Before Editing
- SessionManager: read `SessionManagerTest.kt` first
- UI: check `res/values/colors.xml` (9-color palette)
- Database: update entity, DAO, repository together
- Accessibility: test on real device, not emulator

## Stack
Kotlin, Coroutines, Room, Hilt, MVVM, ViewBinding
