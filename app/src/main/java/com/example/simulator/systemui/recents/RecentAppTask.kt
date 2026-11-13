package com.example.simulator.systemui.recents

import android.content.ComponentName
import android.graphics.Bitmap
import android.graphics.drawable.Drawable

/**
 * Data class representing a recent application task within the simulator.
 */
data class RecentAppTask(
    val taskId: Int,
    val componentName: ComponentName,
    val appName: String,
    val appIcon: Drawable?,
    val previewSnapshot: Bitmap? = null, // Live snapshot of the application screen inside the simulator
    val isLocked: Boolean = false
)
