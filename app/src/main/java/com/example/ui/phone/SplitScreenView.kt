package com.example.ui.phone

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.PixelSimDatabase
import com.example.data.SettingsDataStore
import com.example.model.PixelThemeSettings
import com.example.os.AppRegistry
import com.example.os.SplitScreenState
import com.example.os.SystemStateHolder
import com.example.os.WindowManager
import com.example.ui.components.SimulatedAppIcon

@Composable
fun SplitScreenView(
    splitState: SplitScreenState,
    database: PixelSimDatabase,
    settingsDataStore: SettingsDataStore,
    systemStateHolder: SystemStateHolder,
    windowManager: WindowManager,
    selectedIconPack: String?,
    pixelThemeSettings: PixelThemeSettings,
    hiddenAppIds: Set<String> = emptySet(),
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    var isDragging by remember { mutableStateOf(false) }
    var dragRatio by remember(splitState.splitRatio) { mutableFloatStateOf(splitState.splitRatio) }

    val animatedRatio by animateFloatAsState(
        targetValue = if (isDragging) dragRatio else splitState.splitRatio,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "split_ratio"
    )

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag("split_screen_container")
    ) {
        val totalHeightPx = with(density) { maxHeight.toPx() }.coerceAtLeast(1f)
        val dividerHeightDp = 48.dp
        val contentHeightDp = (maxHeight - dividerHeightDp).coerceAtLeast(120.dp)
        val topHeightDp = contentHeightDp * animatedRatio
        val bottomHeightDp = contentHeightDp * (1f - animatedRatio)

        Column(modifier = Modifier.fillMaxSize()) {
            // Top App Pane
            Surface(
                shape = RoundedCornerShape(bottomStart = 18.dp, bottomEnd = 18.dp),
                color = MaterialTheme.colorScheme.background,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(topHeightDp)
                    .testTag("split_top_pane")
            ) {
                WindowManagerView(
                    activeAppId = splitState.topAppId,
                    database = database,
                    settingsDataStore = settingsDataStore,
                    systemStateHolder = systemStateHolder,
                    windowManager = windowManager,
                    applyStatusBarPadding = false
                )
            }

            // Draggable Divider Bar with Swap & Close
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(dividerHeightDp)
                    .background(Color(0xFF141519))
                    .pointerInput(totalHeightPx) {
                        detectVerticalDragGestures(
                            onDragStart = { isDragging = true },
                            onVerticalDrag = { change, dragAmount ->
                                change.consume()
                                val deltaRatio = dragAmount / totalHeightPx
                                dragRatio = (dragRatio + deltaRatio).coerceIn(0.25f, 0.75f)
                            },
                            onDragEnd = {
                                isDragging = false
                                windowManager.updateSplitRatio(dragRatio, snap = true)
                            },
                            onDragCancel = {
                                isDragging = false
                            }
                        )
                    }
                    .padding(horizontal = 12.dp)
                    .testTag("split_divider_handle"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = { windowManager.swapSplitPanes() },
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("split_swap_btn")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.SwapVert,
                        contentDescription = stringResource(R.string.split_swap_apps),
                        tint = Color.White.copy(alpha = 0.85f)
                    )
                }

                Box(
                    modifier = Modifier
                        .width(48.dp)
                        .height(5.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.65f))
                )

                IconButton(
                    onClick = { windowManager.exitSplitScreen(openTopApp = true) },
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("split_exit_btn")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = stringResource(R.string.split_exit),
                        tint = Color.White.copy(alpha = 0.85f)
                    )
                }
            }

            // Bottom App Pane or Second-App Picker
            Surface(
                shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(bottomHeightDp)
                    .testTag("split_bottom_pane")
            ) {
                val bottomId = splitState.bottomAppId
                if (bottomId != null) {
                    WindowManagerView(
                        activeAppId = bottomId,
                        database = database,
                        settingsDataStore = settingsDataStore,
                        systemStateHolder = systemStateHolder,
                        windowManager = windowManager,
                        applyStatusBarPadding = false
                    )
                } else {
                    SplitAppPicker(
                        excludeAppId = splitState.topAppId,
                        hiddenAppIds = hiddenAppIds,
                        selectedIconPack = selectedIconPack,
                        pixelThemeSettings = pixelThemeSettings,
                        onSelectApp = { chosenId ->
                            windowManager.setSplitBottomApp(chosenId)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun SplitAppPicker(
    excludeAppId: String?,
    hiddenAppIds: Set<String>,
    selectedIconPack: String?,
    pixelThemeSettings: PixelThemeSettings,
    onSelectApp: (String) -> Unit
) {
    val candidates = remember(excludeAppId, hiddenAppIds) {
        AppRegistry.APPS.filter { it.id != excludeAppId && it.id !in hiddenAppIds }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = stringResource(R.string.split_pick_second_app),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(12.dp))
        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            contentPadding = PaddingValues(4.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(candidates, key = { it.id }) { app ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { onSelectApp(app.id) }
                        .padding(6.dp)
                        .testTag("split_pick_${app.id}")
                ) {
                    SimulatedAppIcon(
                        appId = app.id,
                        size = 48.dp,
                        useThemedMonochrome = pixelThemeSettings.useMaterialYouMonochrome,
                        iconPackPackage = selectedIconPack,
                        pixelThemeSettings = pixelThemeSettings
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = stringResource(app.titleRes),
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}
