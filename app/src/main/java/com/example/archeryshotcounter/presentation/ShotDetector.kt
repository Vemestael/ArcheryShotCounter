package com.example.archeryshotcounter.presentation

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.sqrt

enum class Sensitivity(val labelRu: String, val threshold: Float) {
    HIGH("Высокая (8 м/с²)", 8f),
    MEDIUM("Средняя (13 м/с²)", 13f),
    LOW("Низкая (20 м/с²)", 20f)
}

/**
 * Detects archery shots via the wrist accelerometer.
 *
 * Uses TYPE_LINEAR_ACCELERATION (gravity-compensated) to measure the sharp recoil
 * impulse that occurs when the compound bow string is released. A cooldown prevents
 * double-counting vibrations from the same shot.
 */
class ShotDetector(
    private val sensorManager: SensorManager,
    private val onShotDetected: () -> Unit
) : SensorEventListener {

    companion object {
        private const val COOLDOWN_MS = 2000L
        private const val GRAVITY = 9.81f
    }

    var sensitivity = Sensitivity.MEDIUM

    private var lastShotTime = 0L
    private var isRunning = false
    private var useLinearAccel = true

    fun start() {
        if (isRunning) return
        val linearSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)
        val sensor = if (linearSensor != null) {
            useLinearAccel = true
            linearSensor
        } else {
            useLinearAccel = false
            sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) ?: return
        }
        sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_GAME)
        isRunning = true
    }

    fun stop() {
        if (!isRunning) return
        sensorManager.unregisterListener(this)
        isRunning = false
    }

    override fun onSensorChanged(event: SensorEvent) {
        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]
        val raw = sqrt(x * x + y * y + z * z)
        val magnitude = if (useLinearAccel) raw else (raw - GRAVITY).coerceAtLeast(0f)

        val now = System.currentTimeMillis()
        if (magnitude > sensitivity.threshold && now - lastShotTime > COOLDOWN_MS) {
            lastShotTime = now
            onShotDetected()
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
