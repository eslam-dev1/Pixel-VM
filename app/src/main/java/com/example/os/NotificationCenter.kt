package com.example.os

import com.example.data.NotificationDao
import com.example.data.NotificationEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import java.util.UUID

/**
 * Manages simulated app notifications posted to Room and observed via Flow.
 */
class NotificationCenter(
    private val notificationDao: NotificationDao,
    private val scope: CoroutineScope
) {
    fun getNotificationsFlow(): Flow<List<NotificationEntity>> {
        return notificationDao.getAllNotifications()
    }

    suspend fun post(
        title: String,
        message: String,
        targetAppId: String,
        id: String = UUID.randomUUID().toString()
    ) {
        val entity = NotificationEntity(
            id = id,
            title = title,
            message = message,
            timeFormatted = android.text.format.DateFormat.format("hh:mm a", System.currentTimeMillis()).toString(),
            targetAppId = targetAppId,
            timestamp = System.currentTimeMillis()
        )
        notificationDao.insertNotification(entity)
    }

    suspend fun dismiss(id: String) {
        notificationDao.deleteById(id)
    }

    suspend fun clearAll() {
        notificationDao.clearAll()
    }
}
