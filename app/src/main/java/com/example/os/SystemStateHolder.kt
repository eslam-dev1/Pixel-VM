package com.example.os

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
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

    private var clockJob: Job? = null
    private val timeFormat = SimpleDateFormat("h:mm", Locale.getDefault())
    private val dateFormat = SimpleDateFormat("EEE, MMM d", Locale.getDefault())

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
    private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager

    private var networkCallback: ConnectivityManager.NetworkCallback? = null
    private var torchCameraId: String? = null

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_BATTERY_CHANGED) {
                val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                        status == BatteryManager.BATTERY_STATUS_FULL

                val pct = if (level >= 0 && scale > 0) {
                    ((level.toFloat() / scale.toFloat()) * 100).toInt()
                } else {
                    85
                }
                _state.update { it.copy(batteryPercent = pct, isCharging = isCharging) }
            }
        }
    }

    init {
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        context.registerReceiver(batteryReceiver, filter)

        findTorchCamera()
        updateRealNetworkState()
        registerNetworkCallback()
        updateRealAudioState()
        startClock()

        // Load persisted Quick Settings states if settingsDataStore is available
        if (settingsDataStore != null) {
            scope.launch {
                val settings = settingsDataStore.settingsFlow.first()
                _state.update {
                    it.copy(
                        isWifiConnected = settings.qsWifi,
                        isBluetoothEnabled = settings.qsBluetooth,
                        isDndEnabled = settings.qsDnd,
                        isAutoRotateEnabled = settings.qsAutoRotate,
                        isBatterySaverEnabled = settings.qsBatterySaver,
                        brightnessPercent = settings.qsBrightness
                    )
                }
            }
        }
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
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
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
                _state.update { it.copy(timeFormatted = timeStr, dateFormatted = dateStr) }
                delay(1000)
            }
        }
    }

    fun updateRealNetworkState() {
        try {
            val net = connectivityManager?.activeNetwork
            val caps = connectivityManager?.getNetworkCapabilities(net)
            val isWifi = caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true ||
                    caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
            _state.update { it.copy(isWifiConnected = isWifi) }
        } catch (_: Exception) {}
    }

    fun updateRealAudioState() {
        try {
            audioManager?.let { am ->
                val current = am.getStreamVolume(AudioManager.STREAM_MUSIC)
                val max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
                val pct = (current.toFloat() / max.toFloat()).coerceIn(0f, 1f)
                _state.update { it.copy(volumePercent = pct) }
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

    fun toggleWifi() {
        val next = !_state.value.isWifiConnected
        _state.update { it.copy(isWifiConnected = next) }
        if (settingsDataStore != null) {
            scope.launch { settingsDataStore.setQsWifi(next) }
        }
    }

    fun toggleBluetooth() {
        val next = !_state.value.isBluetoothEnabled
        _state.update { it.copy(isBluetoothEnabled = next) }
        if (settingsDataStore != null) {
            scope.launch { settingsDataStore.setQsBluetooth(next) }
        }
    }

    fun toggleDnd() {
        val next = !_state.value.isDndEnabled
        _state.update { it.copy(isDndEnabled = next) }
        if (settingsDataStore != null) {
            scope.launch { settingsDataStore.setQsDnd(next) }
        }
    }

    fun toggleAutoRotate() {
        val next = !_state.value.isAutoRotateEnabled
        _state.update { it.copy(isAutoRotateEnabled = next) }
        if (settingsDataStore != null) {
            scope.launch { settingsDataStore.setQsAutoRotate(next) }
        }
    }

    fun toggleBatterySaver() {
        val next = !_state.value.isBatterySaverEnabled
        _state.update { it.copy(isBatterySaverEnabled = next) }
        if (settingsDataStore != null) {
            scope.launch { settingsDataStore.setQsBatterySaver(next) }
        }
    }

    fun destroy() {
        clockJob?.cancel()
        try {
            context.unregisterReceiver(batteryReceiver)
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
