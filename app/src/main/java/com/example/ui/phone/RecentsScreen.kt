package com.example.ui.phone

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.os.AppRegistry
import com.example.ui.components.SimulatedAppIcon

@Composable
fun RecentsScreen(
    runningApps: List<String>,
    onSelectApp: (String) -> Unit,
    onCloseApp: (String) -> Unit,
    onClearAll: () -> Unit,
    selectedIconPack: String? = null,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.85f))
            .padding(top = 40.dp, bottom = 48.dp)
    ) {
        if (runningApps.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.no_recent_apps),
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 18.sp
                )
            }
        } else {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(horizontal = 48.dp),
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    items(runningApps, key = { it }) { appId ->
                        val appDef = AppRegistry.getApp(appId)
                        Card(
                            modifier = Modifier
                                .width(240.dp)
                                .fillMaxHeight(0.72f)
                                .clickable { onSelectApp(appId) }
                                .testTag("recent_card_$appId"),
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF222328))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        SimulatedAppIcon(
                                            appId = appId,
                                            size = 28.dp,
                                            iconPackPackage = selectedIconPack
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = appDef?.let { stringResource(it.titleRes) } ?: appId,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                    }

                                    IconButton(
                                        onClick = { onCloseApp(appId) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = "Close App",
                                            tint = Color.White.copy(alpha = 0.6f)
                                        )
                                    }
                                }

                                // Simulated App Snapshot preview body
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f)
                                        .padding(vertical = 12.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(Color(0xFF16171B)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        SimulatedAppIcon(
                                            appId = appId,
                                            size = 48.dp,
                                            iconPackPackage = selectedIconPack
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = appDef?.let { stringResource(it.titleRes) } ?: appId,
                                            color = Color.White.copy(alpha = 0.5f),
                                            fontSize = 12.sp
                                        )
                                    }
                                }

                                Text(
                                    text = "Swipe or tap to open",
                                    fontSize = 11.sp,
                                    color = Color.White.copy(alpha = 0.4f),
                                    modifier = Modifier.align(Alignment.CenterHorizontally)
                                )
                            }
                        }
                    }
                }

                // Clear all button
                Button(
                    onClick = onClearAll,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.padding(top = 16.dp).testTag("recents_clear_all_btn")
                ) {
                    Text(stringResource(R.string.clear_all), fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
