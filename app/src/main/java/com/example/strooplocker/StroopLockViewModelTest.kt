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
import org.junit.Assert.*

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