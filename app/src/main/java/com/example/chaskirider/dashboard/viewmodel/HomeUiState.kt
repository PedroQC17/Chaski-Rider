package com.example.chaskirider.dashboard.viewmodel

import com.example.chaskirider.core.platform.location.GeoPoint
import com.example.chaskirider.feature.rider.domain.Rider
import com.example.chaskirider.feature.orders.domain.DemandRecommendation
import com.example.chaskirider.feature.orders.domain.DemandRecommendationService
import com.example.chaskirider.feature.orders.domain.DemandZone

data class HomeUiState(
    val user: Rider? = null,
    val isUpdatingAvailability: Boolean = false,
    val isLocating: Boolean = false,
    val hasLocationPermission: Boolean = false,
    val location: GeoPoint? = null,
    val cameraRequest: Int = 0,
    val errorMessage: String? = null,
    val locationUnavailable: Boolean = false,
    // HU05: zonas de demanda, última actualización y selección.
    val demandZones: List<DemandZone> = emptyList(),
    val currentZones: List<DemandZone> = emptyList(),
    val forecastZones: List<DemandZone> = emptyList(),
    val zonesLoaded: Boolean = false,
    val zonesUpdatedAt: Long? = null,
    val isRefreshingZones: Boolean = false,
    val selectedZone: DemandZone? = null,
    val recommendation: DemandRecommendation? = null,
    val forecastLoaded: Boolean = false
) {
    internal fun updateZones(
        current: List<DemandZone> = currentZones,
        forecast: List<DemandZone> = forecastZones
    ): HomeUiState {
        val combined = forecast.associateBy { it.id }.toMutableMap()
        for (zone in current) {
            val prediction = combined[zone.id]
            combined[zone.id] = if (prediction == null) zone else zone.copy(forecastLevel = prediction.forecastLevel)
        }
        return copy(
            currentZones = current,
            forecastZones = forecast,
            demandZones = combined.values.toList(),
            selectedZone = combined[selectedZone?.id],
            recommendation = DemandRecommendationService.recommend(forecast)
        )
    }
}
