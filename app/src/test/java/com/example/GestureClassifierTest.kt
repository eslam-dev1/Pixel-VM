package com.example

import com.example.os.GestureClassifier
import com.example.os.GestureResult
import org.junit.Assert.assertEquals
import org.junit.Test

class GestureClassifierTest {
    private val classifier = GestureClassifier()
    private val screenWidth = 1080f
    private val screenHeight = 2400f

    @Test
    fun testGoHomeGesture() {
        val result = classifier.classify(
            startX = 540f, startY = 2380f,
            endX = 540f, endY = 2100f,
            velocityX = 0f, velocityY = -1200f,
            holdTimeMs = 50L,
            screenWidth = screenWidth,
            screenHeight = screenHeight
        )
        assertEquals(GestureResult.GO_HOME, result)
    }

    @Test
    fun testOpenRecentsGesture() {
        val result = classifier.classify(
            startX = 540f, startY = 2380f,
            endX = 540f, endY = 2000f,
            velocityX = 0f, velocityY = -100f,
            holdTimeMs = 300L,
            screenWidth = screenWidth,
            screenHeight = screenHeight
        )
        assertEquals(GestureResult.OPEN_RECENTS, result)
    }

    @Test
    fun testBackGesture() {
        val result = classifier.classify(
            startX = 20f, startY = 1200f,
            endX = 200f, endY = 1200f,
            velocityX = 1000f, velocityY = 0f,
            holdTimeMs = 80L,
            screenWidth = screenWidth,
            screenHeight = screenHeight
        )
        assertEquals(GestureResult.BACK, result)
    }

    @Test
    fun testOpenShadeGesture() {
        val result = classifier.classify(
            startX = 540f, startY = 10f,
            endX = 540f, endY = 200f,
            velocityX = 0f, velocityY = 800f,
            holdTimeMs = 60L,
            screenWidth = screenWidth,
            screenHeight = screenHeight
        )
        assertEquals(GestureResult.OPEN_SHADE, result)
    }
}
