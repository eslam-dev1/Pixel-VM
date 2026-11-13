package com.example.os

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.PowerManager
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * State representing battery percentage, charging status, power saver mode,
 * and real hardware telemetry from [Intent.ACTION_BATTERY_CHANGED].
 */
data class BatteryState(
    val levelPercent: Int = 85,
    val isCharging: Boolean = false,
    val isPowerSaver: Boolean = false,
    val temperatureCelsius: Float = 0f,
    val voltageMv: Int = 0,
    val healthLabel: String = "Good",
    val technology: String = "",
    val pluggedSource: String = "Battery"
)

/**
 * Monitors system battery level, hardware telemetry, and status via [Intent.ACTION_BATTERY_CHANGED]
 * and exposes a [StateFlow] for UI consumption in Status Bar, Home Widgets, and Battery Settings.
 */
class BatteryStatusProvider(private val context: Context) {

    private val _batteryState = MutableStateFlow(readStickyBatteryState())
    val batteryState: StateFlow<BatteryState> = _batteryState.asStateFlow()

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_BATTERY_CHANGED) {
                val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
                val isPowerSaver = try { powerManager?.isPowerSaveMode == true } catch (_: Exception) { false }
                val parsed = parseBatteryIntent(intent, isPowerSaver)
                _batteryState.update { parsed }
            }
        }
    }

    init {
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        try {
            ContextCompat.registerReceiver(context, batteryReceiver, filter, ContextCompat.RECEIVER_EXPORTED)
        } catch (_: Exception) {}
    }

    fun refreshNow() {
        _batteryState.value = readStickyBatteryState()
    }

    private fun readStickyBatteryState(): BatteryState {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        val isPowerSaver = try { powerManager?.isPowerSaveMode == true } catch (_: Exception) { false }
        val sticky = try {
            context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        } catch (_: Exception) {
            null
        }
        return if (sticky != null) {
            parseBatteryIntent(sticky, isPowerSaver)
        } else {
            val bm = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
            val pct = try {
                bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)?.takeIf { it in 0..100 } ?: 85
            } catch (_: Exception) {
                85
            }
            BatteryState(
                levelPercent = pct,
                isCharging = try { bm?.isCharging == true } catch (_: Exception) { false },
                isPowerSaver = isPowerSaver
            )
        }
    }

    /**
     * Unregisters the battery receiver to prevent context memory leaks.
     */
    fun unregister() {
        try {
            context.unregisterReceiver(batteryReceiver)
        } catch (_: Exception) {}
    }

    companion object {
        fun parseBatteryIntent(intent: Intent, isPowerSaver: Boolean): BatteryState {
            val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
            val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL

            val pct = if (level >= 0 && scale > 0) {
                ((level.toFloat() / scale.toFloat()) * 100f).toInt().coerceIn(0, 100)
            } else {
                85
            }

            val rawTemp = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0)
            val tempC = if (rawTemp > 0) rawTemp / 10f else 0f
            val voltage = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0).coerceAtLeast(0)
            val tech = intent.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY).orEmpty()

            val healthLabel = when (intent.getIntExtra(BatteryManager.EXTRA_HEALTH, BatteryManager.BATTERY_HEALTH_UNKNOWN)) {
                BatteryManager.BATTERY_HEALTH_GOOD -> "Good"
                BatteryManager.BATTERY_HEALTH_OVERHEAT -> "Overheat"
                BatteryManager.BATTERY_HEALTH_DEAD -> "Dead"
                BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "Over Voltage"
                BatteryManager.BATTERY_HEALTH_COLD -> "Cold"
                else -> "Unknown"
            }

            val plugged = when (intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0)) {
                BatteryManager.BATTERY_PLUGGED_AC -> "AC Charger"
                BatteryManager.BATTERY_PLUGGED_USB -> "USB"
                BatteryManager.BATTERY_PLUGGED_WIRELESS -> "Wireless"
                BatteryManager.BATTERY_PLUGGED_DOCK -> "Dock"
                else -> if (isCharging) "Connected" else "Battery"
            }

            return BatteryState(
                levelPercent = pct,
                isCharging = isCharging,
                isPowerSaver = isPowerSaver,
                temperatureCelsius = tempC,
                voltageMv = voltage,
                healthLabel = healthLabel,
                technology = tech,
                pluggedSource = plugged
            )
        }
    }
}
