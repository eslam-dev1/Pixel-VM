package com.example.ui.phone

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.model.BodyColor
import com.example.model.DeviceProfile
import com.example.model.DeviceProfileConstants

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.systemBars
import androidx.compose.ui.platform.LocalDensity
import com.example.model.DeviceProfileDefaults

@Composable
fun PhoneFrame(
    showBezel: Boolean,
    deviceProfile: DeviceProfile = DeviceProfileDefaults.PIXEL_9_PROFILE,
    bodyColor: BodyColor = BodyColor.OBSIDIAN,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val density = LocalDensity.current
    val cutoutPadding = WindowInsets.displayCutout.asPaddingValues()
    val statusBarPadding = WindowInsets.statusBars.asPaddingValues()

    if (!showBezel) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            content()
            
            // Decorative punch hole only if real cutout is not detected (simulation mode)
            if (cutoutPadding.calculateTopPadding() == 0.dp) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = deviceProfile.punchHoleTopMargin)
                        .size(deviceProfile.punchHoleSize)
                        .clip(CircleShape)
                        .background(Color.Black)
                        .border(1.dp, Color(0xFF1F1F1F), CircleShape)
                )
            }
        }
    } else {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color(0xFF0C0D0F))
                .padding(horizontal = 8.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            // Flat-edge aluminum frame shell
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = deviceProfile.frameWidthMax)
                    .clip(RoundedCornerShape(deviceProfile.bezelCornerRadius))
                    .background(
                        Brush.linearGradient(
                            listOf(
                                bodyColor.frameColor,
                                bodyColor.frameColor.copy(alpha = 0.85f),
                                bodyColor.frameColor
                            )
                        )
                    )
                    .border(1.5.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(deviceProfile.bezelCornerRadius))
                    .padding(deviceProfile.bezelThickness)
            ) {
                // Inner screen container
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(deviceProfile.displayCornerRadius))
                        .background(Color.Black)
                ) {
                    content()

                    // Simulated punch hole inside the frame
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = deviceProfile.punchHoleTopMargin)
                            .size(deviceProfile.punchHoleSize)
                            .clip(CircleShape)
                            .background(Color.Black)
                            .border(1.dp, Color(0xFF1E1E1E), CircleShape)
                    )
                }
            }
        }
    }
}
