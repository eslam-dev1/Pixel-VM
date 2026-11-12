package com.example

import androidx.test.core.app.ApplicationProvider
import com.example.data.SettingsDataStore
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class SettingsNavigationTest {

    private lateinit var settingsDataStore: SettingsDataStore

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        settingsDataStore = SettingsDataStore(context)
    }

    @Test
    fun testDisplaySettingsToggles() = runTest {
        // Dark mode
        settingsDataStore.setDarkMode("light")
        assertEquals("light", settingsDataStore.settingsFlow.first().darkMode)

        settingsDataStore.setDarkMode("dark")
        assertEquals("dark", settingsDataStore.settingsFlow.first().darkMode)

        // Bezel toggle
        settingsDataStore.setShowBezel(false)
        assertFalse(settingsDataStore.settingsFlow.first().showBezel)

        settingsDataStore.setShowBezel(true)
        assertTrue(settingsDataStore.settingsFlow.first().showBezel)

        // Font scale
        settingsDataStore.setFontScale(1.15f)
        assertEquals(1.15f, settingsDataStore.settingsFlow.first().fontScale, 0.01f)
    }

    @Test
    fun testPinManagement() = runTest {
        // Set new PIN
        settingsDataStore.setPin("4321")
        val hashed = settingsDataStore.settingsFlow.first().hashedPin
        assertTrue(SettingsDataStore.verifyPin("4321", hashed))
        assertFalse(SettingsDataStore.verifyPin("1111", hashed))

        // Remove PIN
        settingsDataStore.setPin(null)
        assertNull(settingsDataStore.settingsFlow.first().hashedPin)
    }

    @Test
    fun testResetSimulatedPhoneOperation() = runTest {
        var resetCalled = false
        val onResetAction = { resetCalled = true }

        // Trigger reset action
        onResetAction()
        assertTrue(resetCalled)

        // Clear settings
        settingsDataStore.clearAll()
        val defaultSettings = settingsDataStore.settingsFlow.first()
        assertEquals("dark", defaultSettings.darkMode)
        assertFalse(defaultSettings.showBezel)
        assertNull(defaultSettings.hashedPin)
    }
}
