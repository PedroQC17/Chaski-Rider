package com.example.chaskirider

import com.example.chaskirider.domain.orders.DemandZone
import com.example.chaskirider.ui.screens.home.HomeUiState
import org.junit.Assert.*
import org.junit.Test

class HomeDemandStateTest {
    @Test fun selectionFollowsZoneAcrossLiveAndForecastUpdates() {
        val blue = DemandZone("A", "Lince", "", -12.08, -77.03, 600, 1, "HIGH")
        val red = blue.copy(level = "LOW", forecastLevel = null)
        val selected = HomeUiState().updateZones(forecast = listOf(blue)).copy(selectedZone = blue)
        val both = selected.updateZones(current = listOf(red))
        assertEquals(listOf(red.copy(forecastLevel = "HIGH")), both.demandZones)
        assertEquals(both.demandZones.single(), both.selectedZone)
        val blueOnly = both.updateZones(current = emptyList())
        assertEquals(blue, blueOnly.selectedZone)
        val expired = blueOnly.updateZones(forecast = emptyList())
        assertTrue(expired.demandZones.isEmpty())
        assertNull(expired.selectedZone)
        assertNull(expired.updateZones(current = listOf(red)).selectedZone)
    }
}
