package com.example

import com.example.data.SettingsDataStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PinHashingTest {

    @Test
    fun testPinHashingDeterminism() {
        val pin = "1234"
        val hash1 = SettingsDataStore.hashPin(pin)
        val hash2 = SettingsDataStore.hashPin(pin)

        assertEquals(hash1, hash2)
        assertEquals(64, hash1.length) // SHA-256 hex string is 64 chars
    }

    @Test
    fun testPinVerification() {
        val pin = "5829"
        val hash = SettingsDataStore.hashPin(pin)

        assertTrue(SettingsDataStore.verifyPin("5829", hash))
        assertFalse(SettingsDataStore.verifyPin("1111", hash))
        assertFalse(SettingsDataStore.verifyPin("5828", hash))
    }

    @Test
    fun testNullPinVerification() {
        // When no PIN is set (null hash), any pin returns true
        assertTrue(SettingsDataStore.verifyPin("1234", null))
        assertTrue(SettingsDataStore.verifyPin("", null))
    }
}
