
package com.example.chaskirider.domain.model

data class RideOffer(
    val id: String = "",
    val pickupAddress: String = "",
    val destinationAddress: String = "",
    val fareSoles: Double = 0.0,
    val distanceKm: Double = 0.0,
    val reason: String = "",
    val expiresInSeconds: Int = DEFAULT_EXPIRY_SECONDS
) {
    companion object {
        const val DEFAULT_EXPIRY_SECONDS = 30
    }
}
