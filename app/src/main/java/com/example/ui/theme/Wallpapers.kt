package com.example.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

data class WallpaperTheme(
    val id: String,
    val name: String,
    val category: String,
    val gradientColors: List<Color>,
    val lightColors: ColorScheme,
    val darkColors: ColorScheme
)

object Wallpapers {

    // 1. Obsidian Eclipse
    val ObsidianEclipse = WallpaperTheme(
        id = "wp_obsidian_eclipse",
        name = "Obsidian Eclipse",
        category = "Obsidian",
        gradientColors = listOf(
            Color(0xFF0F1013),
            Color(0xFF1E2129),
            Color(0xFF2C3342),
            Color(0xFF3E4A61),
            Color(0xFF15171C)
        ),
        lightColors = lightColorScheme(
            primary = Color(0xFF3F5F90),
            onPrimary = Color.White,
            primaryContainer = Color(0xFFD6E3FF),
            onPrimaryContainer = Color(0xFF001B3E),
            secondary = Color(0xFF555F71),
            onSecondary = Color.White,
            surface = Color(0xFFF9F9FC),
            onSurface = Color(0xFF191C20),
            surfaceVariant = Color(0xFFE0E2EC),
            onSurfaceVariant = Color(0xFF44474F)
        ),
        darkColors = darkColorScheme(
            primary = Color(0xFFA8C7FA),
            onPrimary = Color(0xFF07305F),
            primaryContainer = Color(0xFF254777),
            onPrimaryContainer = Color(0xFFD6E3FF),
            secondary = Color(0xFFBDC7DC),
            onSecondary = Color(0xFF273141),
            surface = Color(0xFF111318),
            onSurface = Color(0xFFE2E2E9),
            surfaceVariant = Color(0xFF44474F),
            onSurfaceVariant = Color(0xFFC4C6D0)
        )
    )

    // 2. Obsidian Minimal
    val ObsidianMinimal = WallpaperTheme(
        id = "wp_obsidian_minimal",
        name = "Obsidian Slate",
        category = "Obsidian",
        gradientColors = listOf(
            Color(0xFF141416),
            Color(0xFF24252A),
            Color(0xFF3A3B43),
            Color(0xFF4F515C),
            Color(0xFF18191D)
        ),
        lightColors = lightColorScheme(
            primary = Color(0xFF545E75),
            onPrimary = Color.White,
            primaryContainer = Color(0xFFD8E2FF),
            onPrimaryContainer = Color(0xFF111B30),
            secondary = Color(0xFF5B606E),
            onSecondary = Color.White,
            surface = Color(0xFFFAF9FD),
            onSurface = Color(0xFF1A1B1F),
            surfaceVariant = Color(0xFFE2E2EC),
            onSurfaceVariant = Color(0xFF45464F)
        ),
        darkColors = darkColorScheme(
            primary = Color(0xFFBCC7E3),
            onPrimary = Color(0xFF263046),
            primaryContainer = Color(0xFF3C465D),
            onPrimaryContainer = Color(0xFFD8E2FF),
            secondary = Color(0xFFC3C7D7),
            onSecondary = Color(0xFF2D323E),
            surface = Color(0xFF121316),
            onSurface = Color(0xFFE3E2E6),
            surfaceVariant = Color(0xFF45464F),
            onSurfaceVariant = Color(0xFFC6C6D0)
        )
    )

    // 3. Porcelain Dawn
    val PorcelainDawn = WallpaperTheme(
        id = "wp_porcelain_dawn",
        name = "Porcelain Dawn",
        category = "Porcelain",
        gradientColors = listOf(
            Color(0xFF2B261F),
            Color(0xFF6B5E4F),
            Color(0xFFB5A490),
            Color(0xFFE4DACB),
            Color(0xFFF7F3EB)
        ),
        lightColors = lightColorScheme(
            primary = Color(0xFF745A38),
            onPrimary = Color.White,
            primaryContainer = Color(0xFFFFDDB6),
            onPrimaryContainer = Color(0xFF2A1800),
            secondary = Color(0xFF6E5D4A),
            onSecondary = Color.White,
            surface = Color(0xFFFFF8F3),
            onSurface = Color(0xFF201B15),
            surfaceVariant = Color(0xFFEFE0D0),
            onSurfaceVariant = Color(0xFF4F4539)
        ),
        darkColors = darkColorScheme(
            primary = Color(0xFFE5C193),
            onPrimary = Color(0xFF422C0E),
            primaryContainer = Color(0xFF5B4222),
            onPrimaryContainer = Color(0xFFFFDDB6),
            secondary = Color(0xFFDBC2A9),
            onSecondary = Color(0xFF3C2F1F),
            surface = Color(0xFF18130E),
            onSurface = Color(0xFFEEE0D5),
            surfaceVariant = Color(0xFF4F4539),
            onSurfaceVariant = Color(0xFFD3C4B5)
        )
    )

    // 4. Porcelain Sand
    val PorcelainSand = WallpaperTheme(
        id = "wp_porcelain_sand",
        name = "Porcelain Dunes",
        category = "Porcelain",
        gradientColors = listOf(
            Color(0xFF1F1E1B),
            Color(0xFF47433B),
            Color(0xFF7E7769),
            Color(0xFFC7BFA8),
            Color(0xFFEDE8D8)
        ),
        lightColors = lightColorScheme(
            primary = Color(0xFF675E40),
            onPrimary = Color.White,
            primaryContainer = Color(0xFFEFE2BC),
            onPrimaryContainer = Color(0xFF211B04),
            secondary = Color(0xFF635E50),
            onSecondary = Color.White,
            surface = Color(0xFFFAF9F2),
            onSurface = Color(0xFF1D1B16),
            surfaceVariant = Color(0xFFE7E2D3),
            onSurfaceVariant = Color(0xFF49463B)
        ),
        darkColors = darkColorScheme(
            primary = Color(0xFFD2C6A2),
            onPrimary = Color(0xFF373016),
            primaryContainer = Color(0xFF4E462A),
            onPrimaryContainer = Color(0xFFEFE2BC),
            secondary = Color(0xFFCEC5B4),
            onSecondary = Color(0xFF343024),
            surface = Color(0xFF15130F),
            onSurface = Color(0xFFE7E2DA),
            surfaceVariant = Color(0xFF49463B),
            onSurfaceVariant = Color(0xFFCBC6B7)
        )
    )

    // 5. Wintergreen Meadow
    val WintergreenMeadow = WallpaperTheme(
        id = "wp_wintergreen_meadow",
        name = "Wintergreen Meadow",
        category = "Wintergreen",
        gradientColors = listOf(
            Color(0xFF0F1E16),
            Color(0xFF244835),
            Color(0xFF417A5A),
            Color(0xFF75B68D),
            Color(0xFFD1EFE0)
        ),
        lightColors = lightColorScheme(
            primary = Color(0xFF276A43),
            onPrimary = Color.White,
            primaryContainer = Color(0xFFACF2C1),
            onPrimaryContainer = Color(0xFF00210E),
            secondary = Color(0xFF4F6353),
            onSecondary = Color.White,
            surface = Color(0xFFF6FBF5),
            onSurface = Color(0xFF171D18),
            surfaceVariant = Color(0xFFDCE5DC),
            onSurfaceVariant = Color(0xFF414942)
        ),
        darkColors = darkColorScheme(
            primary = Color(0xFF91D5A6),
            onPrimary = Color(0xFF00391E),
            primaryContainer = Color(0xFF08512D),
            onPrimaryContainer = Color(0xFFACF2C1),
            secondary = Color(0xFFB6CCB8),
            onSecondary = Color(0xFF223527),
            surface = Color(0xFF0F1511),
            onSurface = Color(0xFFE0E4DE),
            surfaceVariant = Color(0xFF414942),
            onSurfaceVariant = Color(0xFFC0C9C0)
        )
    )

    // 6. Wintergreen Sage
    val WintergreenSage = WallpaperTheme(
        id = "wp_wintergreen_sage",
        name = "Wintergreen Frost",
        category = "Wintergreen",
        gradientColors = listOf(
            Color(0xFF111E1D),
            Color(0xFF264440),
            Color(0xFF43706B),
            Color(0xFF79A9A3),
            Color(0xFFD5ECE9)
        ),
        lightColors = lightColorScheme(
            primary = Color(0xFF2E6864),
            onPrimary = Color.White,
            primaryContainer = Color(0xFFB1EFEB),
            onPrimaryContainer = Color(0xFF00201E),
            secondary = Color(0xFF4B6361),
            onSecondary = Color.White,
            surface = Color(0xFFF5FAF9),
            onSurface = Color(0xFF161D1C),
            surfaceVariant = Color(0xFFDBE5E3),
            onSurfaceVariant = Color(0xFF3F4948)
        ),
        darkColors = darkColorScheme(
            primary = Color(0xFF96D3CF),
            onPrimary = Color(0xFF003734),
            primaryContainer = Color(0xFF124F4B),
            onPrimaryContainer = Color(0xFFB1EFEB),
            secondary = Color(0xFFB2CCC9),
            onSecondary = Color(0xFF1D3533),
            surface = Color(0xFF0E1514),
            onSurface = Color(0xFFDEE4E3),
            surfaceVariant = Color(0xFF3F4948),
            onSurfaceVariant = Color(0xFFBFC9C7)
        )
    )

    // 7. Peony Petal
    val PeonyPetal = WallpaperTheme(
        id = "wp_peony_petal",
        name = "Peony Petals",
        category = "Peony",
        gradientColors = listOf(
            Color(0xFF2C141B),
            Color(0xFF672839),
            Color(0xFFA84E66),
            Color(0xFFE28AA1),
            Color(0xFFFCE1E7)
        ),
        lightColors = lightColorScheme(
            primary = Color(0xFF8F485B),
            onPrimary = Color.White,
            primaryContainer = Color(0xFFFFD9E1),
            onPrimaryContainer = Color(0xFF3B0719),
            secondary = Color(0xFF75565D),
            onSecondary = Color.White,
            surface = Color(0xFFFFF8F8),
            onSurface = Color(0xFF21191B),
            surfaceVariant = Color(0xFFF3DDE1),
            onSurfaceVariant = Color(0xFF514346)
        ),
        darkColors = darkColorScheme(
            primary = Color(0xFFFFB1C4),
            onPrimary = Color(0xFF561C2E),
            primaryContainer = Color(0xFF723244),
            onPrimaryContainer = Color(0xFFFFD9E1),
            secondary = Color(0xFFE4BDC5),
            onSecondary = Color(0xFF422930),
            surface = Color(0xFF181113),
            onSurface = Color(0xFFEFDFE1),
            surfaceVariant = Color(0xFF514346),
            onSurfaceVariant = Color(0xFFD6C2C5)
        )
    )

    // 8. Peony Sunset
    val PeonySunset = WallpaperTheme(
        id = "wp_peony_sunset",
        name = "Peony Amber",
        category = "Peony",
        gradientColors = listOf(
            Color(0xFF281717),
            Color(0xFF5A2A2E),
            Color(0xFF9E4E4E),
            Color(0xFFDE8778),
            Color(0xFFFBE4DC)
        ),
        lightColors = lightColorScheme(
            primary = Color(0xFF914844),
            onPrimary = Color.White,
            primaryContainer = Color(0xFFFFDAD6),
            onPrimaryContainer = Color(0xFF3B0807),
            secondary = Color(0xFF775653),
            onSecondary = Color.White,
            surface = Color(0xFFFFF8F6),
            onSurface = Color(0xFF231918),
            surfaceVariant = Color(0xFFF5DDDA),
            onSurfaceVariant = Color(0xFF534341)
        ),
        darkColors = darkColorScheme(
            primary = Color(0xFFFFB3AC),
            onPrimary = Color(0xFF571D1A),
            primaryContainer = Color(0xFF73332E),
            onPrimaryContainer = Color(0xFFFFDAD6),
            secondary = Color(0xFFE7BDB8),
            onSecondary = Color(0xFF442927),
            surface = Color(0xFF1A1110),
            onSurface = Color(0xFFF1DFDD),
            surfaceVariant = Color(0xFF534341),
            onSurfaceVariant = Color(0xFFD8C2BE)
        )
    )

    val all = listOf(
        ObsidianEclipse,
        ObsidianMinimal,
        PorcelainDawn,
        PorcelainSand,
        WintergreenMeadow,
        WintergreenSage,
        PeonyPetal,
        PeonySunset
    )

    fun getById(id: String): WallpaperTheme {
        return all.find { it.id == id } ?: ObsidianEclipse
    }

    fun makeBackgroundBrush(wallpaper: WallpaperTheme): Brush {
        return Brush.verticalGradient(
            colors = wallpaper.gradientColors
        )
    }
}
