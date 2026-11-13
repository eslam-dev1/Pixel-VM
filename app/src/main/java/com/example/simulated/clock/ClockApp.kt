@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.example.simulated.clock

import android.Manifest
import android.os.Build
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.AlarmDao
import com.example.data.AlarmEntity
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.Instant
import java.util.Locale

import com.example.os.WindowManager

@Composable
fun ClockApp(
    alarmDao: AlarmDao,
    windowManager: WindowManager? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Alarms", "World Clock", "Stopwatch", "Timer")

    var hasNotificationPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasNotificationPermission = granted
    }

    LaunchedEffect(selectedTab) {
        if (selectedTab == 0 && !hasNotificationPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        PrimaryTabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title, fontWeight = FontWeight.SemiBold) },
                    icon = {
                        val icon = when (index) {
                            0 -> Icons.Default.Alarm
                            1 -> Icons.Default.Public
                            2 -> Icons.Default.Timer
                            else -> Icons.Default.HourglassBottom
                        }
                        Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
                    }
                )
            }
        }

        when (selectedTab) {
            0 -> AlarmsTab(alarmDao = alarmDao)
            1 -> WorldClockTab()
            2 -> StopwatchTab(windowManager = windowManager)
            3 -> TimerTab(windowManager = windowManager)
        }
    }
}

@Composable
private fun AlarmsTab(alarmDao: AlarmDao) {
    val alarms by alarmDao.getAllAlarms().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var showAddDialog by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        if (alarms.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "No alarms set",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(alarms, key = { it.id }) { alarm ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                val timeText = String.format(Locale.US, "%02d:%02d", alarm.hour, alarm.minute)
                                Text(
                                    text = timeText,
                                    fontSize = 36.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (alarm.label.isNotBlank()) {
                                    Text(
                                        text = alarm.label,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Switch(
                                    checked = alarm.isEnabled,
                                    onCheckedChange = { checked ->
                                        scope.launch {
                                            val updated = alarm.copy(isEnabled = checked)
                                            alarmDao.updateAlarm(updated)
                                            if (checked) {
                                                AlarmScheduler.scheduleAlarm(context, updated)
                                            } else {
                                                AlarmScheduler.cancelAlarm(context, alarm.id)
                                            }
                                        }
                                    }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                IconButton(onClick = {
                                    scope.launch {
                                        AlarmScheduler.cancelAlarm(context, alarm.id)
                                        alarmDao.deleteAlarm(alarm)
                                    }
                                }) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete alarm",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = { showAddDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
                .testTag("add_alarm_fab"),
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add Alarm")
        }
    }

    if (showAddDialog) {
        var hourText by remember { mutableStateOf("07") }
        var minuteText by remember { mutableStateOf("00") }
        var labelText by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Add Alarm") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = hourText,
                            onValueChange = { if (it.length <= 2 && it.all { c -> c.isDigit() }) hourText = it },
                            label = { Text("Hour (0-23)") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = minuteText,
                            onValueChange = { if (it.length <= 2 && it.all { c -> c.isDigit() }) minuteText = it },
                            label = { Text("Minute (0-59)") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    OutlinedTextField(
                        value = labelText,
                        onValueChange = { labelText = it },
                        label = { Text("Alarm Label (Optional)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val h = hourText.toIntOrNull()?.coerceIn(0, 23) ?: 7
                    val m = minuteText.toIntOrNull()?.coerceIn(0, 59) ?: 0
                    scope.launch {
                        val newAlarm = AlarmEntity(
                            hour = h,
                            minute = m,
                            label = labelText,
                            isEnabled = true
                        )
                        val id = alarmDao.insertAlarm(newAlarm)
                        AlarmScheduler.scheduleAlarm(context, newAlarm.copy(id = id))
                    }
                    showAddDialog = false
                }) {
                    Text(stringResource(R.string.save_button))
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text(stringResource(R.string.cancel_button))
                }
            }
        )
    }
}

@Composable
private fun WorldClockTab() {
    var currentTime by remember { mutableStateOf(Instant.now()) }

    LaunchedEffect(Unit) {
        while (isActive) {
            currentTime = Instant.now()
            delay(1000)
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(WorldClockManager.POPULAR_CITIES) { city ->
            val info = WorldClockManager.getCityTime(city, currentTime)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = info.cityName,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${info.country} • ${info.offsetHoursFormatted}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (info.isDaytime) Icons.Default.Brightness7 else Icons.Default.Brightness4,
                            contentDescription = null,
                            tint = if (info.isDaytime) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier
                                .size(24.dp)
                                .padding(end = 6.dp)
                        )
                        Text(
                            text = "${info.timeFormatted} ${info.amPm}",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StopwatchTab(windowManager: WindowManager? = null) {
    var isRunning by remember { mutableStateOf(false) }
    var elapsedMillis by remember { mutableLongStateOf(0L) }
    val laps = remember { mutableStateListOf<Long>() }

    LaunchedEffect(isRunning) {
        if (isRunning) {
            val startTime = System.currentTimeMillis() - elapsedMillis
            while (isRunning) {
                elapsedMillis = System.currentTimeMillis() - startTime
                windowManager?.updateStopwatchActivity(elapsedMillis, isRunning = true)
                delay(50)
            }
        } else {
            windowManager?.updateStopwatchActivity(elapsedMillis, isRunning = false)
        }
    }

    val minutes = (elapsedMillis / 60000)
    val seconds = (elapsedMillis % 60000) / 1000
    val millis = (elapsedMillis % 1000) / 10

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = String.format(Locale.US, "%02d:%02d.%02d", minutes, seconds, millis),
                fontSize = 54.sp,
                fontWeight = FontWeight.Light,
                color = MaterialTheme.colorScheme.primary
            )
        }

        if (laps.isNotEmpty()) {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(laps.size) { index ->
                    val lapTime = laps[laps.size - 1 - index]
                    val lMin = lapTime / 60000
                    val lSec = (lapTime % 60000) / 1000
                    val lMil = (lapTime % 1000) / 10
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Lap ${laps.size - index}",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = String.format(Locale.US, "%02d:%02d.%02d", lMin, lSec, lMil),
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            Spacer(modifier = Modifier.weight(1f))
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            // Lap / Reset button
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable {
                        if (isRunning) {
                            laps.add(elapsedMillis)
                        } else {
                            elapsedMillis = 0L
                            laps.clear()
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isRunning) "Lap" else "Reset",
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Start / Stop button
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(if (isRunning) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer)
                    .clickable { isRunning = !isRunning },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isRunning) "Stop" else "Start",
                    fontWeight = FontWeight.SemiBold,
                    color = if (isRunning) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

@Composable
private fun TimerTab(windowManager: WindowManager? = null) {
    var initialSeconds by remember { mutableIntStateOf(60) }
    var remainingSeconds by remember { mutableIntStateOf(60) }
    var isRunning by remember { mutableStateOf(false) }

    LaunchedEffect(isRunning, remainingSeconds) {
        if (isRunning) {
            windowManager?.updateTimerActivity(remainingSeconds, isRunning = true)
        }
    }

    LaunchedEffect(isRunning) {
        if (isRunning) {
            while (isRunning && remainingSeconds > 0) {
                delay(1000)
                remainingSeconds--
                windowManager?.updateTimerActivity(remainingSeconds, isRunning = remainingSeconds > 0)
            }
            if (remainingSeconds <= 0) {
                isRunning = false
                windowManager?.clearTimerActivity()
            }
        }
    }

    val minutes = remainingSeconds / 60
    val seconds = remainingSeconds % 60
    val progress = if (initialSeconds > 0) remainingSeconds.toFloat() / initialSeconds.toFloat() else 0f

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceAround
    ) {
        Box(
            modifier = Modifier.size(240.dp),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxSize(),
                strokeWidth = 8.dp,
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
            Text(
                text = String.format(Locale.US, "%02d:%02d", minutes, seconds),
                fontSize = 44.sp,
                fontWeight = FontWeight.Light,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        // Quick presets
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(60 to "1m", 300 to "5m", 600 to "10m").forEach { (sec, label) ->
                TextButton(onClick = {
                    isRunning = false
                    initialSeconds = sec
                    remainingSeconds = sec
                }) {
                    Text(label)
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable {
                        isRunning = false
                        remainingSeconds = initialSeconds
                        windowManager?.clearTimerActivity()
                    },
                contentAlignment = Alignment.Center
            ) {
                Text("Reset", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(CircleShape)
                    .background(if (isRunning) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer)
                    .clickable { isRunning = !isRunning },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isRunning) "Pause" else "Start",
                    fontWeight = FontWeight.SemiBold,
                    color = if (isRunning) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}
