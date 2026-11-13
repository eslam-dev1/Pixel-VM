package com.example.ui.components

import android.graphics.Bitmap
import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Redo
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.Brush
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Crop
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import kotlin.math.max
import kotlin.math.min

enum class MarkupTool {
    PEN,
    HIGHLIGHTER
}

data class NormalizedPoint(
    val x: Float,
    val y: Float
)

data class MarkupStroke(
    val points: List<NormalizedPoint>,
    val colorArgb: Int,
    val strokeWidthRatio: Float,
    val isHighlighter: Boolean
)

data class NormalizedCropRect(
    val left: Float = 0f,
    val top: Float = 0f,
    val right: Float = 1f,
    val bottom: Float = 1f
) {
    fun clamped(): NormalizedCropRect {
        val l = left.coerceIn(0f, 0.8f)
        val t = top.coerceIn(0f, 0.8f)
        val r = right.coerceIn(l + 0.15f, 1f)
        val b = bottom.coerceIn(t + 0.15f, 1f)
        return NormalizedCropRect(l, t, r, b)
    }

    fun inset(dxLeft: Float, dyTop: Float, dxRight: Float, dyBottom: Float): NormalizedCropRect {
        return NormalizedCropRect(
            left = left + dxLeft,
            top = top + dyTop,
            right = right + dxRight,
            bottom = bottom + dyBottom
        ).clamped()
    }
}

/**
 * Pure Kotlin state manager for Pixel Markup stroke history & crop math.
 */
class MarkupHistoryManager {
    private val _strokes = mutableListOf<MarkupStroke>()
    private val _redoStack = mutableListOf<MarkupStroke>()

    val strokes: List<MarkupStroke> get() = _strokes.toList()
    val canUndo: Boolean get() = _strokes.isNotEmpty()
    val canRedo: Boolean get() = _redoStack.isNotEmpty()

    fun addStroke(stroke: MarkupStroke) {
        if (stroke.points.isEmpty()) return
        _strokes.add(stroke)
        _redoStack.clear()
    }

    fun undo(): MarkupStroke? {
        if (_strokes.isEmpty()) return null
        val removed = _strokes.removeAt(_strokes.lastIndex)
        _redoStack.add(removed)
        return removed
    }

    fun redo(): MarkupStroke? {
        if (_redoStack.isEmpty()) return null
        val restored = _redoStack.removeAt(_redoStack.lastIndex)
        _strokes.add(restored)
        return restored
    }

    fun clear() {
        _strokes.clear()
        _redoStack.clear()
    }
}

fun renderMarkupToBitmap(
    source: Bitmap,
    strokes: List<MarkupStroke>,
    crop: NormalizedCropRect
): Bitmap {
    val working = source.copy(Bitmap.Config.ARGB_8888, true)
    val canvas = android.graphics.Canvas(working)
    val w = working.width.toFloat()
    val h = working.height.toFloat()

    strokes.forEach { stroke ->
        if (stroke.points.isEmpty()) return@forEach
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = stroke.colorArgb
            alpha = if (stroke.isHighlighter) 115 else 255
            style = Paint.Style.STROKE
            strokeWidth = max(4f, stroke.strokeWidthRatio * min(w, h))
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
        val path = android.graphics.Path()
        stroke.points.forEachIndexed { idx, pt ->
            val px = pt.x.coerceIn(0f, 1f) * w
            val py = pt.y.coerceIn(0f, 1f) * h
            if (idx == 0) path.moveTo(px, py) else path.lineTo(px, py)
        }
        if (stroke.points.size == 1) {
            val pt = stroke.points.first()
            canvas.drawPoint(pt.x * w, pt.y * h, paint)
        } else {
            canvas.drawPath(path, paint)
        }
    }

    val safeCrop = crop.clamped()
    val cropX = (safeCrop.left * working.width).toInt().coerceIn(0, working.width - 1)
    val cropY = (safeCrop.top * working.height).toInt().coerceIn(0, working.height - 1)
    val cropW = ((safeCrop.right - safeCrop.left) * working.width).toInt().coerceIn(1, working.width - cropX)
    val cropH = ((safeCrop.bottom - safeCrop.top) * working.height).toInt().coerceIn(1, working.height - cropY)

    return Bitmap.createBitmap(working, cropX, cropY, cropW, cropH)
}

@Composable
fun ScreenshotMarkupEditor(
    bitmap: Bitmap,
    onSave: (Bitmap) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val swatches = remember {
        listOf(
            Color(0xFFEA4335), // Pixel Red
            Color(0xFF4285F4), // Pixel Blue
            Color(0xFFFBBC04), // Pixel Yellow
            Color(0xFF34A853), // Pixel Green
            Color.White
        )
    }

    var selectedTool by remember { mutableStateOf(MarkupTool.PEN) }
    var selectedColor by remember { mutableStateOf(swatches.first()) }
    var isCropMode by remember { mutableStateOf(false) }
    var cropRect by remember { mutableStateOf(NormalizedCropRect()) }

    val history = remember(bitmap) { MarkupHistoryManager() }
    val renderedStrokes = remember(bitmap) { mutableStateListOf<MarkupStroke>() }
    var activePoints by remember { mutableStateOf<List<NormalizedPoint>>(emptyList()) }
    var redoCount by remember { mutableStateOf(0) }

    Surface(
        color = Color(0xFF111216),
        modifier = modifier
            .fillMaxSize()
            .testTag("screenshot_markup_editor")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Toolbar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("markup_close_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = stringResource(R.string.close_button),
                            tint = Color.White
                        )
                    }
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = stringResource(R.string.markup_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = {
                            if (history.undo() != null) {
                                renderedStrokes.clear()
                                renderedStrokes.addAll(history.strokes)
                                redoCount++
                            }
                        },
                        enabled = renderedStrokes.isNotEmpty(),
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("markup_undo_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.Undo,
                            contentDescription = stringResource(R.string.markup_undo),
                            tint = if (renderedStrokes.isNotEmpty()) Color.White else Color.White.copy(alpha = 0.35f)
                        )
                    }
                    IconButton(
                        onClick = {
                            if (history.redo() != null) {
                                renderedStrokes.clear()
                                renderedStrokes.addAll(history.strokes)
                                redoCount++
                            }
                        },
                        enabled = history.canRedo || redoCount > 0 && history.canRedo,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("markup_redo_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.Redo,
                            contentDescription = stringResource(R.string.markup_redo),
                            tint = if (history.canRedo) Color.White else Color.White.copy(alpha = 0.35f)
                        )
                    }
                    Spacer(Modifier.width(4.dp))
                    Button(
                        onClick = {
                            val result = renderMarkupToBitmap(bitmap, renderedStrokes.toList(), cropRect)
                            onSave(result)
                        },
                        modifier = Modifier.testTag("markup_save_btn")
                    ) {
                        Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.save_button))
                    }
                }
            }

            // Center Canvas
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                val aspect = (bitmap.width.toFloat() / bitmap.height.coerceAtLeast(1).toFloat()).coerceIn(0.35f, 2.2f)
                BoxWithConstraints(
                    modifier = Modifier
                        .aspectRatio(aspect)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.Black)
                ) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = stringResource(R.string.markup_title),
                        contentScale = ContentScale.FillBounds,
                        modifier = Modifier.fillMaxSize()
                    )

                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(isCropMode, selectedTool, selectedColor) {
                                detectDragGestures(
                                    onDragStart = { start ->
                                        if (isCropMode) return@detectDragGestures
                                        val nx = (start.x / size.width).coerceIn(0f, 1f)
                                        val ny = (start.y / size.height).coerceIn(0f, 1f)
                                        activePoints = listOf(NormalizedPoint(nx, ny))
                                    },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        if (isCropMode) {
                                            val dx = dragAmount.x / size.width
                                            val dy = dragAmount.y / size.height
                                            val pos = change.position
                                            val nearLeft = pos.x < size.width * 0.4f
                                            val nearTop = pos.y < size.height * 0.4f
                                            cropRect = cropRect.inset(
                                                dxLeft = if (nearLeft) dx else 0f,
                                                dyTop = if (nearTop) dy else 0f,
                                                dxRight = if (!nearLeft) dx else 0f,
                                                dyBottom = if (!nearTop) dy else 0f
                                            )
                                        } else {
                                            val nx = (change.position.x / size.width).coerceIn(0f, 1f)
                                            val ny = (change.position.y / size.height).coerceIn(0f, 1f)
                                            activePoints = activePoints + NormalizedPoint(nx, ny)
                                        }
                                    },
                                    onDragEnd = {
                                        if (!isCropMode && activePoints.isNotEmpty()) {
                                            val stroke = MarkupStroke(
                                                points = activePoints,
                                                colorArgb = selectedColor.toArgb(),
                                                strokeWidthRatio = if (selectedTool == MarkupTool.HIGHLIGHTER) 0.036f else 0.012f,
                                                isHighlighter = selectedTool == MarkupTool.HIGHLIGHTER
                                            )
                                            history.addStroke(stroke)
                                            renderedStrokes.clear()
                                            renderedStrokes.addAll(history.strokes)
                                            activePoints = emptyList()
                                        }
                                    }
                                )
                            }
                    ) {
                        val allStrokes = if (activePoints.isNotEmpty()) {
                            renderedStrokes + MarkupStroke(
                                points = activePoints,
                                colorArgb = selectedColor.toArgb(),
                                strokeWidthRatio = if (selectedTool == MarkupTool.HIGHLIGHTER) 0.036f else 0.012f,
                                isHighlighter = selectedTool == MarkupTool.HIGHLIGHTER
                            )
                        } else {
                            renderedStrokes
                        }

                        allStrokes.forEach { stroke ->
                            if (stroke.points.isEmpty()) return@forEach
                            val path = Path()
                            stroke.points.forEachIndexed { idx, pt ->
                                val px = pt.x * size.width
                                val py = pt.y * size.height
                                if (idx == 0) path.moveTo(px, py) else path.lineTo(px, py)
                            }
                            val strokeColor = Color(stroke.colorArgb).copy(
                                alpha = if (stroke.isHighlighter) 0.45f else 1f
                            )
                            drawPath(
                                path = path,
                                color = strokeColor,
                                style = Stroke(
                                    width = max(4f, stroke.strokeWidthRatio * min(size.width, size.height)),
                                    cap = StrokeCap.Round,
                                    join = StrokeJoin.Round
                                )
                            )
                        }

                        // Draw crop overlay & corner markers
                        val safe = cropRect.clamped()
                        val leftPx = safe.left * size.width
                        val topPx = safe.top * size.height
                        val widthPx = (safe.right - safe.left) * size.width
                        val heightPx = (safe.bottom - safe.top) * size.height

                        if (isCropMode || safe != NormalizedCropRect()) {
                            // Dim outer areas
                            drawRect(
                                color = Color.Black.copy(alpha = 0.55f),
                                topLeft = Offset.Zero,
                                size = Size(size.width, topPx)
                            )
                            drawRect(
                                color = Color.Black.copy(alpha = 0.55f),
                                topLeft = Offset(0f, topPx + heightPx),
                                size = Size(size.width, size.height - (topPx + heightPx))
                            )
                            drawRect(
                                color = Color.Black.copy(alpha = 0.55f),
                                topLeft = Offset(0f, topPx),
                                size = Size(leftPx, heightPx)
                            )
                            drawRect(
                                color = Color.Black.copy(alpha = 0.55f),
                                topLeft = Offset(leftPx + widthPx, topPx),
                                size = Size(size.width - (leftPx + widthPx), heightPx)
                            )

                            drawRect(
                                color = Color.White,
                                topLeft = Offset(leftPx, topPx),
                                size = Size(widthPx, heightPx),
                                style = Stroke(width = 4f)
                            )
                        }
                    }
                }
            }

            // Bottom Tool & Color Controls
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    swatches.forEach { swatch ->
                        val selected = selectedColor == swatch && !isCropMode
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 6.dp)
                                .size(if (selected) 36.dp else 30.dp)
                                .clip(CircleShape)
                                .background(swatch)
                                .border(
                                    width = if (selected) 3.dp else 1.dp,
                                    color = if (selected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.4f),
                                    shape = CircleShape
                                )
                                .clickable {
                                    isCropMode = false
                                    selectedColor = swatch
                                }
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilledTonalIconButton(
                        onClick = {
                            isCropMode = false
                            selectedTool = MarkupTool.PEN
                        },
                        modifier = Modifier.size(48.dp).testTag("markup_pen_btn")
                    ) {
                        Icon(
                            Icons.Rounded.Edit,
                            contentDescription = stringResource(R.string.markup_pen),
                            tint = if (!isCropMode && selectedTool == MarkupTool.PEN) MaterialTheme.colorScheme.primary else Color.White
                        )
                    }

                    FilledTonalIconButton(
                        onClick = {
                            isCropMode = false
                            selectedTool = MarkupTool.HIGHLIGHTER
                        },
                        modifier = Modifier.size(48.dp).testTag("markup_highlighter_btn")
                    ) {
                        Icon(
                            Icons.Rounded.Brush,
                            contentDescription = stringResource(R.string.markup_highlighter),
                            tint = if (!isCropMode && selectedTool == MarkupTool.HIGHLIGHTER) MaterialTheme.colorScheme.primary else Color.White
                        )
                    }

                    FilledTonalIconButton(
                        onClick = { isCropMode = !isCropMode },
                        modifier = Modifier.size(48.dp).testTag("markup_crop_btn")
                    ) {
                        Icon(
                            Icons.Rounded.Crop,
                            contentDescription = stringResource(R.string.markup_crop),
                            tint = if (isCropMode) MaterialTheme.colorScheme.primary else Color.White
                        )
                    }

                    if (cropRect != NormalizedCropRect()) {
                        TextButton(onClick = { cropRect = NormalizedCropRect() }) {
                            Text(stringResource(R.string.reset), color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
    }
}
