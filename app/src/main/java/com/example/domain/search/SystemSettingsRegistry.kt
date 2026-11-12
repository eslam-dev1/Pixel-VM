package com.example.domain.search

import android.provider.Settings
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Accessibility
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.BatteryStd
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Brightness6
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Represents a searchable system setting entry with an Android deep-link action.
 */
data class SystemSettingItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val action: String,
    val icon: ImageVector
)

/**
 * Registry of core system setting deep links available in Universal Search.
 */
object SystemSettingsRegistry {
    val SETTINGS: List<SystemSettingItem> = listOf(
        SystemSettingItem(
            id = "wifi",
            title = "Wi-Fi & Network",
            subtitle = "Internet, hotspot, network preferences",
            action = Settings.ACTION_WIFI_SETTINGS,
            icon = Icons.Default.Wifi
        ),
        SystemSettingItem(
            id = "bluetooth",
            title = "Bluetooth & Devices",
            subtitle = "Connected devices, pairing, Cast",
            action = Settings.ACTION_BLUETOOTH_SETTINGS,
            icon = Icons.Default.Bluetooth
        ),
        SystemSettingItem(
            id = "display",
            title = "Display & Brightness",
            subtitle = "Dark theme, screen timeout, font size",
            action = Settings.ACTION_DISPLAY_SETTINGS,
            icon = Icons.Default.Brightness6
        ),
        SystemSettingItem(
            id = "sound",
            title = "Sound & Vibration",
            subtitle = "Volume, ringtone, Do Not Disturb",
            action = Settings.ACTION_SOUND_SETTINGS,
            icon = Icons.Default.VolumeUp
        ),
        SystemSettingItem(
            id = "battery",
            title = "Battery & Power",
            subtitle = "Battery saver, usage breakdown",
            action = Settings.ACTION_BATTERY_SAVER_SETTINGS,
            icon = Icons.Default.BatteryStd
        ),
        SystemSettingItem(
            id = "apps",
            title = "Apps & Permissions",
            subtitle = "Installed applications, default apps",
            action = Settings.ACTION_APPLICATION_SETTINGS,
            icon = Icons.Default.Apps
        ),
        SystemSettingItem(
            id = "notifications",
            title = "Notifications",
            subtitle = "App alerts, lock screen, notification history",
            action = Settings.ACTION_ALL_APPS_NOTIFICATION_SETTINGS,
            icon = Icons.Default.Notifications
        ),
        SystemSettingItem(
            id = "security",
            title = "Security & Privacy",
            subtitle = "Screen lock, fingerprint, biometrics",
            action = Settings.ACTION_SECURITY_SETTINGS,
            icon = Icons.Default.Lock
        ),
        SystemSettingItem(
            id = "location",
            title = "Location",
            subtitle = "Location access, accuracy, app permissions",
            action = Settings.ACTION_LOCATION_SOURCE_SETTINGS,
            icon = Icons.Default.LocationOn
        ),
        SystemSettingItem(
            id = "date",
            title = "Date & Time",
            subtitle = "Network time, 24-hour clock, time zone",
            action = Settings.ACTION_DATE_SETTINGS,
            icon = Icons.Default.AccessTime
        ),
        SystemSettingItem(
            id = "accessibility",
            title = "Accessibility",
            subtitle = "Display size, TalkBack, captions",
            action = Settings.ACTION_ACCESSIBILITY_SETTINGS,
            icon = Icons.Default.Accessibility
        ),
        SystemSettingItem(
            id = "about",
            title = "About Phone",
            subtitle = "Model, Android version, hardware details",
            action = Settings.ACTION_DEVICE_INFO_SETTINGS,
            icon = Icons.Default.Info
        )
    )
}
