package com.example.chaskirider.feature.orders.data

import com.example.chaskirider.feature.orders.data.local.DemoOrder
import com.example.chaskirider.feature.orders.data.local.DemoOrderCatalog
import com.example.chaskirider.feature.orders.data.local.DemoOrderState
import com.example.chaskirider.feature.orders.data.local.DemoOrderStore
import com.example.chaskirider.core.platform.location.LocationUtils
import com.example.chaskirider.feature.orders.domain.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID
import kotlin.math.roundToInt
import kotlin.random.Random
import com.example.chaskirider.feature.orders.data.local.DemoOrderDao
import com.example.chaskirider.platform.OrderSyncWorker
import com.example.chaskirider.core.network.DeviceConnectivity
import android.content.Context
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.example.chaskirider.feature.orders.domain.OrderSyncRepository
import com.example.chaskirider.feature.orders.domain.OrderSyncState
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged

class OrderRepositoryImpl(
    private val store: DemoOrderStore,
    private val currentUid: () -> String?,
    private val catalog: suspend () -> List<DemoOrder>,
    private val clock: () -> Long = System::currentTimeMillis,
    private val hasOffer: () -> Boolean = { Random.nextInt(100) < 70 },
    private val verifyPin: suspend (String, String, String) -> Unit = { _, _, _ -> throw OfferRequestException(503) },
    private val resolveAbsence: suspend (String, String) -> Long = { _, _ -> throw OfferRequestException(503) },
) : OfferRepository {
    private val mutex = Mutex()
    override suspend fun execute(
        action: OfferAction,
        offerId: String?,
        lat: Double?,
        lng: Double?,
        evidenceUrl: String?,
        deliveryNote: String?,
        deliveryPin: String?,
    ): OfferSnapshot = mutex.withLock {
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
                OrderPolicy.requireNear(lat, lng, snapshot.merchant)
                if (snapshot.arrivedAt != null) snapshot else snapshot.copy(
                    arrivedAt = now, arrivalLocation = RoutePoint(requireNotNull(lat), requireNotNull(lng)))
            }
            OfferAction.REPORT_NOT_READY -> {
                checkActive(snapshot)
                if (snapshot.arrivedAt == null || snapshot.pickedUp) throw OfferRequestException(409)
                OrderPolicy.requireNear(lat, lng, snapshot.merchant)
                snapshot.copy(isNotReadyReported = true, waitStartedAt = snapshot.waitStartedAt ?: now)
            }
            OfferAction.PICKUP -> {
                checkActive(snapshot)
                if (snapshot.arrivedAt == null) throw OfferRequestException(409)
                OrderPolicy.requireNear(lat, lng, snapshot.merchant)
                snapshot.copy(pickedUp = true, pickedUpAt = snapshot.pickedUpAt ?: now)
            }
            OfferAction.DELIVER -> {
                if (snapshot.isDelivered) return@withLock snapshot
                checkActive(snapshot)
                if (!snapshot.pickedUp) throw OfferRequestException(409)
                val orderId = snapshot.batch!!.stops.single().orderId
                OrderPolicy.requireNear(lat, lng, snapshot.batch.stops.single().point)
                if (snapshot.paidByCustomer) {
                    if (deliveryPin == null || !deliveryPin.matches(Regex("[0-9]{4}"))) throw OfferRequestException(428)
                    verifyPin(uid, orderId, deliveryPin)
                    if (currentUid() != uid) throw OfferRequestException(401)
                }
                snapshot.copy(isDelivered = true, deliveredAt = now, evidencePhotoUrl = null, deliveryNote = deliveryNote)
            }
            OfferAction.RESET -> {
                if (snapshot.batch != null && !snapshot.isCompleted) throw OfferRequestException(409)
                empty(now)
            }
        }
        snapshot = snapshot.copy(waitingCompensationCents = 0)
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
            reason, detail.trim(), now, snapshot.pickedUp, cancel, OrderRecord.from(uid, snapshot)!!.status)
        val result = if (cancel) empty(now) else snapshot.copy(serverTime = now, problem = problem)
        store.save(uid, saved.copy(snapshot = result,
            recent = if (cancel) saved.recent + ((saved.catalogRouteId ?: problem.orderId) to now) else saved.recent,
            problems = saved.problems.orEmpty() + problem))
        result
    }
    override suspend fun resolveCustomerAbsence(orderId: String): OfferSnapshot = mutex.withLock {
        val uid = currentUid() ?: throw OfferRequestException(401)
        val saved = store.load(uid) ?: throw OfferRequestException(409)
        val snapshot = saved.snapshot
        if (snapshot.batch?.stops?.singleOrNull()?.orderId != orderId) throw OfferRequestException(409)
        if (snapshot.completedUnsuccessfullyAt != null) return@withLock snapshot
        checkActive(snapshot)
        if (!snapshot.pickedUp) throw OfferRequestException(409)
        val completedAt = resolveAbsence(uid, orderId)
        if (currentUid() != uid) throw OfferRequestException(401)
        require(completedAt > 0)
        val result = snapshot.copy(completedUnsuccessfullyAt = completedAt, serverTime = clock())
        store.save(uid, saved.copy(snapshot = result))
        result
    }

    private fun checkActive(snapshot: OfferSnapshot) {
        if (snapshot.batch == null || snapshot.isCompleted) throw OfferRequestException(409)
    }
    private fun empty(now: Long) = OfferSnapshot(now, false, null, null, RoutePoint(0.0, 0.0))
    private fun offer(order: DemoOrder, lat: Double, lng: Double, now: Long): OfferSnapshot {
        val approach = LocationUtils.distanceMeters(lat, lng, order.pickup.latitude, order.pickup.longitude)
        val delivery = LocationUtils.distanceMeters(order.pickup.latitude, order.pickup.longitude, order.delivery.latitude, order.delivery.longitude)
        val routeCents = OrderPolicy.routeCents(approach, delivery)
        val approachCents = OrderPolicy.routeCents(approach, 0)
        val quote = DeliveryQuote(
            listOf(DeliveryStop("1", UUID.randomUUID().toString(), order.deliveryZone, order.delivery, order.deliveryAddress)),
            approach, delivery, ((approach + delivery) / 4.0).roundToInt() + 300,
            0, approachCents, routeCents - approachCents, order.tipCents, 0,
            routeCents + order.tipCents, 0, emptyList(), "PEN_1_KM_V1"
        )
        return OfferSnapshot(now, false, null, DeliveryOffer(UUID.randomUUID().toString(), now + 45_000, quote, "PROXIMITY", approach),
            order.pickup, order.merchantName, order.pickupZone, merchantAddress = order.pickupAddress, paidByCustomer = order.paidByCustomer)
    }
}

class OrderSyncRepositoryImpl(
    private val context: Context,
    private val dao: DemoOrderDao,
    private val connectivity: DeviceConnectivity
) : OrderSyncRepository {
    override fun observe(uid: String) = combine(dao.observePendingCount(uid),
        WorkManager.getInstance(context).getWorkInfosForUniqueWorkFlow("order-sync-$uid"), connectivity.online
    ) { count, work, online ->
        val active = work.any { it.state in listOf(WorkInfo.State.RUNNING, WorkInfo.State.ENQUEUED, WorkInfo.State.BLOCKED) }
        OrderSyncState(online, count, work.any { it.state == WorkInfo.State.RUNNING },
            count > 0 && !active && work.any { it.state == WorkInfo.State.FAILED })
    }.distinctUntilChanged()

    override fun retry(uid: String) {
        check(FirebaseAuth.getInstance().currentUser?.uid == uid)
        OrderSyncWorker.schedule(context, uid, force = true)
    }
}
