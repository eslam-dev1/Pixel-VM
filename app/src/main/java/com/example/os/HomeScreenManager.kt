package com.example.os

import com.example.data.HomeDao
import com.example.data.HomeItemEntity
import kotlinx.coroutines.flow.Flow

class HomeScreenManager(private val homeDao: HomeDao) {

    val homeItems: Flow<List<HomeItemEntity>> = homeDao.getAllHomeItems()

    suspend fun initializeDefaultLayoutIfNeeded() {
        val count = homeDao.countHomeItems()
        if (count == 0) {
            val initialItems = mutableListOf<HomeItemEntity>()

            // 4 Dock apps
            AppRegistry.DEFAULT_DOCK_APP_IDS.forEachIndexed { index, appId ->
                initialItems.add(
                    HomeItemEntity(
                        itemType = "APP",
                        targetAppId = appId,
                        isDock = true,
                        pageIndex = 0,
                        slotIndex = index
                    )
                )
            }

            // Grid apps
            AppRegistry.DEFAULT_GRID_APP_IDS.forEachIndexed { index, appId ->
                initialItems.add(
                    HomeItemEntity(
                        itemType = "APP",
                        targetAppId = appId,
                        isDock = false,
                        pageIndex = 0,
                        slotIndex = index
                    )
                )
            }

            homeDao.insertItems(initialItems)
        }
    }

    suspend fun swapItems(itemA: HomeItemEntity, itemB: HomeItemEntity) {
        val updatedA = itemA.copy(
            isDock = itemB.isDock,
            pageIndex = itemB.pageIndex,
            slotIndex = itemB.slotIndex
        )
        val updatedB = itemB.copy(
            isDock = itemA.isDock,
            pageIndex = itemA.pageIndex,
            slotIndex = itemA.slotIndex
        )
        homeDao.updateItem(updatedA)
        homeDao.updateItem(updatedB)
    }

    suspend fun createFolder(sourceItem: HomeItemEntity, targetItem: HomeItemEntity, folderName: String = "Folder") {
        if (sourceItem.id == targetItem.id) return

        val app1 = sourceItem.targetAppId ?: return
        val app2 = targetItem.targetAppId ?: return

        // Create folder item replacing targetItem position
        val folder = HomeItemEntity(
            itemType = "FOLDER",
            folderTitle = folderName,
            folderAppIdsCsv = "$app2,$app1",
            isDock = targetItem.isDock,
            pageIndex = targetItem.pageIndex,
            slotIndex = targetItem.slotIndex
        )

        homeDao.deleteById(sourceItem.id)
        homeDao.deleteById(targetItem.id)
        homeDao.insertItem(folder)
    }

    suspend fun resetToDefault() {
        homeDao.clearAll()
        initializeDefaultLayoutIfNeeded()
    }
}
