package com.example.ui.phone

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.data.PixelSimDatabase
import com.example.data.SettingsDataStore
import com.example.data.SystemSettings
import com.example.os.HomeScreenManager
import com.example.os.OsScreen
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
import com.example.ui.theme.Wallpapers
import kotlinx.coroutines.launch

@Composable
fun PixelOsContainer(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val database = remember { PixelSimDatabase.getInstance(context) }
    val settingsDataStore = remember { SettingsDataStore(context) }
    val soundEffectsManager = remember { com.example.os.SoundEffectsManager(context) }
    val systemStateHolder = remember { SystemStateHolder(context, scope, settingsDataStore) }
    val windowManager = remember {
        WindowManager(
            notificationDao = database.notificationDao(),
            settingsDataStore = settingsDataStore,
            scope = scope,
            soundEffectsManager = soundEffectsManager
        )
    }
    val homeScreenManager = remember { HomeScreenManager(database.homeDao()) }
    val parallaxSensorManager = remember { com.example.os.ParallaxSensorManager(context) }

    DisposableEffect(Unit) {
        onDispose {
            systemStateHolder.destroy()
            soundEffectsManager.release()
            parallaxSensorManager.unregister()
        }
    }

    LaunchedEffect(Unit) {
        homeScreenManager.initializeDefaultLayoutIfNeeded()
    }

    val settings by settingsDataStore.settingsFlow.collectAsState(initial = SystemSettings())
    val systemState by systemStateHolder.state.collectAsState()
    val currentScreen by windowManager.currentScreen.collectAsState()
    val activeAppId by windowManager.activeAppId.collectAsState()
    val runningApps by windowManager.runningApps.collectAsState()
    val isShadeOpen by windowManager.isNotificationShadeOpen.collectAsState()
    val notifications by windowManager.notifications.collectAsState()
    val homeItems by homeScreenManager.homeItems.collectAsState(initial = emptyList())

    // Back button handling: respect MagicOS system gesture conflict preference
    BackHandler(enabled = currentScreen != OsScreen.LOCK && !settings.disableEdgeBack) {
        if (!windowManager.handleBackGesture()) {
            windowManager.goHome()
        }
    }

    // Determine Theme & Colors based on selected wallpaper & mode
    val wallpaperTheme = remember(settings.wallpaperId) {
        Wallpapers.getById(settings.wallpaperId)
    }

    val isDark = when (settings.darkMode) {
        "light" -> false
        "dark" -> true
        else -> isSystemInDarkTheme()
    }

    val colorScheme = if (isDark) wallpaperTheme.darkColors else wallpaperTheme.lightColors

    // Smooth color-fade transition for theme switching (light/dark/wallpaper)
    val animSpec = androidx.compose.animation.core.tween<Color>(durationMillis = 500)
    val animatedColorScheme = colorScheme.copy(
        primary = androidx.compose.animation.animateColorAsState(targetValue = colorScheme.primary, animationSpec = animSpec, label = "primary").value,
        onPrimary = androidx.compose.animation.animateColorAsState(targetValue = colorScheme.onPrimary, animationSpec = animSpec, label = "onPrimary").value,
        primaryContainer = androidx.compose.animation.animateColorAsState(targetValue = colorScheme.primaryContainer, animationSpec = animSpec, label = "primaryContainer").value,
        onPrimaryContainer = androidx.compose.animation.animateColorAsState(targetValue = colorScheme.onPrimaryContainer, animationSpec = animSpec, label = "onPrimaryContainer").value,
        secondary = androidx.compose.animation.animateColorAsState(targetValue = colorScheme.secondary, animationSpec = animSpec, label = "secondary").value,
        onSecondary = androidx.compose.animation.animateColorAsState(targetValue = colorScheme.onSecondary, animationSpec = animSpec, label = "onSecondary").value,
        secondaryContainer = androidx.compose.animation.animateColorAsState(targetValue = colorScheme.secondaryContainer, animationSpec = animSpec, label = "secondaryContainer").value,
        onSecondaryContainer = androidx.compose.animation.animateColorAsState(targetValue = colorScheme.onSecondaryContainer, animationSpec = animSpec, label = "onSecondaryContainer").value,
        background = androidx.compose.animation.animateColorAsState(targetValue = colorScheme.background, animationSpec = animSpec, label = "background").value,
        onBackground = androidx.compose.animation.animateColorAsState(targetValue = colorScheme.onBackground, animationSpec = animSpec, label = "onBackground").value,
        surface = androidx.compose.animation.animateColorAsState(targetValue = colorScheme.surface, animationSpec = animSpec, label = "surface").value,
        onSurface = androidx.compose.animation.animateColorAsState(targetValue = colorScheme.onSurface, animationSpec = animSpec, label = "onSurface").value,
        surfaceVariant = androidx.compose.animation.animateColorAsState(targetValue = colorScheme.surfaceVariant, animationSpec = animSpec, label = "surfaceVariant").value,
        onSurfaceVariant = androidx.compose.animation.animateColorAsState(targetValue = colorScheme.onSurfaceVariant, animationSpec = animSpec, label = "onSurfaceVariant").value
    )

    // Scale typography styles dynamically according to accessibility fontScale setting
    val scaledTypography = androidx.compose.material3.Typography(
        displayLarge = com.example.ui.theme.Typography.displayLarge.copy(fontSize = com.example.ui.theme.Typography.displayLarge.fontSize * settings.fontScale),
        displayMedium = com.example.ui.theme.Typography.displayMedium.copy(fontSize = com.example.ui.theme.Typography.displayMedium.fontSize * settings.fontScale),
        displaySmall = com.example.ui.theme.Typography.displaySmall.copy(fontSize = com.example.ui.theme.Typography.displaySmall.fontSize * settings.fontScale),
        headlineLarge = com.example.ui.theme.Typography.headlineLarge.copy(fontSize = com.example.ui.theme.Typography.headlineLarge.fontSize * settings.fontScale),
        headlineMedium = com.example.ui.theme.Typography.headlineMedium.copy(fontSize = com.example.ui.theme.Typography.headlineMedium.fontSize * settings.fontScale),
        headlineSmall = com.example.ui.theme.Typography.headlineSmall.copy(fontSize = com.example.ui.theme.Typography.headlineSmall.fontSize * settings.fontScale),
        titleLarge = com.example.ui.theme.Typography.titleLarge.copy(fontSize = com.example.ui.theme.Typography.titleLarge.fontSize * settings.fontScale),
        titleMedium = com.example.ui.theme.Typography.titleMedium.copy(fontSize = com.example.ui.theme.Typography.titleMedium.fontSize * settings.fontScale),
        titleSmall = com.example.ui.theme.Typography.titleSmall.copy(fontSize = com.example.ui.theme.Typography.titleSmall.fontSize * settings.fontScale),
        bodyLarge = com.example.ui.theme.Typography.bodyLarge.copy(fontSize = com.example.ui.theme.Typography.bodyLarge.fontSize * settings.fontScale),
        bodyMedium = com.example.ui.theme.Typography.bodyMedium.copy(fontSize = com.example.ui.theme.Typography.bodyMedium.fontSize * settings.fontScale),
        bodySmall = com.example.ui.theme.Typography.bodySmall.copy(fontSize = com.example.ui.theme.Typography.bodySmall.fontSize * settings.fontScale),
        labelLarge = com.example.ui.theme.Typography.labelLarge.copy(fontSize = com.example.ui.theme.Typography.labelLarge.fontSize * settings.fontScale),
        labelMedium = com.example.ui.theme.Typography.labelMedium.copy(fontSize = com.example.ui.theme.Typography.labelMedium.fontSize * settings.fontScale),
        labelSmall = com.example.ui.theme.Typography.labelSmall.copy(fontSize = com.example.ui.theme.Typography.labelSmall.fontSize * settings.fontScale)
    )

    val deviceProfile = remember(settings.deviceProfileStyle) {
        com.example.model.DeviceProfileDefaults.getProfile(settings.deviceProfileStyle)
    }

    val tiltOffset by parallaxSensorManager.offset.collectAsState()

    MaterialTheme(colorScheme = animatedColorScheme, typography = scaledTypography) {
        PhoneFrame(
            showBezel = settings.showBezel,
            deviceProfile = deviceProfile
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(
                        translationX = tiltOffset.first,
                        translationY = tiltOffset.second
                    )
                    .background(Wallpapers.makeBackgroundBrush(wallpaperTheme))
            ) {
                // OS Screen Switcher
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 28.dp) // space for navigation bar
                ) {
                    when (currentScreen) {
                        OsScreen.LOCK -> {
                            LockScreen(
                                systemState = systemState,
                                notifications = notifications,
                                onUnlockTriggered = {
                                    windowManager.unlock(requiresPin = settings.hashedPin != null)
                                }
                            )
                        }

                        OsScreen.PIN -> {
                            PinScreen(
                                hashedPin = settings.hashedPin,
                                onSuccess = { windowManager.onPinSuccess() },
                                onCancel = { windowManager.lock() }
                            )
                        }

                        OsScreen.HOME -> {
                            HomeScreen(
                                homeItems = homeItems,
                                homeScreenManager = homeScreenManager,
                                systemState = systemState,
                                selectedIconPack = settings.selectedIconPack,
                                onOpenApp = { appId -> windowManager.openApp(appId) },
                                onOpenDrawer = { windowManager.openDrawer() }
                            )
                        }

                        OsScreen.DRAWER -> {
                            AppDrawer(
                                onOpenApp = { appId -> windowManager.openApp(appId) },
                                onCloseDrawer = { windowManager.closeDrawer() },
                                selectedIconPack = settings.selectedIconPack,
                                searchEngine = settings.searchEngine
                            )
                        }

                        OsScreen.RECENTS -> {
                            RecentsScreen(
                                runningApps = runningApps,
                                onSelectApp = { appId -> windowManager.openApp(appId) },
                                onCloseApp = { appId -> windowManager.closeApp(appId) },
                                onClearAll = { windowManager.clearAllRecentApps() },
                                selectedIconPack = settings.selectedIconPack
                            )
                        }

                        OsScreen.APP -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(top = 34.dp) // status bar clearance
                            ) {
                                WindowManagerView(
                                    activeAppId = activeAppId,
                                    database = database,
                                    settingsDataStore = settingsDataStore,
                                    systemStateHolder = systemStateHolder,
                                    windowManager = windowManager
                                )
                            }
                        }
                    }
                }

                // Status Bar (always visible at top except inside live camera or lock)
                StatusBar(
                    systemState = systemState,
                    textColor = if (currentScreen == OsScreen.APP && !isDark && activeAppId != "camera") Color.Black else Color.White,
                    onOpenShade = { windowManager.setNotificationShadeOpen(true) },
                    modifier = Modifier.align(Alignment.TopCenter)
                )

                // Notification Shade Overlay
                AnimatedVisibility(
                    visible = isShadeOpen,
                    enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
                    modifier = Modifier.fillMaxSize()
                ) {
                    NotificationShade(
                        systemState = systemState,
                        systemStateHolder = systemStateHolder,
                        notifications = notifications,
                        onDismissNotification = { id -> windowManager.dismissNotification(id) },
                        onClearAllNotifications = { windowManager.clearAllNotifications() },
                        onOpenSettings = {
                            windowManager.openApp("settings")
                        },
                        onClose = { windowManager.setNotificationShadeOpen(false) }
                    )
                }

                // Gesture Navigation Bar (at bottom)
                NavigationBar(
                    onHome = { windowManager.goHome() },
                    onRecents = { windowManager.openRecents() },
                    pillColor = if (currentScreen == OsScreen.APP && !isDark && activeAppId != "camera") Color.Black.copy(alpha = 0.7f) else Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
        }
    }
}
