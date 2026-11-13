package com.example.chaskirider.feature.orders.domain

import com.example.chaskirider.core.platform.location.LocationRepository
import com.example.chaskirider.core.platform.location.TrackingPosition
import com.example.chaskirider.feature.user.domain.RegistrationStatus
import com.example.chaskirider.feature.user.domain.RiderSession

class OrderSearchEngine(
    private val repository: OfferRepository,
    private val session: RiderSession,
    private val locations: LocationRepository
) {
    fun canSearch(): Boolean = session.user.value?.let {
        it.isAvailable && it.isEnabled && it.status == RegistrationStatus.APPROVED && it.profilePhotoPath.isNotBlank()
    } == true

    suspend fun poll(search: Boolean): OfferSnapshot? {
        if (!canSearch()) return null
        val uid = session.user.value?.id
        val snapshot = repository.execute(OfferAction.STATE)
        if (session.user.value?.id != uid || !canSearch()) return null
        if (!search || snapshot.offer != null || snapshot.batch != null) return snapshot
        val point = locations.currentLocation().getOrNull() ?: return snapshot
        if (session.user.value?.id != uid || !canSearch()) return null
        return repository.execute(OfferAction.OFFER, lat = point.latitude, lng = point.longitude)
            .takeIf { session.user.value?.id == uid && canSearch() }
    }
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
