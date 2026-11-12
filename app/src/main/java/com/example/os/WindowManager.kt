package com.example.os

import com.example.data.NotificationDao
import com.example.data.NotificationEntity
import com.example.data.SettingsDataStore
import com.example.data.SystemSettings
import com.example.model.SimulatedNotification
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class OsScreen {
    LOCK,
    PIN,
    HOME,
    DRAWER,
    RECENTS,
    APP
}

class WindowManager(
    private val notificationDao: NotificationDao? = null,
    private val settingsDataStore: SettingsDataStore? = null,
    private val scope: CoroutineScope? = null,
    private val soundEffectsManager: SoundEffectsManager? = null
) {
    private val _currentScreen = MutableStateFlow(OsScreen.LOCK)
    val currentScreen: StateFlow<OsScreen> = _currentScreen.asStateFlow()

    private val _activeAppId = MutableStateFlow<String?>(null)
    val activeAppId: StateFlow<String?> = _activeAppId.asStateFlow()

    private val _runningApps = MutableStateFlow<List<String>>(emptyList())
    val runningApps: StateFlow<List<String>> = _runningApps.asStateFlow()

    private val _isNotificationShadeOpen = MutableStateFlow(false)
    val isNotificationShadeOpen: StateFlow<Boolean> = _isNotificationShadeOpen.asStateFlow()

    private val _inMemoryNotifications = MutableStateFlow<List<SimulatedNotification>>(
        listOf(
            SimulatedNotification(
                id = "sim_welcome",
                title = "Welcome to PixelSim",
                message = "Experience native Android & Material You inside your app.",
                timeFormatted = "Now",
                targetAppId = "settings"
            )
        )
    )

    val notifications: StateFlow<List<SimulatedNotification>> = if (notificationDao != null && scope != null) {
        notificationDao.getAllNotifications().map { entities ->
            if (entities.isEmpty()) {
                listOf(
                    SimulatedNotification(
                        id = "sim_welcome",
                        title = "Welcome to PixelSim",
                        message = "Experience native Android & Material You inside your app.",
                        timeFormatted = "Now",
                        targetAppId = "settings"
                    )
                )
            } else {
                entities.map {
                    SimulatedNotification(
                        id = it.id,
                        title = it.title,
                        message = it.message,
                        timeFormatted = it.timeFormatted,
                        targetAppId = it.targetAppId
                    )
                }
            }
        }.stateIn(scope, SharingStarted.Eagerly, _inMemoryNotifications.value)
    } else {
        _inMemoryNotifications.asStateFlow()
    }

    fun unlock(requiresPin: Boolean) {
        if (requiresPin) {
            _currentScreen.value = OsScreen.PIN
        } else {
            _currentScreen.value = OsScreen.HOME
            soundEffectsManager?.playUnlockSound()
        }
    }

    fun onPinSuccess() {
        _currentScreen.value = OsScreen.HOME
        soundEffectsManager?.playUnlockSound()
    }

    fun lock() {
        _currentScreen.value = OsScreen.LOCK
        _isNotificationShadeOpen.value = false
        soundEffectsManager?.playLockSound()
    }

    fun openApp(appId: String) {
        _activeAppId.value = appId
        _currentScreen.value = OsScreen.APP
        _isNotificationShadeOpen.value = false
        soundEffectsManager?.playTapSound()

        _runningApps.update { list ->
            val mutable = list.toMutableList()
            mutable.remove(appId)
            mutable.add(0, appId)
            mutable
        }
    }

    fun goHome() {
        _currentScreen.value = OsScreen.HOME
        _isNotificationShadeOpen.value = false
        soundEffectsManager?.playTapSound()
    }

    fun openDrawer() {
        _currentScreen.value = OsScreen.DRAWER
    }

    fun closeDrawer() {
        _currentScreen.value = OsScreen.HOME
    }

    fun openRecents() {
        _currentScreen.value = OsScreen.RECENTS
        _isNotificationShadeOpen.value = false
    }

    fun closeApp(appId: String) {
        _runningApps.update { it.filter { id -> id != appId } }
        if (_activeAppId.value == appId) {
            val remaining = _runningApps.value
            if (remaining.isNotEmpty()) {
                _activeAppId.value = remaining.first()
            } else {
                _activeAppId.value = null
                _currentScreen.value = OsScreen.HOME
            }
        }
    }

    fun clearAllRecentApps() {
        _runningApps.value = emptyList()
        _activeAppId.value = null
        _currentScreen.value = OsScreen.HOME
    }

    fun toggleNotificationShade() {
        _isNotificationShadeOpen.update { !it }
    }

    fun setNotificationShadeOpen(open: Boolean) {
        _isNotificationShadeOpen.value = open
    }

    fun postNotification(
        id: String,
        title: String,
        message: String,
        targetAppId: String,
        settings: SystemSettings? = null
    ) {
        // Check preferences
        if (settings != null) {
            when (targetAppId) {
                "clock" -> if (!settings.notifClockEnabled) return
                "notes" -> if (!settings.notifNotesEnabled) return
                "messages" -> if (!settings.notifMessagesEnabled) return
            }
        }

        val timeStr = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date())
        soundEffectsManager?.playNotificationSound()

        if (notificationDao != null && scope != null) {
            scope.launch {
                notificationDao.insertNotification(
                    NotificationEntity(
                        id = id,
                        title = title,
                        message = message,
                        timeFormatted = timeStr,
                        targetAppId = targetAppId,
                        timestamp = System.currentTimeMillis()
                    )
                )
            }
        } else {
            _inMemoryNotifications.update { current ->
                listOf(
                    SimulatedNotification(
                        id = id,
                        title = title,
                        message = message,
                        timeFormatted = timeStr,
                        targetAppId = targetAppId
                    )
                ) + current.filter { it.id != id }
            }
        }
    }

    fun dismissNotification(id: String) {
        if (notificationDao != null && scope != null) {
            scope.launch {
                notificationDao.deleteById(id)
            }
        } else {
            _inMemoryNotifications.update { it.filter { n -> n.id != id } }
        }
    }

    fun clearAllNotifications() {
        if (notificationDao != null && scope != null) {
            scope.launch {
                notificationDao.clearAll()
            }
        } else {
            _inMemoryNotifications.value = emptyList()
        }
    }

    fun handleBackGesture(): Boolean {
        if (_isNotificationShadeOpen.value) {
            _isNotificationShadeOpen.value = false
            return true
        }
        return when (_currentScreen.value) {
            OsScreen.APP -> {
                _currentScreen.value = OsScreen.HOME
                true
            }
            OsScreen.DRAWER, OsScreen.RECENTS -> {
                _currentScreen.value = OsScreen.HOME
                true
            }
            OsScreen.PIN -> {
                _currentScreen.value = OsScreen.LOCK
                true
            }
            else -> false
        }
    }
}
