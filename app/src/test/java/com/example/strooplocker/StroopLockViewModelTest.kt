// app/src/test/java/com/example/strooplocker/StroopLockViewModelTest.kt

package com.example.strooplocker

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.MutableLiveData
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
import org.mockito.Mockito
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
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

    private lateinit var viewModel: StroopLockViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        Mockito.`when`(mockDb.lockedAppDao()).thenReturn(mockDao)
        Mockito.`when`(mockApplication.applicationContext).thenReturn(mockApplication)

        viewModel = StroopLockViewModel(mockApplication)

        // Replace repository with mock
        val field = StroopLockViewModel::class.java.getDeclaredField("repository")
        field.isAccessible = true
        field.set(viewModel, mockRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun checkAnswer_correctAnswer_returnsTrue() {
        // Arrange
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
        // Arrange
        val correctAnswer = "Blue"
        val wrongAnswer = "Red"

        // Use reflection to set the expected answer
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
        // Arrange
        Mockito.`when`(mockRepository.getAllLockedApps()).thenReturn(listOf("app1", "app2"))

        // Act
        viewModel.loadLockedApps()
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert - verify repository was called
        Mockito.verify(mockRepository, times(1)).getAllLockedApps()
    }

    @Test
    fun calculateFontSizeForWord_returnsCorrectSize() {
        // Arrange
        val shortWord = "Red"
        val longWord = "Purple"
        val veryLongWord = "UltraViolet"

        // Act & Assert
        assertEquals(26f, viewModel.calculateFontSizeForWord(shortWord), 0.001f)
        assertTrue(viewModel.calculateFontSizeForWord(longWord) < 26f)
        assertTrue(viewModel.calculateFontSizeForWord(veryLongWord) < viewModel.calculateFontSizeForWord(longWord))
        assertTrue(viewModel.calculateFontSizeForWord(veryLongWord) >= 16f) // Min size
    }

    @Test
    fun generateChallenge_setsValidChallengeProperties() {
        // Arrange: setup a predefined color map matching ViewModel
        val colorMap = mapOf(
            "Red" to "#FF0000",
            "Green" to "#00FF00",
            "Blue" to "#3366FF",
            "Yellow" to "#CCFF33",
            "Pink" to "#FF66FF",
            "Orange" to "#FF6600",
            "Brown" to "#FF8000",
            "Cyan" to "#00FFFF",
            "Purple" to "#8A00E6"
        )

        // Use reflection to set the color map
        val colorMapField = StroopLockViewModel::class.java.getDeclaredField("_colorMap")
        colorMapField.isAccessible = true
        val colorMapLiveData = colorMapField.get(viewModel) as MutableLiveData<Map<String, String>>
        colorMapLiveData.value = colorMap

        // Act
        viewModel.generateChallenge()
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert - verify challenge properties are set
        assertNotNull(viewModel.challengeWord.value)
        assertNotNull(viewModel.inkColor.value)
        assertNotNull(viewModel.expectedAnswer.value)
        assertNotNull(viewModel.buttonLabels.value)
        assertEquals(9, viewModel.buttonLabels.value?.size)
    }
}