package com.example

import com.example.os.OsScreen
import com.example.os.WindowManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class WindowManagerTest {

    private lateinit var windowManager: WindowManager

    @Before
    fun setUp() {
        windowManager = WindowManager()
    }

    @Test
    fun testInitialState() {
        assertEquals(OsScreen.LOCK, windowManager.currentScreen.value)
        assertNull(windowManager.activeAppId.value)
        assertTrue(windowManager.runningApps.value.isEmpty())
        assertFalse(windowManager.isNotificationShadeOpen.value)
    }

    @Test
    fun testUnlockFlow() {
        // Without PIN
        windowManager.unlock(requiresPin = false)
        assertEquals(OsScreen.HOME, windowManager.currentScreen.value)

        // Lock again
        windowManager.lock()
        assertEquals(OsScreen.LOCK, windowManager.currentScreen.value)

        // With PIN
        windowManager.unlock(requiresPin = true)
        assertEquals(OsScreen.PIN, windowManager.currentScreen.value)

        windowManager.onPinSuccess()
        assertEquals(OsScreen.HOME, windowManager.currentScreen.value)
    }

    @Test
    fun testOpenAppAndStackManagement() {
        windowManager.unlock(requiresPin = false)

        // Open first app
        windowManager.openApp("calculator")
        assertEquals(OsScreen.APP, windowManager.currentScreen.value)
        assertEquals("calculator", windowManager.activeAppId.value)
        assertEquals(listOf("calculator"), windowManager.runningApps.value)

        // Open second app
        windowManager.openApp("notes")
        assertEquals(OsScreen.APP, windowManager.currentScreen.value)
        assertEquals("notes", windowManager.activeAppId.value)
        assertEquals(listOf("notes", "calculator"), windowManager.runningApps.value)

        // Re-open first app (should bring to top of recents stack)
        windowManager.openApp("calculator")
        assertEquals("calculator", windowManager.activeAppId.value)
        assertEquals(listOf("calculator", "notes"), windowManager.runningApps.value)
    }

    @Test
    fun testNavigationScreens() {
        windowManager.unlock(requiresPin = false)

        windowManager.openDrawer()
        assertEquals(OsScreen.DRAWER, windowManager.currentScreen.value)

        windowManager.closeDrawer()
        assertEquals(OsScreen.HOME, windowManager.currentScreen.value)

        windowManager.openRecents()
        assertEquals(OsScreen.RECENTS, windowManager.currentScreen.value)

        windowManager.goHome()
        assertEquals(OsScreen.HOME, windowManager.currentScreen.value)
    }

    @Test
    fun testCloseAppAndLifecycle() {
        windowManager.unlock(requiresPin = false)
        windowManager.openApp("calculator")
        windowManager.openApp("notes")
        windowManager.openApp("settings")

        assertEquals("settings", windowManager.activeAppId.value)
        assertEquals(listOf("settings", "notes", "calculator"), windowManager.runningApps.value)

        // Close currently active app ("settings")
        windowManager.closeApp("settings")
        assertEquals("notes", windowManager.activeAppId.value)
        assertEquals(listOf("notes", "calculator"), windowManager.runningApps.value)

        // Close non-active app ("calculator")
        windowManager.closeApp("calculator")
        assertEquals("notes", windowManager.activeAppId.value)
        assertEquals(listOf("notes"), windowManager.runningApps.value)

        // Close last remaining app
        windowManager.closeApp("notes")
        assertNull(windowManager.activeAppId.value)
        assertEquals(OsScreen.HOME, windowManager.currentScreen.value)
        assertTrue(windowManager.runningApps.value.isEmpty())
    }

    @Test
    fun testClearAllRecentApps() {
        windowManager.unlock(requiresPin = false)
        windowManager.openApp("calculator")
        windowManager.openApp("notes")

        windowManager.clearAllRecentApps()
        assertNull(windowManager.activeAppId.value)
        assertEquals(OsScreen.HOME, windowManager.currentScreen.value)
        assertTrue(windowManager.runningApps.value.isEmpty())
    }

    @Test
    fun testNotificationShadeState() {
        assertFalse(windowManager.isNotificationShadeOpen.value)

        windowManager.toggleNotificationShade()
        assertTrue(windowManager.isNotificationShadeOpen.value)

        windowManager.setNotificationShadeOpen(false)
        assertFalse(windowManager.isNotificationShadeOpen.value)
    }

    @Test
    fun testBackGestureHandling() {
        windowManager.unlock(requiresPin = false)

        // On Home screen, back gesture returns false (cannot go back further)
        assertFalse(windowManager.handleBackGesture())

        // Inside App
        windowManager.openApp("calculator")
        assertTrue(windowManager.handleBackGesture())
        assertEquals(OsScreen.HOME, windowManager.currentScreen.value)

        // Drawer
        windowManager.openDrawer()
        assertTrue(windowManager.handleBackGesture())
        assertEquals(OsScreen.HOME, windowManager.currentScreen.value)

        // Recents
        windowManager.openRecents()
        assertTrue(windowManager.handleBackGesture())
        assertEquals(OsScreen.HOME, windowManager.currentScreen.value)

        // Notification shade priority over screen
        windowManager.openApp("notes")
        windowManager.setNotificationShadeOpen(true)
        assertTrue(windowManager.handleBackGesture())
        assertFalse(windowManager.isNotificationShadeOpen.value)
        assertEquals(OsScreen.APP, windowManager.currentScreen.value)
    }

    @Test
    fun testNotificationPostingAndDismissal() {
        assertTrue(windowManager.notifications.value.isEmpty())

        windowManager.postNotification(
            id = "test_notif",
            title = "Test Notif",
            message = "Hello World",
            targetAppId = "notes"
        )

        val notifications = windowManager.notifications.value
        assertTrue(notifications.any { it.id == "test_notif" && it.title == "Test Notif" })

        windowManager.dismissNotification("test_notif")
        val updatedNotifications = windowManager.notifications.value
        assertFalse(updatedNotifications.any { it.id == "test_notif" })
        assertTrue(updatedNotifications.isEmpty())

        windowManager.postNotification("n2", "Title", "Body", "notes")
        windowManager.clearAllNotifications()
        assertTrue(windowManager.notifications.value.isEmpty())
    }
}
