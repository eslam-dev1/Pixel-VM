package com.example.ui.settings

import android.content.Context
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import coil.compose.rememberAsyncImagePainter
import com.example.R
import com.example.data.SettingsDataStore
import com.example.model.PixelIconShape
import com.example.model.PixelThemeSettings
import kotlinx.coroutines.launch

@Composable
fun IconPackSettings(
    settingsDataStore: SettingsDataStore,
    currentIconPack: String?,
    pixelThemeSettings: PixelThemeSettings = PixelThemeSettings(),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val resolver = remember(context) { com.example.di.IconPackModule.provideIconPackResolver(context) }
    val installedPacks = remember(resolver) { resolver.getInstalledIconPacks() }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Material You Monochrome & Shape Controls
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = stringResource(R.string.icon_style_title),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(stringResource(R.string.themed_icons_title), fontWeight = FontWeight.Medium)
                            Text(
                                text = stringResource(R.string.themed_icons_desc),
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                        Switch(
                            checked = pixelThemeSettings.useMaterialYouMonochrome,
                            onCheckedChange = { enabled ->
                                scope.launch { settingsDataStore.setUseMaterialYouMonochrome(enabled) }
                            },
                            modifier = Modifier.testTag("themed_icons_switch")
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(stringResource(R.string.tint_pack_icons_title), fontWeight = FontWeight.Medium)
                            Text(
                                text = stringResource(R.string.tint_pack_icons_desc),
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                        Switch(
                            checked = pixelThemeSettings.tintPackIconsWithMaterialYou,
                            onCheckedChange = { enabled ->
                                scope.launch { settingsDataStore.setTintPackIconsWithMaterialYou(enabled) }
                            },
                            modifier = Modifier.testTag("tint_pack_icons_switch")
                        )
                    }

                    Text(
                        text = stringResource(R.string.icon_shape_label),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PixelIconShape.entries.forEach { shape ->
                            val isSelected = pixelThemeSettings.iconShape == shape
                            FilterChip(
                                selected = isSelected,
                                onClick = { scope.launch { settingsDataStore.setPixelIconShape(shape) } },
                                label = {
                                    Text(
                                        text = when (shape) {
                                            PixelIconShape.CIRCLE -> stringResource(R.string.shape_circle)
                                            PixelIconShape.SQUIRCLE -> stringResource(R.string.shape_squircle)
                                            PixelIconShape.TEARDROP -> stringResource(R.string.shape_teardrop)
                                            PixelIconShape.ROUNDED_SQUARE -> stringResource(R.string.shape_rounded)
                                        },
                                        fontSize = 11.sp
                                    )
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("icon_shape_${shape.name.lowercase()}")
                            )
                        }
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        scope.launch { settingsDataStore.setIconPack(null) }
                    }
                    .border(
                        width = if (currentIconPack == null) 2.dp else 0.dp,
                        color = if (currentIconPack == null) MaterialTheme.colorScheme.primary else Color.Transparent,
                        shape = RoundedCornerShape(16.dp)
                    ),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text("Default (Pixel)", fontWeight = FontWeight.Bold)
                        Text("Original Pixel-style icons", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                    }
                }
            }
        }

        items(installedPacks) { pack ->
            val isSelected = pack.packageName == currentIconPack
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        scope.launch { settingsDataStore.setIconPack(pack.packageName) }
                    }
                    .border(
                        width = if (isSelected) 2.dp else 0.dp,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                        shape = RoundedCornerShape(16.dp)
                    ),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val packIcon = remember(pack.packageName) {
                        try { context.packageManager.getApplicationIcon(pack.packageName) } catch (e: Exception) { null }
                    }

                    if (packIcon != null) {
                        Image(
                            painter = rememberAsyncImagePainter(packIcon),
                            contentDescription = null,
                            modifier = Modifier.size(40.dp).clip(CircleShape)
                        )
                    } else {
                        Box(modifier = Modifier.size(40.dp).background(Color.Gray, CircleShape))
                    }

                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(pack.label, fontWeight = FontWeight.Bold)
                        Text(pack.packageName, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                    }
                }
            }
        }

        if (installedPacks.isEmpty()) {
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text("No icon packs found on device", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                }
            }
        }
    }
}
