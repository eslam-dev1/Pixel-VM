package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "calendar_events")
data class LocalCalendarEventEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val description: String = "",
    val startEpochMillis: Long,
    val endEpochMillis: Long
)

@Dao
interface CalendarEventDao {
    @Query("SELECT * FROM calendar_events ORDER BY startEpochMillis ASC")
    fun getAllEvents(): Flow<List<LocalCalendarEventEntity>>

    @Query("SELECT * FROM calendar_events WHERE startEpochMillis >= :startOfDay AND startEpochMillis < :endOfDay ORDER BY startEpochMillis ASC")
    fun getEventsForDay(startOfDay: Long, endOfDay: Long): Flow<List<LocalCalendarEventEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: LocalCalendarEventEntity): Long

    @Delete
    suspend fun deleteEvent(event: LocalCalendarEventEntity)

    @Query("DELETE FROM calendar_events")
    suspend fun clearAll()
}
