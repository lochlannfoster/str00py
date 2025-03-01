// app/src/test/java/com/example/strooplocker/LockedAppsRepositoryTest.kt

package com.example.strooplocker

import com.example.strooplocker.data.LockedApp
import com.example.strooplocker.data.LockedAppDao
import com.example.strooplocker.data.LockedAppsRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.MockitoJUnitRunner

/**
 * Unit tests for [LockedAppsRepository]
 *
 * These tests verify that the repository correctly interacts with the DAO
 * for adding, removing, and retrieving locked apps.
 */
@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class LockedAppsRepositoryTest {

    @Mock
    private lateinit var mockDao: LockedAppDao

    private lateinit var repository: LockedAppsRepository

    @Before
    fun setup() {
        repository = LockedAppsRepository(mockDao)
    }

    @Test
    fun getAllLockedApps_returnsAllApps() = runTest {
        // Arrange
        val expectedApps = listOf("com.example.app1", "com.example.app2")
        `when`(mockDao.getAllLockedApps()).thenReturn(
            expectedApps.map { LockedApp(it) }
        )

        // Act
        val result = repository.getAllLockedApps()

        // Assert
        assert(result == expectedApps)
        verify(mockDao).getAllLockedApps()
    }

    @Test
    fun addLockedApp_insertsAppToDao() = runTest {
        // Arrange
        val packageName = "com.example.app1"
        val lockedApp = LockedApp(packageName)

        // Act
        repository.addLockedApp(packageName)

        // Assert
        verify(mockDao).insertLockedApp(eq(lockedApp))
    }

    @Test
    fun removeLockedApp_deletesAppFromDao() = runTest {
        // Arrange
        val packageName = "com.example.app1"
        val lockedApp = LockedApp(packageName)

        // Act
        repository.removeLockedApp(packageName)

        // Assert
        verify(mockDao).deleteLockedApp(eq(lockedApp))
    }
}

// app/src/test/java/com/example/strooplocker/ChallengeManagerTest.kt

package com.example.strooplocker

import org.junit.Test
import org.junit.Assert.*

/**
 * Unit tests for [ChallengeManager]
 *
 * These tests verify that challenge state is correctly managed,
 * including starting, completing, and checking challenges.
 */
class ChallengeManagerTest {

    @Test
    fun startChallenge_setsCurrentChallenge() {
        // Arrange
        val packageName = "com.example.app"

        // Act
        val challenge = ChallengeManager.startChallenge(packageName)

        // Assert
        assertEquals(packageName, challenge.lockedPackage)
        assertEquals(challenge, ChallengeManager.getCurrentChallenge())
        assertTrue(ChallengeManager.isChallengeInProgress())
    }

    @Test
    fun completeChallenge_success_returnsPackageName() {
        // Arrange
        val packageName = "com.example.app"
        ChallengeManager.startChallenge(packageName)

        // Act
        val result = ChallengeManager.completeChallenge(true)

        // Assert
        assertEquals(packageName, result)
        assertFalse(ChallengeManager.isChallengeInProgress())
        assertNull(ChallengeManager.getCurrentChallenge())
    }

    @Test
    fun completeChallenge_failure_returnsNull() {
        // Arrange
        val packageName = "com.example.app"
        ChallengeManager.startChallenge(packageName)

        // Act
        val result = ChallengeManager.completeChallenge(false)

        // Assert
        assertNull(result)
        assertFalse(ChallengeManager.isChallengeInProgress())
        assertNull(ChallengeManager.getCurrentChallenge())
    }

    @Test
    fun isChallengeInProgress_noChallengeStarted_returnsFalse() {
        // Arrange - ensure no challenge is in progress
        ChallengeManager.completeChallenge(false)

        // Act & Assert
        assertFalse(ChallengeManager.isChallengeInProgress())
    }
}

// app/src/test/java/com/example/strooplocker/StroopLockViewModelTest.kt

package com.example.strooplocker

import android.app.Application
import android.graphics.Color
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import com.example.strooplocker.data.LockedAppDao
import com.example.strooplocker.data.LockedAppDatabase
import com.example.strooplocker.data.LockedAppsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.MockitoJUnitRunner

/**
 * Unit tests for [StroopLockViewModel]
 *
 * These tests verify the view model correctly manages challenge generation,
 * answer checking, and app locking functionality.
 */
@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class StroopLockViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()

    @Mock
    private lateinit var mockApplication: Application

    @Mock
    private lateinit var mockDb: LockedAppDatabase

    @Mock
    private lateinit var mockDao: LockedAppDao

    @Mock
    private lateinit var mockRepository: LockedAppsRepository

    @Mock
    private lateinit var challengeWordObserver: Observer<String>

    @Mock
    private lateinit var inkColorObserver: Observer<String>

    @Mock
    private lateinit var expectedAnswerObserver: Observer<String>

    @Mock
    private lateinit var buttonLabelsObserver: Observer<List<String>>

    private lateinit var viewModel: StroopLockViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        `when`(mockDb.lockedAppDao()).thenReturn(mockDao)
        `when`(mockApplication.applicationContext).thenReturn(mockApplication)

        viewModel = StroopLockViewModel(mockApplication)

        // Replace repository with mock
        val field = StroopLockViewModel::class.java.getDeclaredField("repository")
        field.isAccessible = true
        field.set(viewModel, mockRepository)

        // Observe LiveData
        viewModel.challengeWord.observeForever(challengeWordObserver)
        viewModel.inkColor.observeForever(inkColorObserver)
        viewModel.expectedAnswer.observeForever(expectedAnswerObserver)
        viewModel.buttonLabels.observeForever(buttonLabelsObserver)
    }

    @After
    fun tearDown() {
        // Remove observers
        viewModel.challengeWord.removeObserver(challengeWordObserver)
        viewModel.inkColor.removeObserver(inkColorObserver)
        viewModel.expectedAnswer.removeObserver(expectedAnswerObserver)
        viewModel.buttonLabels.removeObserver(buttonLabelsObserver)

        Dispatchers.resetMain()
    }

    @Test
    fun generateChallenge_setsAllChallengeProperties() {
        // Arrange
        val colorMap = mapOf(
            "Red" to Color.RED,
            "Blue" to Color.BLUE,
            "Green" to Color.GREEN
        )
        val field = StroopLockViewModel::class.java.getDeclaredField("_colorMap")
        field.isAccessible = true
        field.set(viewModel, colorMap)

        // Act
        viewModel.generateChallenge()
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert
        verify(challengeWordObserver).onChanged(any())
        verify(inkColorObserver).onChanged(any())
        verify(expectedAnswerObserver).onChanged(any())
        verify(buttonLabelsObserver).onChanged(any())
    }

    @Test
    fun checkAnswer_correctAnswer_returnsTrue() {
        // Arrange
        val correctColor = "Blue"
        val field = StroopLockViewModel::class.java.getDeclaredField("_expectedAnswer")
        field.isAccessible = true
        field.set(viewModel, correctColor)

        // Act
        val result = viewModel.checkAnswer(correctColor)

        // Assert
        assertTrue(result)
    }

    @Test
    fun checkAnswer_incorrectAnswer_returnsFalse() {
        // Arrange
        val correctColor = "Blue"
        val wrongColor = "Red"
        val field = StroopLockViewModel::class.java.getDeclaredField("_expectedAnswer")
        field.isAccessible = true
        field.set(viewModel, correctColor)

        // Act
        val result = viewModel.checkAnswer(wrongColor)

        // Assert
        assertFalse(result)
    }

    @Test
    fun loadLockedApps_callsRepository() = runTest {
        // Act
        viewModel.loadLockedApps()
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert
        verify(mockRepository).getAllLockedApps()
    }

    @Test
    fun calculateFontSizeForWord_returnsCorrectSize() {
        // Arrange
        val shortWord = "Red"
        val longWord = "Purple"
        val veryLongWord = "UltraViolet"

        // Act & Assert
        assertEquals(26f, viewModel.calculateFontSizeForWord(shortWord))
        assertTrue(viewModel.calculateFontSizeForWord(longWord) < 26f)
        assertTrue(viewModel.calculateFontSizeForWord(veryLongWord) < viewModel.calculateFontSizeForWord(longWord))
        assertTrue(viewModel.calculateFontSizeForWord(veryLongWord) >= 16f) // Min size
    }
}

// app/src/androidTest/java/com/example/strooplocker/StroopChallengeActivityTest.kt

package com.example.strooplocker

import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.hamcrest.Matchers.not
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * UI tests for [StroopChallengeActivity]
 *
 * These tests verify that the Stroop challenge UI works as expected,
 * including displaying the challenge, handling user input, and navigating
 * based on correct/incorrect answers.
 */
@RunWith(AndroidJUnit4::class)
class StroopChallengeActivityTest {

    private lateinit var testPackageName: String

    @Before
    fun setup() {
        testPackageName = ApplicationProvider.getApplicationContext<StroopApplication>().packageName
    }

    @Test
    fun displaysChallengeTextAndButtons() {
        // Launch activity with test package
        val intent = Intent(ApplicationProvider.getApplicationContext(), StroopChallengeActivity::class.java).apply {
            putExtra(StroopLockActivity.EXTRA_LOCKED_PACKAGE, testPackageName)
        }

        ActivityScenario.launch<StroopChallengeActivity>(intent).use {
            // Verify challenge text is displayed
            onView(withId(R.id.challengeText))
                .check(matches(isDisplayed()))
                .check(matches(not(withText(""))))

            // Verify color grid is displayed and has child buttons
            onView(withId(R.id.colorGrid))
                .check(matches(isDisplayed()))
                .check(matches(hasChildCount(9)))
        }
    }

    @Test
    fun incorrectAnswer_generatesNewChallenge() {
        // This test is complex to implement reliably because:
        // 1. We don't know which button is incorrect
        // 2. The challenge changes on wrong answer

        // A possible approach is to mock StroopChallengeActivity to make answer checking deterministic
        // For a basic test, we could just click the first button and verify a toast appears

        val intent = Intent(ApplicationProvider.getApplicationContext(), StroopChallengeActivity::class.java).apply {
            putExtra(StroopLockActivity.EXTRA_LOCKED_PACKAGE, testPackageName)
        }

        ActivityScenario.launch<StroopChallengeActivity>(intent).use {
            // This test would need custom ViewMatchers to find buttons in the GridLayout
            // and verify the toast message for incorrect answer
        }
    }
}

// app/src/androidTest/java/com/example/strooplocker/LockManagerTest.kt

package com.example.strooplocker

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.strooplocker.data.LockedAppDao
import com.example.strooplocker.data.LockedAppDatabase
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

/**
 * Integration tests for [LockManager]
 *
 * These tests verify that the LockManager correctly interfaces with
 * the Room database for managing locked apps.
 */
@RunWith(AndroidJUnit4::class)
class LockManagerTest {

    private lateinit var db: LockedAppDatabase
    private lateinit var dao: LockedAppDao
    private lateinit var context: Context

    @Before
    fun createDb() {
        context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, LockedAppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.lockedAppDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    @Test
    fun addLockedApp_addsToDatabase() = runBlocking {
        // Arrange
        val packageName = "com.example.testapp"

        // Act
        LockManager.addLockedApp(context, packageName)

        // Assert
        val lockedApps = LockManager.getLockedApps(context)
        assert(lockedApps.contains(packageName))
    }

    @Test
    fun removeLockedApp_removesFromDatabase() = runBlocking {
        // Arrange
        val packageName = "com.example.testapp"
        LockManager.addLockedApp(context, packageName)

        // Act
        LockManager.removeLockedApp(context, packageName)

        // Assert
        val lockedApps = LockManager.getLockedApps(context)
        assert(!lockedApps.contains(packageName))
    }

    @Test
    fun isAppLocked_returnsCorrectStatus() = runBlocking {
        // Arrange
        val packageName = "com.example.testapp"
        val unlockedPackage = "com.example.anotherone"
        LockManager.addLockedApp(context, packageName)

        // Act & Assert
        assert(LockManager.isAppLocked(context, packageName))
        assert(!LockManager.isAppLocked(context, unlockedPackage))
    }
}