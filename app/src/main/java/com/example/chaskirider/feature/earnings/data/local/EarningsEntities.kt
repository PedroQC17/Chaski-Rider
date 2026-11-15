package com.example.chaskirider.feature.earnings.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Upsert
import com.example.chaskirider.feature.earnings.domain.ConnectionSession
import com.example.chaskirider.feature.earnings.domain.SettlementRecord
import com.google.gson.Gson

@Entity(tableName = "settlements", primaryKeys = ["id"])
data class SettlementEntity(
    val id: String,
    val riderId: String,
    val periodLabel: String,
    val amountCents: Long,
    val status: String,
    val paidAt: Long?,
    val orderIdsJson: String,
    val periodStart: Long
)

@Entity(tableName = "connection_sessions", primaryKeys = ["riderId", "startedAt"])
data class ConnectionSessionEntity(
    val riderId: String,
    val startedAt: Long,
    val endedAt: Long?
)

@Dao
interface EarningsDao {
    @Upsert
    suspend fun saveSettlements(settlements: List<SettlementEntity>)

    @Upsert
    suspend fun saveConnections(sessions: List<ConnectionSessionEntity>)
}

class EarningsStore(private val dao: EarningsDao) {
    private val gson = Gson()

    suspend fun save(riderId: String, settlements: List<SettlementRecord>, sessions: List<ConnectionSession>) {
        dao.saveSettlements(settlements.map { it.toEntity(riderId, gson) })
        dao.saveConnections(sessions.map { it.toEntity(riderId) })
    }
}

fun SettlementRecord.toEntity(riderId: String, gson: Gson = Gson()) = SettlementEntity(
    id = id,
    riderId = riderId,
    periodLabel = periodLabel,
    amountCents = amountCents,
    status = status.name,
    paidAt = paidAt,
    orderIdsJson = gson.toJson(orderIds),
    periodStart = periodStart
)

fun ConnectionSession.toEntity(riderId: String) =
    ConnectionSessionEntity(riderId, startedAt, endedAt)
