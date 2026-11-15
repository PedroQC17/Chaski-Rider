package com.example.chaskirider.feature.orders.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.example.chaskirider.core.platform.location.LocationUtils
import com.example.chaskirider.feature.orders.domain.AcceptedEarningsQuote
import com.example.chaskirider.feature.orders.domain.CustomerContact
import com.example.chaskirider.feature.orders.domain.CustomerContactStore
import com.example.chaskirider.feature.orders.domain.OfferSnapshot
import com.example.chaskirider.feature.orders.domain.OrderProblem
import com.example.chaskirider.feature.orders.domain.OrderProblemReason
import com.example.chaskirider.feature.orders.domain.OrderRecord
import com.example.chaskirider.feature.orders.domain.OrderStatus
import com.example.chaskirider.feature.orders.domain.RoutePoint
import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow

data class OrderSessionState(
    val snapshot: OfferSnapshot,
    val recent: Map<String, Long> = emptyMap(),
    val problems: List<OrderProblem>? = emptyList(),
    val catalogRouteId: String? = null
)

interface OrderSessionStore {
    suspend fun load(uid: String): OrderSessionState?
    suspend fun save(uid: String, state: OrderSessionState)
}

@Entity(tableName = "orders", primaryKeys = ["riderId", "orderId"])
data class OrderEntity(
    val riderId: String,
    val orderId: String,
    val status: String,
    val merchantName: String,
    val pickupLatitude: Double,
    val pickupLongitude: Double,
    val destinationLatitude: Double,
    val destinationLongitude: Double,
    val acceptedAt: Long? = null,
    val arrivedAt: Long? = null,
    val pickedUpAt: Long? = null,
    val deliveredAt: Long? = null,
    val cancelledAt: Long? = null,
    val waitStartedAt: Long? = null,
    val evidencePhotoUrl: String? = null,
    val deliveryNote: String? = null,
    val paidByCustomer: Boolean = false,
    val earningsApproachMeters: Int? = null,
    val earningsDeliveryMeters: Int? = null,
    val earningsChaskiCents: Int? = null,
    val earningsTipCents: Int? = null,
    val earningsTotalCents: Int? = null,
    val earningsPricingVersion: String? = null,
    val earningsDistanceSource: String? = null,
    val completedAt: Long? = null,
    val completionReason: String? = null,
    val revision: Long,
    val pending: Boolean
)

@Entity(tableName = "order_problems", primaryKeys = ["id"])
data class OrderProblemEntity(
    val id: String,
    val riderId: String,
    val orderId: String,
    val category: String,
    val detail: String,
    val occurredAt: Long,
    val pickedUp: Boolean,
    val cancelled: Boolean,
    val orderStatus: String?
)

fun OrderRecord.toEntity(revision: Long, pending: Boolean) = OrderEntity(
    riderId = riderId,
    orderId = orderId,
    status = status.name,
    merchantName = merchantName,
    pickupLatitude = pickup.latitude,
    pickupLongitude = pickup.longitude,
    destinationLatitude = destination.latitude,
    destinationLongitude = destination.longitude,
    acceptedAt = acceptedAt,
    arrivedAt = arrivedAt,
    pickedUpAt = pickedUpAt,
    deliveredAt = deliveredAt,
    cancelledAt = cancelledAt,
    waitStartedAt = waitStartedAt,
    evidencePhotoUrl = evidencePhotoUrl,
    deliveryNote = deliveryNote,
    paidByCustomer = paidByCustomer,
    earningsApproachMeters = earningsQuote?.approachMeters,
    earningsDeliveryMeters = earningsQuote?.deliveryMeters,
    earningsChaskiCents = earningsQuote?.chaskiCents,
    earningsTipCents = earningsQuote?.tipCents,
    earningsTotalCents = earningsQuote?.totalCents,
    earningsPricingVersion = earningsQuote?.pricingVersion,
    earningsDistanceSource = earningsQuote?.distanceSource,
    completedAt = completedAt,
    completionReason = completionReason,
    revision = revision,
    pending = pending
)

fun OrderEntity.toDomain(problems: List<OrderProblemEntity> = emptyList()) = OrderRecord(
    orderId = orderId,
    riderId = riderId,
    status = OrderStatus.entries.firstOrNull { it.name == status } ?: OrderStatus.ACCEPTED,
    merchantName = merchantName,
    pickup = RoutePoint(pickupLatitude, pickupLongitude),
    destination = RoutePoint(destinationLatitude, destinationLongitude),
    acceptedAt = acceptedAt,
    arrivedAt = arrivedAt,
    pickedUpAt = pickedUpAt,
    deliveredAt = deliveredAt,
    cancelledAt = cancelledAt,
    problems = problems.map { it.toDomain() },
    waitStartedAt = waitStartedAt,
    evidencePhotoUrl = evidencePhotoUrl,
    deliveryNote = deliveryNote,
    paidByCustomer = paidByCustomer,
    earningsQuote = if (earningsApproachMeters != null && earningsDeliveryMeters != null &&
        earningsChaskiCents != null && earningsTipCents != null && earningsTotalCents != null &&
        earningsPricingVersion != null && earningsDistanceSource != null)
        AcceptedEarningsQuote(earningsApproachMeters, earningsDeliveryMeters, earningsChaskiCents,
            earningsTipCents, earningsTotalCents, earningsPricingVersion, earningsDistanceSource) else null,
    completedAt = completedAt,
    completionReason = completionReason
)

fun OrderProblem.toEntity(riderId: String) = OrderProblemEntity(
    id = id,
    riderId = riderId,
    orderId = orderId,
    category = reason.name,
    detail = detail,
    occurredAt = createdAt,
    pickedUp = pickedUp,
    cancelled = cancelled,
    orderStatus = orderStatus?.name
)

fun OrderProblemEntity.toDomain() = OrderProblem(
    id = id,
    orderId = orderId,
    reason = OrderProblemReason.entries.firstOrNull { it.name == category } ?: OrderProblemReason.OTHER,
    detail = detail,
    createdAt = occurredAt,
    pickedUp = pickedUp,
    cancelled = cancelled,
    orderStatus = orderStatus?.let { name -> OrderStatus.entries.firstOrNull { it.name == name } }
)

@Dao
interface OrdersDao {
    @Query("SELECT * FROM orders WHERE riderId = :riderId AND orderId = :orderId")
    suspend fun record(riderId: String, orderId: String): OrderEntity?

    @Query("SELECT * FROM orders WHERE riderId = :riderId AND orderId = :orderId")
    fun observeRecord(riderId: String, orderId: String): Flow<OrderEntity?>

    @Query("SELECT * FROM orders WHERE riderId = :riderId")
    fun observeRecords(riderId: String): Flow<List<OrderEntity>>

    @Query("SELECT * FROM order_problems WHERE riderId = :riderId AND orderId = :orderId")
    suspend fun problems(riderId: String, orderId: String): List<OrderProblemEntity>

    @Upsert
    suspend fun saveProblems(entities: List<OrderProblemEntity>)

    @Query("DELETE FROM order_problems WHERE riderId = :riderId AND orderId = :orderId")
    suspend fun clearProblems(riderId: String, orderId: String)

    @Query("SELECT * FROM orders WHERE riderId = :riderId AND pending = 1 ORDER BY revision LIMIT 50")
    suspend fun pending(riderId: String): List<OrderEntity>

    @Query("SELECT COUNT(*) FROM orders WHERE riderId = :riderId AND pending = 1")
    fun observePendingCount(riderId: String): Flow<Int>

    @Query("UPDATE orders SET pending = 0 WHERE riderId = :riderId AND orderId = :orderId AND revision = :revision")
    suspend fun confirm(riderId: String, orderId: String, revision: Long)

    @Upsert
    suspend fun putRecord(entity: OrderEntity)

    @Transaction
    suspend fun saveOrder(order: OrderRecord, revision: Long, pending: Boolean) {
        putRecord(order.toEntity(revision, pending))
        clearProblems(order.riderId, order.orderId)
        val problems = order.problems.orEmpty().map { it.toEntity(order.riderId) }
        if (problems.isNotEmpty()) saveProblems(problems)
    }

    suspend fun saveRecord(order: OrderRecord, revision: Long, pending: Boolean) =
        saveOrder(order, revision, pending)
}

class StoredOrderSessionStore(
    private val dao: OrdersDao,
    private val scheduleSync: (String) -> Unit = {},
) : OrderSessionStore {
    private val sessions = mutableMapOf<String, OrderSessionState>()

    override suspend fun load(uid: String): OrderSessionState? = sessions[uid]

    override suspend fun save(uid: String, state: OrderSessionState) {
        val previous = sessions[uid]
        sessions[uid] = state
        var order = OrderRecord.from(uid, state.snapshot)
        if (order == null) {
            val previousRecord = previous?.snapshot?.let { OrderRecord.from(uid, it) }
            val cancelled = state.problems.orEmpty().lastOrNull { it.cancelled && it.orderId == previousRecord?.orderId }
            if (previousRecord != null && cancelled != null) order = previousRecord.copy(
                status = OrderStatus.CANCELLED, cancelledAt = cancelled.createdAt)
        }
        order = order?.copy(problems = state.problems.orEmpty().filter { it.orderId == order?.orderId }) ?: return
        val old = dao.record(uid, order.orderId)
        val oldRecord = old?.toDomain(dao.problems(uid, order.orderId))
        val normalized = if (oldRecord == null) order else order.copy(earningsQuote = order.earningsQuote ?: oldRecord.earningsQuote)
        if (oldRecord == normalized) return
        val revision = (old?.revision ?: 0) + 1
        dao.saveOrder(normalized, revision, true)
        try { scheduleSync(uid) } catch (error: RuntimeException) {
            android.util.Log.w("OrderSync", "El registro sigue pendiente en Room", error)
        }
    }
}

@Deprecated("Usar openChaskiDatabase; se mantiene para los tests de dispositivo.")
fun openOrderDatabase(context: android.content.Context, name: String = "chaski.db"): com.example.chaskirider.database.ChaskiDatabase =
    com.example.chaskirider.database.openChaskiDatabase(context, name)

@Entity(tableName = "order_contacts", primaryKeys = ["riderId", "orderId"])
data class OrderContactEntity(
    val riderId: String,
    val orderId: String,
    val arrivedAt: Long,
    val firstCallAt: Long?,
    val firstCallElapsed: Long?,
    val boot: Int?,
    val secondCallAt: Long?,
    val reminderSeen: Boolean,
    val phone: String?,
    val serverRegistered: Boolean
)

@Dao
interface OrderContactDao {
    @Query("SELECT * FROM order_contacts WHERE riderId = :riderId AND orderId = :orderId")
    suspend fun load(riderId: String, orderId: String): OrderContactEntity?
    @Upsert suspend fun save(value: OrderContactEntity)
}

class RoomCustomerContactStore(private val dao: OrderContactDao) : CustomerContactStore {
    override suspend fun load(uid: String, orderId: String): CustomerContact? =
        dao.load(uid, orderId)?.toDomain()
    override suspend fun save(uid: String, orderId: String, contact: CustomerContact) =
        dao.save(contact.toEntity(uid, orderId))
}

private fun OrderContactEntity.toDomain() = CustomerContact(
    arrivedAt = arrivedAt,
    firstCallAt = firstCallAt,
    firstCallElapsed = firstCallElapsed,
    boot = boot,
    secondCallAt = secondCallAt,
    reminderSeen = reminderSeen,
    phone = phone,
    serverRegistered = serverRegistered
)

private fun CustomerContact.toEntity(riderId: String, orderId: String) = OrderContactEntity(
    riderId = riderId,
    orderId = orderId,
    arrivedAt = arrivedAt,
    firstCallAt = firstCallAt,
    firstCallElapsed = firstCallElapsed,
    boot = boot,
    secondCallAt = secondCallAt,
    reminderSeen = reminderSeen,
    phone = phone,
    serverRegistered = serverRegistered
)

data class DemoOrder(
    val id: String, val merchantName: String, val pickupZone: String, val pickupAddress: String,
    val pickup: RoutePoint, val deliveryZone: String, val deliveryAddress: String,
    val delivery: RoutePoint, val tipCents: Int, val paidByCustomer: Boolean = false
)

object DemoOrderCatalog {
    const val SEARCH_RADIUS_METERS = 5000
    fun parse(json: String): List<DemoOrder> {
        val orders = Gson().fromJson(json, Array<DemoOrder>::class.java).toList()
        require(orders.isNotEmpty() && orders.map { it.id }.distinct().size == orders.size)
        require(orders.all { order ->
            order.id.isNotBlank() && order.merchantName.isNotBlank() && order.tipCents >= 0 &&
                order.pickupZone.isNotBlank() && order.deliveryZone.isNotBlank() &&
                order.pickupAddress.isNotBlank() && order.deliveryAddress.isNotBlank() &&
                listOf(order.pickup, order.delivery).all {
                    it.latitude.isFinite() && it.longitude.isFinite() &&
                        it.latitude in -12.5..-11.5 && it.longitude in -77.3..-76.5
                }
        })
        return orders
    }
    fun nearest(orders: List<DemoOrder>, lat: Double, lng: Double, excluded: Set<String>): DemoOrder? =
        orders.filter { it.id !in excluded }.map { it to
            LocationUtils.distanceMeters(lat, lng, it.pickup.latitude, it.pickup.longitude)
        }.filter { it.second <= SEARCH_RADIUS_METERS }.minByOrNull { it.second }?.first
}
