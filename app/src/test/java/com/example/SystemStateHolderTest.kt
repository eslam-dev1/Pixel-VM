package com.example

import androidx.test.core.app.ApplicationProvider
import com.example.os.SystemStateHolder
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class SystemStateHolderTest {

    private lateinit var systemStateHolder: SystemStateHolder
    private val testDispatcher = UnconfinedTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        systemStateHolder = SystemStateHolder(context, testScope)
    }

    @Test
    fun testInitialSystemState() {
        val state = systemStateHolder.state.value
        assertTrue(state.timeFormatted.isNotEmpty())
        assertTrue(state.dateFormatted.isNotEmpty())
        assertTrue(state.batteryPercent in 0..100)
    }

    @Test
    fun testQuickSettingsToggles() {
        val initialWifi = systemStateHolder.state.value.isWifiConnected
        systemStateHolder.toggleWifi()
        assertEquals(!initialWifi, systemStateHolder.state.value.isWifiConnected)

        val initialBt = systemStateHolder.state.value.isBluetoothEnabled
        systemStateHolder.toggleBluetooth()
        assertEquals(!initialBt, systemStateHolder.state.value.isBluetoothEnabled)

        val initialDnd = systemStateHolder.state.value.isDndEnabled
        systemStateHolder.toggleDnd()
        assertEquals(!initialDnd, systemStateHolder.state.value.isDndEnabled)

        val initialAutoRotate = systemStateHolder.state.value.isAutoRotateEnabled
        systemStateHolder.toggleAutoRotate()
        assertEquals(!initialAutoRotate, systemStateHolder.state.value.isAutoRotateEnabled)

        val initialBatterySaver = systemStateHolder.state.value.isBatterySaverEnabled
        systemStateHolder.toggleBatterySaver()
        assertEquals(!initialBatterySaver, systemStateHolder.state.value.isBatterySaverEnabled)
    }

    @Test
    fun testVolumeAndBrightness() {
        systemStateHolder.setVolume(0.5f)
        assertEquals(0.5f, systemStateHolder.state.value.volumePercent, 0.01f)

        systemStateHolder.setBrightness(0.9f)
        assertEquals(0.9f, systemStateHolder.state.value.brightnessPercent, 0.01f)
    }

    @Test
    fun testFlashlightToggle() {
        assertFalse(systemStateHolder.state.value.isFlashlightOn)
        systemStateHolder.toggleFlashlight()
        assertTrue(systemStateHolder.state.value.isFlashlightOn)
        systemStateHolder.toggleFlashlight()
        assertFalse(systemStateHolder.state.value.isFlashlightOn)
    }
}
