package com.example.model

enum class PixelIconShape {
    CIRCLE,
    SQUIRCLE,
    TEARDROP,
    ROUNDED_SQUARE
}

data class PixelThemeSettings(
    val useMaterialYouMonochrome: Boolean = false,
    val tintPackIconsWithMaterialYou: Boolean = false, // Tints even 3rd-party packs in wallpaper colors!
    val iconShape: PixelIconShape = PixelIconShape.CIRCLE
)
