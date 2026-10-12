package com.example.chaskirider.data.orders

import com.example.chaskirider.domain.model.RegistrationStatus
import com.example.chaskirider.domain.orders.*
import com.example.chaskirider.domain.repository.LocationRepository
import com.example.chaskirider.domain.repository.RiderSession

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
