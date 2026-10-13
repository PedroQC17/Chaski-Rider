package com.example.chaskirider.domain.location

import com.example.chaskirider.domain.orders.RoutePoint
import kotlin.math.*

object LocationUtils {
    private const val EARTH_RADIUS_METERS = 6371000.0

    fun distanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Int {
        return exactDistanceMeters(lat1, lon1, lat2, lon2).roundToInt()
    }

    fun exactDistanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
        val bounded = a.coerceIn(0.0, 1.0)
        val c = 2 * atan2(sqrt(bounded), sqrt(1 - bounded))
        return EARTH_RADIUS_METERS * c
    }

    fun isWithinGeofence(
        riderLat: Double, riderLon: Double,
        targetLat: Double, targetLon: Double,
        radiusMeters: Int = 100
    ): Boolean {
        return distanceMeters(riderLat, riderLon, targetLat, targetLon) <= radiusMeters
    }

    fun isOffRoute(
        riderLat: Double, riderLon: Double,
        path: List<RoutePoint>,
        thresholdMeters: Int = 50
    ): Boolean {
        if (path.isEmpty()) return false
        var minDistance = Double.MAX_VALUE
        for (point in path) {
            val dist = distanceMeters(riderLat, riderLon, point.latitude, point.longitude).toDouble()
            if (dist < minDistance) minDistance = dist
        }
        return minDistance > thresholdMeters
    }
}
