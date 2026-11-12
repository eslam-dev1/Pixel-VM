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
class LockScreenCredentialTest {

    private lateinit var settingsDataStore: SettingsDataStore

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        settingsDataStore = SettingsDataStore(context)
    }

    @Test
    fun testPinHashingAndVerification() {
        val pin = "1234"
        val hash = SettingsDataStore.hashPin(pin)

        // Hashing produces valid non-empty string
        assertTrue(hash.isNotEmpty())

        // Correct PIN verifies true
        assertTrue(SettingsDataStore.verifyPin("1234", hash))

        // Incorrect PIN verifies false
        assertFalse(SettingsDataStore.verifyPin("9999", hash))

        // Null or blank hash allows unlock (when no PIN is set)
        assertTrue(SettingsDataStore.verifyPin("1234", null))
        assertTrue(SettingsDataStore.verifyPin("1234", ""))
    }

    @Test
    fun testPinCredentialDataStorePersistence() = runTest {
        // Set PIN
        settingsDataStore.setPin("5678")
        var settings = settingsDataStore.settingsFlow.first()

        val expectedHash = SettingsDataStore.hashPin("5678")
        assertEquals(expectedHash, settings.hashedPin)

        // Verify stored hash
        assertTrue(SettingsDataStore.verifyPin("5678", settings.hashedPin))
        assertFalse(SettingsDataStore.verifyPin("0000", settings.hashedPin))

        // Remove PIN
        settingsDataStore.setPin(null)
        settings = settingsDataStore.settingsFlow.first()
        assertNull(settings.hashedPin)
    }
}
