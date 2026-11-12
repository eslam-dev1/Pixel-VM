package com.example

import com.example.model.DeviceProfile
import com.example.model.DeviceProfileConstants
import com.example.model.DeviceProfileDefaults
import org.junit.Assert.assertEquals
import org.junit.Test
import androidx.compose.ui.unit.dp

class DeviceProfileTest {

    @Test
    fun testDeviceProfileConstants() {
        // Verify object constants
        assertEquals(2424f / 1080f, DeviceProfileConstants.SCREEN_ASPECT_RATIO, 0.001f)
        assertEquals(28.dp, DeviceProfileConstants.DISPLAY_CORNER_RADIUS)
        assertEquals(36.dp, DeviceProfileConstants.BEZEL_CORNER_RADIUS)
        assertEquals(12.dp, DeviceProfileConstants.PUNCH_HOLE_SIZE)
        assertEquals(10.dp, DeviceProfileConstants.PUNCH_HOLE_TOP_MARGIN)
        assertEquals(5.dp, DeviceProfileConstants.BEZEL_THICKNESS)
        assertEquals(440.dp, DeviceProfileConstants.FRAME_WIDTH_MAX)
    }

    @Test
    fun testDeviceProfileDataClassDefaults() {
        // Verify data class defaults match object constants
        val profile = DeviceProfileDefaults.PIXEL_9_PROFILE
        assertEquals(DeviceProfileConstants.SCREEN_ASPECT_RATIO, profile.screenAspectRatio)
        assertEquals(DeviceProfileConstants.DISPLAY_CORNER_RADIUS, profile.displayCornerRadius)
        assertEquals(DeviceProfileConstants.BEZEL_CORNER_RADIUS, profile.bezelCornerRadius)
        assertEquals(DeviceProfileConstants.PUNCH_HOLE_SIZE, profile.punchHoleSize)
        assertEquals(DeviceProfileConstants.PUNCH_HOLE_TOP_MARGIN, profile.punchHoleTopMargin)
        assertEquals(DeviceProfileConstants.BEZEL_THICKNESS, profile.bezelThickness)
        assertEquals(DeviceProfileConstants.FRAME_WIDTH_MAX, profile.frameWidthMax)
    }

    @Test
    fun testCoordinateCalculations() {
        val profile = DeviceProfileDefaults.PIXEL_9_PROFILE
        val testWidth = 400.dp

        // Test display width with and without bezel
        assertEquals(400.dp, profile.calculateDisplayWidth(testWidth, showBezel = false))
        assertEquals(390.dp, profile.calculateDisplayWidth(testWidth, showBezel = true)) // 400 - 5*2

        // Test punch hole center calculation
        assertEquals(16.dp, profile.calculatePunchHoleCenterY()) // 10 + 12/2

        // Test top inset clearance
        assertEquals(34.dp, profile.calculateTopInsetClearance()) // 10 + 12 + 12

        // Test object constant calculation helpers
        assertEquals(390.dp, DeviceProfileConstants.calculateDisplayWidth(testWidth, showBezel = true))
        assertEquals(16.dp, DeviceProfileConstants.calculatePunchHoleCenterY())
        assertEquals(34.dp, DeviceProfileConstants.calculateTopInsetClearance())
    }

    @Test
    fun testHonorX9dProfile() {
        val honorProfile = DeviceProfileDefaults.HONOR_X9D_PROFILE
        assertEquals(2640f / 1200f, honorProfile.screenAspectRatio, 0.001f)
        assertEquals(32.dp, honorProfile.displayCornerRadius)
        assertEquals(10.dp, honorProfile.punchHoleSize)

        val retrieved = DeviceProfileDefaults.getProfile("honor_x9d")
        assertEquals(honorProfile, retrieved)

        val pixelRetrieved = DeviceProfileDefaults.getProfile("pixel_9")
        assertEquals(DeviceProfileDefaults.PIXEL_9_PROFILE, pixelRetrieved)
    }
}
