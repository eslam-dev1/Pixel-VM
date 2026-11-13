package com.example.ui.phone

import android.content.ComponentName
import android.view.KeyEvent
import androidx.activity.compose.BackHandler
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.PixelSimDatabase
import com.example.data.SettingsDataStore
import com.example.data.SystemSettings
import com.example.os.AppRegistry
import com.example.os.HomeScreenManager
import com.example.os.LiveActivityState
import com.example.os.OsScreen
import com.example.os.SmartSelectExtractor
import com.example.os.SystemStateHolder
import com.example.os.WindowManager
import com.example.simulator.systemui.PixelBottomGesturePill
import com.example.simulator.systemui.PixelCircleToSearchOverlay
import com.example.simulator.systemui.recents.PixelRecentsScreen
import com.example.simulator.systemui.recents.RecentAppTask
import com.example.ui.components.MediaLivePillCard
import com.example.ui.components.ScreenshotMarkupEditor
import com.example.ui.components.SmartSelectOverlay
import com.example.ui.theme.Wallpapers
import com.example.util.ScreenshotHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun PixelOsContainer(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val view = LocalView.current
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

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        systemStateHolder.refreshSystemStates()
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
    val splitState by windowManager.splitScreenState.collectAsState()
    val liveActivity by windowManager.currentLiveActivity.collectAsState()
    val isPrivateUnlocked by windowManager.isPrivateSpaceUnlocked.collectAsState()
    val smartSelectAppId by windowManager.smartSelectAppId.collectAsState()
    val markupBitmap by windowManager.markupBitmap.collectAsState()
    val taskPreviews by windowManager.taskPreviews.collectAsState()
    val isCircleToSearchOpen by windowManager.isCircleToSearchOpen.collectAsState()
    val isDeviceMediaPlaying by systemStateHolder.isDeviceMediaPlaying.collectAsState()

    var showMediaPillPopup by remember { mutableStateOf(false) }
    var drawerSearchInitialQuery by remember { mutableStateOf("") }

    val pixelThemeSettings = remember(settings) { settings.toPixelThemeSettings() }
    val privateApps = remember(settings) { settings.privateSpaceAppIds() }
    val hiddenApps = remember(privateApps, isPrivateUnlocked) {
        if (isPrivateUnlocked) emptySet() else privateApps
    }
    val visibleNotifications = remember(notifications, privateApps, isPrivateUnlocked) {
        WindowManager.filterNotificationsForPrivateSpace(notifications, privateApps, isPrivateUnlocked)
    }

    // Live Activity ticker for active simulated Phone calls & popup lifecycle
    LaunchedEffect(liveActivity, isDeviceMediaPlaying) {
        if (liveActivity !is LiveActivityState.MediaActivity && !isDeviceMediaPlaying) {
            showMediaPillPopup = false
        }
        if (liveActivity is LiveActivityState.PhoneCallActivity) {
            while (true) {
                delay(1000L)
                windowManager.tickSimulatedCall()
            }
        }
    }

    val captureActiveAppSnapshot: () -> Unit = {
        if (currentScreen == OsScreen.APP && activeAppId != null) {
            ScreenshotHelper.captureBitmap(view.rootView)?.let { captured ->
                windowManager.saveTaskPreview(activeAppId!!, captured)
            }
        }
    }

    // Back button handling: respect MagicOS system gesture conflict preference
    BackHandler(enabled = currentScreen != OsScreen.LOCK && !settings.disableEdgeBack) {
        captureActiveAppSnapshot()
        if (!windowManager.handleBackGesture()) {
            windowManager.goHome()
        }
    }

    // Apply Activity window screenBrightness directly without WRITE_SETTINGS permission
    DisposableEffect(systemState.brightnessPercent) {
        val activity = context as? android.app.Activity
        activity?.window?.let { window ->
            val lp = window.attributes
            lp.screenBrightness = systemState.brightnessPercent.coerceIn(0.01f, 1.0f)
            window.attributes = lp
        }
        onDispose {
            activity?.window?.let { window ->
                val lp = window.attributes
                lp.screenBrightness = android.view.WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
                window.attributes = lp
            }
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

    val liveActivityStatusText = when (val activity = liveActivity) {
        is LiveActivityState.PhoneCallActivity -> "${activity.formattedDuration()} • ${activity.callerOrNumber}"
        is LiveActivityState.TimerActivity -> activity.formattedRemaining()
        is LiveActivityState.StopwatchActivity -> activity.formattedElapsed()
        is LiveActivityState.MediaActivity -> "${activity.trackTitle} — ${activity.artist}"
        LiveActivityState.None -> null
    }

    androidx.compose.material3.MaterialTheme(colorScheme = animatedColorScheme, typography = scaledTypography) {
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
                        .padding(bottom = if (currentScreen == OsScreen.LOCK) 0.dp else 44.dp)
                ) {
                    when (currentScreen) {
                        OsScreen.LOCK -> {
                            LockScreen(
                                systemState = systemState,
                                notifications = if (settings.notifLockScreenVisible) visibleNotifications else emptyList(),
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
                                pixelThemeSettings = pixelThemeSettings,
                                hiddenAppIds = hiddenApps,
                                onOpenApp = { appId -> windowManager.openApp(appId) },
                                onOpenDrawer = {
                                    drawerSearchInitialQuery = ""
                                    windowManager.openDrawer()
                                },
                                onOpenDrawerWithQuery = { spoken ->
                                    drawerSearchInitialQuery = spoken
                                    windowManager.openDrawer()
                                }
                            )
                        }

                        OsScreen.DRAWER -> {
                            AppDrawer(
                                onOpenApp = { appId ->
                                    drawerSearchInitialQuery = ""
                                    windowManager.openApp(appId)
                                },
                                onCloseDrawer = {
                                    drawerSearchInitialQuery = ""
                                    windowManager.closeDrawer()
                                },
                                selectedIconPack = settings.selectedIconPack,
                                searchEngine = settings.searchEngine,
                                pixelThemeSettings = pixelThemeSettings,
                                privateSpaceApps = privateApps,
                                isPrivateSpaceUnlocked = isPrivateUnlocked,
                                privateSpacePinHash = settings.privateSpacePinHash,
                                onUnlockPrivateSpace = { windowManager.unlockPrivateSpace() },
                                onLockPrivateSpace = { windowManager.lockPrivateSpace() },
                                onToggleAppInPrivateSpace = { appId ->
                                    val updated = if (appId in privateApps) privateApps - appId else privateApps + appId
                                    scope.launch { settingsDataStore.setPrivateSpaceApps(updated) }
                                },
                                initialSearchQuery = drawerSearchInitialQuery
                            )
                        }

                        OsScreen.RECENTS -> {
                            val recentTasks = runningApps.mapIndexed { index, appId ->
                                val appDef = AppRegistry.getApp(appId)
                                val label = appDef?.let { stringResource(it.titleRes) } ?: appId
                                RecentAppTask(
                                    taskId = index + 1,
                                    componentName = ComponentName(context.packageName, appId),
                                    appName = label,
                                    appIcon = null,
                                    previewSnapshot = taskPreviews[appId],
                                    isLocked = appId in privateApps && !isPrivateUnlocked
                                )
                            }

                            PixelRecentsScreen(
                                recentTasks = recentTasks,
                                onTaskSelected = { task ->
                                    windowManager.openApp(task.componentName.className)
                                },
                                onTaskDismissed = { task ->
                                    windowManager.closeApp(task.componentName.className)
                                },
                                onClearAllTasks = {
                                    windowManager.clearAllRecentApps()
                                },
                                onTakeScreenshot = { task ->
                                    val targetBmp = taskPreviews[task.componentName.className]
                                        ?: ScreenshotHelper.captureBitmap(view.rootView)
                                    if (targetBmp != null) {
                                        windowManager.openMarkupEditor(targetBmp)
                                    }
                                },
                                onSelectContent = { task ->
                                    windowManager.openSmartSelect(task.componentName.className)
                                },
                                onSplitScreen = { task ->
                                    windowManager.startSplitScreen(task.componentName.className)
                                },
                                onDismissOverview = {
                                    windowManager.goHome()
                                },
                                selectedIconPack = settings.selectedIconPack,
                                pixelThemeSettings = pixelThemeSettings
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

                        OsScreen.SPLIT_SCREEN -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(top = 34.dp) // status bar clearance
                            ) {
                                SplitScreenView(
                                    splitState = splitState,
                                    database = database,
                                    settingsDataStore = settingsDataStore,
                                    systemStateHolder = systemStateHolder,
                                    windowManager = windowManager,
                                    selectedIconPack = settings.selectedIconPack,
                                    pixelThemeSettings = pixelThemeSettings,
                                    hiddenAppIds = hiddenApps
                                )
                            }
                        }
                    }
                }

                // Status Bar (always visible at top with Live Activity Pill)
                val effectiveLiveActivity = if (liveActivity == LiveActivityState.None && isDeviceMediaPlaying) {
                    LiveActivityState.MediaActivity(
                        trackTitle = stringResource(R.string.media_playing_active),
                        artist = "",
                        isPlaying = true
                    )
                } else {
                    liveActivity
                }
                StatusBar(
                    systemState = systemState,
                    textColor = if ((currentScreen == OsScreen.APP || currentScreen == OsScreen.SPLIT_SCREEN) && !isDark && activeAppId != "camera") Color.Black else Color.White,
                    onOpenShade = { windowManager.setNotificationShadeOpen(true) },
                    liveActivity = effectiveLiveActivity,
                    onLivePillClick = {
                        when {
                            effectiveLiveActivity is LiveActivityState.PhoneCallActivity -> windowManager.openApp("phone")
                            effectiveLiveActivity is LiveActivityState.TimerActivity ||
                                effectiveLiveActivity is LiveActivityState.StopwatchActivity -> windowManager.openApp("clock")
                            effectiveLiveActivity is LiveActivityState.MediaActivity -> showMediaPillPopup = !showMediaPillPopup
                        }
                    },
                    modifier = Modifier.align(Alignment.TopCenter)
                )

                // Collapsible Media Live Pill Mini-Player
                AnimatedVisibility(
                    visible = showMediaPillPopup && effectiveLiveActivity is LiveActivityState.MediaActivity,
                    enter = slideInVertically(initialOffsetY = { -it / 2 }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { -it / 2 }) + fadeOut(),
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 42.dp)
                ) {
                    val media = effectiveLiveActivity as? LiveActivityState.MediaActivity
                    if (media != null) {
                        MediaLivePillCard(
                            media = media,
                            onTogglePlayPause = { systemStateHolder.dispatchMediaKey(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE) },
                            onNextTrack = { systemStateHolder.dispatchMediaKey(KeyEvent.KEYCODE_MEDIA_NEXT) },
                            onClose = {
                                systemStateHolder.dispatchMediaKey(KeyEvent.KEYCODE_MEDIA_STOP)
                                windowManager.stopMedia()
                            },
                            onDismissPopup = { showMediaPillPopup = false }
                        )
                    }
                }

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
                        notifications = visibleNotifications,
                        onDismissNotification = { id -> windowManager.dismissNotification(id) },
                        onClearAllNotifications = { windowManager.clearAllNotifications() },
                        onOpenSettings = {
                            windowManager.openApp("settings")
                        },
                        onClose = { windowManager.setNotificationShadeOpen(false) },
                        onTakeMarkupScreenshot = {
                            windowManager.setNotificationShadeOpen(false)
                            ScreenshotHelper.captureBitmap(view.rootView)?.let { bmp ->
                                windowManager.openMarkupEditor(bmp)
                            }
                        }
                    )
                }

                // Live Context Items for Smart Select & Circle to Search
                val liveScreenExtraContent = remember(systemState, visibleNotifications, runningApps) {
                    buildList {
                        add("${systemState.dateFormatted} • ${systemState.timeFormatted}")
                        add("Battery ${systemState.batteryPercent}%${if (systemState.isCharging) " Charging" else ""}")
                        visibleNotifications.take(3).forEach { notif ->
                            add("${notif.title}: ${notif.message}")
                        }
                        if (runningApps.isNotEmpty()) {
                            add("Open tasks: ${runningApps.joinToString(", ")}")
                        }
                    }
                }

                // Overview Smart Select Overlay
                smartSelectAppId?.let { appId ->
                    SmartSelectOverlay(
                        appId = appId,
                        liveStatusText = liveActivityStatusText,
                        extraContent = liveScreenExtraContent,
                        onDismiss = { windowManager.closeSmartSelect() }
                    )
                }

                // Pixel Markup Screenshot Editor Overlay
                markupBitmap?.let { bmp ->
                    ScreenshotMarkupEditor(
                        bitmap = bmp,
                        onSave = { editedBitmap ->
                            ScreenshotHelper.saveBitmapWithFeedback(context, editedBitmap)
                            windowManager.closeMarkupEditor()
                        },
                        onDismiss = { windowManager.closeMarkupEditor() }
                    )
                }

                // Circle to Search Overlay (Long-press gesture pill to launch)
                val circleTargetId = when (currentScreen) {
                    OsScreen.APP -> activeAppId ?: "home"
                    OsScreen.SPLIT_SCREEN -> splitState.topAppId ?: "home"
                    OsScreen.HOME -> "home"
                    OsScreen.DRAWER -> "drawer"
                    OsScreen.RECENTS -> "recents"
                    else -> "pixel"
                }
                val circleDisplayName = AppRegistry.getApp(circleTargetId)?.let { stringResource(it.titleRes) }
                    ?: stringResource(R.string.app_name)
                val circleSnippets = remember(circleTargetId, circleDisplayName, liveActivityStatusText, liveScreenExtraContent) {
                    SmartSelectExtractor.extractSnippets(
                        appId = circleTargetId,
                        appDisplayName = circleDisplayName,
                        liveStatusText = liveActivityStatusText,
                        extraContent = liveScreenExtraContent
                    )
                }

                PixelCircleToSearchOverlay(
                    isVisible = isCircleToSearchOpen,
                    onDismiss = { windowManager.closeCircleToSearch() },
                    onRegionCircled = { rect ->
                        if (circleTargetId != "home" && circleTargetId != "pixel") {
                            android.widget.Toast.makeText(
                                context,
                                "$circleDisplayName (${rect.width.toInt()}×${rect.height.toInt()})",
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        }
                    },
                    onTranslateScreen = {
                        android.widget.Toast.makeText(
                            context,
                            context.getString(R.string.circle_to_search_translated_mode),
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    },
                    detectedSnippets = circleSnippets,
                    searchEngine = settings.searchEngine
                )

                // Pixel Gesture Navigation Pill (Swipe Up -> Home/Recents, Long-Press -> Circle to Search)
                PixelBottomGesturePill(
                    onSwipeUpHome = {
                        captureActiveAppSnapshot()
                        windowManager.goHome()
                    },
                    onSwipeUpRecents = {
                        captureActiveAppSnapshot()
                        windowManager.openRecents()
                    },
                    onTriggerCircleToSearch = {
                        if (currentScreen != OsScreen.LOCK && currentScreen != OsScreen.PIN) {
                            windowManager.openCircleToSearch()
                        }
                    },
                    pillColor = if ((currentScreen == OsScreen.APP || currentScreen == OsScreen.SPLIT_SCREEN) && !isDark && activeAppId != "camera") {
                        Color.Black.copy(alpha = 0.7f)
                    } else {
                        Color.White.copy(alpha = 0.85f)
                    },
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
        }
    }
}
