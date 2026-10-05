package com.example.chaskirider.domain.orders

// HU05: zona de demanda con su nivel (HIGH/MEDIUM/LOW) y última actualización.
data class DemandZone(val id: String, val name: String, val level: String,
    val latitude: Double, val longitude: Double, val radiusMeters: Int, val updatedAt: Long)
data class DeliveryOffer(val id: String, val expiresAt: Long, val quote: DeliveryQuote,
    // HU06: razón real de asignación (p. ej. proximidad) con su distancia en metros.
    val reason: String? = null, val reasonMeters: Int? = null)
data class OfferSnapshot(val serverTime: Long, val pickedUp: Boolean,
    val batch: DeliveryQuote?, val offer: DeliveryOffer?, val merchant: RoutePoint,
    val merchantName: String? = null, val merchantZone: String? = null,
    val demandZones: List<DemandZone> = emptyList(),
    // HU08: estado de llegada al establecimiento, reporte de pedido no listo y compensación por espera.
    val arrivedAt: Long? = null, val waitStartedAt: Long? = null,
    val isNotReadyReported: Boolean = false, val waitingCompensationCents: Int = 0,
    // HU09: timestamps de recojo y entrega al cliente.
    val pickedUpAt: Long? = null, val deliveredAt: Long? = null,
    val isDelivered: Boolean = false)
enum class OfferAction { STATE, OFFER, ACCEPT, REJECT, ARRIVE_MERCHANT, REPORT_NOT_READY, PICKUP, DELIVER, RESET }
interface OfferRepository {
    suspend fun execute(
        action: OfferAction,
        offerId: String? = null,
        lat: Double? = null,
        lng: Double? = null
    ): OfferSnapshot
}
class OfferRequestException(val status: Int) : Exception()
