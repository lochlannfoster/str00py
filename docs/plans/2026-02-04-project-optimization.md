# Project Optimization: Documentation, Skills, and Cleanup

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Create project-specific documentation, Claude skills/agents, and clean repository artifacts for the str00py Android app.

**Architecture:** Create a `.claude/` directory structure with project-specific CLAUDE.md, custom skills for Android development workflows, and agent configurations. Clean tracked files that should be gitignored.

**Tech Stack:** Markdown, YAML (for skills), Git, Android/Gradle ecosystem

---

## Context Summary

**Project:** str00py - Android productivity app that locks selected apps behind Stroop color-word cognitive challenges to encourage mindful scrolling.

**MCP Server Status:**
- context-please: Active (indexing in progress)
- claude-self-reflect: Active (LOCAL mode, 384 dimensions)
- context7: Not configured in this project

**Artifacts to Clean:**
- `local.properties` - Tracked but should be gitignored (contains user-specific SDK path)
- `log` - Tracked but should be gitignored (contains runtime logcat output)

**Current Documentation:** Only README.md exists

---

## Task 1: Clean Repository Artifacts

**Files:**
- Modify: `/home/loch/str00py/.gitignore`
- Remove from tracking: `local.properties`, `log`

**Step 1: Update .gitignore with additional patterns**

Add these patterns to `.gitignore`:
```
# Logs
/log
*.log
logs/

# Local configuration (ensure coverage)
local.properties
/local.properties
```

**Step 2: Remove files from git tracking (keep locally)**

Run:
```bash
cd /home/loch/str00py
git rm --cached local.properties log 2>/dev/null || true
```
Expected: Files removed from index, kept on disk

**Step 3: Verify removal**

Run: `git status`
Expected: Shows `local.properties` and `log` as deleted from index, listed in "Changes to be committed"

**Step 4: Commit the cleanup**

```bash
git add .gitignore
git commit -m "chore: remove local.properties and log from version control

- Remove files that were committed before being added to .gitignore
- Add /log and *.log patterns to .gitignore
- local.properties contains user-specific SDK paths
- log contains runtime debug output

Co-Authored-By: Claude Opus 4.5 <noreply@anthropic.com>"
```

---

## Task 2: Create Project CLAUDE.md

**Files:**
- Create: `/home/loch/str00py/CLAUDE.md`

**Step 1: Create project-specific CLAUDE.md**

```markdown
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

## When Working on This Project

1. **Before modifying SessionManager:** Read existing tests in `SessionManagerTest.kt`
2. **Before UI changes:** Check `res/values/colors.xml` for the 9-color palette
3. **Accessibility changes:** Test on real device - emulator behavior differs
4. **Database changes:** Update LockedApp entity, DAO, and repository together
```

**Step 2: Verify file created**

Run: `cat /home/loch/str00py/CLAUDE.md | head -20`
Expected: Shows beginning of CLAUDE.md content

---

## Task 3: Create Skills Directory Structure

**Files:**
- Create: `/home/loch/str00py/.claude/skills/android-testing.md`
- Create: `/home/loch/str00py/.claude/skills/session-debugging.md`
- Create: `/home/loch/str00py/.claude/skills/accessibility-service.md`

**Step 1: Create .claude/skills directory**

Run:
```bash
mkdir -p /home/loch/str00py/.claude/skills
```

**Step 2: Create android-testing skill**

Create `/home/loch/str00py/.claude/skills/android-testing.md`:
```markdown
---
name: android-testing
description: Use when writing or running tests for the str00py Android app
---

# Android Testing Skill

## Test Types

### Unit Tests (Fast, No Device)
Location: `app/src/test/java/com/example/strooplocker/`

```bash
# Run all unit tests
./gradlew test

# Run specific test class
./gradlew test --tests "com.example.strooplocker.SessionManagerTest"

# Run with verbose output
./gradlew test --info
```

### Instrumented Tests (Requires Device/Emulator)
Location: `app/src/androidTest/java/com/example/strooplocker/`

```bash
# Run all instrumented tests
./gradlew connectedAndroidTest

# Ensure emulator is running first
adb devices
```

## Test Patterns in This Project

### Mocking with Mockito
```kotlin
@Mock
private lateinit var mockDao: LockedAppDao

@Before
fun setup() {
    MockitoAnnotations.openMocks(this)
}
```

### Testing Coroutines
```kotlin
@get:Rule
val instantTaskExecutorRule = InstantTaskExecutorRule()

private val testDispatcher = StandardTestDispatcher()
```

### Testing LiveData
```kotlin
viewModel.someState.observeForever { result ->
    // Assert on result
}
```

## Checklist When Adding Tests

- [ ] Test file in correct directory (test/ vs androidTest/)
- [ ] Test class name ends with `Test`
- [ ] @Before setup initializes mocks
- [ ] @After cleanup if needed
- [ ] Each test method is independent
- [ ] Assertions are specific (not just "no exception")
```

**Step 3: Create session-debugging skill**

Create `/home/loch/str00py/.claude/skills/session-debugging.md`:
```markdown
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
```

**Step 4: Create accessibility-service skill**

Create `/home/loch/str00py/.claude/skills/accessibility-service.md`:
```markdown
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
```

**Step 5: Verify skills created**

Run: `ls -la /home/loch/str00py/.claude/skills/`
Expected: Shows 3 markdown files

---

## Task 4: Create Agent Configuration

**Files:**
- Create: `/home/loch/str00py/.claude/agents.md`

**Step 1: Create agents documentation**

Create `/home/loch/str00py/.claude/agents.md`:
```markdown
# str00py Agent Configurations

## Recommended Agent Patterns

### 1. Codebase Exploration Agent

**When to use:** Understanding unfamiliar parts of the codebase

**Prompt pattern:**
```
Explore the [component] in /home/loch/str00py. I need to understand:
1. What files are involved
2. How data flows through the component
3. Key methods and their purposes
4. Any tests that exist
```

**Best for:**
- SessionManager flow
- Database layer
- UI/ViewModel interaction

### 2. Test Writing Agent

**When to use:** Adding test coverage

**Prompt pattern:**
```
Write tests for [component] in str00py following the existing test patterns.
Check app/src/test/java for unit tests and app/src/androidTest for instrumented tests.
Use Mockito for mocking, follow existing test structure.
```

### 3. Debugging Agent

**When to use:** Investigating session/lock issues

**Prompt pattern:**
```
Debug the [symptom] issue in str00py.
1. Trace the code path from StroopAccessibilityService
2. Check SessionManager state transitions
3. Look for race conditions or timing issues
4. Propose fix with test
```

### 4. Android Build Agent

**When to use:** Build issues, dependency problems

**Prompt pattern:**
```
Investigate build issue in str00py:
[error message]

Check:
- app/build.gradle.kts for dependencies
- settings.gradle.kts for project structure
- gradle.properties for configuration
```

## Agent Communication

Agents should report:
1. Files examined
2. Key findings
3. Recommendations with file paths and line numbers
4. Any blockers or questions

## Context to Provide Agents

Always include:
- Project path: `/home/loch/str00py`
- Relevant component name
- Specific symptom or goal
- Any error messages
```

**Step 2: Verify agent config created**

Run: `cat /home/loch/str00py/.claude/agents.md | head -10`
Expected: Shows beginning of agents.md

---

## Task 5: Stage and Commit All Documentation

**Files:**
- Stage: `CLAUDE.md`, `.claude/skills/*`, `.claude/agents.md`, `docs/plans/*`

**Step 1: Check git status**

Run: `cd /home/loch/str00py && git status`
Expected: Shows new untracked files

**Step 2: Stage documentation files**

Run:
```bash
cd /home/loch/str00py
git add CLAUDE.md .claude/ docs/
```

**Step 3: Commit documentation**

```bash
git commit -m "docs: add Claude Code project documentation and skills

- Add CLAUDE.md with project overview, architecture, and conventions
- Add .claude/skills/ with android-testing, session-debugging, accessibility-service
- Add .claude/agents.md with agent configuration patterns
- Add docs/plans/ directory for implementation plans

Co-Authored-By: Claude Opus 4.5 <noreply@anthropic.com>"
```

**Step 4: Verify commit**

Run: `git log --oneline -1`
Expected: Shows new commit message

---

## Task 6: Verify MCP Integration

**Step 1: Test context-please search**

Run semantic search to verify indexing:
```
mcp__context-please__search_code with query "session management" path "/home/loch/str00py"
```
Expected: Returns relevant results from SessionManager.kt

**Step 2: Test claude-self-reflect**

Store a reflection about this work:
```
mcp__claude-self-reflect__store_reflection with content "Created str00py project documentation: CLAUDE.md, skills for android-testing, session-debugging, accessibility-service, and agent patterns."
```
Expected: Reflection stored successfully

---

## Summary

| Task | Description | Files Changed |
|------|-------------|---------------|
| 1 | Clean artifacts | .gitignore, remove local.properties, log |
| 2 | Create CLAUDE.md | CLAUDE.md |
| 3 | Create skills | .claude/skills/*.md (3 files) |
| 4 | Create agents | .claude/agents.md |
| 5 | Commit docs | Git commit |
| 6 | Verify MCP | Test searches |

**Total new files:** 6
**Files cleaned:** 2 (removed from tracking)
**Commits:** 2 (cleanup + documentation)
