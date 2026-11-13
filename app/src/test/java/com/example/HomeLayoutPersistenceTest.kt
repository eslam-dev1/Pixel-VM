package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.PixelSimDatabase
import com.example.os.HomeScreenManager
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class HomeLayoutPersistenceTest {

    private lateinit var context: Context
    private lateinit var db: PixelSimDatabase
    private lateinit var manager: HomeScreenManager

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, PixelSimDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        manager = HomeScreenManager(db.homeDao())
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun testDefaultLayoutInitialization() = runBlocking {
        manager.initializeDefaultLayoutIfNeeded()

        val items = db.homeDao().getHomeItemsSync()
        val dockItems = items.filter { it.isDock }
        val gridItems = items.filter { !it.isDock }

        assertEquals(4, dockItems.size) // 4 apps in dock
        assertEquals(7, gridItems.size) // 7 default grid apps

        // Ensure calling initialize again does not duplicate items
        manager.initializeDefaultLayoutIfNeeded()
        val itemsAfterSecondCall = db.homeDao().getHomeItemsSync()
        assertEquals(11, itemsAfterSecondCall.size)
    }

    @Test
    fun testSwapHomeItems() = runBlocking {
        manager.initializeDefaultLayoutIfNeeded()
        val items = db.homeDao().getHomeItemsSync()

        val item1 = items[0]
        val item2 = items[1]
        val origSlot1 = item1.slotIndex
        val origSlot2 = item2.slotIndex

        manager.swapItems(item1, item2)

        val updated1 = db.homeDao().getHomeItemsSync().find { it.id == item1.id }
        val updated2 = db.homeDao().getHomeItemsSync().find { it.id == item2.id }

        assertNotNull(updated1)
        assertNotNull(updated2)
        assertEquals(origSlot2, updated1?.slotIndex)
        assertEquals(origSlot1, updated2?.slotIndex)
    }

    @Test
    fun testCreateFolder() = runBlocking {
        manager.initializeDefaultLayoutIfNeeded()
        val items = db.homeDao().getHomeItemsSync()

        val grid1 = items.first { !it.isDock }
        val grid2 = items.filter { !it.isDock }[1]

        manager.createFolder(grid1, grid2, "Utilities")

        val newItems = db.homeDao().getHomeItemsSync()
        val folder = newItems.find { it.itemType == "FOLDER" }

        assertNotNull("Folder should be created", folder)
        assertEquals("Utilities", folder?.folderTitle)
        assertTrue(folder?.folderAppIdsCsv?.contains(grid1.targetAppId!!) == true)
        assertTrue(folder?.folderAppIdsCsv?.contains(grid2.targetAppId!!) == true)
    }
}
