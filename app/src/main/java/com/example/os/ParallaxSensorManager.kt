package com.example.os

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ParallaxSensorManager(context: Context) : SensorEventListener {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    private val _offset = MutableStateFlow(Pair(0f, 0f))
    val offset: StateFlow<Pair<Float, Float>> = _offset.asStateFlow()

    init {
        try {
            sensorManager?.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME)
        } catch (_: Exception) {}
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_ACCELEROMETER) {
            // x ranges roughly -10 to 10 (tilt left/right), y ranges roughly -10 to 10 (tilt forward/back)
            val x = event.values[0]
            val y = event.values[1]

            // Normalize and scale for subtle parallax shift (max 25dp offset)
            val offsetX = (-x * 4f).coerceIn(-25f, 25f)
            val offsetY = (y * 4f).coerceIn(-25f, 25f)
            _offset.value = Pair(offsetX, offsetY)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    fun unregister() {
        try {
            sensorManager?.unregisterListener(this)
        } catch (_: Exception) {}
    }
}
