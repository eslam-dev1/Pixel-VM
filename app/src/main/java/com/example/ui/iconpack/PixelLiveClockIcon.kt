package com.example.ui.iconpack

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.delay
import java.util.Calendar

/**
 * Renders an authentic Google Pixel Launcher live analog clock icon.
 *
 * @param modifier Modifier for sizing and clipping (e.g., PixelIconShape).
 * @param iconSize Size of the icon (default 48.dp).
 * @param smoothSecondHand True for smooth 120Hz sweep, false for authentic 1-second quartz ticks.
 * @param useMaterialYouTheme True to use Material You dynamic wallpaper colors, false for classic Google Clock dark/light colors.
 */
@Composable
fun PixelLiveClockIcon(
    modifier: Modifier = Modifier,
    iconSize: Dp = 48.dp,
    smoothSecondHand: Boolean = true,
    useMaterialYouTheme: Boolean = true
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    var currentTimeMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }

    // Lifecycle-aware clock loop: automatically pauses when launcher goes to background
    LaunchedEffect(lifecycleOwner, smoothSecondHand) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            if (smoothSecondHand) {
                while (true) {
                    withFrameNanos {
                        currentTimeMillis = System.currentTimeMillis()
                    }
                }
            } else {
                while (true) {
                    currentTimeMillis = System.currentTimeMillis()
                    // Align delay precisely to the start of the next second
                    val millisUntilNextSecond = 1000L - (currentTimeMillis % 1000L)
                    delay(millisUntilNextSecond)
                }
            }
        }
    }

    // Pixel Material You vs Classic Google Clock Palette
    val faceColor = if (useMaterialYouTheme) {
        MaterialTheme.colorScheme.secondaryContainer
    } else {
        Color(0xFF202124) // Google Dark Grey
    }

    val hourHandColor = if (useMaterialYouTheme) {
        MaterialTheme.colorScheme.onSecondaryContainer
    } else {
        Color(0xFFE8EAED)
    }

    val minuteHandColor = if (useMaterialYouTheme) {
        MaterialTheme.colorScheme.primary
    } else {
        Color(0xFF8AB4F8) // Google Blue 300
    }

    val secondHandColor = if (useMaterialYouTheme) {
        MaterialTheme.colorScheme.tertiary
    } else {
        Color(0xFFF28B82) // Pixel Coral Accent
    }

    val tickColor = hourHandColor.copy(alpha = 0.35f)

    // Compute time angles
    val calendar = remember(currentTimeMillis) {
        Calendar.getInstance().apply { timeInMillis = currentTimeMillis }
    }

    val milliseconds = calendar.get(Calendar.MILLISECOND)
    val seconds = calendar.get(Calendar.SECOND) + if (smoothSecondHand) milliseconds / 1000f else 0f
    val minutes = calendar.get(Calendar.MINUTE) + seconds / 60f
    val hours = (calendar.get(Calendar.HOUR) % 12) + minutes / 60f

    val hourAngle = hours * 30f         // 360 degrees / 12 hours = 30 deg/hr
    val minuteAngle = minutes * 6f      // 360 degrees / 60 mins = 6 deg/min
    val secondAngle = seconds * 6f      // 360 degrees / 60 secs = 6 deg/sec

    Canvas(
        modifier = modifier.size(iconSize)
    ) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val radius = size.width / 2f

        // 1. Draw Clock Face
        drawCircle(
            color = faceColor,
            radius = radius,
            center = center
        )

        // 2. Draw 12 hour ticks
        for (i in 0 until 12) {
            rotate(degrees = i * 30f, pivot = center) {
                drawLine(
                    color = tickColor,
                    start = Offset(center.x, center.y - radius * 0.88f),
                    end = Offset(center.x, center.y - radius * 0.78f),
                    strokeWidth = size.width * 0.04f,
                    cap = StrokeCap.Round
                )
            }
        }

        // 3. Draw Hour Hand
        rotate(degrees = hourAngle, pivot = center) {
            drawLine(
                color = hourHandColor,
                start = center,
                end = Offset(center.x, center.y - radius * 0.50f),
                strokeWidth = size.width * 0.08f,
                cap = StrokeCap.Round
            )
        }

        // 4. Draw Minute Hand
        rotate(degrees = minuteAngle, pivot = center) {
            drawLine(
                color = minuteHandColor,
                start = center,
                end = Offset(center.x, center.y - radius * 0.72f),
                strokeWidth = size.width * 0.06f,
                cap = StrokeCap.Round
            )
        }

        // 5. Draw Second Hand
        rotate(degrees = secondAngle, pivot = center) {
            drawLine(
                color = secondHandColor,
                start = Offset(center.x, center.y + radius * 0.20f),
                end = Offset(center.x, center.y - radius * 0.82f),
                strokeWidth = size.width * 0.03f,
                cap = StrokeCap.Round
            )
        }

        // 6. Draw Center Pivot Cap
        drawCircle(
            color = secondHandColor,
            radius = size.width * 0.05f,
            center = center
        )
    }
}
