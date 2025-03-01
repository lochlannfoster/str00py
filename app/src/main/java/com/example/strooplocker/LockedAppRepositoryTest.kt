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