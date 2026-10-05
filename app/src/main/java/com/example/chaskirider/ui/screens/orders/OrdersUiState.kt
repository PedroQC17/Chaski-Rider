package com.example.chaskirider.ui.screens.orders

import com.example.chaskirider.domain.model.GeoPoint
import com.example.chaskirider.domain.orders.OfferSnapshot

data class OrdersUiState(
    val snapshot: OfferSnapshot? = null,
    val busy: Boolean = false,
    val secondsLeft: Int = 0,
    val error: Int? = null,
    val scheduling: Boolean = false,
    // HU08: estado de geocerca, ubicación GPS y tiempo de espera
    val riderLocation: GeoPoint? = null,
    val distanceToMerchantMeters: Int? = null,
    val isWithinGeofence: Boolean = false,
    val isOffRoute: Boolean = false,
    val elapsedWaitSeconds: Int = 0,
    // HU09: tracking hacia el cliente y entrega
    val distanceToCustomerMeters: Int? = null,
    val isTrackingActive: Boolean = false
)
