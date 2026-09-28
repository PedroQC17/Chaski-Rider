// HU06 - Parte 1: modelo de oferta de pedido para el repartidor.
// Contiene lo mínimo que la pantalla de oferta debe mostrar: recogida,
// entrega, precio, distancia, la razón real de asignación (criterio HU06)
// y el tiempo de expiración de la oferta.
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
