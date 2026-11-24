package com.example.chaskirider.feature.orders

import com.example.chaskirider.dashboard.viewmodel.HomeUiState
import com.example.chaskirider.feature.orders.domain.DemandRecommendationService
import com.example.chaskirider.feature.orders.domain.DemandZone
import com.example.chaskirider.feature.rider.domain.Rider
import org.junit.Assert.*
import org.junit.Test

class DemandRecommendationTest {
    private val hour = 18 * 3_600_000L

    private fun zone(id: String, level: String, forecast: String?) = DemandZone(
        id = id,
        name = id,
        level = level,
        latitude = -12.0,
        longitude = -77.0,
        radiusMeters = 600,
        updatedAt = hour,
        forecastLevel = forecast
    )

    @Test fun picksHighestForecastAndBuildsOneHourPeriod() {
        val recommendation = DemandRecommendationService.recommend(
            listOf(
                zone("low", "", "LOW"),
                zone("high", "", "HIGH"),
                zone("medium", "", "MEDIUM")
            )
        )

        assertNotNull(recommendation)
        assertEquals("high", recommendation!!.zone.id)
        assertEquals("HIGH", recommendation.level)
        assertEquals(hour, recommendation.periodStart)
        assertEquals(hour + 3_600_000L, recommendation.periodEnd)
    }

    @Test fun returnsNullWhenThereIsNoUsefulForecast() {
        assertNull(DemandRecommendationService.recommend(emptyList()))
        assertNull(DemandRecommendationService.recommend(listOf(zone("unknown", "", null))))
        assertNull(DemandRecommendationService.recommend(listOf(zone("unknown", "", "UNKNOWN"))))
    }

    @Test fun recommendationDoesNotChangeAvailability() {
        val state = HomeUiState()
            .copy(user = Rider(id = "alice", isAvailable = false))
            .updateZones(forecast = listOf(zone("high", "", "HIGH")))

        assertNotNull(state.recommendation)
        assertEquals(false, state.user?.isAvailable)
    }
}
