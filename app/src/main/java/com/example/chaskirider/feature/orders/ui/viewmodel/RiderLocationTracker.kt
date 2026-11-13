package com.example.chaskirider.feature.orders.ui.viewmodel

import com.example.chaskirider.core.platform.location.GeoPoint
import com.example.chaskirider.core.platform.location.LocationRepository
import com.example.chaskirider.core.platform.location.LocationUtils
import com.example.chaskirider.feature.orders.domain.OrderPolicy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/** Ubicación del repartidor: geocerca, distancias y tracking periódico hacia el cliente. */
internal class RiderLocationTracker(
    private val scope: CoroutineScope,
    private val locations: LocationRepository,
    private val elapsed: () -> Long,
    private val state: MutableStateFlow<OrdersUiState>,
) {
    private var tracking: Job? = null
    private var locationJob: Job? = null
    private var confirmedAt: Long? = null

    fun update(point: GeoPoint) {
        if (!point.latitude.isFinite() || !point.longitude.isFinite() ||
            point.latitude !in -90.0..90.0 || point.longitude !in -180.0..180.0) {
            invalidate(); return
        }
        confirmedAt = elapsed()
        state.update { it.copy(riderLocation = point) }
        recomputeDistances()
    }

    fun invalidate() {
        confirmedAt = null
        state.update {
            it.copy(distanceToMerchantMeters = null, distanceToCustomerMeters = null,
                isWithinGeofence = false, isNearCustomer = false)
        }
    }

    fun recomputeDistances() {
        val point = state.value.riderLocation ?: return
        if (confirmedAt?.let { elapsed() - it in 0..30_000 } != true) { invalidate(); return }
        val snapshot = state.value.snapshot
        val active = snapshot?.offer != null || snapshot?.batch != null
        val merchant = snapshot?.merchant?.takeIf { active }
        val customer = snapshot?.batch?.stops?.firstOrNull()?.point
        val merchantDistance = merchant?.let {
            LocationUtils.distanceMeters(point.latitude, point.longitude, it.latitude, it.longitude)
        }
        val customerDistance = customer?.let {
            LocationUtils.distanceMeters(point.latitude, point.longitude, it.latitude, it.longitude)
        }
        state.update {
            it.copy(distanceToMerchantMeters = merchantDistance,
                distanceToCustomerMeters = customerDistance,
                isWithinGeofence = merchant?.let { target -> OrderPolicy.isNear(point.latitude, point.longitude, target) } == true,
                isNearCustomer = customer?.let { target -> OrderPolicy.isNear(point.latitude, point.longitude, target) } == true)
        }
    }

    fun fetch(isCurrent: () -> Boolean) {
        locationJob?.cancel()
        locationJob = scope.launch {
            val point = locations.currentLocation().getOrNull()
            if (isCurrent()) {
                if (point != null) update(point) else invalidate()
            }
        }
    }

    fun restartTracking(active: Boolean) {
        state.update { it.copy(isTrackingActive = active) }
        if (!active) { tracking?.cancel(); return }
        if (tracking?.isActive == true) return
        tracking = scope.launch {
            while (isActive) {
                val point = locations.currentLocation().getOrNull()
                if (point != null) update(point) else invalidate()
                delay(5_000)
            }
        }
    }

    fun stop() {
        tracking?.cancel()
        locationJob?.cancel()
        invalidate()
    }
}
