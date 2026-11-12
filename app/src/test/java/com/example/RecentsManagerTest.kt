package com.example

import com.example.os.RecentsManager
import org.junit.Assert.assertEquals
import org.junit.Test

class RecentsManagerTest {
    @Test
    fun testOpenAndCloseApps() {
        val manager = RecentsManager()
        manager.openApp("calculator")
        manager.openApp("clock")
        
        assertEquals(listOf("clock", "calculator"), manager.runningApps.value)

        manager.openApp("calculator")
        assertEquals(listOf("calculator", "clock"), manager.runningApps.value)

        manager.closeApp("clock")
        assertEquals(listOf("calculator"), manager.runningApps.value)

        manager.clearAll()
        assertEquals(emptyList<String>(), manager.runningApps.value)
    }
}
