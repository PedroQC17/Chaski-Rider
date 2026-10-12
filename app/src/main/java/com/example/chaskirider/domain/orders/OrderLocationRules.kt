package com.example.chaskirider.domain.orders

import com.example.chaskirider.domain.location.LocationUtils

object OrderLocationRules {
    const val RADIUS_METERS = 100

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
