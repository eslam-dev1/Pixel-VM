package com.example.ui.phone

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.HomeItemEntity
import com.example.model.PixelThemeSettings
import com.example.model.SimulatedSystemState
import com.example.os.AppRegistry
import com.example.os.HomeScreenManager
import com.example.ui.components.SimulatedAppIcon
import com.example.ui.components.AtAGlanceWidget
import com.example.ui.components.PixelBatteryWidget
import com.example.ui.components.PixelBottomSearchPill
import com.example.ui.components.PixelClockHomeWidget
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    homeItems: List<HomeItemEntity>,
    homeScreenManager: HomeScreenManager,
    systemState: SimulatedSystemState,
    selectedIconPack: String? = null,
    pixelThemeSettings: PixelThemeSettings = PixelThemeSettings(),
    hiddenAppIds: Set<String> = emptySet(),
    onOpenApp: (String) -> Unit,
    onOpenDrawer: () -> Unit,
    onOpenDrawerWithQuery: (String) -> Unit = { onOpenDrawer() },
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    var openedFolder by remember { mutableStateOf<HomeItemEntity?>(null) }
    var itemToReorder by remember { mutableStateOf<HomeItemEntity?>(null) }

    BackHandler(enabled = openedFolder != null || itemToReorder != null) {
        if (openedFolder != null) {
            openedFolder = null
        } else {
            itemToReorder = null
        }
    }

    val voiceSearchLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spoken = result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()
                ?.trim()
            if (!spoken.isNullOrEmpty()) {
                onOpenDrawerWithQuery(spoken)
            }
        }
    }

    val dockItems = homeItems.filter { it.isDock }.sortedBy { it.slotIndex }
    val gridItems = remember(homeItems, hiddenAppIds) {
        homeItems
            .filter { !it.isDock && (it.targetAppId == null || it.targetAppId !in hiddenAppIds) }
            .sortedBy { it.slotIndex }
    }

    // Chunk grid items into pages of up to 12 items + 1 dedicated Pixel Widgets page
    val appPages = remember(gridItems) {
        val chunked = gridItems.chunked(12)
        if (chunked.isEmpty()) listOf(emptyList()) else chunked
    }
    val totalPages = appPages.size + 1

    val pagerState = rememberPagerState(pageCount = { totalPages })

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                var dragY = 0f
                detectVerticalDragGestures(
                    onDragStart = { dragY = 0f },
                    onVerticalDrag = { _, amount -> dragY += amount },
                    onDragEnd = {
                        if (dragY < -50f) {
                            onOpenDrawer()
                        }
                    }
                )
            }
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Extensible "At a Glance" Widget with HorizontalPager
            AtAGlanceWidget(systemState = systemState)

            // Paged App Grid & Pixel Widgets (HorizontalPager)
            Column(modifier = Modifier.weight(1f)) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) { page ->
                    if (page < appPages.size) {
                        val pageItems = appPages.getOrElse(page) { emptyList() }
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(4),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(pageItems, key = { it.id }) { item ->
                                HomeGridCell(
                                    item = item,
                                    iconPackPackage = selectedIconPack,
                                    pixelThemeSettings = pixelThemeSettings,
                                    onClick = {
                                        if (item.itemType == "FOLDER") {
                                            openedFolder = item
                                        } else if (item.targetAppId != null) {
                                            onOpenApp(item.targetAppId)
                                        }
                                    },
                                    onLongClick = {
                                        if (itemToReorder == null) {
                                            itemToReorder = item
                                        } else {
                                            scope.launch {
                                                homeScreenManager.swapItems(itemToReorder!!, item)
                                                itemToReorder = null
                                            }
                                        }
                                    },
                                    isHighlighted = itemToReorder?.id == item.id
                                )
                            }
                        }
                    } else {
                        // Dedicated Material You Widgets Page
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(top = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            PixelBatteryWidget(
                                systemState = systemState,
                                onClick = { onOpenApp("settings") }
                            )
                            PixelClockHomeWidget(
                                systemState = systemState,
                                onClick = { onOpenApp("clock") }
                            )
                        }
                    }
                }

                // Page indicator dots & quick jump to Widgets
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (i in 0 until totalPages) {
                        val isSelected = pagerState.currentPage == i
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .size(if (isSelected) 8.dp else 6.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) Color.White else Color.White.copy(alpha = 0.4f))
                                .clickable {
                                    scope.launch { pagerState.animateScrollToPage(i) }
                                }
                        )
                    }
                }
            }

            // Persistent Bottom Section: Search Pill & 4-App Dock
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Search Pill
                PixelBottomSearchPill(
                    onSearchClick = onOpenDrawer,
                    onVoiceClick = {
                        val speechIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                            putExtra(RecognizerIntent.EXTRA_PROMPT, context.getString(R.string.search_apps))
                        }
                        try {
                            if (speechIntent.resolveActivity(context.packageManager) != null) {
                                voiceSearchLauncher.launch(speechIntent)
                            } else {
                                Toast.makeText(
                                    context,
                                    context.getString(R.string.voice_search_unavailable),
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        } catch (_: Exception) {
                            Toast.makeText(
                                context,
                                context.getString(R.string.voice_search_unavailable),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    },
                    onLensClick = { onOpenApp("camera") },
                    modifier = Modifier.testTag("home_search_pill")
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Dock (4 Apps)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    dockItems.forEach { dockItem ->
                        dockItem.targetAppId?.let { appId ->
                            SimulatedAppIcon(
                                appId = appId,
                                size = 56.dp,
                                iconPackPackage = selectedIconPack,
                                pixelThemeSettings = pixelThemeSettings,
                                modifier = Modifier
                                    .clickable { onOpenApp(appId) }
                                    .testTag("dock_app_$appId")
                            )
                        }
                    }
                }
            }
        }
    }

    // Folder dialog
    openedFolder?.let { folder ->
        val appIds = folder.folderAppIdsCsv?.split(",")?.filter { it.isNotBlank() && it !in hiddenAppIds } ?: emptyList()
        AlertDialog(
            onDismissRequest = { openedFolder = null },
            title = { Text(folder.folderTitle ?: "Folder") },
            text = {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier.fillMaxWidth().height(180.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(appIds) { appId ->
                        val appDef = AppRegistry.getApp(appId)
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.clickable {
                                openedFolder = null
                                onOpenApp(appId)
                            }
                        ) {
                            SimulatedAppIcon(
                                appId = appId,
                                size = 50.dp,
                                iconPackPackage = selectedIconPack,
                                pixelThemeSettings = pixelThemeSettings
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = appDef?.let { stringResource(it.titleRes) } ?: appId,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { openedFolder = null }) {
                    Text(stringResource(R.string.close_button))
                }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HomeGridCell(
    item: HomeItemEntity,
    iconPackPackage: String? = null,
    pixelThemeSettings: PixelThemeSettings = PixelThemeSettings(),
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    isHighlighted: Boolean
) {
    val appDef = item.targetAppId?.let { AppRegistry.getApp(it) }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(54.dp)
                .then(
                    if (isHighlighted) {
                        Modifier.background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f), CircleShape)
                    } else {
                        Modifier
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            if (item.itemType == "FOLDER") {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = 0.35f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Folder,
                        contentDescription = "Folder",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
            } else if (item.targetAppId != null) {
                SimulatedAppIcon(
                    appId = item.targetAppId,
                    size = 54.dp,
                    iconPackPackage = iconPackPackage,
                    pixelThemeSettings = pixelThemeSettings,
                    modifier = Modifier.testTag("home_app_${item.targetAppId}")
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = when {
                item.itemType == "FOLDER" -> item.folderTitle ?: "Folder"
                appDef != null -> stringResource(appDef.titleRes)
                else -> item.targetAppId ?: ""
            },
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
