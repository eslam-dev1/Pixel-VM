package com.example.ui.phone

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.BatterySaver
import androidx.compose.material.icons.filled.BatteryStd
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.DoNotDisturb
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.SimulatedSystemState

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.statusBars

@Composable
fun StatusBar(
    systemState: SimulatedSystemState,
    textColor: Color = Color.White,
    onOpenShade: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val statusBarPadding = WindowInsets.statusBars.asPaddingValues()
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(statusBarPadding.calculateTopPadding() + 4.dp)
            .clickable(onClick = onOpenShade)
            .displayCutoutPadding()
            .padding(start = 16.dp, end = 16.dp, top = statusBarPadding.calculateTopPadding() / 2f),
        contentAlignment = Alignment.Center
    ) {
        // Left: Time
        Row(
            modifier = Modifier.align(Alignment.CenterStart),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = systemState.timeFormatted,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = textColor
            )
        }

        // Right: System status icons
        Row(
            modifier = Modifier.align(Alignment.CenterEnd),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (systemState.isDndEnabled) {
                Icon(
                    imageVector = Icons.Default.DoNotDisturb,
                    contentDescription = "Do Not Disturb",
                    tint = textColor,
                    modifier = Modifier.size(14.dp)
                )
            }

            if (systemState.isFlashlightOn) {
                Icon(
                    imageVector = Icons.Default.FlashlightOn,
                    contentDescription = "Flashlight on",
                    tint = textColor,
                    modifier = Modifier.size(14.dp)
                )
            }

            if (systemState.isBluetoothEnabled) {
                Icon(
                    imageVector = Icons.Default.Bluetooth,
                    contentDescription = "Bluetooth",
                    tint = textColor,
                    modifier = Modifier.size(14.dp)
                )
            }

            if (systemState.isBatterySaverEnabled) {
                Icon(
                    imageVector = Icons.Default.BatterySaver,
                    contentDescription = "Battery Saver",
                    tint = textColor,
                    modifier = Modifier.size(14.dp)
                )
            }

            Icon(
                imageVector = if (systemState.isWifiConnected) Icons.Default.Wifi else Icons.Default.WifiOff,
                contentDescription = if (systemState.isWifiConnected) "Wi-Fi connected" else "Wi-Fi disconnected",
                tint = textColor,
                modifier = Modifier.size(16.dp)
            )

            Spacer(modifier = Modifier.width(2.dp))

            Text(
                text = "${systemState.batteryPercent}%",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = textColor
            )

            val batteryIcon = when {
                systemState.isCharging -> Icons.Default.BatteryChargingFull
                systemState.batteryPercent > 80 -> Icons.Default.BatteryFull
                systemState.batteryPercent > 20 -> Icons.Default.BatteryStd
                else -> Icons.Default.BatteryAlert
            }

            Icon(
                imageVector = batteryIcon,
                contentDescription = if (systemState.isCharging) "Battery charging ${systemState.batteryPercent}%" else "Battery ${systemState.batteryPercent}%",
                tint = if (systemState.isBatterySaverEnabled) Color(0xFFFFB74D) else textColor,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
