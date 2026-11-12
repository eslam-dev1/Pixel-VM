package com.example.simulated.clock

import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

data class WorldCity(
    val cityName: String,
    val country: String,
    val zoneId: String
)

data class CityTimeInfo(
    val cityName: String,
    val country: String,
    val zoneId: String,
    val timeFormatted: String,
    val amPm: String,
    val dateFormatted: String,
    val offsetHoursFormatted: String,
    val isDaytime: Boolean
)

object WorldClockManager {

    val POPULAR_CITIES = listOf(
        WorldCity("London", "United Kingdom", "Europe/London"),
        WorldCity("New York", "United States", "America/New_York"),
        WorldCity("Tokyo", "Japan", "Asia/Tokyo"),
        WorldCity("Sydney", "Australia", "Australia/Sydney"),
        WorldCity("Cairo", "Egypt", "Africa/Cairo"),
        WorldCity("Los Angeles", "United States", "America/Los_Angeles"),
        WorldCity("Dubai", "United Arab Emirates", "Asia/Dubai"),
        WorldCity("Paris", "France", "Europe/Paris"),
        WorldCity("Singapore", "Singapore", "Asia/Singapore")
    )

    private val timeFormatter = DateTimeFormatter.ofPattern("h:mm", Locale.getDefault())
    private val amPmFormatter = DateTimeFormatter.ofPattern("a", Locale.getDefault())
    private val dateFormatter = DateTimeFormatter.ofPattern("EEE, MMM d", Locale.getDefault())

    fun getCityTime(
        city: WorldCity,
        now: Instant = Instant.now(),
        localZone: ZoneId = ZoneId.systemDefault()
    ): CityTimeInfo {
        val targetZone = try {
            ZoneId.of(city.zoneId)
        } catch (_: Exception) {
            ZoneId.of("UTC")
        }

        val targetDateTime = ZonedDateTime.ofInstant(now, targetZone)
        val localDateTime = ZonedDateTime.ofInstant(now, localZone)

        val targetOffsetSec = targetDateTime.offset.totalSeconds
        val localOffsetSec = localDateTime.offset.totalSeconds
        val diffSec = targetOffsetSec - localOffsetSec
        val diffHours = diffSec / 3600.0

        val offsetString = when {
            diffHours == 0.0 -> "Same time"
            diffHours > 0 -> {
                val formatted = if (diffHours % 1.0 == 0.0) "${diffHours.toInt()}" else String.format(Locale.US, "%.1f", diffHours)
                "+$formatted hrs"
            }
            else -> {
                val absH = -diffHours
                val formatted = if (absH % 1.0 == 0.0) "${absH.toInt()}" else String.format(Locale.US, "%.1f", absH)
                "-$formatted hrs"
            }
        }

        val hourOfDay = targetDateTime.hour
        val isDaytime = hourOfDay in 6..18

        return CityTimeInfo(
            cityName = city.cityName,
            country = city.country,
            zoneId = city.zoneId,
            timeFormatted = targetDateTime.format(timeFormatter),
            amPm = targetDateTime.format(amPmFormatter),
            dateFormatted = targetDateTime.format(dateFormatter),
            offsetHoursFormatted = offsetString,
            isDaytime = isDaytime
        )
    }
}
