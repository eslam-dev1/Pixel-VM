@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.example.simulated.calendar

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CalendarContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.R
import com.example.data.CalendarEventDao
import com.example.data.LocalCalendarEventEntity
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class DisplayEvent(
    val id: String,
    val title: String,
    val description: String,
    val startEpochMillis: Long,
    val isDeviceEvent: Boolean
)

@Composable
fun CalendarApp(
    calendarEventDao: CalendarEventDao,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selectedTab by remember { mutableIntStateOf(0) }
    var currentYearMonth by remember { mutableStateOf(YearMonth.now()) }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }

    var hasCalendarPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCalendarPermission = granted
    }

    val localEvents by calendarEventDao.getAllEvents().collectAsState(initial = emptyList())
    var deviceEvents by remember { mutableStateOf<List<DisplayEvent>>(emptyList()) }
    var showAddEventDialog by remember { mutableStateOf(false) }

    LaunchedEffect(hasCalendarPermission) {
        if (hasCalendarPermission) {
            deviceEvents = loadDeviceCalendarEvents(context)
        }
    }

    val allEvents = remember(localEvents, deviceEvents) {
        val convertedLocal = localEvents.map {
            DisplayEvent(
                id = "local_${it.id}",
                title = it.title,
                description = it.description,
                startEpochMillis = it.startEpochMillis,
                isDeviceEvent = false
            )
        }
        (convertedLocal + deviceEvents).sortedBy { it.startEpochMillis }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            PrimaryTabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Month") },
                    icon = { Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(20.dp)) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Agenda") },
                    icon = { Icon(Icons.Default.ViewAgenda, contentDescription = null, modifier = Modifier.size(20.dp)) }
                )
            }

            if (selectedTab == 0) {
                // Month View
                Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                    // Month Navigation header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { currentYearMonth = currentYearMonth.minusMonths(1) }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Previous Month")
                        }
                        Text(
                            text = currentYearMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        IconButton(onClick = { currentYearMonth = currentYearMonth.plusMonths(1) }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next Month")
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Days of week header (Sun - Sat)
                    Row(modifier = Modifier.fillMaxWidth()) {
                        listOf("S", "M", "T", "W", "T", "F", "S").forEach { day ->
                            Text(
                                text = day,
                                modifier = Modifier.weight(1f),
                                textAlign = TextAlign.Center,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Calendar grid
                    val firstDayOfWeek = currentYearMonth.atDay(1).dayOfWeek.value % 7 // 0 = Sun
                    val daysInMonth = currentYearMonth.lengthOfMonth()
                    val totalSlots = ((firstDayOfWeek + daysInMonth + 6) / 7) * 7

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(7),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 280.dp)
                    ) {
                        items((0 until totalSlots).toList()) { slot ->
                            val dayNum = slot - firstDayOfWeek + 1
                            if (dayNum in 1..daysInMonth) {
                                val date = currentYearMonth.atDay(dayNum)
                                val isSelected = date == selectedDate
                                val isToday = date == LocalDate.now()

                                val hasEvent = allEvents.any {
                                    val eventDate = InstantToLocalDate(it.startEpochMillis)
                                    eventDate == date
                                }

                                Box(
                                    modifier = Modifier
                                        .aspectRatio(1f)
                                        .padding(2.dp)
                                        .clip(CircleShape)
                                        .background(
                                            when {
                                                isSelected -> MaterialTheme.colorScheme.primary
                                                isToday -> MaterialTheme.colorScheme.primaryContainer
                                                else -> Color.Transparent
                                            }
                                        )
                                        .clickable { selectedDate = date },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = dayNum.toString(),
                                            fontSize = 14.sp,
                                            fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = when {
                                                isSelected -> MaterialTheme.colorScheme.onPrimary
                                                isToday -> MaterialTheme.colorScheme.onPrimaryContainer
                                                else -> MaterialTheme.colorScheme.onBackground
                                            }
                                        )
                                        if (hasEvent) {
                                            Box(
                                                modifier = Modifier
                                                    .size(4.dp)
                                                    .clip(CircleShape)
                                                    .background(
                                                        if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary
                                                    )
                                            )
                                        }
                                    }
                                }
                            } else {
                                Box(modifier = Modifier.aspectRatio(1f))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Events on selected date
                    val selectedDateEvents = allEvents.filter {
                        InstantToLocalDate(it.startEpochMillis) == selectedDate
                    }

                    Text(
                        text = "Events on ${selectedDate.format(DateTimeFormatter.ofPattern("MMM d, yyyy"))}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    if (selectedDateEvents.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.TopStart
                        ) {
                            Text(
                                text = "No events scheduled for this day.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(bottom = 84.dp)
                        ) {
                            items(selectedDateEvents) { event ->
                                EventItemCard(event = event)
                            }
                        }
                    }
                }
            } else {
                // Agenda View
                Column(modifier = Modifier.padding(16.dp)) {
                    if (!hasCalendarPermission) {
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "Device Calendar Sync",
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = stringResource(R.string.calendar_permission_rationale),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(
                                    onClick = { permissionLauncher.launch(Manifest.permission.READ_CALENDAR) },
                                    modifier = Modifier.testTag("grant_calendar_permission_btn")
                                ) {
                                    Text(stringResource(R.string.grant_permission))
                                }
                            }
                        }
                    }

                    if (allEvents.isEmpty()) {
                        Box(
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No upcoming events",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(bottom = 84.dp)
                        ) {
                            items(allEvents) { event ->
                                EventItemCard(event = event)
                            }
                        }
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = { showAddEventDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
                .testTag("add_calendar_event_fab"),
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add Event")
        }
    }

    if (showAddEventDialog) {
        var title by remember { mutableStateOf("") }
        var desc by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddEventDialog = false },
            title = { Text("Add Event") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Event Title") },
                        modifier = Modifier.fillMaxWidth().testTag("event_title_input")
                    )
                    OutlinedTextField(
                        value = desc,
                        onValueChange = { desc = it },
                        label = { Text("Description (Optional)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = "Date: ${selectedDate.format(DateTimeFormatter.ofPattern("EEE, MMM d, yyyy"))}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (title.isNotBlank()) {
                            val cal = Calendar.getInstance().apply {
                                set(selectedDate.year, selectedDate.monthValue - 1, selectedDate.dayOfMonth, 9, 0, 0)
                            }
                            val startMillis = cal.timeInMillis
                            val endMillis = startMillis + 3600000 // 1 hr later

                            scope.launch {
                                calendarEventDao.insertEvent(
                                    LocalCalendarEventEntity(
                                        title = title,
                                        description = desc,
                                        startEpochMillis = startMillis,
                                        endEpochMillis = endMillis
                                    )
                                )
                            }
                            showAddEventDialog = false
                        }
                    }
                ) {
                    Text(stringResource(R.string.save_button))
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddEventDialog = false }) {
                    Text(stringResource(R.string.cancel_button))
                }
            }
        )
    }
}

@Composable
private fun EventItemCard(event: DisplayEvent) {
    val timeFormat = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }
    val dateFormat = remember { SimpleDateFormat("EEE, MMM d", Locale.getDefault()) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        if (event.isDeviceEvent) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.primaryContainer
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Event,
                    contentDescription = null,
                    tint = if (event.isDeviceEvent) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = event.title,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${dateFormat.format(Date(event.startEpochMillis))} at ${timeFormat.format(Date(event.startEpochMillis))}" +
                            if (event.isDeviceEvent) " • Device" else " • Local",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
    }
}

private fun InstantToLocalDate(epochMillis: Long): LocalDate {
    val cal = Calendar.getInstance().apply { timeInMillis = epochMillis }
    return LocalDate.of(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH))
}

private fun loadDeviceCalendarEvents(context: Context): List<DisplayEvent> {
    val events = mutableListOf<DisplayEvent>()
    val projection = arrayOf(
        CalendarContract.Events._ID,
        CalendarContract.Events.TITLE,
        CalendarContract.Events.DESCRIPTION,
        CalendarContract.Events.DTSTART
    )

    try {
        val cursor = context.contentResolver.query(
            CalendarContract.Events.CONTENT_URI,
            projection,
            null,
            null,
            "${CalendarContract.Events.DTSTART} ASC"
        )

        cursor?.use {
            val idIdx = it.getColumnIndex(CalendarContract.Events._ID)
            val titleIdx = it.getColumnIndex(CalendarContract.Events.TITLE)
            val descIdx = it.getColumnIndex(CalendarContract.Events.DESCRIPTION)
            val dtIdx = it.getColumnIndex(CalendarContract.Events.DTSTART)

            while (it.moveToNext()) {
                val id = if (idIdx >= 0) it.getString(idIdx) ?: "" else ""
                val title = if (titleIdx >= 0) it.getString(titleIdx) ?: "Untitled Event" else "Untitled Event"
                val desc = if (descIdx >= 0) it.getString(descIdx) ?: "" else ""
                val dt = if (dtIdx >= 0) it.getLong(dtIdx) else 0L

                events.add(
                    DisplayEvent(
                        id = "dev_$id",
                        title = title,
                        description = desc,
                        startEpochMillis = dt,
                        isDeviceEvent = true
                    )
                )
            }
        }
    } catch (_: Exception) {}
    return events
}
