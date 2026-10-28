package com.example.chaskirider.domain.orders

data class TrackingPosition(val latitude: Double, val longitude: Double, val accuracyMeters: Float, val recordedAt: Long) {
    fun isRecent(now: Long): Boolean = latitude.isFinite() && longitude.isFinite() &&
        latitude in -90.0..90.0 && longitude in -180.0..180.0 &&
        accuracyMeters.isFinite() && accuracyMeters in 0f..100f && recordedAt > 0 && now - recordedAt in 0..30_000
}

class OrderTracking(
    private val uid: String,
    private val orderId: String,
    private val currentOrder: suspend () -> OrderRecord?,
    private val currentPosition: suspend () -> TrackingPosition?,
    private val publish: suspend (TrackingPosition) -> Boolean,
    private val now: () -> Long = System::currentTimeMillis
) {
    enum class Result { STOPPED, WAITING_GPS, WAITING_SYNC, SENT }
    private suspend fun active(): Boolean = currentOrder()?.let {
        it.riderId == uid && it.orderId == orderId && it.status == OrderStatus.ON_THE_WAY && it.pickedUpAt != null
    } == true

    suspend fun update(): Result {
        if (!active()) return Result.STOPPED
        val position = currentPosition() ?: return Result.WAITING_GPS
        if (!active()) return Result.STOPPED
        if (!position.isRecent(now())) return Result.WAITING_GPS
        return if (publish(position)) Result.SENT else Result.WAITING_SYNC
    }
}
