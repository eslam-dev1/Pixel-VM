package com.example.model

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class BodyColor(
    val id: String,
    val displayName: String,
    val backColor: Color,
    val frameColor: Color,
    val cameraBarColor: Color,
    val accentColor: Color,
    val defaultWallpaperId: String
) {
    OBSIDIAN(
        id = "obsidian",
        displayName = "Obsidian",
        backColor = Color(0xFF1E2024),
        frameColor = Color(0xFF2C2F36),
        cameraBarColor = Color(0xFF16181B),
        accentColor = Color(0xFFA8C7FA),
        defaultWallpaperId = "wp_obsidian_eclipse"
    ),
    PORCELAIN(
        id = "porcelain",
        displayName = "Porcelain",
        backColor = Color(0xFFF3EFE6),
        frameColor = Color(0xFFDED8CB),
        cameraBarColor = Color(0xFFE4DFD3),
        accentColor = Color(0xFF6B5E4F),
        defaultWallpaperId = "wp_porcelain_dawn"
    ),
    WINTERGREEN(
        id = "wintergreen",
        displayName = "Wintergreen",
        backColor = Color(0xFFD6E7DC),
        frameColor = Color(0xFFB8D3C1),
        cameraBarColor = Color(0xFFC4DDCD),
        accentColor = Color(0xFF2E6A44),
        defaultWallpaperId = "wp_wintergreen_meadow"
    ),
    PEONY(
        id = "peony",
        displayName = "Peony",
        backColor = Color(0xFFF7D6DB),
        frameColor = Color(0xFFE5B4BC),
        cameraBarColor = Color(0xFFEDBFC6),
        accentColor = Color(0xFF944558),
        defaultWallpaperId = "wp_peony_petal"
    );

    companion object {
        fun fromId(id: String): BodyColor {
            return entries.find { it.id.equals(id, ignoreCase = true) } ?: OBSIDIAN
        }
    }
}

data class DeviceProfile(
    val screenAspectRatio: Float,
    val displayCornerRadius: Dp,
    val bezelCornerRadius: Dp,
    val punchHoleSize: Dp,
    val punchHoleTopMargin: Dp,
    val bezelThickness: Dp,
    val frameWidthMax: Dp
) {
    fun calculateFrameHeight(width: Dp): Dp = width * screenAspectRatio

    fun calculateDisplayWidth(frameWidth: Dp, showBezel: Boolean): Dp =
        if (showBezel) frameWidth - (bezelThickness * 2) else frameWidth

    fun calculateDisplayHeight(frameHeight: Dp, showBezel: Boolean): Dp =
        if (showBezel) frameHeight - (bezelThickness * 2) else frameHeight

    fun calculatePunchHoleCenterY(): Dp = punchHoleTopMargin + (punchHoleSize / 2)

    fun calculateTopInsetClearance(): Dp = punchHoleTopMargin + punchHoleSize + 12.dp
}

object DeviceProfileDefaults {
    val PIXEL_9_PROFILE = DeviceProfile(
        screenAspectRatio = 2424f / 1080f,
        displayCornerRadius = 28.dp,
        bezelCornerRadius = 36.dp,
        punchHoleSize = 12.dp,
        punchHoleTopMargin = 10.dp,
        bezelThickness = 5.dp,
        frameWidthMax = 440.dp
    )

    val HONOR_X9D_PROFILE = DeviceProfile(
        screenAspectRatio = 2640f / 1200f,
        displayCornerRadius = 32.dp,
        bezelCornerRadius = 36.dp,
        punchHoleSize = 10.dp,
        punchHoleTopMargin = 12.dp,
        bezelThickness = 5.dp,
        frameWidthMax = 440.dp
    )

    fun getProfile(style: String): DeviceProfile {
        return if (style.equals("honor_x9d", ignoreCase = true)) HONOR_X9D_PROFILE else PIXEL_9_PROFILE
    }
}

object DeviceProfileConstants {
    // Pixel 9-style proportions: 6.3" screen at 1080x2424 (~20:9 aspect ratio)
    const val SCREEN_ASPECT_RATIO: Float = 2424f / 1080f // 2.244f (~20:9)

    // Corner radius of the inner simulated screen display
    val DISPLAY_CORNER_RADIUS: Dp = 28.dp

    // Corner radius of the outer flat-edge aluminum frame bezel
    val BEZEL_CORNER_RADIUS: Dp = 36.dp

    // Punch hole cutout for centered selfie camera
    val PUNCH_HOLE_SIZE: Dp = 12.dp
    val PUNCH_HOLE_TOP_MARGIN: Dp = 10.dp

    // Outer bezel thickness for decorative frame mode
    val BEZEL_THICKNESS: Dp = 5.dp

    // Maximum width when rendered on large screens/tablets
    val FRAME_WIDTH_MAX: Dp = 440.dp

    // Gesture navigation pill
    val GESTURE_PILL_WIDTH: Dp = 72.dp
    val GESTURE_PILL_HEIGHT: Dp = 4.dp

    fun calculateFrameHeight(width: Dp): Dp = width * SCREEN_ASPECT_RATIO

    fun calculateDisplayWidth(frameWidth: Dp, showBezel: Boolean): Dp =
        if (showBezel) frameWidth - (BEZEL_THICKNESS * 2) else frameWidth

    fun calculateDisplayHeight(frameHeight: Dp, showBezel: Boolean): Dp =
        if (showBezel) frameHeight - (BEZEL_THICKNESS * 2) else frameHeight

    fun calculatePunchHoleCenterY(): Dp = PUNCH_HOLE_TOP_MARGIN + (PUNCH_HOLE_SIZE / 2)

    fun calculateTopInsetClearance(): Dp = PUNCH_HOLE_TOP_MARGIN + PUNCH_HOLE_SIZE + 12.dp
}

