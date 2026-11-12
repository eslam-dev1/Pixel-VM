package com.example.os

import androidx.compose.ui.graphics.vector.ImageVector

enum class QsState {
    ON, OFF, UNAVAILABLE
}

enum class QsKind {
    REAL, SIMULATED, OPENS_SETTINGS
}

data class QsTileItem(
    val id: String,
    val label: String,
    val icon: ImageVector,
    val state: QsState,
    val kind: QsKind,
    val description: String = ""
)
