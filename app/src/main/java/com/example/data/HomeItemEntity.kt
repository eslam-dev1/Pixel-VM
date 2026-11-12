package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "home_items")
data class HomeItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val itemType: String, // "APP" or "FOLDER"
    val targetAppId: String? = null,
    val folderTitle: String? = null,
    val folderAppIdsCsv: String? = null, // e.g. "clock,calculator,files"
    val isDock: Boolean = false,
    val pageIndex: Int = 0,
    val slotIndex: Int = 0
)

@Dao
interface HomeDao {
    @Query("SELECT * FROM home_items ORDER BY isDock ASC, pageIndex ASC, slotIndex ASC")
    fun getAllHomeItems(): Flow<List<HomeItemEntity>>

    @Query("SELECT * FROM home_items ORDER BY isDock ASC, pageIndex ASC, slotIndex ASC")
    suspend fun getHomeItemsSync(): List<HomeItemEntity>

    @Query("SELECT COUNT(*) FROM home_items")
    suspend fun countHomeItems(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: HomeItemEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<HomeItemEntity>)

    @Update
    suspend fun updateItem(item: HomeItemEntity)

    @Delete
    suspend fun deleteItem(item: HomeItemEntity)

    @Query("DELETE FROM home_items WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM home_items")
    suspend fun clearAll()
}
