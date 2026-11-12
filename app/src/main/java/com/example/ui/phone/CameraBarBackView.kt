package com.example.ui.phone

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.BodyColor
import com.example.model.DeviceProfile
import com.example.model.DeviceProfileConstants

@Composable
fun CameraBarBackView(
    bodyColor: BodyColor,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(340.dp)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        // Phone Back Housing Shell
        Box(
            modifier = Modifier
                .width(190.dp)
                .fillMaxHeight()
                .shadow(12.dp, RoundedCornerShape(DeviceProfileConstants.BEZEL_CORNER_RADIUS))
                .clip(RoundedCornerShape(DeviceProfileConstants.BEZEL_CORNER_RADIUS))
                .background(bodyColor.frameColor)
                .border(2.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(DeviceProfileConstants.BEZEL_CORNER_RADIUS))
                .padding(3.dp)
        ) {
            // Matte glass back cover
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(DeviceProfileConstants.DISPLAY_CORNER_RADIUS))
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                bodyColor.backColor,
                                bodyColor.backColor.copy(alpha = 0.95f),
                                bodyColor.backColor
                            )
                        )
                    )
            ) {
                // Original Pill-Shaped Horizontal Camera Visor Bar
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 28.dp, start = 12.dp, end = 12.dp)
                        .fillMaxWidth()
                        .height(52.dp)
                        .shadow(6.dp, RoundedCornerShape(26.dp))
                        .clip(RoundedCornerShape(26.dp))
                        .background(bodyColor.cameraBarColor)
                        .border(1.5.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(26.dp))
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Dual Camera pill cutout
                        Box(
                            modifier = Modifier
                                .height(32.dp)
                                .width(68.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xFF0A0A0C))
                                .border(1.dp, Color(0xFF222226), RoundedCornerShape(16.dp))
                                .padding(horizontal = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Main Lens
                                Box(
                                    modifier = Modifier
                                        .size(18.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF14151B))
                                        .border(1.dp, Color(0xFF333540), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF1E2838))
                                    )
                                }

                                // Ultrawide Lens
                                Box(
                                    modifier = Modifier
                                        .size(18.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF14151B))
                                        .border(1.dp, Color(0xFF333540), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF1E2838))
                                    )
                                }
                            }
                        }

                        // Right side: Flash & Sensor
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // Mic hole
                            Box(
                                modifier = Modifier
                                    .size(4.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black)
                            )

                            // LED Flash
                            Box(
                                modifier = Modifier
                                    .size(14.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFFFF9E6))
                                    .border(1.dp, Color(0xFFD6CDB8), CircleShape)
                            )
                        }
                    }
                }

                // Minimalist stylized back accent mark (original vector mark, not a logo)
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.12f))
                )
            }
        }
    }
}
