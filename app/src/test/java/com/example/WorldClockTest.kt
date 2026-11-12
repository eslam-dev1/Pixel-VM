package com.example

import com.example.simulated.clock.WorldCity
import com.example.simulated.clock.WorldClockManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.ZoneId

class WorldClockTest {

    @Test
    fun testWorldClockTimeConversion() {
        // Given an instant: 2026-11-12T12:00:00Z
        val instant = Instant.parse("2026-11-12T12:00:00Z")

        val tokyo = WorldCity("Tokyo", "Japan", "Asia/Tokyo") // UTC+9
        val tokyoTime = WorldClockManager.getCityTime(
            city = tokyo,
            now = instant,
            localZone = ZoneId.of("UTC")
        )

        assertEquals("Tokyo", tokyoTime.cityName)
        assertEquals("9:00", tokyoTime.timeFormatted)
        assertEquals("PM", tokyoTime.amPm)
        assertEquals("+9 hrs", tokyoTime.offsetHoursFormatted)
    }

    @Test
    fun testSameZoneOffset() {
        val instant = Instant.parse("2026-11-12T15:00:00Z")
        val london = WorldCity("London", "UK", "UTC")
        val info = WorldClockManager.getCityTime(
            city = london,
            now = instant,
            localZone = ZoneId.of("UTC")
        )

        assertEquals("Same time", info.offsetHoursFormatted)
        assertTrue(info.isDaytime) // 3pm is daytime
    }
}
