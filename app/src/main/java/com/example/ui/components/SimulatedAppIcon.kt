package com.example.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun SimulatedAppIcon(
    appId: String,
    size: Dp = 56.dp,
    modifier: Modifier = Modifier,
    useThemedMonochrome: Boolean = false,
    iconPackPackage: String? = null
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val resolver = remember(context) { com.example.di.IconPackModule.provideIconPackResolver(context) }
    
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.80f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "icon_squish"
    )

    val pressModifier = modifier
        .pointerInput(Unit) {
            awaitPointerEventScope {
                while (true) {
                    val event = awaitPointerEvent()
                    isPressed = event.changes.any { it.pressed }
                }
            }
        }
        .graphicsLayer(
            scaleX = scale,
            scaleY = scale
        )

    // Map simulator IDs to common package/activity names for icon pack matching
    val virtualComponentName = remember(appId) {
        when (appId) {
            "phone" -> "com.google.android.dialer" to "com.google.android.dialer.extensions.GoogleDialtactsActivity"
            "messages" -> "com.google.android.apps.messaging" to "com.google.android.apps.messaging.ui.ConversationListActivity"
            "camera" -> "com.google.android.GoogleCamera" to "com.android.camera.CameraLauncher"
            "clock" -> "com.google.android.deskclock" to "com.android.deskclock.DeskClock"
            "calculator" -> "com.google.android.calculator" to "com.android.calculator2.Calculator"
            "notes" -> "com.google.android.keep" to "com.google.android.apps.notes.MainActivity"
            "gallery" -> "com.google.android.apps.photos" to "com.google.android.apps.photos.home.HomeActivity"
            "settings" -> "com.android.settings" to "com.android.settings.Settings"
            "files" -> "com.google.android.apps.nbu.files" to "com.google.android.apps.nbu.files.home.HomeActivity"
            "contacts" -> "com.google.android.contacts" to "com.android.contacts.activities.PeopleActivity"
            "calendar" -> "com.google.android.calendar" to "com.android.calendar.AllInOneActivity"
            else -> "com.android.launcher" to "com.android.launcher.Launcher"
        }
    }

    val customIcon = remember(appId, iconPackPackage) {
        resolver.getIcon(virtualComponentName.first, virtualComponentName.second, iconPackPackage)
    }

    if (customIcon != null) {
        androidx.compose.foundation.Image(
            painter = coil.compose.rememberAsyncImagePainter(customIcon),
            contentDescription = null,
            modifier = pressModifier
                .size(size)
                .clip(RoundedCornerShape(size * 0.28f))
        )
    } else {
        val (icon, bgColors, fgColor) = when (appId) {
            "phone" -> Triple(Icons.Default.Call, listOf(Color(0xFF2E7D32), Color(0xFF4CAF50)), Color.White)
            "messages" -> Triple(Icons.Default.ChatBubble, listOf(Color(0xFF1565C0), Color(0xFF42A5F5)), Color.White)
            "camera" -> Triple(Icons.Default.PhotoCamera, listOf(Color(0xFF37474F), Color(0xFF546E7A)), Color.White)
            "clock" -> Triple(Icons.Default.Alarm, listOf(Color(0xFF00838F), Color(0xFF26C6DA)), Color.White)
            "calculator" -> Triple(Icons.Default.Calculate, listOf(Color(0xFF4527A0), Color(0xFF7E57C2)), Color.White)
            "notes" -> Triple(Icons.Default.EditNote, listOf(Color(0xFFE65100), Color(0xFFFFA726)), Color.White)
            "gallery" -> Triple(Icons.Default.PhotoLibrary, listOf(Color(0xFFC2185B), Color(0xFFEC407A)), Color.White)
            "settings" -> Triple(Icons.Default.Settings, listOf(Color(0xFF455A64), Color(0xFF78909C)), Color.White)
            "files" -> Triple(Icons.Default.Folder, listOf(Color(0xFF0277BD), Color(0xFF29B6F6)), Color.White)
            "contacts" -> Triple(Icons.Default.People, listOf(Color(0xFF00695C), Color(0xFF26A69A)), Color.White)
            "calendar" -> Triple(Icons.Default.CalendarMonth, listOf(Color(0xFFAD1457), Color(0xFFF06292)), Color.White)
            else -> Triple(Icons.Default.Folder, listOf(Color(0xFF546E7A), Color(0xFF78909C)), Color.White)
        }

        val cornerRadius = size * 0.28f
        val iconSize = size * 0.52f

        val finalBackground = if (useThemedMonochrome) {
            Modifier.background(MaterialTheme.colorScheme.primaryContainer)
        } else {
            Modifier.background(Brush.linearGradient(bgColors))
        }

        val finalTint = if (useThemedMonochrome) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            fgColor
        }

        Box(
            modifier = pressModifier
                .size(size)
                .clip(RoundedCornerShape(cornerRadius))
                .then(finalBackground),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = finalTint,
                modifier = Modifier.size(iconSize)
            )
        }
    }
}
