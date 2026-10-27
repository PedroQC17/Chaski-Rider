package com.example.chaskirider.domain.orders

//zonas de demanda de los pedidos
data class DemandZone(val id: String, val name: String, val level: String,
    val latitude: Double, val longitude: Double, val radiusMeters: Int, val updatedAt: Long)
data class DeliveryOffer(val id: String, val expiresAt: Long, val quote: DeliveryQuote,
    // razon de asignacion del pedido
    val reason: String? = null, val reasonMeters: Int? = null)
data class OfferSnapshot(val serverTime: Long, val pickedUp: Boolean,
    val batch: DeliveryQuote?, val offer: DeliveryOffer?, val merchant: RoutePoint,
    val merchantName: String? = null, val merchantZone: String? = null,
    val demandZones: List<DemandZone> = emptyList(),
    // llegada de establecimiento
    val arrivedAt: Long? = null, val waitStartedAt: Long? = null,
    val isNotReadyReported: Boolean = false, val waitingCompensationCents: Int = 0,
    // tiempo de recojo y entrega
    val pickedUpAt: Long? = null, val deliveredAt: Long? = null,
    val isDelivered: Boolean = false, val merchantAddress: String? = null,
    val problem: OrderProblem? = null,
    val arrivalLocation: RoutePoint? = null,
    val acceptedAt: Long? = null)
enum class OfferAction { STATE, OFFER, ACCEPT, REJECT, ARRIVE_MERCHANT, REPORT_NOT_READY, PICKUP, DELIVER, RESET }
enum class OrderProblemReason { LOCAL_CLOSED, ORDER_UNAVAILABLE, VEHICLE_PROBLEM, CUSTOMER_UNREACHABLE, ADDRESS_PROBLEM, OTHER }
data class OrderProblem(val id: String, val orderId: String, val reason: OrderProblemReason,
    val detail: String, val createdAt: Long, val pickedUp: Boolean, val cancelled: Boolean)
interface OfferRepository {
    suspend fun reportProblem(reason: OrderProblemReason, detail: String, cancel: Boolean): OfferSnapshot =
        throw OfferRequestException(501)
    suspend fun execute(
        action: OfferAction,
        offerId: String? = null,
        lat: Double? = null,
        lng: Double? = null
    ): OfferSnapshot
}
class OfferRequestException(val status: Int) : Exception()
