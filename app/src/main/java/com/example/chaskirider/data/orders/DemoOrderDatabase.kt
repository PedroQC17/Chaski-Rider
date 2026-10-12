package com.example.chaskirider.data.orders

import androidx.room.Database
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Upsert
import com.example.chaskirider.domain.orders.OfferSnapshot
import com.google.gson.Gson

data class DemoOrderState(val snapshot: OfferSnapshot, val recent: Map<String, Long> = emptyMap(),
    val problems: List<com.example.chaskirider.domain.orders.OrderProblem>? = emptyList())
interface DemoOrderStore {
    suspend fun load(uid: String): DemoOrderState?
    suspend fun save(uid: String, state: DemoOrderState)
}
@Entity(tableName = "demo_orders")
data class DemoOrderEntity(@PrimaryKey val uid: String, val state: String)
@Dao
interface DemoOrderDao {
    @Query("SELECT * FROM demo_orders WHERE uid = :uid")
    suspend fun load(uid: String): DemoOrderEntity?
    @Upsert suspend fun save(entity: DemoOrderEntity)
}
@Database(entities = [DemoOrderEntity::class], version = 1, exportSchema = true)
abstract class DemoOrderDatabase : RoomDatabase() {
    abstract fun orders(): DemoOrderDao
}
class RoomDemoOrderStore(private val dao: DemoOrderDao) : DemoOrderStore {
    private val gson = Gson()
    override suspend fun load(uid: String) = dao.load(uid)?.let { gson.fromJson(it.state, DemoOrderState::class.java) }
    override suspend fun save(uid: String, state: DemoOrderState) = dao.save(DemoOrderEntity(uid, gson.toJson(state)))
}
