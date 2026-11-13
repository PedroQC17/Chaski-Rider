package com.example.chaskirider.feature.orders.data.local

import com.example.chaskirider.feature.orders.domain.OrderProblem
import androidx.room.Database
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Upsert
import androidx.room.Transaction
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.chaskirider.feature.orders.domain.OrderRecord
import com.example.chaskirider.feature.orders.domain.OrderStatus
import com.example.chaskirider.feature.orders.domain.OfferSnapshot
import com.google.gson.Gson
import androidx.room.*
import com.example.chaskirider.feature.orders.domain.CustomerContact
import com.example.chaskirider.feature.orders.domain.CustomerContactStore
import com.example.chaskirider.core.platform.location.LocationUtils
import com.example.chaskirider.feature.orders.domain.RoutePoint

data class DemoOrderState(val snapshot: OfferSnapshot, val recent: Map<String, Long> = emptyMap(),
    val problems: List<com.example.chaskirider.feature.orders.domain.OrderProblem>? = emptyList(),
    val catalogRouteId: String? = null)
interface DemoOrderStore {
    suspend fun load(uid: String): DemoOrderState?
    suspend fun save(uid: String, state: DemoOrderState)
}
@Entity(tableName = "demo_orders")
data class DemoOrderEntity(@PrimaryKey val uid: String, val state: String)
@Entity(tableName = "order_records", primaryKeys = ["uid", "orderId"])
data class OrderRecordEntity(val uid: String, val orderId: String, val content: String,
    val revision: Long, val pending: Boolean)
@Entity(tableName = "order_actions", primaryKeys = ["uid", "actionId"])
data class OrderActionEntity(val uid: String, val actionId: String, val orderId: String,
    val revision: Long, val content: String, val kind: String, val pending: Boolean)
@Dao
interface DemoOrderDao {
    @Query("SELECT * FROM demo_orders WHERE uid = :uid")
    suspend fun load(uid: String): DemoOrderEntity?
    @Upsert suspend fun save(entity: DemoOrderEntity)
    @Query("SELECT * FROM order_records WHERE uid = :uid AND orderId = :orderId")
    suspend fun record(uid: String, orderId: String): OrderRecordEntity?
    @Query("SELECT * FROM order_records WHERE uid = :uid AND orderId = :orderId")
    fun observeRecord(uid: String, orderId: String): kotlinx.coroutines.flow.Flow<OrderRecordEntity?>
    @Query("SELECT * FROM order_records WHERE uid = :uid")
    fun observeRecords(uid: String): kotlinx.coroutines.flow.Flow<List<OrderRecordEntity>>
    @Query("SELECT * FROM order_records WHERE uid = :uid AND pending = 1 ORDER BY revision LIMIT 50")
    suspend fun pending(uid: String): List<OrderRecordEntity>
    @Query("UPDATE order_records SET pending = 0 WHERE uid = :uid AND orderId = :orderId AND revision = :revision")
    suspend fun confirm(uid: String, orderId: String, revision: Long)
    @Upsert suspend fun putRecord(entity: OrderRecordEntity)
    @androidx.room.Insert(onConflict = androidx.room.OnConflictStrategy.IGNORE)
    suspend fun insertAction(entity: OrderActionEntity)
    @Query("SELECT * FROM order_actions WHERE uid = :uid AND pending = 1 ORDER BY orderId, revision LIMIT 50")
    suspend fun pendingActions(uid: String): List<OrderActionEntity>
    @Query("SELECT COUNT(*) FROM order_actions WHERE uid = :uid AND pending = 1")
    fun observePendingCount(uid: String): kotlinx.coroutines.flow.Flow<Int>
    @Query("SELECT DISTINCT orderId FROM order_actions WHERE uid = :uid AND pending = 1 ORDER BY orderId")
    suspend fun pendingOrderIds(uid: String): List<String>
    @Query("SELECT * FROM order_actions WHERE uid = :uid AND orderId = :orderId AND pending = 1 ORDER BY revision LIMIT 50")
    suspend fun pendingOrderActions(uid: String, orderId: String): List<OrderActionEntity>
    @Query("SELECT * FROM order_actions WHERE uid = :uid AND orderId = :orderId ORDER BY revision")
    suspend fun actions(uid: String, orderId: String): List<OrderActionEntity>
    @Query("UPDATE order_actions SET pending = 0 WHERE uid = :uid AND actionId = :actionId")
    suspend fun confirmAction(uid: String, actionId: String)
    @Transaction
    suspend fun acknowledge(entity: OrderActionEntity) {
        confirmAction(entity.uid, entity.actionId)
        confirm(entity.uid, entity.orderId, entity.revision)
    }
    @Transaction
    suspend fun saveRecord(entity: OrderRecordEntity) {
        val old = record(entity.uid, entity.orderId)
        putRecord(entity)
        if (entity.pending && (old == null || old.revision != entity.revision)) {
            val gson = Gson()
            val next = gson.fromJson(entity.content, OrderRecord::class.java)
            val previous = old?.let { gson.fromJson(it.content, OrderRecord::class.java) }
            val kind = when {
                previous == null -> "RESTORE_SNAPSHOT"
                next.status != previous.status -> next.status.name
                next.waitStartedAt != previous.waitStartedAt -> "WAIT_STARTED"
                next.problems.orEmpty() != previous.problems.orEmpty() -> "INCIDENT"
                else -> "STATE_UPDATED"
            }
            insertAction(OrderActionEntity(entity.uid, "${entity.orderId}_${entity.revision}", entity.orderId,
                entity.revision, entity.content, if (previous == null && entity.revision == 1L && next.status == OrderStatus.ACCEPTED)
                    "ACCEPTED" else kind, true))
        }
    }
    @Transaction
    suspend fun saveWithRecord(entity: DemoOrderEntity, orderId: String?, content: String?): Boolean {
        save(entity)
        if (orderId != null && content != null) {
            val old = record(entity.uid, orderId)
            val normalized = if (old == null) content else {
                val gson = Gson()
                val previous = gson.fromJson(old.content, OrderRecord::class.java)
                val next = gson.fromJson(content, OrderRecord::class.java)
                gson.toJson(next.copy(earningsQuote = previous.earningsQuote))
            }
            if (old?.content != normalized) {
                saveRecord(OrderRecordEntity(entity.uid, orderId, normalized, (old?.revision ?: 0) + 1, true))
                return true
            }
        }
        return false
    }
}
@Database(entities = [DemoOrderEntity::class, OrderRecordEntity::class, OrderActionEntity::class], version = 3, exportSchema = true)
abstract class DemoOrderDatabase : RoomDatabase() {
    abstract fun orders(): DemoOrderDao
    companion object {
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS order_actions (uid TEXT NOT NULL, actionId TEXT NOT NULL, orderId TEXT NOT NULL, revision INTEGER NOT NULL, content TEXT NOT NULL, kind TEXT NOT NULL, pending INTEGER NOT NULL, PRIMARY KEY(uid, actionId))")
                db.execSQL("INSERT INTO order_actions SELECT uid, orderId || '_' || revision, orderId, revision, content, 'RESTORE_SNAPSHOT', 1 FROM order_records WHERE pending = 1")
            }
        }
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS order_records (uid TEXT NOT NULL, orderId TEXT NOT NULL, content TEXT NOT NULL, revision INTEGER NOT NULL, pending INTEGER NOT NULL, PRIMARY KEY(uid, orderId))")
            }
        }
    }
}
class RoomDemoOrderStore(private val dao: DemoOrderDao, private val scheduleSync: (String) -> Unit = {}) : DemoOrderStore {
    private val gson = Gson()
    override suspend fun load(uid: String) = dao.load(uid)?.let { gson.fromJson(it.state, DemoOrderState::class.java) }
    override suspend fun save(uid: String, state: DemoOrderState) {
        var record = OrderRecord.from(uid, state.snapshot)
        if (record == null) {
            val previous = load(uid)?.snapshot
            val previousRecord = previous?.let { OrderRecord.from(uid, it) }
            val cancelled = state.problems.orEmpty().lastOrNull { it.cancelled && it.orderId == previousRecord?.orderId }
            if (previousRecord != null && cancelled != null) record = previousRecord.copy(
                status = OrderStatus.CANCELLED, cancelledAt = cancelled.createdAt)
        }
        record = record?.copy(problems = state.problems.orEmpty().filter { it.orderId == record?.orderId })
        val changed = dao.saveWithRecord(DemoOrderEntity(uid, gson.toJson(state)), record?.orderId, record?.let { gson.toJson(it) })
        try { if (changed) scheduleSync(uid) } catch (error: RuntimeException) {
            android.util.Log.w("OrderSync", "El registro sigue pendiente en Room", error)
        }
    }
}

fun openOrderDatabase(context: android.content.Context, name: String = "demo_orders.db"): DemoOrderDatabase = androidx.room.Room
    .databaseBuilder(context.applicationContext, DemoOrderDatabase::class.java, name)
    .enableMultiInstanceInvalidation()
    .addMigrations(DemoOrderDatabase.MIGRATION_1_2, DemoOrderDatabase.MIGRATION_2_3).build()

@Entity(tableName = "customer_contact", primaryKeys = ["uid", "orderId"])
data class CustomerContactEntity(val uid: String, val orderId: String, val content: String)

@Dao
interface CustomerContactDao {
    @Query("SELECT * FROM customer_contact WHERE uid = :uid AND orderId = :orderId")
    suspend fun load(uid: String, orderId: String): CustomerContactEntity?
    @Upsert suspend fun save(value: CustomerContactEntity)
}

@Database(entities = [CustomerContactEntity::class], version = 1, exportSchema = true)
abstract class CustomerContactDatabase : RoomDatabase() {
    abstract fun contacts(): CustomerContactDao
}

class RoomCustomerContactStore(private val dao: CustomerContactDao) : CustomerContactStore {
    private val gson = Gson()
    override suspend fun load(uid: String, orderId: String): CustomerContact? =
        dao.load(uid, orderId)?.let { gson.fromJson(it.content, CustomerContact::class.java) }
    override suspend fun save(uid: String, orderId: String, contact: CustomerContact) =
        dao.save(CustomerContactEntity(uid, orderId, gson.toJson(contact)))
}

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
