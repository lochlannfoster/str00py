package com.example.strooplocker

import android.content.Context
import android.content.SharedPreferences
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.anyBoolean
import org.mockito.Mockito.anyInt
import org.mockito.Mockito.anyString
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.MockitoJUnitRunner
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue

/**
 * Unit tests for [SettingsManager]
 *
 * Tests verify correct SharedPreferences interaction for all settings,
 * including default values and persistence.
 */
@RunWith(MockitoJUnitRunner::class)
class SettingsManagerTest {

    @Mock
    private lateinit var mockContext: Context

    @Mock
    private lateinit var mockSharedPreferences: SharedPreferences

    @Mock
    private lateinit var mockEditor: SharedPreferences.Editor

    private lateinit var settingsManager: SettingsManager

    @Before
    fun setup() {
        `when`(mockContext.getSharedPreferences("str00py_settings", Context.MODE_PRIVATE))
            .thenReturn(mockSharedPreferences)
        `when`(mockSharedPreferences.edit()).thenReturn(mockEditor)
        `when`(mockEditor.putInt(anyString(), anyInt())).thenReturn(mockEditor)
        `when`(mockEditor.putBoolean(anyString(), anyBoolean())).thenReturn(mockEditor)
        `when`(mockEditor.putString(anyString(), anyString())).thenReturn(mockEditor)

        settingsManager = SettingsManager(mockContext)
    }

    @After
    fun teardown() {
        SettingsManager.resetInstance()
    }

    // Challenge Timeout Tests

    @Test
    fun challengeTimeout_defaultsToZero_meaningInfinite() {
        `when`(mockSharedPreferences.getInt("challenge_timeout", 0)).thenReturn(0)
        assertEquals(0, settingsManager.challengeTimeout)
    }

    @Test
    fun challengeTimeout_returnsStoredValue() {
        `when`(mockSharedPreferences.getInt("challenge_timeout", 0)).thenReturn(30)
        assertEquals(30, settingsManager.challengeTimeout)
    }

    @Test
    fun challengeTimeout_setsValueCorrectly() {
        settingsManager.challengeTimeout = 45
        verify(mockEditor).putInt("challenge_timeout", 45)
        verify(mockEditor).apply()
    }

    // Session Duration Tests

    @Test
    fun sessionDuration_defaultsToZero_meaningUntilLeave() {
        `when`(mockSharedPreferences.getInt("session_duration", 0)).thenReturn(0)
        assertEquals(0, settingsManager.sessionDuration)
    }

    @Test
    fun sessionDuration_returnsStoredValue() {
        `when`(mockSharedPreferences.getInt("session_duration", 0)).thenReturn(60)
        assertEquals(60, settingsManager.sessionDuration)
    }

    @Test
    fun sessionDuration_setsValueCorrectly() {
        settingsManager.sessionDuration = 120
        verify(mockEditor).putInt("session_duration", 120)
        verify(mockEditor).apply()
    }

    // Sound Enabled Tests

    @Test
    fun soundEnabled_defaultsToFalse() {
        `when`(mockSharedPreferences.getBoolean("sound_enabled", false)).thenReturn(false)
        assertFalse(settingsManager.soundEnabled)
    }

    @Test
    fun soundEnabled_returnsStoredValue() {
        `when`(mockSharedPreferences.getBoolean("sound_enabled", false)).thenReturn(true)
        assertTrue(settingsManager.soundEnabled)
    }

    @Test
    fun soundEnabled_setsValueCorrectly() {
        settingsManager.soundEnabled = true
        verify(mockEditor).putBoolean("sound_enabled", true)
        verify(mockEditor).apply()
    }

    // Vibration Enabled Tests

    @Test
    fun vibrationEnabled_defaultsToFalse() {
        `when`(mockSharedPreferences.getBoolean("vibration_enabled", false)).thenReturn(false)
        assertFalse(settingsManager.vibrationEnabled)
    }

    @Test
    fun vibrationEnabled_returnsStoredValue() {
        `when`(mockSharedPreferences.getBoolean("vibration_enabled", false)).thenReturn(true)
        assertTrue(settingsManager.vibrationEnabled)
    }

    @Test
    fun vibrationEnabled_setsValueCorrectly() {
        settingsManager.vibrationEnabled = true
        verify(mockEditor).putBoolean("vibration_enabled", true)
        verify(mockEditor).apply()
    }

    // Challenges Required Tests

    @Test
    fun challengesRequired_defaultsToOne() {
        `when`(mockSharedPreferences.getInt("challenges_required", 1)).thenReturn(1)
        assertEquals(1, settingsManager.challengesRequired)
    }

    @Test
    fun challengesRequired_returnsStoredValue() {
        `when`(mockSharedPreferences.getInt("challenges_required", 1)).thenReturn(3)
        assertEquals(3, settingsManager.challengesRequired)
    }

    @Test
    fun challengesRequired_setsValueCorrectly() {
        settingsManager.challengesRequired = 5
        verify(mockEditor).putInt("challenges_required", 5)
        verify(mockEditor).apply()
    }

    // Theme Tests

    @Test
    fun theme_defaultsToSystem() {
        `when`(mockSharedPreferences.getString("theme", "system")).thenReturn("system")
        assertEquals("system", settingsManager.theme)
    }

    @Test
    fun theme_returnsStoredValue() {
        `when`(mockSharedPreferences.getString("theme", "system")).thenReturn("dark")
        assertEquals("dark", settingsManager.theme)
    }

    @Test
    fun theme_setsValueCorrectly() {
        settingsManager.theme = "light"
        verify(mockEditor).putString("theme", "light")
        verify(mockEditor).apply()
    }

    // Master Disable Tests

    @Test
    fun masterDisable_defaultsToFalse() {
        `when`(mockSharedPreferences.getBoolean("master_disable", false)).thenReturn(false)
        assertFalse(settingsManager.masterDisable)
    }

    @Test
    fun masterDisable_returnsStoredValue() {
        `when`(mockSharedPreferences.getBoolean("master_disable", false)).thenReturn(true)
        assertTrue(settingsManager.masterDisable)
    }

    @Test
    fun masterDisable_setsValueCorrectly() {
        settingsManager.masterDisable = true
        verify(mockEditor).putBoolean("master_disable", true)
        verify(mockEditor).apply()
    }
}
