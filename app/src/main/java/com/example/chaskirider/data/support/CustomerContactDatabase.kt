package com.example.chaskirider.data.support

import androidx.room.*
import com.example.chaskirider.domain.support.CustomerContact
import com.example.chaskirider.domain.support.CustomerContactStore
import com.google.gson.Gson

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
