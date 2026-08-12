package com.vemestael.archeryshotcounter.presentation

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Handler
import android.os.Looper
import androidx.annotation.StringRes
import com.vemestael.archeryshotcounter.R
import kotlin.math.sqrt

enum class Sensitivity(@param:StringRes val labelRes: Int, val threshold: Float) {
    HIGH(R.string.sensitivity_high, 8f),
    MEDIUM(R.string.sensitivity_medium, 13f),
    LOW(R.string.sensitivity_low, 20f),
    CUSTOM(R.string.sensitivity_custom, 0f)
}

/**
 * Detects archery shots via the wrist accelerometer.
 *
 * Uses TYPE_LINEAR_ACCELERATION (gravity-compensated) to measure the sharp recoil
 * impulse that occurs when the compound bow string is released. The listener is fully
 * unregistered for [cooldownMs] after each shot instead of just ignoring events, so no
 * samples are polled at all during the dead time.
 *
 * Deliberately not using registerListener's maxReportLatencyUs batching: a batched flush
 * delivers several buffered samples through onSensorChanged in one synchronous burst, and
 * in practice this produced double-counted shots when the wrist kept moving right around
 * the cooldown boundary.
 *
 * The actual accept/reject decision (same-batch guard + post-registration settle window) lives
 * in [ShotDetectionPolicy], which is plain Kotlin and unit tested independently of this class.
 */
class ShotDetector(
    private val sensorManager: SensorManager,
    private val onShotDetected: (magnitude: Float) -> Unit
) : SensorEventListener {

    companion object {
        private const val GRAVITY = 9.81f
        const val DEFAULT_COOLDOWN_MS = 10_000L
    }

    var sensitivity = Sensitivity.MEDIUM
    var customThreshold: Float = 15f
    var cooldownMs: Long = DEFAULT_COOLDOWN_MS

    private val handler = Handler(Looper.getMainLooper())
    private val policy = ShotDetectionPolicy()
    private var reregisterRunnable: Runnable? = null
    private var activeSensor: Sensor? = null
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
            sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        } ?: return
        activeSensor = sensor
        isRunning = true
        registerSensor()
    }

    fun stop() {
        if (!isRunning) return
        cancelPendingReregister()
        sensorManager.unregisterListener(this)
        isRunning = false
        activeSensor = null
    }

    /** Suppresses detection for [cooldownMs] without polling the sensor, e.g. right after resuming from a manual pause. */
    fun resetCooldown() {
        if (!isRunning) return
        sensorManager.unregisterListener(this)
        schedulePendingReregister()
    }

    private fun registerSensor() {
        val sensor = activeSensor ?: return
        policy.onRegistered(System.currentTimeMillis())
        sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_GAME)
    }

    private fun schedulePendingReregister() {
        cancelPendingReregister()
        val runnable = Runnable {
            reregisterRunnable = null
            if (isRunning) registerSensor()
        }
        reregisterRunnable = runnable
        handler.postDelayed(runnable, cooldownMs)
    }

    private fun cancelPendingReregister() {
        reregisterRunnable?.let { handler.removeCallbacks(it) }
        reregisterRunnable = null
    }

    override fun onSensorChanged(event: SensorEvent) {
        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]
        val raw = sqrt(x * x + y * y + z * z)
        val magnitude = if (useLinearAccel) raw else (raw - GRAVITY).coerceAtLeast(0f)
        val threshold = if (sensitivity == Sensitivity.CUSTOM) customThreshold else sensitivity.threshold

        if (policy.evaluate(magnitude, threshold, System.currentTimeMillis())) {
            sensorManager.unregisterListener(this)
            onShotDetected(magnitude)
            schedulePendingReregister()
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
