package com.example.chaskirider.feature.orders.domain

data class DemandRecommendation(
    val zone: DemandZone,
    val level: String,
    val periodStart: Long,
    val periodEnd: Long
)

object DemandRecommendationService {
    fun recommend(forecast: List<DemandZone>): DemandRecommendation? {
        val best = forecast
            .filter { rank(it.forecastLevel) > 0 }
            .maxWithOrNull(compareBy<DemandZone> { rank(it.forecastLevel) }.thenBy { it.name })
            ?: return null
        val level = best.forecastLevel ?: return null
        return DemandRecommendation(
            zone = best,
            level = level,
            periodStart = best.updatedAt,
            periodEnd = best.updatedAt + 3_600_000L
        )
    }

    private fun rank(level: String?): Int = when (level) {
        "HIGH" -> 3
        "MEDIUM" -> 2
        "LOW" -> 1
        else -> 0
    }
}
