package com.example

import com.example.data.HomeItemEntity
import com.example.model.SimulatedSystemState
import com.example.os.AppRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeScreenLayoutTest {

    @Test
    fun testDefaultDockItemsCount() {
        val dockAppIds = AppRegistry.DEFAULT_DOCK_APP_IDS
        assertEquals(4, dockAppIds.size)
        assertEquals(listOf("phone", "messages", "camera", "settings"), dockAppIds)
    }

    @Test
    fun testAppGridChunkingForPagedLayout() {
        // Create 20 test home items (not dock)
        val items = (1..20).map { i ->
            HomeItemEntity(
                id = i.toLong(),
                slotIndex = i,
                isDock = false,
                itemType = "APP",
                targetAppId = "app_$i"
            )
        }

        // Chunk into 12 items per page
        val pages = items.chunked(12)
        assertEquals(2, pages.size)
        assertEquals(12, pages[0].size)
        assertEquals(8, pages[1].size)
    }

    @Test
    fun testAtAGlanceWidgetFormatting() {
        val state = SimulatedSystemState(
            timeFormatted = "10:42 PM",
            dateFormatted = "Wed, Nov 12"
        )
        assertEquals("10:42 PM", state.timeFormatted)
        assertEquals("Wed, Nov 12", state.dateFormatted)
    }
}
