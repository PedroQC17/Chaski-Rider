package com.example.chaskirider.feature.orders.domain

import kotlinx.coroutines.flow.Flow

data class RoutePoint(val latitude: Double, val longitude: Double)

//paradero de entrega donde se debe entregar el pedido
data class DeliveryStop(val id: String, val orderId: String, val zone: String?, val point: RoutePoint,
    val address: String? = null, val customerPhone: String? = null)
data class DeliveryQuote(
    val stops: List<DeliveryStop>, val approachMeters: Int, val deliveryMeters: Int,
    val durationSeconds: Int, val baseCents: Int, val approachCents: Int, val deliveryCents: Int,
    val tipCents: Int, val guaranteeCents: Int, val totalCents: Int, val additionalCents: Int,
    val path: List<RoutePoint>,
    val pricingVersion: String? = null
)

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
    val acceptedAt: Long? = null,
    val arrivalLocation: RoutePoint? = null,
    val paidByCustomer: Boolean = false,
    val requiresPhoto: Boolean = false,
    val evidencePhotoUrl: String? = null,
    val deliveryNote: String? = null,
    val completedUnsuccessfullyAt: Long? = null) {
    val isCompleted get() = isDelivered || completedUnsuccessfullyAt != null
}
enum class OfferAction { STATE, OFFER, ACCEPT, REJECT, ARRIVE_MERCHANT, REPORT_NOT_READY, PICKUP, DELIVER, RESET }
enum class OrderProblemReason { LOCAL_CLOSED, ORDER_UNAVAILABLE, VEHICLE_PROBLEM, CUSTOMER_UNREACHABLE, ADDRESS_PROBLEM, OTHER }
data class OrderProblem(val id: String, val orderId: String, val reason: OrderProblemReason,
    val detail: String, val createdAt: Long, val pickedUp: Boolean, val cancelled: Boolean,
    val orderStatus: OrderStatus? = null)

data class AcceptedEarningsQuote(
    val approachMeters: Int,
    val deliveryMeters: Int,
    val chaskiCents: Int,
    val tipCents: Int,
    val totalCents: Int,
    val pricingVersion: String,
    val distanceSource: String
) {
    fun valid() = approachMeters in 0..1_000_000 && deliveryMeters in 0..1_000_000 && chaskiCents >= 0 && tipCents >= 0 &&
        totalCents.toLong() == chaskiCents.toLong() + tipCents &&
        pricingVersion in listOf("PEN_1_KM_V1", "LEGACY_QUOTE") && distanceSource == "SIMULATED_GEODESIC" &&
        (pricingVersion != "PEN_1_KM_V1" || chaskiCents == OrderPolicy.routeCents(approachMeters, deliveryMeters))

    companion object {
        fun from(quote: DeliveryQuote) = AcceptedEarningsQuote(
            quote.approachMeters, quote.deliveryMeters, quote.totalCents - quote.tipCents,
            quote.tipCents, quote.totalCents, quote.pricingVersion ?: "LEGACY_QUOTE", "SIMULATED_GEODESIC")
    }
}

enum class OrderStatus { ACCEPTED, AT_MERCHANT, ON_THE_WAY, DELIVERED, CANCELLED, COMPLETED_UNSUCCESSFUL }

data class OrderRecord(
    val orderId: String,
    val riderId: String,
    val status: OrderStatus,
    val merchantName: String,
    val pickup: RoutePoint,
    val destination: RoutePoint,
    val acceptedAt: Long?,
    val arrivedAt: Long?,
    val pickedUpAt: Long?,
    val deliveredAt: Long?,
    val cancelledAt: Long? = null,
    val problems: List<OrderProblem>? = emptyList(),
    val waitStartedAt: Long? = null,
    val evidencePhotoUrl: String? = null,
    val deliveryNote: String? = null,
    val paidByCustomer: Boolean = false,
    val earningsQuote: AcceptedEarningsQuote? = null,
    val completedAt: Long? = null,
    val completionReason: String? = null
) {
    companion object {
        fun from(uid: String, snapshot: OfferSnapshot): OrderRecord? {
            val stop = snapshot.batch?.stops?.singleOrNull() ?: return null
            val status = when {
                snapshot.completedUnsuccessfullyAt != null -> OrderStatus.COMPLETED_UNSUCCESSFUL
                snapshot.isDelivered -> OrderStatus.DELIVERED
                snapshot.pickedUp -> OrderStatus.ON_THE_WAY
                snapshot.arrivedAt != null -> OrderStatus.AT_MERCHANT
                else -> OrderStatus.ACCEPTED
            }
            return OrderRecord(stop.orderId, uid, status, snapshot.merchantName.orEmpty(),
                snapshot.merchant, stop.point, snapshot.acceptedAt, snapshot.arrivedAt,
                snapshot.pickedUpAt, snapshot.deliveredAt, waitStartedAt = snapshot.waitStartedAt,
                evidencePhotoUrl = snapshot.evidencePhotoUrl, deliveryNote = snapshot.deliveryNote, paidByCustomer = snapshot.paidByCustomer,
                earningsQuote = snapshot.batch?.let { AcceptedEarningsQuote.from(it) },
                completedAt = snapshot.completedUnsuccessfullyAt,
                completionReason = if (snapshot.completedUnsuccessfullyAt != null) "CUSTOMER_UNREACHABLE" else null)
        }
    }
}

data class CustomerContact(
    val arrivedAt: Long,
    val firstCallAt: Long? = null,
    val firstCallElapsed: Long? = null,
    val boot: Int? = null,
    val secondCallAt: Long? = null,
    val reminderSeen: Boolean = false,
    val phone: String? = null,
    val serverRegistered: Boolean = false
) {
    fun elapsedMillis(now: Long, elapsed: Long, currentBoot: Int): Long {
        val started = firstCallAt ?: return 0
        return if (boot == currentBoot && firstCallElapsed != null)
            (elapsed - firstCallElapsed).coerceAtLeast(0)
        else (now - started).coerceAtLeast(0)
    }
    fun canCallAgain(waited: Long) = firstCallAt == null || (secondCallAt == null && waited >= 180_000)
    fun canContactSupport(waited: Long) = firstCallAt != null && secondCallAt != null && waited >= 600_000
    fun showReminder(waited: Long) = firstCallAt != null && !reminderSeen && waited in 480_000 until 600_000
}

interface CustomerContactStore {
    suspend fun load(uid: String, orderId: String): CustomerContact?
    suspend fun save(uid: String, orderId: String, contact: CustomerContact)
}

data class ContactCheckpoint(val firstCallAt: Long? = null, val secondCallAt: Long? = null)
interface CustomerAbsenceService {
    suspend fun arrive(uid: String, orderId: String, latitude: Double, longitude: Double)
    suspend fun call(uid: String, orderId: String, attempt: Int): ContactCheckpoint
}

enum class PhoneCallState { IDLE, RINGING, OFFHOOK }

interface PhoneCalls {
    fun states(): Flow<PhoneCallState>
    fun start(number: String)
}

class CallAttemptDetector {
    private var state: PhoneCallState? = null
    private var armed = false

    fun arm(): Boolean {
        if (state != PhoneCallState.IDLE || armed) return false
        armed = true
        return true
    }

    fun cancel() { armed = false }

    fun onState(value: PhoneCallState): Boolean {
        val previous = state
        state = value
        if (value == PhoneCallState.RINGING) armed = false
        if (armed && previous == PhoneCallState.IDLE && value == PhoneCallState.OFFHOOK) {
            armed = false
            return true
        }
        return false
    }
}
