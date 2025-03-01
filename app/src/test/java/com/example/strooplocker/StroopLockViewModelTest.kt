@file:Suppress("UNCHECKED_CAST")

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
 * Unit tests for StroopLockViewModel
 *
 * Using MockitoJUnitRunner.Silent to avoid unnecessary stubbing errors since our
 * ViewModel initializes with repository calls that not all tests care about.
 */
@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner.Silent::class) // Use Silent version to avoid unnecessary stubbing errors
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
    fun setup() = runTest {
        // Set the main dispatcher to our test dispatcher
        Dispatchers.setMain(testDispatcher)

        // Setup basic mocks
        Mockito.`when`(mockDb.lockedAppDao()).thenReturn(mockDao)
        Mockito.`when`(mockApplication.applicationContext).thenReturn(mockApplication)

        // Set up repository mock for initial ViewModel loading
        Mockito.`when`(mockRepository.getAllLockedApps()).thenReturn(emptyList())

        // Create the ViewModel
        viewModel = StroopLockViewModel(mockApplication)

        // Replace repository with mock to control method calls
        val field = StroopLockViewModel::class.java.getDeclaredField("repository")
        field.isAccessible = true
        field.set(viewModel, mockRepository)

        // Let any init coroutines complete
        testDispatcher.scheduler.advanceUntilIdle()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `load locked apps calls repository`() = runTest {
        // Arrange
        val expectedApps = listOf("app1", "app2")

        // Re-stub the repository with our test data
        Mockito.`when`(mockRepository.getAllLockedApps()).thenReturn(expectedApps)

        // Act
        viewModel.loadLockedApps()
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert - we expect at least one call (there was also one during init)
        verify(mockRepository, times(2)).getAllLockedApps()

        // Additional assertions to check LiveData
        val loadedApps = viewModel.lockedApps.value
        assertEquals(expectedApps, loadedApps)
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
        // Arrange with specific test data
        val testApps = listOf("app1", "app2")
        Mockito.`when`(mockRepository.getAllLockedApps()).thenReturn(testApps)

        // Act
        viewModel.loadLockedApps()
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert - verify repository was called more than once (init + our explicit call)
        verify(mockRepository, times(2)).getAllLockedApps()

        // Verify the data was properly set
        assertEquals(testApps, viewModel.lockedApps.value)
    }

    @Test
    fun calculateFontSizeForWord_returnsCorrectSize() {
        // Arrange
        val shortWord = "Red"
        val longWord = "Purple"
        val veryLongWord = "UltraViolet"
    }
}
