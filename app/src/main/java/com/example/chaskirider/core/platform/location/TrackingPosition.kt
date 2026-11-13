package com.example.chaskirider.core.platform.location

data class TrackingPosition(val latitude: Double, val longitude: Double, val accuracyMeters: Float, val recordedAt: Long) {
    fun isRecent(now: Long): Boolean = latitude.isFinite() && longitude.isFinite() &&
        latitude in -90.0..90.0 && longitude in -180.0..180.0 &&
        accuracyMeters.isFinite() && accuracyMeters in 0f..100f && recordedAt > 0 && now - recordedAt in 0..30_000
}
