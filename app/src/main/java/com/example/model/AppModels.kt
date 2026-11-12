package com.example.model

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector

data class AppDefinition(
    val id: String,
    @StringRes val titleRes: Int,
    val iconKey: String,
    val iconVector: ImageVector? = null,
    val requiredPermission: String? = null,
    @StringRes val permissionRationaleRes: Int? = null,
    val isSystemApp: Boolean = true
)

data class SimulatedNotification(
    val id: String,
    val title: String,
    val message: String,
    val timeFormatted: String,
    val targetAppId: String
)

data class SimulatedSystemState(
    val timeFormatted: String = "12:00",
    val dateFormatted: String = "Wed, Nov 12",
    val batteryPercent: Int = 85,
    val isCharging: Boolean = false,
    val isWifiConnected: Boolean = true,
    val isBluetoothEnabled: Boolean = false,
    val isFlashlightOn: Boolean = false,
    val isDndEnabled: Boolean = false,
    val isAutoRotateEnabled: Boolean = true,
    val isBatterySaverEnabled: Boolean = false,
    val volumePercent: Float = 0.7f,
    val brightnessPercent: Float = 0.8f
)
