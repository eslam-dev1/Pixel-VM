package com.example.os

import android.app.NotificationManager
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.view.KeyEvent
import androidx.core.content.ContextCompat
import com.example.data.SettingsDataStore
import com.example.model.SimulatedSystemState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SystemStateHolder(
    private val context: Context,
    private val scope: CoroutineScope,
    private val settingsDataStore: SettingsDataStore? = null
) {
    private val _state = MutableStateFlow(SimulatedSystemState())
    val state: StateFlow<SimulatedSystemState> = _state.asStateFlow()

    private val _isDeviceMediaPlaying = MutableStateFlow(false)
    val isDeviceMediaPlaying: StateFlow<Boolean> = _isDeviceMediaPlaying.asStateFlow()

    private var clockJob: Job? = null
    private val timeFormat = SimpleDateFormat("h:mm", Locale.getDefault())
    private val dateFormat = SimpleDateFormat("EEE, MMM d", Locale.getDefault())

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
    private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
    private val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    private val batteryProvider = BatteryStatusProvider(context)
    val batteryTelemetry: StateFlow<BatteryState> = batteryProvider.batteryState

    private var networkCallback: ConnectivityManager.NetworkCallback? = null
    private var torchCameraId: String? = null
    private var systemEventsReceiver: BroadcastReceiver? = null

    init {
        scope.launch {
            batteryProvider.batteryState.collect { battery ->
                _state.update {
                    it.copy(
                        batteryPercent = battery.levelPercent,
                        isCharging = battery.isCharging,
                        isBatterySaverEnabled = battery.isPowerSaver
                    )
                }
            }
        }

        findTorchCamera()
        refreshSystemStates()
        registerNetworkCallback()
        registerSystemBroadcastReceiver()
        startClock()

        // Load only user's in-window brightness preference; hardware/OS states stay synced to real Android OS
        if (settingsDataStore != null) {
            scope.launch {
                val settings = settingsDataStore.settingsFlow.first()
                _state.update { it.copy(brightnessPercent = settings.qsBrightness) }
            }
        }
    }

    fun refreshSystemStates() {
        batteryProvider.refreshNow()
        updateRealNetworkState()
        updateRealBluetoothState()
        updateRealDndState()
        updateRealAutoRotateState()
        updateRealPowerSaverState()
        updateRealAudioState()
    }

    private fun registerSystemBroadcastReceiver() {
        val filter = IntentFilter().apply {
            addAction(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED)
            addAction(NotificationManager.ACTION_INTERRUPTION_FILTER_CHANGED)
            addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
            addAction("android.media.VOLUME_CHANGED_ACTION")
        }
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                when (intent?.action) {
                    PowerManager.ACTION_POWER_SAVE_MODE_CHANGED -> updateRealPowerSaverState()
                    NotificationManager.ACTION_INTERRUPTION_FILTER_CHANGED -> updateRealDndState()
                    BluetoothAdapter.ACTION_STATE_CHANGED -> updateRealBluetoothState()
                    "android.media.VOLUME_CHANGED_ACTION" -> updateRealAudioState()
                }
            }
        }
        systemEventsReceiver = receiver
        try {
            ContextCompat.registerReceiver(context, receiver, filter, ContextCompat.RECEIVER_EXPORTED)
        } catch (_: Exception) {}
    }

    private fun registerNetworkCallback() {
        try {
            networkCallback = object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: android.net.Network) {
                    updateRealNetworkState()
                }
                override fun onLost(network: android.net.Network) {
                    updateRealNetworkState()
                }
                override fun onCapabilitiesChanged(
                    network: android.net.Network,
                    networkCapabilities: NetworkCapabilities
                ) {
                    updateRealNetworkState()
                }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                connectivityManager?.registerDefaultNetworkCallback(networkCallback!!)
            }
        } catch (_: Exception) {}
    }

    private fun findTorchCamera() {
        try {
            cameraManager?.cameraIdList?.forEach { id ->
                val chars = cameraManager.getCameraCharacteristics(id)
                val hasFlash = chars.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
                val facing = chars.get(CameraCharacteristics.LENS_FACING)
                if (hasFlash && facing == CameraCharacteristics.LENS_FACING_BACK) {
                    torchCameraId = id
                    return
                }
            }
        } catch (_: Exception) {
            torchCameraId = null
        }
    }

    private fun startClock() {
        clockJob?.cancel()
        clockJob = scope.launch(Dispatchers.Default) {
            while (isActive) {
                val now = Date()
                val timeStr = timeFormat.format(now)
                val dateStr = dateFormat.format(now)
                val playing = try { audioManager?.isMusicActive == true } catch (_: Exception) { false }
                _isDeviceMediaPlaying.value = playing
                _state.update { it.copy(timeFormatted = timeStr, dateFormatted = dateStr) }
                delay(1000)
            }
        }
    }

    fun updateRealNetworkState() {
        try {
            val net = connectivityManager?.activeNetwork
            val caps = connectivityManager?.getNetworkCapabilities(net)
            val connected = caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true ||
                caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
            _state.update { it.copy(isWifiConnected = connected) }
        } catch (_: Exception) {}
    }

    fun updateRealBluetoothState() {
        try {
            val enabled = bluetoothManager?.adapter?.isEnabled == true
            _state.update { it.copy(isBluetoothEnabled = enabled) }
        } catch (_: SecurityException) {
            // Android 12+ may restrict adapter state read without BLUETOOTH_CONNECT
        } catch (_: Exception) {}
    }

    fun updateRealDndState() {
        try {
            val filter = notificationManager?.currentInterruptionFilter ?: NotificationManager.INTERRUPTION_FILTER_ALL
            val dnd = filter != NotificationManager.INTERRUPTION_FILTER_ALL &&
                filter != NotificationManager.INTERRUPTION_FILTER_UNKNOWN
            _state.update { it.copy(isDndEnabled = dnd) }
        } catch (_: Exception) {}
    }

    fun updateRealAutoRotateState() {
        try {
            val autoRotate = Settings.System.getInt(
                context.contentResolver,
                Settings.System.ACCELEROMETER_ROTATION,
                1
            ) == 1
            _state.update { it.copy(isAutoRotateEnabled = autoRotate) }
        } catch (_: Exception) {}
    }

    fun updateRealPowerSaverState() {
        try {
            val saver = powerManager?.isPowerSaveMode == true
            _state.update { it.copy(isBatterySaverEnabled = saver) }
        } catch (_: Exception) {}
    }

    fun updateRealAudioState() {
        try {
            audioManager?.let { am ->
                val current = am.getStreamVolume(AudioManager.STREAM_MUSIC)
                val max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
                val pct = (current.toFloat() / max.toFloat()).coerceIn(0f, 1f)
                _state.update { it.copy(volumePercent = pct) }
                _isDeviceMediaPlaying.value = am.isMusicActive
            }
        } catch (_: Exception) {}
    }

    fun setVolume(percent: Float) {
        val clamped = percent.coerceIn(0f, 1f)
        _state.update { it.copy(volumePercent = clamped) }
        try {
            audioManager?.let { am ->
                val max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                val targetVol = (clamped * max).toInt()
                am.setStreamVolume(AudioManager.STREAM_MUSIC, targetVol, 0)
            }
        } catch (_: Exception) {}
    }

    fun dispatchMediaKey(keyCode: Int) {
        try {
            audioManager?.let { am ->
                am.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, keyCode))
                am.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, keyCode))
            }
        } catch (_: Exception) {}
    }

    fun setBrightness(percent: Float) {
        val clamped = percent.coerceIn(0.1f, 1f)
        _state.update { it.copy(brightnessPercent = clamped) }
        if (settingsDataStore != null) {
            scope.launch { settingsDataStore.setQsBrightness(clamped) }
        }
    }

    fun toggleFlashlight() {
        val nextState = !_state.value.isFlashlightOn
        _state.update { it.copy(isFlashlightOn = nextState) }

        val camId = torchCameraId
        if (camId != null && cameraManager != null) {
            try {
                cameraManager.setTorchMode(camId, nextState)
            } catch (_: Exception) {}
        }
    }

    fun canControlDndDirectly(): Boolean {
        return try {
            notificationManager?.isNotificationPolicyAccessGranted == true
        } catch (_: Exception) {
            false
        }
    }

    fun canWriteSystemSettings(): Boolean {
        return try {
            Settings.System.canWrite(context)
        } catch (_: Exception) {
            false
        }
    }

    fun toggleWifi() {
        val next = !_state.value.isWifiConnected
        _state.update { it.copy(isWifiConnected = next) }
    }

    fun toggleBluetooth() {
        val next = !_state.value.isBluetoothEnabled
        _state.update { it.copy(isBluetoothEnabled = next) }
    }

    fun toggleDnd() {
        val next = !_state.value.isDndEnabled
        _state.update { it.copy(isDndEnabled = next) }
        if (canControlDndDirectly()) {
            try {
                notificationManager?.setInterruptionFilter(
                    if (next) NotificationManager.INTERRUPTION_FILTER_PRIORITY
                    else NotificationManager.INTERRUPTION_FILTER_ALL
                )
            } catch (_: Exception) {}
        }
    }

    fun toggleAutoRotate() {
        val next = !_state.value.isAutoRotateEnabled
        _state.update { it.copy(isAutoRotateEnabled = next) }
        if (canWriteSystemSettings()) {
            try {
                Settings.System.putInt(
                    context.contentResolver,
                    Settings.System.ACCELEROMETER_ROTATION,
                    if (next) 1 else 0
                )
            } catch (_: Exception) {}
        }
    }

    fun toggleBatterySaver() {
        val next = !_state.value.isBatterySaverEnabled
        _state.update { it.copy(isBatterySaverEnabled = next) }
    }

    fun destroy() {
        clockJob?.cancel()
        batteryProvider.unregister()
        try {
            systemEventsReceiver?.let { context.unregisterReceiver(it) }
        } catch (_: Exception) {}
        try {
            networkCallback?.let { connectivityManager?.unregisterNetworkCallback(it) }
        } catch (_: Exception) {}
        if (_state.value.isFlashlightOn && torchCameraId != null) {
            try {
                cameraManager?.setTorchMode(torchCameraId!!, false)
            } catch (_: Exception) {}
        }
    }
}
