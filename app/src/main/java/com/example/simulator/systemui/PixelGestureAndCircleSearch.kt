package com.example.simulator.systemui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.view.HapticFeedbackConstants
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.SettingsDataStore
import com.example.os.SmartSelectSnippet

/**
 * Pixel bottom gesture navigation pill supporting swipe up to Home / Recents
 * and long-press to invoke Circle to Search.
 */
@Composable
fun PixelBottomGesturePill(
    onSwipeUpHome: () -> Unit,
    onSwipeUpRecents: () -> Unit,
    onTriggerCircleToSearch: () -> Unit,
    pillColor: Color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    var dragDistanceY by remember { mutableFloatStateOf(0f) }

    Box(
        contentAlignment = Alignment.BottomCenter,
        modifier = modifier
            .width(160.dp)
            .defaultMinSize(minHeight = 48.dp)
            .padding(bottom = 10.dp)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onSwipeUpHome() },
                    onLongPress = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                        }
                        onTriggerCircleToSearch()
                    }
                )
            }
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragStart = { dragDistanceY = 0f },
                    onDragEnd = {
                        when {
                            dragDistanceY < -140f -> onSwipeUpHome()
                            dragDistanceY < -45f -> onSwipeUpRecents()
                        }
                        dragDistanceY = 0f
                    },
                    onVerticalDrag = { change, dragAmount ->
                        change.consume()
                        dragDistanceY += dragAmount
                    }
                )
            }
            .testTag("pixel_bottom_gesture_pill")
    ) {
        Box(
            modifier = Modifier
                .width(116.dp)
                .height(4.5.dp)
                .clip(CircleShape)
                .background(pillColor)
        )
    }
}

/**
 * Interactive Circle to Search / Gemini Overlay with lasso highlighting,
 * contextual query extraction, web search launch, and screen translation.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PixelCircleToSearchOverlay(
    isVisible: Boolean,
    onDismiss: () -> Unit,
    onRegionCircled: (Rect) -> Unit,
    onTranslateScreen: () -> Unit,
    detectedSnippets: List<SmartSelectSnippet> = emptyList(),
    searchEngine: String = "google",
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(tween(250)),
        exit = fadeOut(tween(200)),
        modifier = modifier.fillMaxSize()
    ) {
        val strokePoints = remember { mutableStateListOf<Offset>() }
        var circledRect by remember { mutableStateOf<Rect?>(null) }
        var queryText by remember { mutableStateOf("") }
        var translateModeActive by remember { mutableStateOf(false) }

        LaunchedEffect(isVisible) {
            if (!isVisible) {
                strokePoints.clear()
                circledRect = null
                queryText = ""
                translateModeActive = false
            }
        }

        val infiniteTransition = rememberInfiniteTransition(label = "GeminiGlow")
        val glowAlpha by infiniteTransition.animateFloat(
            initialValue = 0.22f,
            targetValue = 0.42f,
            animationSpec = infiniteRepeatable(
                animation = tween(1400, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "GlowPulse"
        )

        val geminiBrush = remember {
            Brush.sweepGradient(
                colors = listOf(
                    Color(0xFF4285F4),
                    Color(0xFF9B72CB),
                    Color(0xFFD96570),
                    Color(0xFF34A853),
                    Color(0xFF4285F4)
                )
            )
        }

        fun launchWebSearch(query: String) {
            val clean = query.trim()
            if (clean.isEmpty()) return
            val url = SettingsDataStore.getSearchUrl(searchEngine, clean)
            try {
                context.startActivity(
                    Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                )
            } catch (_: Exception) {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                clipboard?.setPrimaryClip(ClipData.newPlainText("CircleSearch", clean))
                Toast.makeText(
                    context,
                    context.getString(R.string.smart_select_copied, clean),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.35f))
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            strokePoints.clear()
                            circledRect = null
                            strokePoints.add(offset)
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            strokePoints.add(change.position)
                        },
                        onDragEnd = {
                            if (strokePoints.size > 5) {
                                val minX = strokePoints.minOf { it.x }
                                val maxX = strokePoints.maxOf { it.x }
                                val minY = strokePoints.minOf { it.y }
                                val maxY = strokePoints.maxOf { it.y }
                                val rect = Rect(minX, minY, maxX, maxY)
                                circledRect = rect
                                if (queryText.isBlank()) {
                                    queryText = detectedSnippets.firstOrNull()?.text.orEmpty()
                                }
                                onRegionCircled(rect)
                            }
                        }
                    )
                }
                .testTag("circle_to_search_overlay")
        ) {
            // Animated screen border glow + drawn lasso & circled region box
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawRect(
                    brush = geminiBrush,
                    alpha = glowAlpha,
                    style = Stroke(width = 24.dp.toPx())
                )

                circledRect?.let { rect ->
                    drawRoundRect(
                        color = Color(0xFF8AB4F8).copy(alpha = 0.16f),
                        topLeft = Offset(rect.left, rect.top),
                        size = Size(rect.width.coerceAtLeast(16f), rect.height.coerceAtLeast(16f)),
                        cornerRadius = CornerRadius(18.dp.toPx(), 18.dp.toPx())
                    )
                    drawRoundRect(
                        brush = geminiBrush,
                        topLeft = Offset(rect.left, rect.top),
                        size = Size(rect.width.coerceAtLeast(16f), rect.height.coerceAtLeast(16f)),
                        cornerRadius = CornerRadius(18.dp.toPx(), 18.dp.toPx()),
                        style = Stroke(width = 3.dp.toPx())
                    )
                }

                if (strokePoints.size > 1) {
                    val path = Path().apply {
                        moveTo(strokePoints.first().x, strokePoints.first().y)
                        strokePoints.drop(1).forEach { lineTo(it.x, it.y) }
                    }
                    drawPath(
                        path = path,
                        brush = geminiBrush,
                        style = Stroke(
                            width = 6.dp.toPx(),
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )
                }
            }

            // Close Circle to Search button
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(16.dp)
                    .size(48.dp)
                    .align(Alignment.TopStart)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .testTag("circle_to_search_close_btn")
            ) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = stringResource(R.string.circle_to_search_close)
                )
            }

            // Bottom Interactive Results Sheet & Search Pill
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (circledRect != null || translateModeActive) {
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        tonalElevation = 8.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = false) {}
                            .testTag("circle_to_search_results_card")
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = if (translateModeActive) {
                                    stringResource(R.string.circle_to_search_translated_mode)
                                } else {
                                    stringResource(R.string.circle_to_search_region_selected)
                                },
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )

                            if (detectedSnippets.isNotEmpty()) {
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    detectedSnippets.take(4).forEach { snippet ->
                                        AssistChip(
                                            onClick = { queryText = snippet.text },
                                            label = {
                                                Text(
                                                    text = snippet.text,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        )
                                    }
                                }
                            }

                            OutlinedTextField(
                                value = queryText,
                                onValueChange = { queryText = it },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("circle_to_search_query_input"),
                                placeholder = { Text(stringResource(R.string.circle_to_search_prompt)) }
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = {
                                        val copyVal = queryText.ifBlank {
                                            detectedSnippets.firstOrNull()?.text.orEmpty()
                                        }
                                        if (copyVal.isNotEmpty()) {
                                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                            clipboard?.setPrimaryClip(ClipData.newPlainText("CircleSearch", copyVal))
                                            Toast.makeText(
                                                context,
                                                context.getString(R.string.smart_select_copied, copyVal),
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    },
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.ContentCopy,
                                        contentDescription = stringResource(R.string.smart_select_copy)
                                    )
                                }

                                if (translateModeActive) {
                                    Button(
                                        onClick = {
                                            val textToTranslate = queryText.ifBlank {
                                                detectedSnippets.joinToString(" • ") { it.text }.ifBlank { "PixelSim" }
                                            }
                                            val targetLang = if (context.resources.configuration.locales[0].language == "ar") "en" else "ar"
                                            val translateUrl = "https://translate.google.com/?sl=auto&tl=$targetLang&text=${Uri.encode(textToTranslate)}"
                                            try {
                                                context.startActivity(
                                                    Intent(Intent.ACTION_VIEW, Uri.parse(translateUrl)).apply {
                                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                                    }
                                                )
                                            } catch (_: Exception) {
                                                Toast.makeText(context, textToTranslate, Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        modifier = Modifier.testTag("circle_to_search_open_translate_btn")
                                    ) {
                                        Icon(Icons.Rounded.Translate, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text(stringResource(R.string.circle_to_search_open_translate))
                                    }
                                } else {
                                    Button(
                                        onClick = {
                                            launchWebSearch(
                                                queryText.ifBlank {
                                                    detectedSnippets.firstOrNull()?.text ?: "Google Pixel"
                                                }
                                            )
                                        },
                                        modifier = Modifier.testTag("circle_to_search_web_btn")
                                    ) {
                                        Icon(Icons.Rounded.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text(stringResource(R.string.circle_to_search_web))
                                    }
                                }
                            }
                        }
                    }
                }

                // Bottom Search Pill
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    tonalElevation = 8.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 48.dp)
                        .clickable {
                            if (circledRect == null) {
                                val fallback = detectedSnippets.firstOrNull()?.text.orEmpty()
                                queryText = fallback
                                circledRect = Rect(0f, 0f, 100f, 100f)
                            }
                        }
                        .testTag("circle_to_search_pill")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AutoAwesome,
                            contentDescription = null,
                            tint = Color(0xFF8AB4F8),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = stringResource(R.string.circle_to_search_prompt),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = {
                                translateModeActive = !translateModeActive
                                if (translateModeActive && queryText.isBlank()) {
                                    queryText = detectedSnippets.joinToString(" • ") { it.text }
                                }
                                onTranslateScreen()
                            },
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("circle_to_search_translate_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Translate,
                                contentDescription = stringResource(R.string.circle_to_search_translate),
                                tint = if (translateModeActive) {
                                    MaterialTheme.colorScheme.tertiary
                                } else {
                                    MaterialTheme.colorScheme.primary
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
