package com.example.chaskirider.feature.orders.domain

import com.example.chaskirider.core.platform.location.LocationUtils

object OrderPolicy {
    const val RADIUS_METERS = 100

    fun routeCents(approachMeters: Int, deliveryMeters: Int): Int {
        require(approachMeters >= 0 && deliveryMeters >= 0)
        return Math.toIntExact((approachMeters.toLong() + deliveryMeters + 5) / 10)
    }

    fun usableFix(lat: Double, lng: Double, accuracyMeters: Float, ageMillis: Long): Boolean =
        LocationUtils.usableFix(lat, lng, accuracyMeters, ageMillis)

    fun isNear(lat: Double?, lng: Double?, target: RoutePoint): Boolean =
        lat != null && lng != null && lat.isFinite() && lng.isFinite() &&
            lat in -90.0..90.0 && lng in -180.0..180.0 &&
            LocationUtils.exactDistanceMeters(lat, lng, target.latitude, target.longitude) <= RADIUS_METERS

    fun requireNear(lat: Double?, lng: Double?, target: RoutePoint) {
        if (lat == null || lng == null || !lat.isFinite() || !lng.isFinite() ||
            lat !in -90.0..90.0 || lng !in -180.0..180.0) throw OfferRequestException(422)
        if (!isNear(lat, lng, target)) throw OfferRequestException(412)
    }
}
