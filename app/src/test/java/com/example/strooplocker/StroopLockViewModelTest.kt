package com.example.strooplocker

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.LiveData
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
import org.mockito.Mockito
import org.mockito.Mockito.lenient
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.junit.MockitoJUnitRunner
import org.junit.Assert.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

/**
 * Unit tests for StroopLockViewModel
 *
 * These tests verify that the ViewModel correctly manages challenge state,
 * interacts with the repository, and provides proper data to the UI.
 */
@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner.Silent::class)
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

        // Setup basic mocks with lenient mocking to avoid unnecessary stubbing errors
        lenient().`when`(mockDb.lockedAppDao()).thenReturn(mockDao)
        lenient().`when`(mockApplication.applicationContext).thenReturn(mockApplication)

        // Use lenient mocking for repository calls
        lenient().`when`(mockRepository.getAllLockedApps()).thenReturn(emptyList())

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
    fun `generateChallenge creates valid challenge with different word and ink color`() = runTest {
        // Act
        viewModel.generateChallenge()
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert - get the LiveData values
        val challengeWord = viewModel.challengeWord.getOrAwaitValue()
        val inkColor = viewModel.inkColor.getOrAwaitValue()
        val expectedAnswer = viewModel.expectedAnswer.getOrAwaitValue()

        // Verify the challenge is valid
        assertNotNull("Challenge word should not be null", challengeWord)
        assertNotNull("Ink color should not be null", inkColor)
        assertNotEquals("Word and ink color should be different", challengeWord, inkColor)
        assertEquals("Expected answer should match ink color", inkColor, expectedAnswer)

        // Verify buttons are generated
        val buttonLabels = viewModel.buttonLabels.getOrAwaitValue()
        val buttonColors = viewModel.buttonColors.getOrAwaitValue()

        assertNotNull("Button labels should not be null", buttonLabels)
        assertNotNull("Button colors should not be null", buttonColors)
        assertTrue("Button labels should contain the correct answer",
            buttonLabels.contains(expectedAnswer))
    }

    @Test
    fun `checkAnswer returns true for correct answer`() {
        // Arrange
        val expectedAnswer = "Blue"
        setExpectedAnswer(expectedAnswer)

        // Act
        val result = viewModel.checkAnswer(expectedAnswer)

        // Assert
        assertTrue("Should return true for correct answer", result)
    }

    @Test
    fun `checkAnswer returns false for incorrect answer`() {
        // Arrange
        val expectedAnswer = "Blue"
        val wrongAnswer = "Red"
        setExpectedAnswer(expectedAnswer)

        // Act
        val result = viewModel.checkAnswer(wrongAnswer)

        // Assert
        assertFalse("Should return false for incorrect answer", result)
    }

    @Test
    fun `loadLockedApps calls repository and updates LiveData`() = runTest {
        // Arrange
        val testApps = listOf("app1", "app2")
        Mockito.`when`(mockRepository.getAllLockedApps()).thenReturn(testApps)

        // Act
        viewModel.loadLockedApps()
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert
        verify(mockRepository, times(2)).getAllLockedApps()
        assertEquals(testApps, viewModel.lockedApps.getOrAwaitValue())
    }

    @Test
    fun `addLockedApp calls repository and refreshes list`() = runTest {
        // Arrange
        val packageName = "com.example.app1"

        // Act
        viewModel.addLockedApp(packageName)
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert
        verify(mockRepository).addLockedApp(packageName)
        verify(mockRepository, times(2)).getAllLockedApps()
    }

    @Test
    fun `removeLockedApp calls repository and refreshes list`() = runTest {
        // Arrange
        val packageName = "com.example.app1"

        // Act
        viewModel.removeLockedApp(packageName)
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert
        verify(mockRepository).removeLockedApp(packageName)
        verify(mockRepository, times(2)).getAllLockedApps()
    }

    @Test
    fun `setAppToLaunch updates appToLaunch LiveData`() {
        // Arrange
        val packageName = "com.example.app1"

        // Act
        viewModel.setAppToLaunch(packageName)

        // Assert
        assertEquals(packageName, viewModel.appToLaunch.getOrAwaitValue())
    }

    @Test
    fun `calculateFontSizeForWord returns appropriate size based on word length`() {
        // Test cases with different word lengths
        assertEquals(26f, viewModel.calculateFontSizeForWord("Red"), 0.01f)
        assertEquals(24.5f, viewModel.calculateFontSizeForWord("Blue"), 0.01f)
        assertEquals(21.5f, viewModel.calculateFontSizeForWord("Yellow"), 0.01f)
        assertEquals(16f, viewModel.calculateFontSizeForWord("UltraViolet"), 0.01f)
    }

    // Helper method to set the expected answer for testing
    private fun setExpectedAnswer(answer: String) {
        val field = StroopLockViewModel::class.java.getDeclaredField("_expectedAnswer")
        field.isAccessible = true
        (field.get(viewModel) as MutableLiveData<String>).value = answer
    }

    // Extension function to get LiveData value with a timeout
    @Throws(InterruptedException::class, TimeoutException::class)
    private fun <T> LiveData<T>.getOrAwaitValue(
        time: Long = 2,
        timeUnit: TimeUnit = TimeUnit.SECONDS
    ): T {
        var data: T? = null
        val latch = CountDownLatch(1)
        val observer = object : Observer<T> {
            override fun onChanged(value: T) {
                data = value
                latch.countDown()
                this@getOrAwaitValue.removeObserver(this)
            }
        }
        this.observeForever(observer)

        // Don't wait indefinitely if the LiveData is not set
        if (!latch.await(time, timeUnit)) {
            this.removeObserver(observer)
            throw TimeoutException("LiveData value was never set.")
        }

        @Suppress("UNCHECKED_CAST")
        return data as T
    }
}