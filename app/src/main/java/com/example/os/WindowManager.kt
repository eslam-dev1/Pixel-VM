package com.example.os

import android.graphics.Bitmap
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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

enum class OsScreen {
    LOCK,
    PIN,
    HOME,
    DRAWER,
    RECENTS,
    APP,
    SPLIT_SCREEN
}

data class SplitScreenState(
    val topAppId: String? = null,
    val bottomAppId: String? = null,
    val splitRatio: Float = 0.5f
) {
    companion object {
        val SNAP_TARGETS = listOf(0.3f, 0.5f, 0.7f)

        fun snapRatio(rawRatio: Float): Float {
            val clamped = rawRatio.coerceIn(0.25f, 0.75f)
            return SNAP_TARGETS.minByOrNull { abs(it - clamped) } ?: 0.5f
        }
    }
}

sealed class LiveActivityState {
    data object None : LiveActivityState()

    data class PhoneCallActivity(
        val callerOrNumber: String,
        val durationSeconds: Int,
        val isOnHold: Boolean = false
    ) : LiveActivityState() {
        fun formattedDuration(): String {
            val mins = durationSeconds / 60
            val secs = durationSeconds % 60
            return String.format(Locale.US, "%02d:%02d", mins, secs)
        }
    }

    data class TimerActivity(
        val label: String,
        val remainingSeconds: Int,
        val isRunning: Boolean
    ) : LiveActivityState() {
        fun formattedRemaining(): String {
            val mins = remainingSeconds / 60
            val secs = remainingSeconds % 60
            return String.format(Locale.US, "%02d:%02d", mins, secs)
        }
    }

    data class StopwatchActivity(
        val elapsedMillis: Long,
        val isRunning: Boolean
    ) : LiveActivityState() {
        fun formattedElapsed(): String {
            val mins = elapsedMillis / 60000
            val secs = (elapsedMillis % 60000) / 1000
            return String.format(Locale.US, "%02d:%02d", mins, secs)
        }
    }

    data class MediaActivity(
        val trackTitle: String,
        val artist: String,
        val isPlaying: Boolean
    ) : LiveActivityState()
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

    // Split-Screen Multitasking
    private val _splitScreenState = MutableStateFlow(SplitScreenState())
    val splitScreenState: StateFlow<SplitScreenState> = _splitScreenState.asStateFlow()

    // Live Activities & Dynamic Pill
    private val _phoneCallActivity = MutableStateFlow<LiveActivityState.PhoneCallActivity?>(null)
    private val _timerActivity = MutableStateFlow<LiveActivityState.TimerActivity?>(null)
    private val _stopwatchActivity = MutableStateFlow<LiveActivityState.StopwatchActivity?>(null)
    private val _mediaActivity = MutableStateFlow<LiveActivityState.MediaActivity?>(null)

    val liveActivityState: StateFlow<LiveActivityState> = combine(
        _phoneCallActivity,
        _timerActivity,
        _stopwatchActivity,
        _mediaActivity
    ) { call, timer, stopwatch, media ->
        resolveLiveActivityPriority(call, timer, stopwatch, media)
    }.let { flow ->
        if (scope != null) {
            flow.stateIn(scope, SharingStarted.Eagerly, LiveActivityState.None)
        } else {
            MutableStateFlow<LiveActivityState>(LiveActivityState.None).also { fallback ->
                // Synchronous fallback when constructed without scope in unit tests
            }
        }
    }

    // For scope-less unit test access without waiting on StateIn
    private val _directLiveActivity = MutableStateFlow<LiveActivityState>(LiveActivityState.None)
    val currentLiveActivity: StateFlow<LiveActivityState>
        get() = if (scope != null) liveActivityState else _directLiveActivity.asStateFlow()

    // Android 15 Private Space ephemeral unlock state
    private val _isPrivateSpaceUnlocked = MutableStateFlow(false)
    val isPrivateSpaceUnlocked: StateFlow<Boolean> = _isPrivateSpaceUnlocked.asStateFlow()

    // Overview Smart Select & Pixel Markup Screenshot Editor
    private val _smartSelectAppId = MutableStateFlow<String?>(null)
    val smartSelectAppId: StateFlow<String?> = _smartSelectAppId.asStateFlow()

    private val _markupBitmap = MutableStateFlow<Bitmap?>(null)
    val markupBitmap: StateFlow<Bitmap?> = _markupBitmap.asStateFlow()

    private val _taskPreviews = MutableStateFlow<Map<String, Bitmap>>(emptyMap())
    val taskPreviews: StateFlow<Map<String, Bitmap>> = _taskPreviews.asStateFlow()

    private val _isCircleToSearchOpen = MutableStateFlow(false)
    val isCircleToSearchOpen: StateFlow<Boolean> = _isCircleToSearchOpen.asStateFlow()

    private val _inMemoryNotifications = MutableStateFlow<List<SimulatedNotification>>(emptyList())

    val notifications: StateFlow<List<SimulatedNotification>> = if (notificationDao != null && scope != null) {
        notificationDao.getAllNotifications().map { entities ->
            entities.map {
                SimulatedNotification(
                    id = it.id,
                    title = it.title,
                    message = it.message,
                    timeFormatted = it.timeFormatted,
                    targetAppId = it.targetAppId
                )
            }
        }.stateIn(scope, SharingStarted.Eagerly, emptyList())
    } else {
        _inMemoryNotifications.asStateFlow()
    }

    private fun syncDirectLiveActivity() {
        _directLiveActivity.value = resolveLiveActivityPriority(
            _phoneCallActivity.value,
            _timerActivity.value,
            _stopwatchActivity.value,
            _mediaActivity.value
        )
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
        _isPrivateSpaceUnlocked.value = false
        _isCircleToSearchOpen.value = false
        _smartSelectAppId.value = null
        _markupBitmap.value = null
        soundEffectsManager?.playLockSound()
    }

    fun openApp(appId: String) {
        _activeAppId.value = appId
        _currentScreen.value = OsScreen.APP
        _isNotificationShadeOpen.value = false
        _isCircleToSearchOpen.value = false
        _smartSelectAppId.value = null
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
        _isCircleToSearchOpen.value = false
        _smartSelectAppId.value = null
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
        _taskPreviews.update { it - appId }
        if (_splitScreenState.value.topAppId == appId || _splitScreenState.value.bottomAppId == appId) {
            exitSplitScreen(openTopApp = false)
        }
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
        _taskPreviews.value = emptyMap()
        _splitScreenState.value = SplitScreenState()
        _activeAppId.value = null
        _currentScreen.value = OsScreen.HOME
    }

    // --- Split-Screen Controls ---

    fun startSplitScreen(primaryAppId: String, secondaryAppId: String? = null) {
        val autoSecondary = secondaryAppId
            ?: _runningApps.value.firstOrNull { it != primaryAppId }
        _splitScreenState.value = SplitScreenState(
            topAppId = primaryAppId,
            bottomAppId = autoSecondary,
            splitRatio = 0.5f
        )
        _activeAppId.value = primaryAppId
        _currentScreen.value = OsScreen.SPLIT_SCREEN
        _isNotificationShadeOpen.value = false
        _runningApps.update { list ->
            val mutable = list.toMutableList()
            if (autoSecondary != null) {
                mutable.remove(autoSecondary)
                mutable.add(0, autoSecondary)
            }
            mutable.remove(primaryAppId)
            mutable.add(0, primaryAppId)
            mutable
        }
    }

    fun setSplitBottomApp(appId: String) {
        _splitScreenState.update { it.copy(bottomAppId = appId) }
        _runningApps.update { list ->
            val mutable = list.toMutableList()
            mutable.remove(appId)
            mutable.add(0, appId)
            mutable
        }
    }

    fun setSplitTopApp(appId: String) {
        _splitScreenState.update { it.copy(topAppId = appId) }
        _activeAppId.value = appId
    }

    fun swapSplitPanes() {
        _splitScreenState.update { state ->
            state.copy(topAppId = state.bottomAppId, bottomAppId = state.topAppId)
        }
    }

    fun updateSplitRatio(ratio: Float, snap: Boolean = false) {
        val next = if (snap) SplitScreenState.snapRatio(ratio) else ratio.coerceIn(0.25f, 0.75f)
        _splitScreenState.update { it.copy(splitRatio = next) }
    }

    fun exitSplitScreen(openTopApp: Boolean = true) {
        val top = _splitScreenState.value.topAppId
        _splitScreenState.value = SplitScreenState()
        if (openTopApp && top != null) {
            _activeAppId.value = top
            _currentScreen.value = OsScreen.APP
        } else {
            _currentScreen.value = OsScreen.HOME
        }
    }

    // --- Live Activity Pill Controls ---

    fun startSimulatedCall(callerOrNumber: String) {
        _phoneCallActivity.value = LiveActivityState.PhoneCallActivity(
            callerOrNumber = callerOrNumber,
            durationSeconds = 0
        )
        syncDirectLiveActivity()
    }

    fun tickSimulatedCall() {
        _phoneCallActivity.update { current ->
            current?.copy(durationSeconds = current.durationSeconds + 1)
        }
        syncDirectLiveActivity()
    }

    fun endSimulatedCall() {
        _phoneCallActivity.value = null
        syncDirectLiveActivity()
    }

    fun updateTimerActivity(remainingSeconds: Int, isRunning: Boolean, label: String = "Timer") {
        _timerActivity.value = if (remainingSeconds <= 0 && !isRunning) {
            null
        } else {
            LiveActivityState.TimerActivity(
                label = label,
                remainingSeconds = remainingSeconds,
                isRunning = isRunning
            )
        }
        syncDirectLiveActivity()
    }

    fun clearTimerActivity() {
        _timerActivity.value = null
        syncDirectLiveActivity()
    }

    fun updateStopwatchActivity(elapsedMillis: Long, isRunning: Boolean) {
        _stopwatchActivity.value = if (elapsedMillis <= 0L && !isRunning) {
            null
        } else {
            LiveActivityState.StopwatchActivity(
                elapsedMillis = elapsedMillis,
                isRunning = isRunning
            )
        }
        syncDirectLiveActivity()
    }

    fun setMediaActivity(trackTitle: String, artist: String, isPlaying: Boolean) {
        _mediaActivity.value = LiveActivityState.MediaActivity(
            trackTitle = trackTitle,
            artist = artist,
            isPlaying = isPlaying
        )
        syncDirectLiveActivity()
    }

    fun stopMedia() {
        _mediaActivity.value = null
        syncDirectLiveActivity()
    }

    // --- Private Space Controls ---

    fun unlockPrivateSpace() {
        _isPrivateSpaceUnlocked.value = true
    }

    fun lockPrivateSpace() {
        _isPrivateSpaceUnlocked.value = false
    }

    // --- Smart Select, Previews & Markup Controls ---

    fun saveTaskPreview(appId: String, bitmap: Bitmap) {
        _taskPreviews.update { current ->
            val trimmed = if (current.size >= 8) {
                current - current.keys.first()
            } else {
                current
            }
            trimmed + (appId to bitmap)
        }
    }

    fun openSmartSelect(appId: String) {
        _smartSelectAppId.value = appId
    }

    fun closeSmartSelect() {
        _smartSelectAppId.value = null
    }

    fun openMarkupEditor(bitmap: Bitmap) {
        _markupBitmap.value = bitmap
    }

    fun closeMarkupEditor() {
        _markupBitmap.value = null
    }

    fun openCircleToSearch() {
        _isCircleToSearchOpen.value = true
    }

    fun closeCircleToSearch() {
        _isCircleToSearchOpen.value = false
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
        if (_isCircleToSearchOpen.value) {
            _isCircleToSearchOpen.value = false
            return true
        }
        if (_markupBitmap.value != null) {
            _markupBitmap.value = null
            return true
        }
        if (_smartSelectAppId.value != null) {
            _smartSelectAppId.value = null
            return true
        }
        if (_isNotificationShadeOpen.value) {
            _isNotificationShadeOpen.value = false
            return true
        }
        return when (_currentScreen.value) {
            OsScreen.APP -> {
                _currentScreen.value = OsScreen.HOME
                true
            }
            OsScreen.SPLIT_SCREEN -> {
                _splitScreenState.value = SplitScreenState()
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

    companion object {

        fun resolveLiveActivityPriority(
            call: LiveActivityState.PhoneCallActivity?,
            timer: LiveActivityState.TimerActivity?,
            stopwatch: LiveActivityState.StopwatchActivity?,
            media: LiveActivityState.MediaActivity?
        ): LiveActivityState {
            if (call != null) return call
            if (timer != null && (timer.isRunning || timer.remainingSeconds > 0)) return timer
            if (stopwatch != null && (stopwatch.isRunning || stopwatch.elapsedMillis > 0L)) return stopwatch
            if (media != null) return media
            return LiveActivityState.None
        }

        fun filterNotificationsForPrivateSpace(
            notifications: List<SimulatedNotification>,
            privateAppIds: Set<String>,
            isPrivateUnlocked: Boolean
        ): List<SimulatedNotification> {
            if (isPrivateUnlocked || privateAppIds.isEmpty()) return notifications
            return notifications.map { notif ->
                if (notif.targetAppId in privateAppIds) {
                    notif.copy(
                        title = "Private Space",
                        message = "Unlock Private Space to view notification"
                    )
                } else {
                    notif
                }
            }
        }
    }
}
