package com.example.ui.phone

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.data.PixelSimDatabase
import com.example.data.SettingsDataStore
import com.example.os.SystemStateHolder
import com.example.os.WindowManager
import com.example.simulated.calculator.CalculatorApp
import com.example.simulated.calendar.CalendarApp
import com.example.simulated.camera.CameraApp
import com.example.simulated.clock.ClockApp
import com.example.simulated.contacts.ContactsApp
import com.example.simulated.files.FilesApp
import com.example.simulated.gallery.GalleryApp
import com.example.simulated.messages.MessagesApp
import com.example.simulated.notes.NotesApp
import com.example.simulated.phone.PhoneApp
import com.example.simulated.settings.SettingsApp
import kotlinx.coroutines.launch
@Composable
fun WindowManagerView(
    activeAppId: String?,
    database: PixelSimDatabase,
    settingsDataStore: SettingsDataStore,
    systemStateHolder: SystemStateHolder,
    windowManager: WindowManager,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()

    val settings by settingsDataStore.settingsFlow.collectAsState(initial = com.example.data.SystemSettings())
    val animationStiffness = if (settings.smoothScrolling120Hz) Spring.StiffnessMediumLow else Spring.StiffnessLow

    AnimatedContent(
        targetState = activeAppId,
        transitionSpec = {
            (scaleIn(
                initialScale = 0.8f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = animationStiffness
                )
            ) + fadeIn(animationSpec = tween(220))) togetherWith
                    (scaleOut(
                        targetScale = 0.85f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = animationStiffness
                        )
                    ) + fadeOut(animationSpec = tween(180)))
        },
        label = "app_transition"
    ) { appId ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()) // dynamic clearance
        ) {
            when (appId) {
                "calculator" -> CalculatorApp()
                "clock" -> ClockApp(alarmDao = database.alarmDao())
                "notes" -> NotesApp(noteDao = database.noteDao())
                "contacts" -> ContactsApp()
                "phone" -> PhoneApp()
                "messages" -> MessagesApp()
                "camera" -> CameraApp(onOpenGallery = { windowManager.openApp("gallery") })
                "gallery" -> GalleryApp()
                "files" -> FilesApp()
                "calendar" -> CalendarApp(calendarEventDao = database.calendarEventDao())
                "settings" -> SettingsApp(
                    settingsDataStore = settingsDataStore,
                    systemStateHolder = systemStateHolder,
                    onResetPhone = {
                        scope.launch {
                            database.noteDao().clearAll()
                            database.alarmDao().clearAll()
                            database.calendarEventDao().clearAll()
                            settingsDataStore.clearAll()
                            windowManager.clearAllRecentApps()
                            windowManager.lock()
                        }
                    }
                )

            }
        }
    }
}
