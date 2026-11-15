package com.example.chaskirider.feature.support.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Upsert
import com.example.chaskirider.feature.support.domain.SupportMessage
import com.example.chaskirider.feature.support.domain.SupportTicket

@Entity(tableName = "support_tickets", primaryKeys = ["id"])
data class SupportTicketEntity(
    val id: String,
    val riderId: String,
    val topic: String,
    val status: String,
    val createdAt: Long,
    val needsHuman: Boolean,
    val orderId: String? = null,
    val orderMerchantName: String? = null,
    val orderPickupAddress: String? = null,
    val orderDeliveryAddress: String? = null,
    val orderPickedUp: Boolean? = null,
    val orderProblemId: String? = null,
    val orderStatus: String? = null,
    val orderProblemCategory: String? = null,
    val locationLatitude: Double? = null,
    val locationLongitude: Double? = null,
    val locationCapturedAt: Long? = null
)

@Entity(tableName = "support_messages", primaryKeys = ["id"])
data class SupportMessageEntity(
    val id: String,
    val riderId: String,
    val ticketId: String,
    val sender: String,
    val body: String,
    val createdAt: Long,
    val pending: Boolean,
    val mode: String? = null
)

@Dao
interface SupportDao {
    @Upsert
    suspend fun saveTickets(tickets: List<SupportTicketEntity>)

    @Upsert
    suspend fun saveMessages(messages: List<SupportMessageEntity>)
}

class SupportStore(private val dao: SupportDao) {
    suspend fun saveTickets(riderId: String, tickets: List<SupportTicket>) =
        dao.saveTickets(tickets.map { it.toEntity(riderId) })

    suspend fun saveMessages(riderId: String, ticketId: String, messages: List<SupportMessage>) =
        dao.saveMessages(messages.map { it.toEntity(riderId, ticketId) })
}

fun SupportTicket.toEntity(riderId: String) = SupportTicketEntity(
    id = id,
    riderId = riderId,
    topic = topic.name,
    status = status.name,
    createdAt = createdAt,
    needsHuman = needsHuman,
    orderId = order?.orderId,
    orderMerchantName = order?.merchantName,
    orderPickupAddress = order?.pickupAddress,
    orderDeliveryAddress = order?.deliveryAddress,
    orderPickedUp = order?.pickedUp,
    orderProblemId = order?.problemId,
    orderStatus = order?.orderStatus?.name,
    orderProblemCategory = order?.problemCategory?.name,
    locationLatitude = location?.latitude,
    locationLongitude = location?.longitude,
    locationCapturedAt = location?.capturedAt
)

fun SupportMessage.toEntity(riderId: String, ticketId: String) = SupportMessageEntity(
    id = id,
    riderId = riderId,
    ticketId = ticketId,
    sender = sender.name,
    body = body,
    createdAt = createdAt,
    pending = pending,
    mode = mode
)
