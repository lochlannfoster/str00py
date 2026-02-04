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
