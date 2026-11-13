@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.example.simulated.phone

import android.content.Context
import android.media.AudioManager
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.os.LiveActivityState
import com.example.os.WindowManager
import com.example.simulated.contacts.ContactsHelper
import com.example.simulated.contacts.DeviceContact

@Composable
fun PhoneApp(
    windowManager: WindowManager? = null,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val context = LocalContext.current
    var dialedNumber by remember { mutableStateOf("") }
    val audioManager = remember(context) {
        context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    }
    var favorites by remember { mutableStateOf<List<DeviceContact>>(emptyList()) }
    var isMuted by remember { mutableStateOf(false) }
    var isSpeaker by remember { mutableStateOf(false) }

    val liveActivity by (windowManager?.currentLiveActivity?.collectAsState()
        ?: remember { mutableStateOf(LiveActivityState.None) })
    val activeCall = liveActivity as? LiveActivityState.PhoneCallActivity

    DisposableEffect(activeCall != null) {
        if (activeCall != null) {
            try {
                audioManager?.mode = AudioManager.MODE_IN_COMMUNICATION
            } catch (_: Exception) {}
        }
        onDispose {
            isMuted = false
            isSpeaker = false
            try {
                audioManager?.isMicrophoneMute = false
                audioManager?.isSpeakerphoneOn = false
                audioManager?.mode = AudioManager.MODE_NORMAL
            } catch (_: Exception) {}
        }
    }

    LaunchedEffect(Unit) {
        if (ContactsHelper.hasContactsPermission(context)) {
            val all = ContactsHelper.loadContacts(context)
            favorites = all.filter { it.isStarred }
        }
    }

    if (activeCall != null) {
        // Active Simulated In-Call UI synced with StatusBar Live Pill
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(Color(0xFF0E1B14))
                .padding(24.dp)
                .testTag("simulated_in_call_screen"),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 32.dp)
            ) {
                Text(
                    text = stringResource(R.string.phone_call_active),
                    color = Color(0xFF81C784),
                    style = MaterialTheme.typography.labelLarge
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = activeCall.callerOrNumber,
                    color = Color.White,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = activeCall.formattedDuration(),
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 18.sp
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = {
                        val next = !isMuted
                        isMuted = next
                        try { audioManager?.isMicrophoneMute = next } catch (_: Exception) {}
                    },
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (isMuted) Color.White.copy(alpha = 0.2f) else Color.Transparent,
                        contentColor = Color.White
                    )
                ) {
                    Icon(Icons.Default.MicOff, contentDescription = stringResource(R.string.phone_mute))
                }
                OutlinedButton(
                    onClick = {
                        val next = !isSpeaker
                        isSpeaker = next
                        try {
                            audioManager?.mode = AudioManager.MODE_IN_COMMUNICATION
                            audioManager?.isSpeakerphoneOn = next
                        } catch (_: Exception) {}
                    },
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (isSpeaker) Color.White.copy(alpha = 0.2f) else Color.Transparent,
                        contentColor = Color.White
                    )
                ) {
                    Icon(Icons.Default.VolumeUp, contentDescription = stringResource(R.string.phone_speaker))
                }
            }

            FloatingActionButton(
                onClick = { windowManager?.endSimulatedCall() },
                containerColor = Color(0xFFD32F2F),
                contentColor = Color.White,
                modifier = Modifier
                    .size(72.dp)
                    .padding(bottom = 8.dp)
                    .testTag("end_sim_call_btn")
            ) {
                Icon(Icons.Default.CallEnd, contentDescription = stringResource(R.string.phone_end_call), modifier = Modifier.size(30.dp))
            }
        }
        return
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
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Keypad") },
                icon = { Icon(Icons.Default.Dialpad, contentDescription = null, modifier = Modifier.size(20.dp)) }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Favorites") },
                icon = { Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(20.dp)) }
            )
        }

        if (selectedTab == 0) {
            // Keypad screen
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Dialed number display
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (dialedNumber.isEmpty()) " " else dialedNumber,
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }

                // Dial pad buttons 3x4
                val keys = listOf(
                    listOf("1" to "", "2" to "ABC", "3" to "DEF"),
                    listOf("4" to "GHI", "5" to "JKL", "6" to "MNO"),
                    listOf("7" to "PQRS", "8" to "TUV", "9" to "WXYZ"),
                    listOf("*" to "", "0" to "+", "#" to "")
                )

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    keys.forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(0.85f),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            row.forEach { (digit, sub) ->
                                DialKey(
                                    digit = digit,
                                    subtitle = sub,
                                    onClick = { dialedNumber += digit }
                                )
                            }
                        }
                    }

                    // Call & Backspace controls
                    Row(
                        modifier = Modifier
                            .fillMaxWidth(0.92f)
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (windowManager != null) {
                            Button(
                                onClick = {
                                    val target = dialedNumber.ifBlank { "Pixel Hotline" }
                                    windowManager.startSimulatedCall(target)
                                },
                                modifier = Modifier.testTag("sim_call_btn")
                            ) {
                                Text(stringResource(R.string.phone_sim_call), fontSize = 12.sp)
                            }
                        } else {
                            Spacer(modifier = Modifier.size(56.dp))
                        }

                        FloatingActionButton(
                            onClick = {
                                if (dialedNumber.isNotBlank()) {
                                    ContactsHelper.openDialer(context, dialedNumber)
                                }
                            },
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier
                                .size(64.dp)
                                .testTag("dial_call_btn")
                        ) {
                            Icon(Icons.Default.Call, contentDescription = "Call", modifier = Modifier.size(28.dp))
                        }

                        if (dialedNumber.isNotEmpty()) {
                            IconButton(
                                onClick = { dialedNumber = dialedNumber.dropLast(1) },
                                modifier = Modifier.size(56.dp)
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.Backspace,
                                    contentDescription = "Backspace",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            Spacer(modifier = Modifier.size(56.dp))
                        }
                    }
                }
            }
        } else {
            // Favorites screen
            if (favorites.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No starred contacts found on device",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(favorites) { contact ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .clickable {
                                    contact.phoneNumber?.let {
                                        ContactsHelper.openDialer(context, it)
                                    }
                                }
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = contact.name,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                contact.phoneNumber?.let {
                                    Text(
                                        text = it,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            Icon(Icons.Default.Call, contentDescription = "Call", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DialKey(
    digit: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(70.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick)
            .testTag("dial_key_$digit"),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = digit,
                fontSize = 26.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (subtitle.isNotEmpty()) {
                Text(
                    text = subtitle,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
        }
    }
}
