package com.example.os

enum class GestureResult {
    GO_HOME,
    OPEN_RECENTS,
    BACK,
    OPEN_SHADE,
    NONE
}

object GestureThresholds {
    const val SWIPE_UP_MIN_DISTANCE = 50f
    const val SWIPE_DOWN_MIN_DISTANCE = 50f
    const val SWIPE_SIDE_MIN_DISTANCE = 40f
    const val HOLD_TIME_MIN_MS = 150L
}

/**
 * Classifies touch gestures (swipe, hold, edges) for system navigation.
 */
class GestureClassifier {
    fun classify(
        startX: Float,
        startY: Float,
        endX: Float,
        endY: Float,
        velocityX: Float,
        velocityY: Float,
        holdTimeMs: Long,
        screenWidth: Float,
        screenHeight: Float,
        edgeMargin: Float = 48f
    ): GestureResult {
        val deltaX = endX - startX
        val deltaY = endY - startY
        val absDeltaX = kotlin.math.abs(deltaX)
        val absDeltaY = kotlin.math.abs(deltaY)

        // Edge swipe from left or right -> BACK
        if (startX <= edgeMargin || startX >= screenWidth - edgeMargin) {
            if (absDeltaX > GestureThresholds.SWIPE_SIDE_MIN_DISTANCE && absDeltaX > absDeltaY) {
                return GestureResult.BACK
            }
        }

        // Top edge swipe down -> OPEN_SHADE
        if (startY <= edgeMargin + 20f && deltaY > GestureThresholds.SWIPE_DOWN_MIN_DISTANCE && absDeltaY > absDeltaX) {
            return GestureResult.OPEN_SHADE
        }

        // Bottom edge swipe up
        if (startY >= screenHeight - edgeMargin - 30f && deltaY < -GestureThresholds.SWIPE_UP_MIN_DISTANCE && absDeltaY > absDeltaX) {
            if (holdTimeMs >= GestureThresholds.HOLD_TIME_MIN_MS && absDeltaY < screenHeight * 0.4f) {
                return GestureResult.OPEN_RECENTS
            }
            return GestureResult.GO_HOME
        }

        return GestureResult.NONE
    }
}
