package com.example

import android.content.Intent
import android.os.BatteryManager
import com.example.os.BatteryState
import com.example.os.BatteryStatusProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BatteryStatusProviderTest {

    @Test
    fun testDefaultBatteryStateValues() {
        val state = BatteryState()
        assertEquals(85, state.levelPercent)
        assertFalse(state.isCharging)
        assertFalse(state.isPowerSaver)
    }

    @Test
    fun testCustomBatteryState() {
        val state = BatteryState(levelPercent = 95, isCharging = true, isPowerSaver = false)
        assertEquals(95, state.levelPercent)
        assertTrue(state.isCharging)
        assertFalse(state.isPowerSaver)
    }

    @Test
    fun testParseBatteryIntentHardwareTelemetry() {
        val intent = Intent(Intent.ACTION_BATTERY_CHANGED).apply {
            putExtra(BatteryManager.EXTRA_LEVEL, 64)
            putExtra(BatteryManager.EXTRA_SCALE, 100)
            putExtra(BatteryManager.EXTRA_STATUS, BatteryManager.BATTERY_STATUS_CHARGING)
            putExtra(BatteryManager.EXTRA_TEMPERATURE, 315)
            putExtra(BatteryManager.EXTRA_VOLTAGE, 4120)
            putExtra(BatteryManager.EXTRA_TECHNOLOGY, "Li-ion")
            putExtra(BatteryManager.EXTRA_HEALTH, BatteryManager.BATTERY_HEALTH_GOOD)
            putExtra(BatteryManager.EXTRA_PLUGGED, BatteryManager.BATTERY_PLUGGED_USB)
        }

        val parsed = BatteryStatusProvider.parseBatteryIntent(intent, isPowerSaver = true)
        assertEquals(64, parsed.levelPercent)
        assertTrue(parsed.isCharging)
        assertTrue(parsed.isPowerSaver)
        assertEquals(31.5f, parsed.temperatureCelsius, 0.01f)
        assertEquals(4120, parsed.voltageMv)
        assertEquals("Li-ion", parsed.technology)
        assertEquals("Good", parsed.healthLabel)
        assertEquals("USB", parsed.pluggedSource)
    }
}
