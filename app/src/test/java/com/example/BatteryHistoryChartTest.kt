package com.example

import com.example.ui.components.HourlyBatteryData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class BatteryHistoryChartTest {

    @Test
    fun testHourlyBatteryDataBounds() {
        val testData = HourlyBatteryData(
            hourOfDay = 14,
            hourLabel = "2 PM",
            batteryPercent = 75,
            isCharging = false
        )

        assertEquals(14, testData.hourOfDay)
        assertEquals("2 PM", testData.hourLabel)
        assertEquals(75, testData.batteryPercent)
        assertTrue(testData.batteryPercent in 0..100)
    }

    @Test
    fun test24HourDataPointsGeneration() {
        val cal = Calendar.getInstance()
        val currentHour = cal.get(Calendar.HOUR_OF_DAY)

        // Verify hour wrapping logic
        val hours = (0 until 24).map { i ->
            (currentHour - (23 - i) + 24) % 24
        }

        assertEquals(24, hours.size)
        assertEquals(currentHour, hours.last())
    }
}
