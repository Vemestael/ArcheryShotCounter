package com.vemestael.archeryshotcounter.presentation

/**
 * Pure decision logic for shot detection: given a magnitude sample and the current time,
 * decides whether it counts as a shot. Kept free of any Android Sensor APIs so it can be
 * unit tested without a device or Robolectric.
 *
 * After a sample counts as a shot, further samples are ignored until [onRegistered] is called
 * again — this is what prevented a single batched delivery from counting one shot twice (see
 * ShotDetector's kdoc). [settleMs] additionally discards samples for a brief window right after
 * [onRegistered], because re-registering itself can immediately catch wrist motion already in
 * progress (observed in practice as little as 11-17ms after registration).
 */
class ShotDetectionPolicy(private val settleMs: Long = SETTLE_MS_DEFAULT) {

    companion object {
        const val SETTLE_MS_DEFAULT = 300L
    }

    private var awaitingReregister = false
    private var listeningSinceMs = 0L

    /** Call when the sensor listener is (re)registered at [nowMs]. */
    fun onRegistered(nowMs: Long) {
        awaitingReregister = false
        listeningSinceMs = nowMs
    }

    /**
     * Evaluates one sample. Returns true at most once per [onRegistered] call: once it returns
     * true, further calls return false until [onRegistered] is invoked again.
     */
    fun evaluate(magnitude: Float, threshold: Float, nowMs: Long): Boolean {
        if (awaitingReregister) return false
        if (nowMs - listeningSinceMs < settleMs) return false
        if (magnitude <= threshold) return false
        awaitingReregister = true
        return true
    }
}
