package com.example.simulated.settings

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.view.KeyEvent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.material3.Button
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.BatteryStd
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.DisplaySettings
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.SettingsDataStore
import com.example.data.SystemSettings
import com.example.os.BatteryState
import com.example.os.SystemStateHolder
import com.example.os.WindowManager
import com.example.ui.theme.WallpaperTheme
import com.example.ui.theme.Wallpapers
import com.example.ui.settings.IconPackSettings
import kotlinx.coroutines.launch
import java.util.Locale

private enum class SettingsSection {
    HOME, DISPLAY, WALLPAPER, NOTIFICATIONS, SOUND, BATTERY, SECURITY, RELIABILITY, ICON_PACK, ABOUT
}

@Composable
fun SettingsApp(
    settingsDataStore: SettingsDataStore,
    systemStateHolder: SystemStateHolder,
    windowManager: WindowManager? = null,
    onResetPhone: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val settings by settingsDataStore.settingsFlow.collectAsState(initial = SystemSettings())
    val systemState by systemStateHolder.state.collectAsState()
    val batteryTelemetry by systemStateHolder.batteryTelemetry.collectAsState()
    var currentSection by remember { mutableStateOf(SettingsSection.HOME) }
    var showResetDialog by remember { mutableStateOf(false) }

    BackHandler(enabled = currentSection != SettingsSection.HOME) {
        currentSection = SettingsSection.HOME
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // App bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (currentSection != SettingsSection.HOME) {
                IconButton(onClick = { currentSection = SettingsSection.HOME }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
            }
            Text(
                text = when (currentSection) {
                    SettingsSection.HOME -> stringResource(R.string.app_settings)
                    SettingsSection.DISPLAY -> stringResource(R.string.settings_display)
                    SettingsSection.NOTIFICATIONS -> stringResource(R.string.settings_notifications)
                    SettingsSection.WALLPAPER -> stringResource(R.string.settings_wallpaper)
                    SettingsSection.SOUND -> stringResource(R.string.settings_sound)
                    SettingsSection.BATTERY -> stringResource(R.string.settings_battery)
                    SettingsSection.SECURITY -> stringResource(R.string.settings_security)
                    SettingsSection.RELIABILITY -> "MagicOS Reliability"
                    SettingsSection.ICON_PACK -> "Icon Packs"
                    SettingsSection.ABOUT -> stringResource(R.string.settings_about)
                },
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(start = if (currentSection == SettingsSection.HOME) 8.dp else 4.dp)
            )
        }

        when (currentSection) {
            SettingsSection.HOME -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        SettingsItem(
                            icon = Icons.Default.DisplaySettings,
                            title = stringResource(R.string.settings_display),
                            subtitle = "Dark theme, font size, phone frame bezel",
                            testTag = "settings_item_display",
                            onClick = { currentSection = SettingsSection.DISPLAY }
                        )
                    }
                    item {
                        SettingsItem(
                            icon = Icons.Default.Notifications,
                            title = stringResource(R.string.settings_notifications),
                            subtitle = "Lock screen & app notification preferences",
                            testTag = "settings_item_notifications",
                            onClick = { currentSection = SettingsSection.NOTIFICATIONS }
                        )
                    }
                    item {
                        SettingsItem(
                            icon = Icons.Default.ColorLens,
                            title = stringResource(R.string.settings_wallpaper),
                            subtitle = "Material You dynamic theme and original wallpapers",
                            testTag = "settings_item_wallpaper",
                            onClick = { currentSection = SettingsSection.WALLPAPER }
                        )
                    }
                    item {
                        SettingsItem(
                            icon = Icons.Default.ColorLens,
                            title = "Icon Packs",
                            subtitle = "Customize app icons with installed packs",
                            testTag = "settings_item_icon_pack",
                            onClick = { currentSection = SettingsSection.ICON_PACK }
                        )
                    }
                    item {
                        SettingsItem(
                            icon = Icons.Default.VolumeUp,
                            title = stringResource(R.string.settings_sound),
                            subtitle = "Media and system volume controls",
                            testTag = "settings_item_sound",
                            onClick = { currentSection = SettingsSection.SOUND }
                        )
                    }
                    item {
                        SettingsItem(
                            icon = Icons.Default.BatteryStd,
                            title = stringResource(R.string.settings_battery),
                            subtitle = "${systemState.batteryPercent}% • ${if (systemState.isCharging) "Charging" else "On battery"}",
                            testTag = "settings_item_battery",
                            onClick = { currentSection = SettingsSection.BATTERY }
                        )
                    }
                    item {
                        SettingsItem(
                            icon = Icons.Default.Lock,
                            title = stringResource(R.string.settings_security),
                            subtitle = if (settings.hashedPin != null) "PIN protection enabled" else "No lock screen PIN set",
                            testTag = "settings_item_security",
                            onClick = { currentSection = SettingsSection.SECURITY }
                        )
                    }
                    item {
                        SettingsItem(
                            icon = Icons.Default.VerifiedUser,
                            title = "MagicOS Reliability Guide",
                            subtitle = "Fix background alarms, notifications & battery restrictions",
                            testTag = "settings_item_reliability",
                            onClick = { currentSection = SettingsSection.RELIABILITY }
                        )
                    }
                    item {
                        SettingsItem(
                            icon = Icons.Default.Info,
                            title = stringResource(R.string.settings_about),
                            subtitle = "Real device specs & simulation notice",
                            testTag = "settings_item_about",
                            onClick = { currentSection = SettingsSection.ABOUT }
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        SettingsItem(
                            icon = Icons.Default.RestartAlt,
                            title = stringResource(R.string.settings_reset),
                            subtitle = "Restore simulated OS to factory defaults",
                            iconTint = MaterialTheme.colorScheme.error,
                            testTag = "settings_item_reset",
                            onClick = { showResetDialog = true }
                        )
                    }
                }
            }

            SettingsSection.DISPLAY -> {
                DisplaySettingsScreen(
                    settings = settings,
                    onDarkModeChange = { mode -> scope.launch { settingsDataStore.setDarkMode(mode) } },
                    onBezelToggle = { show -> scope.launch { settingsDataStore.setShowBezel(show) } },
                    onFontScaleChange = { scale -> scope.launch { settingsDataStore.setFontScale(scale) } },
                    onBodyColorChange = { color ->
                        scope.launch {
                            settingsDataStore.setBodyColor(color.id)
                            settingsDataStore.setWallpaperId(color.defaultWallpaperId)
                        }
                    },
                    onSmooth120HzChange = { enabled -> scope.launch { settingsDataStore.setSmoothScrolling120Hz(enabled) } },
                    onDeviceProfileChange = { style -> scope.launch { settingsDataStore.setDeviceProfileStyle(style) } },
                    onDisableEdgeBackChange = { disabled -> scope.launch { settingsDataStore.setDisableEdgeBack(disabled) } },
                    onSearchEngineChange = { engine -> scope.launch { settingsDataStore.setSearchEngine(engine) } }
                )
            }

            SettingsSection.NOTIFICATIONS -> {
                NotificationSettingsScreen(
                    settings = settings,
                    onLockScreenChange = { visible -> scope.launch { settingsDataStore.setNotifLockScreenVisible(visible) } },
                    onClockNotifChange = { enabled -> scope.launch { settingsDataStore.setNotifClockEnabled(enabled) } },
                    onNotesNotifChange = { enabled -> scope.launch { settingsDataStore.setNotifNotesEnabled(enabled) } },
                    onMessagesNotifChange = { enabled -> scope.launch { settingsDataStore.setNotifMessagesEnabled(enabled) } }
                )
            }

            SettingsSection.WALLPAPER -> {
                WallpaperSettingsScreen(
                    currentWallpaperId = settings.wallpaperId,
                    onSelectWallpaper = { id -> scope.launch { settingsDataStore.setWallpaperId(id) } }
                )
            }

            SettingsSection.SOUND -> {
                SoundSettingsScreen(
                    systemStateHolder = systemStateHolder,
                    currentVolume = systemState.volumePercent
                )
            }

            SettingsSection.BATTERY -> {
                BatterySettingsScreen(
                    systemState = systemState,
                    batteryTelemetry = batteryTelemetry
                )
            }

            SettingsSection.SECURITY -> {
                SecuritySettingsScreen(
                    currentHashedPin = settings.hashedPin,
                    onSetPin = { pin -> scope.launch { settingsDataStore.setPin(pin) } },
                    privateSpaceHasPin = settings.privateSpacePinHash != null,
                    onSetPrivateSpacePin = { pin -> scope.launch { settingsDataStore.setPrivateSpacePin(pin) } }
                )
            }

            SettingsSection.RELIABILITY -> {
                ReliabilitySettingsScreen()
            }

            SettingsSection.ICON_PACK -> {
                IconPackSettings(
                    settingsDataStore = settingsDataStore,
                    currentIconPack = settings.selectedIconPack,
                    pixelThemeSettings = settings.toPixelThemeSettings()
                )
            }

            SettingsSection.ABOUT -> {
                AboutPhoneScreen(bodyColor = com.example.model.BodyColor.fromId(settings.bodyColor))
            }
        }
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text(stringResource(R.string.reset_confirm_title)) },
            text = { Text(stringResource(R.string.reset_confirm_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showResetDialog = false
                        onResetPhone()
                    }
                ) {
                    Text(stringResource(R.string.reset_button), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text(stringResource(R.string.cancel_button))
                }
            }
        )
    }
}

@Composable
private fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    iconTint: Color = MaterialTheme.colorScheme.primary,
    testTag: String? = null,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(iconTint.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = iconTint)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = subtitle,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
private fun DisplaySettingsScreen(
    settings: SystemSettings,
    onDarkModeChange: (String) -> Unit,
    onBezelToggle: (Boolean) -> Unit,
    onFontScaleChange: (Float) -> Unit,
    onBodyColorChange: (com.example.model.BodyColor) -> Unit,
    onSmooth120HzChange: (Boolean) -> Unit,
    onDeviceProfileChange: (String) -> Unit,
    onDisableEdgeBackChange: (Boolean) -> Unit,
    onSearchEngineChange: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Body Color (Pixel 9 style)
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = "Device Body Color", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(
                    text = "Customizes the flat-edge aluminum frame, accent tint, and paired default wallpaper.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    com.example.model.BodyColor.entries.forEach { color ->
                        val isSelected = settings.bodyColor.equals(color.id, ignoreCase = true)
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clickable { onBodyColorChange(color) }
                                .padding(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(color.backColor)
                                    .border(
                                        width = if (isSelected) 3.dp else 1.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Black.copy(alpha = 0.2f),
                                        shape = CircleShape
                                    )
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = color.displayName,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // Theme Mode
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = "Theme Mode", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(8.dp))
                listOf("system" to "System default", "light" to "Light theme", "dark" to "Dark theme").forEach { (mode, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onDarkModeChange(mode) }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = settings.darkMode == mode,
                            onClick = { onDarkModeChange(mode) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        // Search Engine
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = "Default Search Engine", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(
                    text = "Search engine used for web queries in the App Drawer search bar",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.height(8.dp))
                listOf(
                    "google" to "Google",
                    "duckduckgo" to "DuckDuckGo",
                    "bing" to "Microsoft Bing"
                ).forEach { (engine, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSearchEngineChange(engine) }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = settings.searchEngine.equals(engine, ignoreCase = true),
                            onClick = { onSearchEngineChange(engine) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        // 120Hz Smooth Display
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Smooth Display (120Hz)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Text(
                        text = "Dynamically adjusts simulated animation pacing up to 120Hz",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
                Switch(
                    checked = settings.smoothScrolling120Hz,
                    onCheckedChange = onSmooth120HzChange
                )
            }
        }

        // Device Profile Style
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = "Device Profile & Frame Mode", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(
                    text = "Switch between Honor X9d native fullscreen layout and Pixel 9 decorative bezel style.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    listOf("honor_x9d" to "Honor X9d Native", "pixel_9" to "Pixel 9 Style").forEach { (styleId, label) ->
                        val isSelected = settings.deviceProfileStyle.equals(styleId, ignoreCase = true)
                        OutlinedButton(
                            onClick = { onDeviceProfileChange(styleId) },
                            colors = if (isSelected) {
                                ButtonDefaults.outlinedButtonColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                            } else {
                                ButtonDefaults.outlinedButtonColors()
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(label, fontSize = 13.sp)
                        }
                    }
                }
            }
        }

        // Bezel Mode
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.bezel_toggle),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Text(
                        text = stringResource(R.string.bezel_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
                Switch(
                    checked = settings.showBezel,
                    onCheckedChange = onBezelToggle,
                    modifier = Modifier.testTag("bezel_switch")
                )
            }
        }

        // Disable Edge Back Gesture
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Disable Simulated Edge-Back",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Text(
                        text = "Prevents gesture conflicts with MagicOS system edge-back navigation",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
                Switch(
                    checked = settings.disableEdgeBack,
                    onCheckedChange = onDisableEdgeBackChange
                )
            }
        }

        // Font scale
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = "Font Scale", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    listOf(0.85f to "Small", 1.0f to "Default", 1.15f to "Large").forEach { (scale, name) ->
                        OutlinedButton(
                            onClick = { onFontScaleChange(scale) },
                            colors = if (settings.fontScale == scale) {
                                ButtonDefaults.outlinedButtonColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                            } else {
                                ButtonDefaults.outlinedButtonColors()
                            }
                        ) {
                            Text(name)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WallpaperSettingsScreen(
    currentWallpaperId: String,
    onSelectWallpaper: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Choose Wallpaper & Material You Palette",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = "Dynamic colors are extracted from the chosen original wallpaper and applied across the simulated OS.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(vertical = 4.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(Wallpapers.all) { wp ->
                val isSelected = wp.id == currentWallpaperId
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectWallpaper(wp.id) }
                        .border(
                            width = if (isSelected) 3.dp else 0.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                            shape = RoundedCornerShape(16.dp)
                        ),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = wp.name,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            if (isSelected) {
                                Text(
                                    text = "Applied",
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Gradient preview bar
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Wallpapers.makeBackgroundBrush(wp))
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Palette swatches
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(
                                wp.darkColors.primary,
                                wp.darkColors.secondary,
                                wp.darkColors.tertiary,
                                wp.darkColors.surfaceVariant
                            ).forEach { color ->
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(color)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SoundSettingsScreen(
    systemStateHolder: SystemStateHolder,
    currentVolume: Float
) {
    var volume by remember(currentVolume) { mutableFloatStateOf(currentVolume) }
    val isMediaPlaying by systemStateHolder.isDeviceMediaPlaying.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = stringResource(R.string.quick_settings_volume),
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.VolumeUp, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(12.dp))
                    Slider(
                        value = volume,
                        onValueChange = {
                            volume = it
                            systemStateHolder.setVolume(it)
                        },
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("${(volume * 100).toInt()}%", fontSize = 14.sp)
                }
            }
        }

        if (isMediaPlaying) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = stringResource(R.string.media_playing_active),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { systemStateHolder.dispatchMediaKey(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE) }) {
                            Text(stringResource(R.string.pause))
                        }
                        OutlinedButton(onClick = { systemStateHolder.dispatchMediaKey(KeyEvent.KEYCODE_MEDIA_NEXT) }) {
                            Text(stringResource(R.string.media_next_track))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BatterySettingsScreen(
    systemState: com.example.model.SimulatedSystemState,
    batteryTelemetry: BatteryState
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "${systemState.batteryPercent}%",
                    fontSize = 54.sp,
                    fontWeight = FontWeight.Light,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = if (systemState.isCharging) "Charging (${batteryTelemetry.pluggedSource})" else "On ${batteryTelemetry.pluggedSource}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
            }
        }

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                BatteryTelemetryRow(
                    label = stringResource(R.string.battery_health_label),
                    value = batteryTelemetry.healthLabel
                )
                BatteryTelemetryRow(
                    label = stringResource(R.string.battery_source_label),
                    value = batteryTelemetry.pluggedSource
                )
                BatteryTelemetryRow(
                    label = stringResource(R.string.battery_temp_label),
                    value = if (batteryTelemetry.temperatureCelsius > 0f) {
                        String.format(Locale.getDefault(), "%.1f °C", batteryTelemetry.temperatureCelsius)
                    } else "--"
                )
                BatteryTelemetryRow(
                    label = stringResource(R.string.battery_voltage_label),
                    value = if (batteryTelemetry.voltageMv > 0) "${batteryTelemetry.voltageMv} mV" else "--"
                )
                if (batteryTelemetry.technology.isNotBlank()) {
                    BatteryTelemetryRow(
                        label = stringResource(R.string.battery_tech_label),
                        value = batteryTelemetry.technology
                    )
                }
            }
        }

        OutlinedButton(
            onClick = {
                val usageIntent = Intent(Intent.ACTION_POWER_USAGE_SUMMARY)
                if (!safeStartActivity(context, usageIntent)) {
                    safeStartActivity(context, Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS))
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.open_system_battery_usage))
        }
    }
}

@Composable
private fun BatteryTelemetryRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SecuritySettingsScreen(
    currentHashedPin: String?,
    onSetPin: (String?) -> Unit,
    privateSpaceHasPin: Boolean = false,
    onSetPrivateSpacePin: (String?) -> Unit = {}
) {
    var newPin by remember { mutableStateOf("") }
    var statusMessage by remember { mutableStateOf("") }
    var privatePin by remember { mutableStateOf("") }
    var privatePinMsg by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Lock Screen PIN",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Text(
                    text = if (currentHashedPin != null) "PIN protection is currently ACTIVE." else "No PIN set. Lock screen unlocks with a swipe.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                    modifier = Modifier.padding(vertical = 4.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = newPin,
                    onValueChange = { if (it.length <= 6 && it.all { c -> c.isDigit() }) newPin = it },
                    label = { Text("Enter 4-6 digit PIN") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(
                        onClick = {
                            if (newPin.length >= 4) {
                                onSetPin(newPin)
                                statusMessage = "New PIN set successfully!"
                                newPin = ""
                            } else {
                                statusMessage = "PIN must be at least 4 digits"
                            }
                        }
                    ) {
                        Text("Set PIN")
                    }

                    if (currentHashedPin != null) {
                        OutlinedButton(
                            onClick = {
                                onSetPin(null)
                                statusMessage = "PIN removed."
                                newPin = ""
                            }
                        ) {
                            Text("Remove PIN")
                        }
                    }
                }

                if (statusMessage.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = statusMessage,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 13.sp
                    )
                }
            }
        }

        // Private Space PIN configuration
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = stringResource(R.string.private_space_pin_title),
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Text(
                    text = if (privateSpaceHasPin) stringResource(R.string.private_space_pin_active) else stringResource(R.string.private_space_pin_default),
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                    modifier = Modifier.padding(vertical = 4.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = privatePin,
                    onValueChange = { if (it.length <= 6 && it.all { c -> c.isDigit() }) privatePin = it },
                    label = { Text(stringResource(R.string.private_space_pin_hint)) },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(
                        onClick = {
                            if (privatePin.length >= 4) {
                                onSetPrivateSpacePin(privatePin)
                                privatePinMsg = "Private Space PIN updated!"
                                privatePin = ""
                            } else {
                                privatePinMsg = "PIN must be at least 4 digits"
                            }
                        }
                    ) {
                        Text("Set PIN")
                    }

                    if (privateSpaceHasPin) {
                        OutlinedButton(
                            onClick = {
                                onSetPrivateSpacePin(null)
                                privatePinMsg = "Private Space PIN cleared."
                                privatePin = ""
                            }
                        ) {
                            Text("Remove PIN")
                        }
                    }
                }

                if (privatePinMsg.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = privatePinMsg,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun AboutPhoneScreen(bodyColor: com.example.model.BodyColor) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Camera Bar Back View Preview
        com.example.ui.phone.CameraBarBackView(bodyColor = bodyColor)

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "System Details",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text("OS Flavor: PixelSim Android Simulator")
                Text("Model: ${Build.MANUFACTURER} ${Build.MODEL}")
                Text("Android Version: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
                Text("Hardware: ${Build.HARDWARE}")
                Text("Board: ${Build.BOARD}")
            }
        }

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = stringResource(R.string.simulation_notice),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "PixelSim is a self-contained native Android simulator demonstration built with Jetpack Compose. All trademarks, designs and properties belong to their respective owners.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun NotificationSettingsScreen(
    settings: SystemSettings,
    onLockScreenChange: (Boolean) -> Unit,
    onClockNotifChange: (Boolean) -> Unit,
    onNotesNotifChange: (Boolean) -> Unit,
    onMessagesNotifChange: (Boolean) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(text = "General", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Lock Screen Notifications", fontWeight = FontWeight.Medium)
                        Text("Display incoming alerts on the lock screen", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                    }
                    Switch(checked = settings.notifLockScreenVisible, onCheckedChange = onLockScreenChange)
                }
            }
        }

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(text = "Simulated App Notifications", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Clock & Alarms", fontWeight = FontWeight.Medium)
                        Text("Alarm triggers and timer completions", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                    }
                    Switch(checked = settings.notifClockEnabled, onCheckedChange = onClockNotifChange)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Notes", fontWeight = FontWeight.Medium)
                        Text("Alerts when notes are pinned or modified", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                    }
                    Switch(checked = settings.notifNotesEnabled, onCheckedChange = onNotesNotifChange)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Messages", fontWeight = FontWeight.Medium)
                        Text("SMS send confirmations and alerts", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                    }
                    Switch(checked = settings.notifMessagesEnabled, onCheckedChange = onMessagesNotifChange)
                }
            }
        }
    }
}

@Composable
private fun ReliabilitySettingsScreen() {
    val context = LocalContext.current
    
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "MagicOS / Background Reliability",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Follow these steps to ensure PixelSim alarms and notifications work reliably on MagicOS and other background-restricted systems.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        item {
            ReliabilityStep(
                step = "1. Battery Optimization",
                description = "Set PixelSim to 'Unrestricted' or 'Don't optimize' to prevent the system from killing the app in the background.",
                buttonText = "Open Battery Settings",
                onClick = {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", context.packageName, null)
                    }
                    safeStartActivity(context, intent)
                }
            )
        }

        item {
            ReliabilityStep(
                step = "2. Auto-launch & App Launch",
                description = "On MagicOS, go to Settings -> Apps -> App launch, find PixelSim, and set to 'Manage manually'. Ensure 'Auto-launch', 'Secondary launch', and 'Run in background' are all enabled.",
                buttonText = "Open App Launch Settings",
                onClick = {
                    // Try Honor/Huawei specific intent, fallback to details
                    val intent = Intent().apply {
                        setClassName("com.huawei.systemmanager", "com.huawei.systemmanager.optimize.process.ProtectActivity")
                    }
                    if (!safeStartActivity(context, intent)) {
                        val fallback = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", context.packageName, null)
                        }
                        safeStartActivity(context, fallback)
                    }
                }
            )
        }

        item {
            ReliabilityStep(
                step = "3. Lock in Recents",
                description = "Open the system Recents screen (swipe up and hold), find PixelSim, and swipe down on the app card to lock it (a padlock icon should appear). This prevents the 'Clear All' action from killing the app.",
                buttonText = null,
                onClick = null
            )
        }

        item {
            ReliabilityStep(
                step = "4. Notification Permissions",
                description = "Ensure all notification categories are enabled so alerts can reach you even when the phone is locked.",
                buttonText = "Open Notification Settings",
                onClick = {
                    val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                        putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                    }
                    safeStartActivity(context, intent)
                }
            )
        }

        item {
            ReliabilityStep(
                step = "5. Exact Alarms",
                description = "If you are on Android 12 or higher, PixelSim needs permission to schedule exact alarms for the Clock app to be precise.",
                buttonText = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) "Open Alarm Permissions" else null,
                onClick = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    {
                        val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                            data = Uri.fromParts("package", context.packageName, null)
                        }
                        safeStartActivity(context, intent)
                    }
                } else null
            )
        }
    }
}

@Composable
private fun ReliabilityStep(
    step: String,
    description: String,
    buttonText: String? = null,
    onClick: (() -> Unit)? = null
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = step, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
            )
            if (buttonText != null && onClick != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onClick,
                    modifier = Modifier.align(Alignment.End),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(buttonText)
                }
            }
        }
    }
}

private fun safeStartActivity(context: android.content.Context, intent: Intent): Boolean {
    return try {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (intent.resolveActivity(context.packageManager) != null) {
            context.startActivity(intent)
            true
        } else {
            false
        }
    } catch (e: Exception) {
        false
    }
}



