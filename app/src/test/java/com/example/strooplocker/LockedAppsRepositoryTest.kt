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
import org.mockito.Mockito
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
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
        // Create a real repository with mocked DAO
        repository = LockedAppsRepository(mockDao)
    }

    @Test
    fun getAllLockedApps_returnsAllApps() = runTest {
        // Arrange
        val expectedAppList = listOf(
            LockedApp("com.example.app1"),
            LockedApp("com.example.app2")
        )
        val expectedStringList = expectedAppList.map { it.packageName }

        Mockito.`when`(mockDao.getAllLockedApps()).thenReturn(expectedAppList)

        // Act
        val result = repository.getAllLockedApps()

        // Assert
        assert(result == expectedStringList)
        verify(mockDao, times(1)).getAllLockedApps()
    }

    @Test
    fun addLockedApp_insertsAppToDao() = runTest {
        // Arrange
        val packageName = "com.example.app1"
        val lockedApp = LockedApp(packageName)

        // Act
        repository.addLockedApp(packageName)

        // Assert
        verify(mockDao, times(1)).insertLockedApp(lockedApp)
    }

    @Test
    fun removeLockedApp_deletesAppFromDao() = runTest {
        // Arrange
        val packageName = "com.example.app1"
        val lockedApp = LockedApp(packageName)

        // Act
        repository.removeLockedApp(packageName)

        // Assert
        verify(mockDao, times(1)).deleteLockedApp(lockedApp)
    }
}