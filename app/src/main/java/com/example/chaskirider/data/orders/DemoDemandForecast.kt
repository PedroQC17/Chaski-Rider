package com.example.chaskirider.data.orders

import android.content.res.AssetManager
import com.example.chaskirider.domain.orders.DemandZone
import org.json.JSONObject

class DemoDemandForecast(assets: AssetManager) {
    private val data by lazy {
        assets.open("demo_demand.json").bufferedReader().use { JSONObject(it.readText()) }
    }

    fun zones(hour: Long): List<DemandZone> {
        val values = data.getJSONObject("hours").optJSONObject(hour.toString()) ?: return emptyList()
        val centers = data.getJSONObject("centers")
        return values.keys().asSequence().mapNotNull { id ->
            val count = values.getDouble(id)
            val level = when {
                count >= 8 -> "HIGH"
                count >= 4 -> "MEDIUM"
                count >= 1 -> "LOW"
                else -> return@mapNotNull null
            }
            val center = centers.getJSONArray(id)
            DemandZone(id, center.getString(0), "", center.getDouble(1), center.getDouble(2),
                600, hour * 3_600_000L, level)
        }.toList()
    }
}
