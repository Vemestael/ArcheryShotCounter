package com.vemestael.archeryshotcounter.presentation

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ShotDetectionPolicyTest {

    private val threshold = 20f

    @Test
    fun `magnitude below threshold is not counted`() {
        val policy = ShotDetectionPolicy(settleMs = 0)
        policy.onRegistered(0)
        assertFalse(policy.evaluate(magnitude = 10f, threshold = threshold, nowMs = 1000))
    }

    @Test
    fun `magnitude exactly at threshold is not counted`() {
        // Strict '>' comparison: a magnitude equal to the threshold must not count.
        val policy = ShotDetectionPolicy(settleMs = 0)
        policy.onRegistered(0)
        assertFalse(policy.evaluate(magnitude = threshold, threshold = threshold, nowMs = 1000))
    }

    @Test
    fun `magnitude above threshold after settle window is counted`() {
        val policy = ShotDetectionPolicy(settleMs = 300)
        policy.onRegistered(0)
        assertTrue(policy.evaluate(magnitude = 25f, threshold = threshold, nowMs = 1000))
    }

    @Test
    fun `sample during the settle window is ignored even above threshold`() {
        // Regression test: shots were observed as little as 11-17ms after registerSensor()
        // because the wrist was still moving right when the sensor came back online.
        val policy = ShotDetectionPolicy(settleMs = 300)
        policy.onRegistered(0)
        assertFalse(policy.evaluate(magnitude = 25f, threshold = threshold, nowMs = 17))
    }

    @Test
    fun `sample exactly at the settle boundary is accepted`() {
        val policy = ShotDetectionPolicy(settleMs = 300)
        policy.onRegistered(0)
        assertTrue(policy.evaluate(magnitude = 25f, threshold = threshold, nowMs = 300))
    }

    @Test
    fun `sample one millisecond before the settle boundary is rejected`() {
        val policy = ShotDetectionPolicy(settleMs = 300)
        policy.onRegistered(0)
        assertFalse(policy.evaluate(magnitude = 25f, threshold = threshold, nowMs = 299))
    }

    @Test
    fun `a second sample in the same delivery burst is ignored`() {
        // Regression test: sensor batching used to deliver several buffered samples through
        // onSensorChanged in one synchronous burst, double-counting a single shot.
        val policy = ShotDetectionPolicy(settleMs = 0)
        policy.onRegistered(0)
        assertTrue(policy.evaluate(magnitude = 25f, threshold = threshold, nowMs = 1000))
        // Same instant, no onRegistered() in between - simulates a second buffered sample.
        assertFalse(policy.evaluate(magnitude = 30f, threshold = threshold, nowMs = 1000))
    }

    @Test
    fun `re-registering allows a new shot to be counted`() {
        val policy = ShotDetectionPolicy(settleMs = 300)
        policy.onRegistered(0)
        assertTrue(policy.evaluate(magnitude = 25f, threshold = threshold, nowMs = 1000))
        assertFalse(policy.evaluate(magnitude = 25f, threshold = threshold, nowMs = 1001))

        policy.onRegistered(11000)
        assertFalse(policy.evaluate(magnitude = 25f, threshold = threshold, nowMs = 11017)) // still settling
        assertTrue(policy.evaluate(magnitude = 25f, threshold = threshold, nowMs = 11300))
    }
}
