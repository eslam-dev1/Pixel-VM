package com.example

import com.example.data.SystemSettings
import com.example.model.AppDefinition
import com.example.model.PixelIconShape
import com.example.model.SimulatedNotification
import com.example.os.LiveActivityState
import com.example.os.OsScreen
import com.example.os.SmartSelectExtractor
import com.example.os.SplitScreenState
import com.example.os.WindowManager
import com.example.ui.components.MarkupHistoryManager
import com.example.ui.components.MarkupStroke
import com.example.ui.components.NormalizedCropRect
import com.example.ui.components.NormalizedPoint
import com.example.ui.phone.AppDrawerFilter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class PixelFeaturesTest {

    private lateinit var windowManager: WindowManager

    @Before
    fun setUp() {
        windowManager = WindowManager()
        windowManager.unlock(requiresPin = false)
    }

    @Test
    fun testSplitScreenLifecycleAndSnapping() {
        windowManager.openApp("notes")
        windowManager.openApp("calculator")

        // Start split screen from calculator; auto-picks notes as secondary
        windowManager.startSplitScreen("calculator")
        assertEquals(OsScreen.SPLIT_SCREEN, windowManager.currentScreen.value)
        assertEquals("calculator", windowManager.splitScreenState.value.topAppId)
        assertEquals("notes", windowManager.splitScreenState.value.bottomAppId)
        assertEquals(0.5f, windowManager.splitScreenState.value.splitRatio, 0.001f)

        // Swap panes
        windowManager.swapSplitPanes()
        assertEquals("notes", windowManager.splitScreenState.value.topAppId)
        assertEquals("calculator", windowManager.splitScreenState.value.bottomAppId)

        // Snap ratio checks
        windowManager.updateSplitRatio(0.32f, snap = true)
        assertEquals(0.3f, windowManager.splitScreenState.value.splitRatio, 0.001f)

        windowManager.updateSplitRatio(0.68f, snap = true)
        assertEquals(0.7f, windowManager.splitScreenState.value.splitRatio, 0.001f)

        windowManager.updateSplitRatio(0.49f, snap = true)
        assertEquals(0.5f, windowManager.splitScreenState.value.splitRatio, 0.001f)

        // Update bottom app
        windowManager.setSplitBottomApp("clock")
        assertEquals("clock", windowManager.splitScreenState.value.bottomAppId)

        // Exit split screen to top app
        windowManager.exitSplitScreen(openTopApp = true)
        assertEquals(OsScreen.APP, windowManager.currentScreen.value)
        assertEquals("notes", windowManager.activeAppId.value)

        // Re-enter and dismiss via Back gesture
        windowManager.startSplitScreen("notes", "clock")
        assertTrue(windowManager.handleBackGesture())
        assertEquals(OsScreen.HOME, windowManager.currentScreen.value)
        assertNull(windowManager.splitScreenState.value.topAppId)
    }

    @Test
    fun testCloseAppInSplitScreenExitsSplitMode() {
        windowManager.openApp("notes")
        windowManager.openApp("calculator")
        windowManager.startSplitScreen("calculator", "notes")
        assertEquals(OsScreen.SPLIT_SCREEN, windowManager.currentScreen.value)

        windowManager.closeApp("notes")
        assertEquals(OsScreen.HOME, windowManager.currentScreen.value)
        assertNull(windowManager.splitScreenState.value.bottomAppId)
    }

    @Test
    fun testSplitScreenSnapTargetHelpers() {
        assertEquals(0.3f, SplitScreenState.snapRatio(0.10f), 0.001f)
        assertEquals(0.5f, SplitScreenState.snapRatio(0.51f), 0.001f)
        assertEquals(0.7f, SplitScreenState.snapRatio(0.95f), 0.001f)
    }

    @Test
    fun testLiveActivityPriorityAndTransitions() {
        assertEquals(LiveActivityState.None, windowManager.currentLiveActivity.value)

        // 1. Lowest priority: Media
        windowManager.setMediaActivity("Audio Stream", "Music Player", isPlaying = true)
        val media = windowManager.currentLiveActivity.value as LiveActivityState.MediaActivity
        assertTrue(media.isPlaying)
        assertEquals("Audio Stream", media.trackTitle)

        // 2. Stopwatch overrides Media
        windowManager.updateStopwatchActivity(elapsedMillis = 65_000L, isRunning = true)
        val sw = windowManager.currentLiveActivity.value as LiveActivityState.StopwatchActivity
        assertEquals("01:05", sw.formattedElapsed())

        // 3. Timer overrides Stopwatch
        windowManager.updateTimerActivity(remainingSeconds = 125, isRunning = true)
        val timer = windowManager.currentLiveActivity.value as LiveActivityState.TimerActivity
        assertEquals("02:05", timer.formattedRemaining())

        // 4. Active Phone Call overrides everything
        windowManager.startSimulatedCall("555-0199")
        windowManager.tickSimulatedCall()
        windowManager.tickSimulatedCall()
        val call = windowManager.currentLiveActivity.value as LiveActivityState.PhoneCallActivity
        assertEquals("555-0199", call.callerOrNumber)
        assertEquals("00:02", call.formattedDuration())

        // Ending call falls back down the priority stack cleanly
        windowManager.endSimulatedCall()
        assertTrue(windowManager.currentLiveActivity.value is LiveActivityState.TimerActivity)

        windowManager.clearTimerActivity()
        assertTrue(windowManager.currentLiveActivity.value is LiveActivityState.StopwatchActivity)

        windowManager.updateStopwatchActivity(elapsedMillis = 0L, isRunning = false)
        assertTrue(windowManager.currentLiveActivity.value is LiveActivityState.MediaActivity)

        windowManager.stopMedia()
        assertEquals(LiveActivityState.None, windowManager.currentLiveActivity.value)
    }

    @Test
    fun testPrivateSpaceLockingAndNotificationMasking() {
        val sampleNotifs = listOf(
            SimulatedNotification("n1", "Secret Note", "Bank PIN is 9999", "Now", "notes"),
            SimulatedNotification("n2", "Alarm", "07:00 AM", "Now", "clock")
        )
        val privateIds = setOf("notes", "gallery")

        // Locked: private app notification must be masked
        val masked = WindowManager.filterNotificationsForPrivateSpace(
            notifications = sampleNotifs,
            privateAppIds = privateIds,
            isPrivateUnlocked = false
        )
        assertEquals("Private Space", masked[0].title)
        assertEquals("Unlock Private Space to view notification", masked[0].message)
        assertEquals("Alarm", masked[1].title)

        // Unlocked: real content restored
        windowManager.unlockPrivateSpace()
        assertTrue(windowManager.isPrivateSpaceUnlocked.value)
        val revealed = WindowManager.filterNotificationsForPrivateSpace(
            notifications = sampleNotifs,
            privateAppIds = privateIds,
            isPrivateUnlocked = windowManager.isPrivateSpaceUnlocked.value
        )
        assertEquals("Secret Note", revealed[0].title)
        assertEquals("Bank PIN is 9999", revealed[0].message)

        // Locking device relocks Private Space automatically
        windowManager.lock()
        assertFalse(windowManager.isPrivateSpaceUnlocked.value)
    }

    @Test
    fun testAppDrawerFilteringForPrivateSpace() {
        val apps = listOf(
            AppDefinition("notes", R.string.app_notes, "notes") to "Notes",
            AppDefinition("clock", R.string.app_clock, "clock") to "Clock",
            AppDefinition("gallery", R.string.app_gallery, "gallery") to "Gallery"
        )
        val privateIds = setOf("notes", "gallery")

        val publicAll = AppDrawerFilter.filterPublicApps(apps, privateIds, searchQuery = "")
        assertEquals(listOf("clock"), publicAll.map { it.first.id })

        val publicSearchSecret = AppDrawerFilter.filterPublicApps(apps, privateIds, searchQuery = "Note")
        assertTrue(publicSearchSecret.isEmpty())

        val privateList = AppDrawerFilter.filterPrivateApps(apps, privateIds)
        assertEquals(listOf("notes", "gallery"), privateList.map { it.first.id })
    }

    @Test
    fun testMarkupHistoryUndoRedoAndCropBounds() {
        val history = MarkupHistoryManager()
        val stroke1 = MarkupStroke(
            points = listOf(NormalizedPoint(0.1f, 0.1f), NormalizedPoint(0.5f, 0.5f)),
            colorArgb = -65536,
            strokeWidthRatio = 0.01f,
            isHighlighter = false
        )
        val stroke2 = MarkupStroke(
            points = listOf(NormalizedPoint(0.2f, 0.2f), NormalizedPoint(0.8f, 0.8f)),
            colorArgb = -16776961,
            strokeWidthRatio = 0.03f,
            isHighlighter = true
        )

        assertFalse(history.canUndo)
        assertFalse(history.canRedo)

        history.addStroke(stroke1)
        history.addStroke(stroke2)
        assertEquals(2, history.strokes.size)
        assertTrue(history.canUndo)

        val undone = history.undo()
        assertEquals(stroke2, undone)
        assertEquals(1, history.strokes.size)
        assertTrue(history.canRedo)

        val redone = history.redo()
        assertEquals(stroke2, redone)
        assertEquals(2, history.strokes.size)
        assertFalse(history.canRedo)

        // Adding a new stroke after undo clears redo stack
        history.undo()
        assertTrue(history.canRedo)
        history.addStroke(stroke1)
        assertFalse(history.canRedo)

        // Crop clamping guarantees minimum 15% box inside [0, 1]
        val extremeCrop = NormalizedCropRect(left = -0.5f, top = -0.5f, right = 0.01f, bottom = 1.8f).clamped()
        assertEquals(0f, extremeCrop.left, 0.001f)
        assertEquals(0f, extremeCrop.top, 0.001f)
        assertTrue(extremeCrop.right >= 0.15f)
        assertEquals(1f, extremeCrop.bottom, 0.001f)

        val insetCrop = NormalizedCropRect().inset(0.1f, 0.1f, -0.1f, -0.1f)
        assertEquals(0.1f, insetCrop.left, 0.001f)
        assertEquals(0.9f, insetCrop.right, 0.001f)
    }

    @Test
    fun testSmartSelectExtractorSnippets() {
        val snippets = SmartSelectExtractor.extractSnippets(
            appId = "messages",
            appDisplayName = "Messages",
            liveStatusText = "01:12 • 555-0199"
        )
        assertTrue(snippets.any { it.label == "Screen" && it.text == "Messages" })
        assertTrue(snippets.any { it.label == "Live Activity" && it.category == "Live" && it.text == "01:12 • 555-0199" })
        assertEquals(2, snippets.size)

        // Deduplication + URL/Phone classification check
        val deduped = SmartSelectExtractor.extractSnippets(
            appId = "messages",
            appDisplayName = "Messages",
            extraContent = listOf(
                "Messages",
                "+1 (555) 123-4567",
                "https://developer.android.com",
                "Meeting notes"
            )
        )
        assertEquals(1, deduped.count { it.text == "Messages" })
        assertTrue(deduped.any { it.label == "Phone" && it.text == "+1 (555) 123-4567" })
        assertTrue(deduped.any { it.label == "Link" && it.text == "https://developer.android.com" })
        assertTrue(deduped.any { it.label == "Content" && it.text == "Meeting notes" })
    }

    @Test
    fun testSystemSettingsThemeAndPrivateSpaceParsing() {
        val valid = SystemSettings(
            pixelIconShape = PixelIconShape.TEARDROP.name,
            useMaterialYouMonochrome = true,
            tintPackIconsWithMaterialYou = true,
            privateSpaceAppsCsv = " notes , , gallery , calculator "
        )
        val theme = valid.toPixelThemeSettings()
        assertEquals(PixelIconShape.TEARDROP, theme.iconShape)
        assertTrue(theme.useMaterialYouMonochrome)
        assertTrue(theme.tintPackIconsWithMaterialYou)
        assertEquals(setOf("notes", "gallery", "calculator"), valid.privateSpaceAppIds())

        val invalidEnum = SystemSettings(pixelIconShape = "NON_EXISTENT_SHAPE")
        assertNotNull(invalidEnum.toPixelThemeSettings())
        assertEquals(PixelIconShape.CIRCLE, invalidEnum.toPixelThemeSettings().iconShape)
    }

    @Test
    fun testCircleToSearchLifecycleAndScreenSnippets() {
        assertFalse(windowManager.isCircleToSearchOpen.value)

        windowManager.openApp("notes")
        windowManager.openCircleToSearch()
        assertTrue(windowManager.isCircleToSearchOpen.value)

        // Back gesture dismisses Circle to Search before exiting the active app
        assertTrue(windowManager.handleBackGesture())
        assertFalse(windowManager.isCircleToSearchOpen.value)
        assertEquals(OsScreen.APP, windowManager.currentScreen.value)

        // Locking device also closes Circle to Search
        windowManager.openCircleToSearch()
        assertTrue(windowManager.isCircleToSearchOpen.value)
        windowManager.lock()
        assertFalse(windowManager.isCircleToSearchOpen.value)

        // Screen-level snippet extraction uses strictly real display name + extraContent
        val homeSnippets = SmartSelectExtractor.extractSnippets(
            appId = "home",
            appDisplayName = "PixelSim",
            extraContent = listOf("Tue, Nov 12 • 3:30")
        )
        assertEquals(2, homeSnippets.size)
        assertTrue(homeSnippets.any { it.text == "PixelSim" })
        assertTrue(homeSnippets.any { it.text == "Tue, Nov 12 • 3:30" })
    }
}
