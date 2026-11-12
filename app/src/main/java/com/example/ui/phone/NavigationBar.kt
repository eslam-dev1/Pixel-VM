package com.example.ui.phone

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

@Composable
fun NavigationBar(
    onHome: () -> Unit,
    onRecents: () -> Unit,
    pillColor: Color = Color.White.copy(alpha = 0.8f),
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(28.dp)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onHome() },
                    onLongPress = { onRecents() }
                )
            }
            .pointerInput(Unit) {
                var totalDragY = 0f
                detectDragGestures(
                    onDragStart = { totalDragY = 0f },
                    onDrag = { _, dragAmount ->
                        totalDragY += dragAmount.y
                    },
                    onDragEnd = {
                        if (totalDragY < -50f) {
                            if (totalDragY < -180f) {
                                onRecents()
                            } else {
                                onHome()
                            }
                        }
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        // Minimalist pill
        Box(
            modifier = Modifier
                .width(72.dp)
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(pillColor)
        )
    }
}
