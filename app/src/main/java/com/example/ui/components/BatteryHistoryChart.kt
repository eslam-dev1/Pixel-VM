package com.example.ui.components

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Calendar
import java.util.Locale

data class HourlyBatteryData(
    val hourOfDay: Int,       // 0-23
    val hourLabel: String,    // e.g. "2 AM", "12 PM"
    val batteryPercent: Int,  // 0-100
    val isCharging: Boolean   // true if charging during this hour
)

@Composable
fun BatteryHistoryChart(
    currentBatteryPercent: Int,
    isCurrentlyCharging: Boolean,
    modifier: Modifier = Modifier
) {
    // Generate 24 hourly data points ending at current hour
    val hourlyData = remember(currentBatteryPercent, isCurrentlyCharging) {
        generate24HourBatteryHistory(currentBatteryPercent, isCurrentlyCharging)
    }

    var selectedDataPoint by remember { mutableStateOf<HourlyBatteryData?>(hourlyData.lastOrNull()) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("battery_history_chart_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Battery Usage (24 Hours)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = selectedDataPoint?.let { "${it.hourLabel}: ${it.batteryPercent}% ${if (it.isCharging) "(Charging)" else ""}" }
                            ?: "Tap any bar to inspect",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .width(10.dp)
                            .height(10.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(MaterialTheme.colorScheme.primary)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Discharge", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                    Spacer(modifier = Modifier.width(8.dp))

                    Box(
                        modifier = Modifier
                            .width(10.dp)
                            .height(10.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(Color(0xFF81C784))
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Charging", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Canvas Bar Chart
            val primaryColor = MaterialTheme.colorScheme.primary
            val chargingColor = Color(0xFF81C784)
            val gridColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f)
            val selectedHighlightColor = MaterialTheme.colorScheme.tertiary

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("battery_canvas_chart")
                ) {
                    val width = size.width
                    val height = size.height
                    val barCount = hourlyData.size
                    val spacing = 4.dp.toPx()
                    val totalSpacing = spacing * (barCount - 1)
                    val barWidth = (width - totalSpacing) / barCount

                    // Draw baseline grid lines at 100%, 50%, 0%
                    val gridLines = listOf(0.0f, 0.5f, 1.0f)
                    gridLines.forEach { fraction ->
                        val y = height * (1.0f - fraction)
                        drawLine(
                            color = gridColor,
                            start = Offset(0f, y),
                            end = Offset(width, y),
                            strokeWidth = 1.dp.toPx()
                        )
                    }

                    // Draw 24 hourly bars
                    hourlyData.forEachIndexed { index, data ->
                        val barHeight = (data.batteryPercent / 100f) * height
                        val x = index * (barWidth + spacing)
                        val y = height - barHeight

                        val isSelected = selectedDataPoint == data
                        val barColor = when {
                            isSelected -> selectedHighlightColor
                            data.isCharging -> chargingColor
                            else -> primaryColor
                        }

                        drawRoundRect(
                            color = barColor,
                            topLeft = Offset(x, y),
                            size = Size(barWidth, barHeight),
                            cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                        )
                    }
                }

                // Invisible tap overlay for interactive bar selection
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    hourlyData.forEach { data ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxSize()
                                .clickable { selectedDataPoint = data }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // X-Axis Time Labels
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                listOf("24h ago", "18h ago", "12h ago", "6h ago", "Now").forEach { label ->
                    Text(
                        text = label,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}

private fun generate24HourBatteryHistory(
    currentPercent: Int,
    isCurrentlyCharging: Boolean
): List<HourlyBatteryData> {
    val list = mutableListOf<HourlyBatteryData>()
    val cal = Calendar.getInstance()
    val currentHour = cal.get(Calendar.HOUR_OF_DAY)

    // Simulate realistic 24-hour drain and charge curve ending at currentPercent
    var tempLevel = currentPercent.toFloat()
    val levels = FloatArray(24)
    val charging = BooleanArray(24)

    levels[23] = currentPercent.toFloat()
    charging[23] = isCurrentlyCharging

    for (i in 22 downTo 0) {
        val hourVal = (currentHour - (23 - i) + 24) % 24
        val isChargePeriod = (hourVal in 1..3) || (isCurrentlyCharging && i >= 20)
        charging[i] = isChargePeriod

        if (isChargePeriod) {
            tempLevel = (tempLevel - (12..25).random()).coerceIn(15f, 100f)
        } else {
            tempLevel = (tempLevel + (1..3).random()).coerceIn(15f, 100f)
        }
        levels[i] = tempLevel
    }

    for (i in 0 until 24) {
        val hourVal = (currentHour - (23 - i) + 24) % 24
        val amPmStr = when {
            hourVal == 0 -> "12 AM"
            hourVal == 12 -> "12 PM"
            hourVal > 12 -> "${hourVal - 12} PM"
            else -> "$hourVal AM"
        }

        list.add(
            HourlyBatteryData(
                hourOfDay = hourVal,
                hourLabel = amPmStr,
                batteryPercent = levels[i].toInt().coerceIn(5, 100),
                isCharging = charging[i]
            )
        )
    }

    return list
}
