package com.example.chaskirider.dashboard.viewmodel

import com.example.chaskirider.core.platform.location.GeoPoint
import com.example.chaskirider.feature.user.domain.RiderUser
import com.example.chaskirider.feature.orders.domain.DemandZone

data class HomeUiState(
    val user: RiderUser? = null,
    val isUpdatingAvailability: Boolean = false,
    val isLocating: Boolean = false,
    val hasLocationPermission: Boolean = false,
    val location: GeoPoint? = null,
    val cameraRequest: Int = 0,
    val errorMessage: String? = null,
    val locationUnavailable: Boolean = false,
    // HU05: zonas de demanda, última actualización y selección.
    val demandZones: List<DemandZone> = emptyList(),
    val zonesLoaded: Boolean = false,
    val zonesUpdatedAt: Long? = null,
    val isRefreshingZones: Boolean = false,
    val selectedZone: DemandZone? = null
)
