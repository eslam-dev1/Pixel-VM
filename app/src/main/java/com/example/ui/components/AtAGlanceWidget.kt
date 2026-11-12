package com.example.ui.components

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.provider.CalendarContract
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.SimulatedSystemState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class CalendarEventInfo(val title: String, val timeFormatted: String)
data class SmartspacerItem(val title: String, val subtitle: String, val icon: String)

@Composable
fun AtAGlanceWidget(
    systemState: SimulatedSystemState,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var weatherText by remember { mutableStateOf("22°C • Sunny") }
    var upcomingEvent by remember { mutableStateOf<CalendarEventInfo?>(null) }
    var smartspacerItem by remember { mutableStateOf<SmartspacerItem?>(null) }

    // Fetch Weather from Open-Meteo (no API key required)
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            try {
                val url = URL("https://api.open-meteo.com/v1/forecast?latitude=37.7749&longitude=-122.4194&current=temperature_2m,weather_code")
                val connection = url.openConnection() as HttpURLConnection
                connection.connectTimeout = 3000
                connection.readTimeout = 3000
                if (connection.responseCode == 200) {
                    val jsonStr = connection.inputStream.bufferedReader().use { it.readText() }
                    val json = JSONObject(jsonStr)
                    val current = json.getJSONObject("current")
                    val temp = current.getDouble("temperature_2m")
                    val code = current.getInt("weather_code")
                    val condition = when (code) {
                        0 -> "Clear Sky"
                        in 1..3 -> "Partly Cloudy"
                        in 51..67 -> "Rain Showers"
                        in 71..77 -> "Snow"
                        else -> "Pleasant"
                    }
                    weatherText = "${temp.toInt()}°C • $condition"
                }
            } catch (_: Exception) {
                // Keep default fallback weather
            }
        }
    }

    // Load upcoming calendar event
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            try {
                val projection = arrayOf(
                    CalendarContract.Events.TITLE,
                    CalendarContract.Events.DTSTART
                )
                val cursor = context.contentResolver.query(
                    CalendarContract.Events.CONTENT_URI,
                    projection,
                    "${CalendarContract.Events.DTSTART} >= ?",
                    arrayOf(System.currentTimeMillis().toString()),
                    "${CalendarContract.Events.DTSTART} ASC"
                )
                cursor?.use {
                    if (it.moveToFirst()) {
                        val titleIdx = it.getColumnIndex(CalendarContract.Events.TITLE)
                        val dtIdx = it.getColumnIndex(CalendarContract.Events.DTSTART)
                        val title = if (titleIdx >= 0) it.getString(titleIdx) else "Event"
                        val dt = if (dtIdx >= 0) it.getLong(dtIdx) else System.currentTimeMillis()
                        val timeStr = android.text.format.DateFormat.format("hh:mm a", dt).toString()
                        upcomingEvent = CalendarEventInfo(title = title, timeFormatted = timeStr)
                    }
                }
            } catch (_: Exception) {}
        }
    }

    // Register BroadcastReceiver for external Smartspacer / notification provider updates
    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                if (intent?.action == "com.example.SMARTSPACER_UPDATE") {
                    val title = intent.getStringExtra("title") ?: "Alert"
                    val subtitle = intent.getStringExtra("subtitle") ?: ""
                    smartspacerItem = SmartspacerItem(title, subtitle, "notification")
                }
            }
        }
        val filter = IntentFilter("com.example.SMARTSPACER_UPDATE")
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                context.registerReceiver(receiver, filter)
            }
        } catch (_: Exception) {}

        onDispose {
            try {
                context.unregisterReceiver(receiver)
            } catch (_: Exception) {}
        }
    }

    // Build list of active provider pages
    val providers = remember(weatherText, upcomingEvent, smartspacerItem) {
        val list = mutableListOf<@Composable () -> Unit>()

        // Page 1: Clock & Weather Provider
        list.add {
            ProviderCard(
                icon = { Icon(Icons.Default.WbSunny, contentDescription = null, tint = Color.Yellow, modifier = Modifier.size(16.dp)) },
                primaryText = systemState.timeFormatted,
                secondaryText = "${systemState.dateFormatted} • $weatherText"
            )
        }

        // Page 2: Calendar Provider
        if (upcomingEvent != null) {
            list.add {
                ProviderCard(
                    icon = { Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp)) },
                    primaryText = upcomingEvent!!.title,
                    secondaryText = "Upcoming at ${upcomingEvent!!.timeFormatted}"
                )
            }
        }

        // Page 3: External Smartspacer Broadcast Provider (if active)
        if (smartspacerItem != null) {
            list.add {
                ProviderCard(
                    icon = { Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = Color.Cyan, modifier = Modifier.size(16.dp)) },
                    primaryText = smartspacerItem!!.title,
                    secondaryText = smartspacerItem!!.subtitle
                )
            }
        }

        list
    }

    val pagerState = rememberPagerState(pageCount = { providers.size })

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 38.dp, start = 8.dp, end = 8.dp)
            .testTag("at_a_glance_widget")
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth()
        ) { page ->
            providers[page]()
        }

        if (providers.size > 1) {
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                for (i in providers.indices) {
                    val isSelected = pagerState.currentPage == i
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 2.dp)
                            .size(if (isSelected) 6.dp else 4.dp)
                            .clip(CircleShape)
                            .background(if (isSelected) Color.White else Color.White.copy(alpha = 0.4f))
                    )
                }
            }
        }
    }
}

@Composable
private fun ProviderCard(
    icon: @Composable () -> Unit,
    primaryText: String,
    secondaryText: String
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Column {
                Text(
                    text = primaryText,
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.testTag("at_a_glance_time")
                )
                Text(
                    text = secondaryText,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White.copy(alpha = 0.85f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.testTag("at_a_glance_subtitle")
                )
            }
        }
    }
}
