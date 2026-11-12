package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        NoteEntity::class,
        AlarmEntity::class,
        HomeItemEntity::class,
        LocalCalendarEventEntity::class,
        NotificationEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class PixelSimDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao
    abstract fun alarmDao(): AlarmDao
    abstract fun homeDao(): HomeDao
    abstract fun calendarEventDao(): CalendarEventDao
    abstract fun notificationDao(): NotificationDao

    companion object {
        @Volatile
        private var INSTANCE: PixelSimDatabase? = null

        fun getInstance(context: Context): PixelSimDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    PixelSimDatabase::class.java,
                    "pixelsim.db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
