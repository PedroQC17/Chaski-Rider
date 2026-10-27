package com.example.chaskirider.data.orders

import com.example.chaskirider.domain.location.LocationUtils
import com.example.chaskirider.domain.orders.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.random.Random

class LocalDemoOfferRepository(
    private val store: DemoOrderStore,
    private val currentUid: () -> String?,
    private val catalog: suspend () -> List<DemoOrder>,
    private val clock: () -> Long = System::currentTimeMillis,
    private val hasOffer: () -> Boolean = { Random.nextInt(100) < 70 }
) : OfferRepository {
    private val mutex = Mutex()
    override suspend fun execute(action: OfferAction, offerId: String?, lat: Double?, lng: Double?): OfferSnapshot = mutex.withLock {
        val uid = currentUid() ?: throw OfferRequestException(401)
        val now = clock()
        val saved = store.load(uid) ?: DemoOrderState(empty(now))
        var snapshot = saved.snapshot.copy(serverTime = now)
        var routeId = saved.catalogRouteId
        val recent = saved.recent.filterValues { now - it < 5 * 60_000 }.toMutableMap()
        if (snapshot.offer != null && snapshot.offer!!.expiresAt <= now) snapshot = empty(now)
        snapshot = when (action) {
            OfferAction.STATE -> snapshot
            OfferAction.OFFER -> {
                if (snapshot.offer != null || snapshot.batch != null) snapshot
                else {
                    require(lat != null && lng != null && lat.isFinite() && lng.isFinite()) { "Ubicación necesaria" }
                    val order = DemoOrderCatalog.nearest(catalog(), lat, lng, recent.keys)
                    if (order == null || !hasOffer()) empty(now) else {
                        recent[order.id] = now
                        routeId = order.id
                        offer(order, lat, lng, now)
                    }
                }
            }
            OfferAction.ACCEPT -> {
                val offer = snapshot.offer ?: throw OfferRequestException(409)
                if (offer.id != offerId) throw OfferRequestException(409)
                snapshot.copy(batch = offer.quote, offer = null, acceptedAt = now)
            }
            OfferAction.REJECT -> {
                if (snapshot.offer?.id != offerId || offerId == null) throw OfferRequestException(409)
                empty(now)
            }
            OfferAction.ARRIVE_MERCHANT -> {
                checkActive(snapshot)
                if (snapshot.pickedUp) throw OfferRequestException(409)
                OrderLocationRules.requireNear(lat, lng, snapshot.merchant)
                if (snapshot.arrivedAt != null) snapshot else snapshot.copy(
                    arrivedAt = now, arrivalLocation = RoutePoint(requireNotNull(lat), requireNotNull(lng)))
            }
            OfferAction.REPORT_NOT_READY -> {
                checkActive(snapshot)
                if (snapshot.arrivedAt == null || snapshot.pickedUp) throw OfferRequestException(409)
                OrderLocationRules.requireNear(lat, lng, snapshot.merchant)
                snapshot.copy(isNotReadyReported = true, waitStartedAt = snapshot.waitStartedAt ?: now)
            }
            OfferAction.PICKUP -> {
                checkActive(snapshot)
                if (snapshot.arrivedAt == null) throw OfferRequestException(409)
                OrderLocationRules.requireNear(lat, lng, snapshot.merchant)
                snapshot.copy(pickedUp = true, pickedUpAt = snapshot.pickedUpAt ?: now)
            }
            OfferAction.DELIVER -> {
                if (snapshot.batch == null || !snapshot.pickedUp) throw OfferRequestException(409)
                OrderLocationRules.requireNear(lat, lng, snapshot.batch!!.stops.first().point)
                snapshot.copy(isDelivered = true, deliveredAt = snapshot.deliveredAt ?: now)
            }
            OfferAction.RESET -> {
                if (snapshot.batch != null && !snapshot.isDelivered) throw OfferRequestException(409)
                empty(now)
            }
        }
        val waitEnd = snapshot.pickedUpAt ?: now
        val extraMillis = snapshot.waitStartedAt?.let { (waitEnd - it - 300_000).coerceAtLeast(0) } ?: 0
        snapshot = snapshot.copy(waitingCompensationCents = ((extraMillis + 59_999) / 60_000).toInt() * 20)
        store.save(uid, DemoOrderState(snapshot, recent, saved.problems.orEmpty(), routeId))
        snapshot
    }
    override suspend fun reportProblem(reason: OrderProblemReason, detail: String, cancel: Boolean): OfferSnapshot = mutex.withLock {
        val uid = currentUid() ?: throw OfferRequestException(401)
        val saved = store.load(uid) ?: throw OfferRequestException(409)
        val snapshot = saved.snapshot
        checkActive(snapshot)
        if (cancel && snapshot.pickedUp) throw OfferRequestException(409)
        val allowed = if (snapshot.pickedUp) setOf(OrderProblemReason.CUSTOMER_UNREACHABLE,
            OrderProblemReason.ADDRESS_PROBLEM, OrderProblemReason.VEHICLE_PROBLEM, OrderProblemReason.OTHER)
            else setOf(OrderProblemReason.LOCAL_CLOSED, OrderProblemReason.ORDER_UNAVAILABLE,
                OrderProblemReason.VEHICLE_PROBLEM, OrderProblemReason.OTHER)
        if (reason !in allowed || detail.trim().length !in 10..500) throw OfferRequestException(400)
        if (!cancel && snapshot.problem != null) return@withLock snapshot
        val now = clock()
        val problem = OrderProblem(UUID.randomUUID().toString(), snapshot.batch!!.stops.first().orderId,
            reason, detail.trim(), now, snapshot.pickedUp, cancel)
        val result = if (cancel) empty(now) else snapshot.copy(serverTime = now, problem = problem)
        store.save(uid, saved.copy(snapshot = result,
            recent = if (cancel) saved.recent + ((saved.catalogRouteId ?: problem.orderId) to now) else saved.recent,
            problems = saved.problems.orEmpty() + problem))
        result
    }
    private fun checkActive(snapshot: OfferSnapshot) {
        if (snapshot.batch == null || snapshot.isDelivered) throw OfferRequestException(409)
    }
    private fun empty(now: Long) = OfferSnapshot(now, false, null, null, RoutePoint(0.0, 0.0))
    private fun offer(order: DemoOrder, lat: Double, lng: Double, now: Long): OfferSnapshot {
        val approach = LocationUtils.distanceMeters(lat, lng, order.pickup.latitude, order.pickup.longitude)
        val delivery = LocationUtils.distanceMeters(order.pickup.latitude, order.pickup.longitude, order.delivery.latitude, order.delivery.longitude)
        val approachCents = (approach / 1000.0 * 80).roundToInt()
        val deliveryCents = (delivery / 1000.0 * 120).roundToInt()
        val guarantee = max(0, 600 - 300 - approachCents - deliveryCents)
        val quote = DeliveryQuote(
            listOf(DeliveryStop("1", UUID.randomUUID().toString(), order.deliveryZone, order.delivery, order.deliveryAddress)),
            approach, delivery, ((approach + delivery) / 4.0).roundToInt() + 300,
            300, approachCents, deliveryCents, order.tipCents, guarantee,
            300 + approachCents + deliveryCents + order.tipCents + guarantee, 0, emptyList()
        )
        return OfferSnapshot(now, false, null, DeliveryOffer(UUID.randomUUID().toString(), now + 45_000, quote, "PROXIMITY", approach),
            order.pickup, order.merchantName, order.pickupZone, merchantAddress = order.pickupAddress)
    }
}
