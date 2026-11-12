package com.example.ui.phone

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatterySaver
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BrightnessLow
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DoNotDisturb
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.Screenshot
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.SimulatedNotification
import com.example.model.SimulatedSystemState
import com.example.os.SystemStateHolder

private data class QuickSettingTile(
    val id: String,
    val title: String,
    val isRealFeature: Boolean,
    val isActive: Boolean,
    val icon: ImageVector,
    val onToggle: () -> Unit
)

@Composable
fun NotificationShade(
    systemState: SimulatedSystemState,
    systemStateHolder: SystemStateHolder,
    notifications: List<SimulatedNotification>,
    onDismissNotification: (String) -> Unit,
    onClearAllNotifications: () -> Unit,
    onOpenSettings: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val view = androidx.compose.ui.platform.LocalView.current

    val tiles = listOf(
        QuickSettingTile(
            id = "wifi",
            title = stringResource(R.string.quick_settings_wifi),
            isRealFeature = true,
            isActive = systemState.isWifiConnected,
            icon = Icons.Default.Wifi,
            onToggle = { systemStateHolder.toggleWifi() }
        ),
        QuickSettingTile(
            id = "bluetooth",
            title = stringResource(R.string.quick_settings_bluetooth),
            isRealFeature = false,
            isActive = systemState.isBluetoothEnabled,
            icon = Icons.Default.Bluetooth,
            onToggle = { systemStateHolder.toggleBluetooth() }
        ),
        QuickSettingTile(
            id = "flashlight",
            title = stringResource(R.string.quick_settings_flashlight),
            isRealFeature = true,
            isActive = systemState.isFlashlightOn,
            icon = Icons.Default.FlashlightOn,
            onToggle = { systemStateHolder.toggleFlashlight() }
        ),
        QuickSettingTile(
            id = "dnd",
            title = stringResource(R.string.quick_settings_dnd),
            isRealFeature = false,
            isActive = systemState.isDndEnabled,
            icon = Icons.Default.DoNotDisturb,
            onToggle = { systemStateHolder.toggleDnd() }
        ),
        QuickSettingTile(
            id = "screenshot",
            title = "Screenshot",
            isRealFeature = true,
            isActive = false,
            icon = Icons.Default.Screenshot,
            onToggle = {
                com.example.util.ScreenshotHelper.takeScreenshotAndSave(context, view.rootView) {
                    onClose()
                }
            }
        ),
        QuickSettingTile(
            id = "battery",
            title = stringResource(R.string.quick_settings_battery),
            isRealFeature = false,
            isActive = systemState.isBatterySaverEnabled,
            icon = Icons.Default.BatterySaver,
            onToggle = { systemStateHolder.toggleBatterySaver() }
        )
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF141518).copy(alpha = 0.98f))
            .padding(top = 34.dp, bottom = 16.dp, start = 16.dp, end = 16.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header: Time, Date & Settings action
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = systemState.timeFormatted,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Light,
                        color = Color.White
                    )
                    Text(
                        text = systemState.dateFormatted,
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.75f)
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onOpenSettings, modifier = Modifier.testTag("shade_settings_btn")) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings", tint = Color.White)
                    }
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Quick Settings 2-column Grid
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(tiles, key = { it.id }) { tile ->
                    Box(
                        modifier = Modifier
                            .height(60.dp)
                            .clip(RoundedCornerShape(28.dp))
                            .background(
                                if (tile.isActive) MaterialTheme.colorScheme.primary else Color(0xFF2C2D32)
                            )
                            .clickable(onClick = tile.onToggle)
                            .padding(horizontal = 14.dp)
                            .testTag("qs_tile_${tile.id}"),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = tile.icon,
                                contentDescription = tile.title,
                                tint = if (tile.isActive) MaterialTheme.colorScheme.onPrimary else Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = tile.title,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (tile.isActive) MaterialTheme.colorScheme.onPrimary else Color.White
                                )
                                Text(
                                    text = if (tile.isRealFeature) "Device" else "Simulated",
                                    fontSize = 10.sp,
                                    color = if (tile.isActive) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.75f) else Color.White.copy(alpha = 0.5f)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Brightness slider
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.BrightnessLow,
                    contentDescription = "Brightness",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Slider(
                    value = systemState.brightnessPercent,
                    onValueChange = { systemStateHolder.setBrightness(it) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("qs_brightness_slider")
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Notifications header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Notifications",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White.copy(alpha = 0.85f)
                )

                if (notifications.isNotEmpty()) {
                    TextButton(onClick = onClearAllNotifications) {
                        Text(
                            text = stringResource(R.string.clear_all),
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Notifications List
            if (notifications.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No new notifications",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 14.sp
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(notifications, key = { it.id }) { notif ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF24252A))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.Notifications,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = notif.title,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color.White,
                                            fontSize = 14.sp
                                        )
                                        Text(
                                            text = notif.timeFormatted,
                                            fontSize = 11.sp,
                                            color = Color.White.copy(alpha = 0.5f)
                                        )
                                    }
                                    Text(
                                        text = notif.message,
                                        fontSize = 12.sp,
                                        color = Color.White.copy(alpha = 0.75f)
                                    )
                                }
                                IconButton(onClick = { onDismissNotification(notif.id) }) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Dismiss",
                                        tint = Color.White.copy(alpha = 0.5f),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Bottom handle to swipe up
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onClose)
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .width(48.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color.White.copy(alpha = 0.4f))
                )
            }
        }
    }
}
