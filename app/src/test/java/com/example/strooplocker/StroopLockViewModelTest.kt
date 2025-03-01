// app/src/test/java/com/example/strooplocker/StroopLockViewModelTest.kt

package com.example.strooplocker

import android.app.Application
import android.graphics.Color
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.MutableLiveData
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

        // Use reflection to set the MutableLiveData value
        val colorMapField = StroopLockViewModel::class.java.getDeclaredField("_colorMap")
        colorMapField.isAccessible = true
        val colorMapLiveData = colorMapField.get(viewModel) as MutableLiveData<Map<String, Int>>
        colorMapLiveData.value = colorMap

        // Act
        viewModel.generateChallenge()
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert - verify that the observers were notified
        verify(challengeWordObserver, atLeastOnce()).onChanged(any())
        verify(inkColorObserver, atLeastOnce()).onChanged(any())
        verify(expectedAnswerObserver, atLeastOnce()).onChanged(any())
        verify(buttonLabelsObserver, atLeastOnce()).onChanged(any())
    }

    @Test
    fun checkAnswer_correctAnswer_returnsTrue() {
        // Instead of subclassing, we'll use the actual viewModel and modify its fields directly

        val expectedAnswer = "Blue"

        // Use reflection to set the expected answer
        val field = StroopLockViewModel::class.java.getDeclaredField("_expectedAnswer")
        field.isAccessible = true
        (field.get(viewModel) as MutableLiveData<String>).value = expectedAnswer

        // Act
        val result = viewModel.checkAnswer(expectedAnswer)

        // Assert
        assertTrue(result)
    }

    @Test
    fun checkAnswer_incorrectAnswer_returnsFalse() {
        // Use reflection to set the expected answer
        val correctAnswer = "Blue"
        val wrongAnswer = "Red"

        val field = StroopLockViewModel::class.java.getDeclaredField("_expectedAnswer")
        field.isAccessible = true
        (field.get(viewModel) as MutableLiveData<String>).value = correctAnswer

        // Act
        val result = viewModel.checkAnswer(wrongAnswer)

        // Assert
        assertFalse(result)
    }

    @Test
    fun loadLockedApps_callsRepository() = runTest {
        // Since loadLockedApps is called in init, reset the mock first
        reset(mockRepository)

        // Arrange
        `when`(mockRepository.getAllLockedApps()).thenReturn(listOf("app1", "app2"))

        // Act
        viewModel.loadLockedApps()
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert
        verify(mockRepository, times(1)).getAllLockedApps()
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