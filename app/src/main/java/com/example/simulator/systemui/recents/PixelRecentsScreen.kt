package com.example.simulator.systemui.recents

import androidx.compose.ui.platform.testTag
import android.os.Build
import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Screenshot
import androidx.compose.material.icons.rounded.Splitscreen
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import androidx.core.graphics.drawable.toBitmap
import com.example.model.PixelThemeSettings
import com.example.ui.components.SimulatedAppIcon
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

/**
 * Simulates the Recent Apps / Overview screen of Google Pixel 10 (Android 16).
 */
@Composable
fun PixelRecentsScreen(
    recentTasks: List<RecentAppTask>,
    onTaskSelected: (RecentAppTask) -> Unit,
    onTaskDismissed: (RecentAppTask) -> Unit,
    onClearAllTasks: () -> Unit,
    onTakeScreenshot: (RecentAppTask) -> Unit,
    onSelectContent: (RecentAppTask) -> Unit, // Pixel Smart Select mode for text and images
    onSplitScreen: (RecentAppTask) -> Unit,
    onDismissOverview: () -> Unit,
    selectedIconPack: String? = null,
    pixelThemeSettings: PixelThemeSettings = PixelThemeSettings(),
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    val pagerState = rememberPagerState(
        initialPage = 0,
        pageCount = { recentTasks.size }
    )
    LaunchedEffect(recentTasks.size) {
        if (recentTasks.isNotEmpty() && pagerState.currentPage >= recentTasks.size) {
            pagerState.scrollToPage(recentTasks.size - 1)
        }
    }

    // Subtle haptic feedback when scrolling between open app cards (Pixel Haptics)
    LaunchedEffect(pagerState.currentPage) {
        if (recentTasks.isNotEmpty() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            view.performHapticFeedback(HapticFeedbackConstants.TEXT_HANDLE_MOVE)
        }
    }

    val scrimInteractionSource = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.65f))
            .clickable(
                interactionSource = scrimInteractionSource,
                indication = null,
                onClick = onDismissOverview
            )
    ) {
        if (recentTasks.isEmpty()) {
            // Classic Pixel message when all apps are closed
            Text(
                text = "No recent items",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.Center)
            )
        } else {
            val activeTask = recentTasks.getOrNull(pagerState.currentPage)

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // 1. "Clear all" button at the start of the list
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                            }
                            onClearAllTasks()
                        },
                        modifier = Modifier.testTag("recents_clear_all_btn")
                    ) {
                        Text(
                            text = "Clear all",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // 2. Open apps preview carousel (Horizontal Carousel)
                HorizontalPager(
                    state = pagerState,
                    contentPadding = PaddingValues(horizontal = 56.dp),
                    pageSpacing = 18.dp,
                    pageSize = PageSize.Fill,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) { pageIndex ->
                    val task = recentTasks.getOrNull(pageIndex) ?: return@HorizontalPager
                    val pageOffset = (
                        (pagerState.currentPage - pageIndex) + pagerState.currentPageOffsetFraction
                    ).absoluteValue

                    // Smooth zoom in/out for side cards like Pixel Launcher
                    val cardScale = lerp(
                        start = 0.90f,
                        stop = 1.0f,
                        fraction = 1f - pageOffset.coerceIn(0f, 1f)
                    )

                    RecentTaskCard(
                        task = task,
                        scale = cardScale,
                        selectedIconPack = selectedIconPack,
                        pixelThemeSettings = pixelThemeSettings,
                        onOpen = { onTaskSelected(task) },
                        onDismiss = { onTaskDismissed(task) },
                        onHeaderClick = { onSplitScreen(task) }
                    )
                }

                // 3. Bottom Pixel 10 action bar (Screenshot • Select • Split Screen)
                AnimatedVisibility(
                    visible = activeTask != null,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    activeTask?.let { task ->
                        PixelOverviewActionBar(
                            onScreenshotClick = { onTakeScreenshot(task) },
                            onSelectClick = { onSelectContent(task) },
                            onSplitClick = { onSplitScreen(task) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 20.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Individual recent task card with "Swipe Up to Dismiss" capability.
 */
@Composable
private fun RecentTaskCard(
    task: RecentAppTask,
    scale: Float,
    selectedIconPack: String?,
    pixelThemeSettings: PixelThemeSettings,
    onOpen: () -> Unit,
    onDismiss: () -> Unit,
    onHeaderClick: () -> Unit
) {
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    val offsetY = remember(task.componentName.className) { Animatable(0f) }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxHeight(0.84f)
            .offset { IntOffset(0, offsetY.value.roundToInt()) }
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                alpha = (1f - (-offsetY.value / 700f)).coerceIn(0.2f, 1f)
            }
            .pointerInput(task.taskId) {
                detectVerticalDragGestures(
                    onDragEnd = {
                        scope.launch {
                            // If user swipes card up far enough -> close app
                            if (offsetY.value < -260f && !task.isLocked) {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                    view.performHapticFeedback(HapticFeedbackConstants.GESTURE_END)
                                }
                                offsetY.animateTo(-1400f)
                                onDismiss()
                            } else {
                                // Spring back to original position
                                offsetY.animateTo(
                                    targetValue = 0f,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessMedium
                                    )
                                )
                            }
                        }
                    },
                    onVerticalDrag = { change, dragAmount ->
                        change.consume()
                        scope.launch {
                            // Allow only upward swipe (or light downward resistance)
                            val nextOffset = (offsetY.value + dragAmount).coerceAtMost(40f)
                            offsetY.snapTo(nextOffset)
                        }
                    }
                )
            }
    ) {
        // App icon + name header (clicking opens split-screen options)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(CircleShape)
                .clickable { onHeaderClick() }
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            if (task.appIcon != null) {
                Image(
                    bitmap = task.appIcon.toBitmap(96, 96).asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                )
            } else {
                SimulatedAppIcon(
                    appId = task.componentName.className,
                    size = 28.dp,
                    iconPackPackage = selectedIconPack,
                    pixelThemeSettings = pixelThemeSettings
                )
            }
            Spacer(Modifier.width(8.dp))
            Text(
                text = task.appName,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (task.isLocked) {
                Spacer(Modifier.width(6.dp))
                Icon(
                    imageVector = Icons.Rounded.Lock,
                    contentDescription = "Locked",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(14.dp)
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        // Card body (live app preview with 28dp rounded corners matching Pixel 10 screen)
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 8.dp,
            shadowElevation = 12.dp,
            modifier = Modifier
                .fillMaxSize()
                .clickable { onOpen() }
        ) {
            if (task.previewSnapshot != null) {
                Image(
                    bitmap = task.previewSnapshot.asImageBitmap(),
                    contentDescription = task.appName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                // Elegant fallback when live preview is not available
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        if (task.appIcon != null) {
                            Image(
                                bitmap = task.appIcon.toBitmap(160, 160).asImageBitmap(),
                                contentDescription = null,
                                modifier = Modifier.size(64.dp)
                            )
                        } else {
                            SimulatedAppIcon(
                                appId = task.componentName.className,
                                size = 64.dp,
                                iconPackPackage = selectedIconPack,
                                pixelThemeSettings = pixelThemeSettings
                            )
                        }
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = task.appName,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

/**
 * Pixel bottom action bar: Screenshot, Select, and Split Screen.
 */
@Composable
private fun PixelOverviewActionBar(
    onScreenshotClick: () -> Unit,
    onSelectClick: () -> Unit,
    onSplitClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        PixelActionPillButton(
            label = "Screenshot",
            icon = Icons.Rounded.Screenshot,
            testTag = "recents_screenshot_btn",
            onClick = onScreenshotClick
        )

        PixelActionPillButton(
            label = "Select",
            icon = Icons.Rounded.ContentCopy,
            testTag = "recents_select_btn",
            onClick = onSelectClick
        )

        PixelActionPillButton(
            label = "Split",
            icon = Icons.Rounded.Splitscreen,
            testTag = "recents_split_btn",
            onClick = onSplitClick
        )
    }
}

@Composable
private fun PixelActionPillButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    testTag: String,
    onClick: () -> Unit
) {
    val view = LocalView.current
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.secondaryContainer,
        tonalElevation = 4.dp,
        modifier = Modifier
            .defaultMinSize(minHeight = 48.dp)
            .clickable {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                }
                onClick()
            }
            .testTag(testTag)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}
