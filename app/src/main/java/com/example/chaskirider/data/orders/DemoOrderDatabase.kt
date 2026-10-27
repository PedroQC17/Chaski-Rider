package com.example.chaskirider.data.orders

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
import com.example.chaskirider.domain.orders.OrderRecord
import com.example.chaskirider.domain.orders.OrderStatus
import com.example.chaskirider.domain.orders.OfferSnapshot
import com.google.gson.Gson

data class DemoOrderState(val snapshot: OfferSnapshot, val recent: Map<String, Long> = emptyMap(),
    val problems: List<com.example.chaskirider.domain.orders.OrderProblem>? = emptyList(),
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
@Dao
interface DemoOrderDao {
    @Query("SELECT * FROM demo_orders WHERE uid = :uid")
    suspend fun load(uid: String): DemoOrderEntity?
    @Upsert suspend fun save(entity: DemoOrderEntity)
    @Query("SELECT * FROM order_records WHERE uid = :uid AND orderId = :orderId")
    suspend fun record(uid: String, orderId: String): OrderRecordEntity?
    @Query("SELECT * FROM order_records WHERE uid = :uid AND pending = 1 ORDER BY revision LIMIT 50")
    suspend fun pending(uid: String): List<OrderRecordEntity>
    @Query("UPDATE order_records SET pending = 0 WHERE uid = :uid AND orderId = :orderId AND revision = :revision")
    suspend fun confirm(uid: String, orderId: String, revision: Long)
    @Upsert suspend fun saveRecord(entity: OrderRecordEntity)
    @Transaction
    suspend fun saveWithRecord(entity: DemoOrderEntity, orderId: String?, content: String?): Boolean {
        save(entity)
        if (orderId != null && content != null) {
            val old = record(entity.uid, orderId)
            if (old?.content != content) {
                saveRecord(OrderRecordEntity(entity.uid, orderId, content, (old?.revision ?: 0) + 1, true))
                return true
            }
        }
        return false
    }
}
@Database(entities = [DemoOrderEntity::class, OrderRecordEntity::class], version = 2, exportSchema = true)
abstract class DemoOrderDatabase : RoomDatabase() {
    abstract fun orders(): DemoOrderDao
    companion object {
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
        val changed = dao.saveWithRecord(DemoOrderEntity(uid, gson.toJson(state)), record?.orderId, record?.let { gson.toJson(it) })
        try { if (changed) scheduleSync(uid) } catch (error: RuntimeException) {
            android.util.Log.w("OrderSync", "El registro sigue pendiente en Room", error)
        }
    }
}

fun openOrderDatabase(context: android.content.Context): DemoOrderDatabase = androidx.room.Room
    .databaseBuilder(context.applicationContext, DemoOrderDatabase::class.java, "demo_orders.db")
    .addMigrations(DemoOrderDatabase.MIGRATION_1_2).build()
