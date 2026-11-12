package com.example.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "simulated_notifications")
data class NotificationEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val message: String,
    val timeFormatted: String,
    val targetAppId: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Dao
interface NotificationDao {
    @Query("SELECT * FROM simulated_notifications ORDER BY timestamp DESC")
    fun getAllNotifications(): Flow<List<NotificationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: NotificationEntity)

    @Query("DELETE FROM simulated_notifications WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM simulated_notifications")
    suspend fun clearAll()

    @Query("SELECT COUNT(*) FROM simulated_notifications")
    suspend fun count(): Int
}
