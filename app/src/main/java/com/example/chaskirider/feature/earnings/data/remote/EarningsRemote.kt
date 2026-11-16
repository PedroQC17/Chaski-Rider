package com.example.chaskirider.feature.earnings.data.remote

import com.example.chaskirider.core.firebase.FirebaseRoot
import com.example.chaskirider.feature.earnings.data.local.SettlementParser
import com.example.chaskirider.feature.earnings.domain.ConnectionSession
import com.example.chaskirider.feature.earnings.domain.SettlementRecord
import com.example.chaskirider.feature.orders.data.remote.OrderRoutes
import com.example.chaskirider.feature.orders.domain.AcceptedEarningsQuote
import com.example.chaskirider.feature.orders.domain.OrderRecord
import com.example.chaskirider.feature.orders.domain.OrderStatus
import com.example.chaskirider.feature.orders.domain.RoutePoint
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.MetadataChanges
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*

data class Payments(val records: List<SettlementRecord> = emptyList(), val warning: String? = null)
data class Connections(val sessions: List<ConnectionSession> = emptyList(), val warning: String? = null)

class EarningsRemote(private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()) {
    fun orders(uid: String): Flow<List<OrderRecord>> = callbackFlow {
        val listener = firestore.collection(OrderRoutes.records(uid))
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                trySend(snapshot?.documents.orEmpty().mapNotNull { it.toOrderRecord(uid) })
            }
        awaitClose { listener.remove() }
    }

    fun payments(uid: String): Flow<Payments> = callbackFlow {
        val listener = firestore.collection(EarningsRoutes.settlements(uid))
            .addSnapshotListener(MetadataChanges.INCLUDE) { snapshot, error ->
                if (error != null) {
                    trySend(Payments(warning = "No se pudieron actualizar los pagos."))
                    return@addSnapshotListener
                }
                var invalid = false
                val seen = mutableSetOf<String>()
                val records = snapshot?.documents.orEmpty().mapNotNull { doc ->
                    val payment = SettlementParser.parse(doc.id, doc.data.orEmpty(), System.currentTimeMillis())
                    if (payment == null || payment.orderIds.any { it in seen }) { invalid = true; null }
                    else { seen.addAll(payment.orderIds); payment }
                }
                trySend(Payments(records, when {
                    invalid -> "Un pago requiere revisión de soporte."
                    snapshot?.metadata?.isFromCache == true -> "Pagos pendientes de actualizar."
                    else -> null
                }))
            }
        awaitClose { listener.remove() }
    }.onStart { emit(Payments(warning = "Actualizando pagos…")) }

    fun connections(uid: String): Flow<Connections> {
        val profile = callbackFlow {
            val listener = firestore.document(FirebaseRoot.riderDoc(uid)).addSnapshotListener { doc, error ->
                trySend(if (error != null) null else doc?.getLong("connectionStartedAt"))
            }
            awaitClose { listener.remove() }
        }
        val history = callbackFlow {
            val listener = firestore.collection(EarningsRoutes.connectionSessions(uid)).addSnapshotListener { snapshot, error ->
                val sessions = snapshot?.documents.orEmpty().mapNotNull { doc ->
                    val start = doc.getLong("startedAt")
                    val end = doc.getLong("endedAt")
                    if (start != null && end != null && start > 0 && end >= start) ConnectionSession(start, end) else null
                }
                trySend(Connections(sessions, if (error != null) "No se pudo consultar el tiempo conectado."
                    else "El tiempo conectado incluye únicamente las conexiones registradas desde esta actualización."))
            }
            awaitClose { listener.remove() }
        }
        return combine(profile, history) { start, feed ->
            feed.copy(sessions = feed.sessions + listOfNotNull(start?.let { ConnectionSession(it) }))
        }
    }
}

private fun DocumentSnapshot.toOrderRecord(uid: String): OrderRecord? {
    val orderId = getString("orderId") ?: id
    val riderId = getString("riderId") ?: return null
    if (riderId != uid) return null
    val status = getString("status")?.let { value ->
        OrderStatus.entries.firstOrNull { it.name == value }
    } ?: return null
    if (status !in listOf(OrderStatus.DELIVERED, OrderStatus.CANCELLED, OrderStatus.COMPLETED_UNSUCCESSFUL)) return null

    val pickup = (get("pickup") as? Map<*, *>)?.toRoutePoint() ?: return null
    val destination = (get("destination") as? Map<*, *>)?.toRoutePoint() ?: return null
    val deliveredAt = getLong("deliveredAt")
    val cancelledAt = getLong("cancelledAt")
    val completedAt = getLong("completedAt")
        ?: if (status == OrderStatus.COMPLETED_UNSUCCESSFUL) getTimestamp("updatedAt")?.toDate()?.time else null
    val hasTime = when (status) {
        OrderStatus.DELIVERED -> deliveredAt != null
        OrderStatus.CANCELLED -> cancelledAt != null
        else -> completedAt != null
    }
    if (!hasTime) return null

    return OrderRecord(
        orderId = orderId,
        riderId = uid,
        status = status,
        merchantName = getString("merchantName").orEmpty(),
        pickup = pickup,
        destination = destination,
        acceptedAt = getLong("acceptedAt"),
        arrivedAt = getLong("arrivedAt"),
        pickedUpAt = getLong("pickedUpAt"),
        deliveredAt = deliveredAt,
        cancelledAt = cancelledAt,
        paidByCustomer = getBoolean("paidByCustomer") ?: false,
        earningsQuote = (get("earningsQuote") as? Map<*, *>)?.toQuote(),
        completedAt = completedAt,
        completionReason = getString("completionReason")
    )
}

private fun Map<*, *>.toRoutePoint(): RoutePoint? {
    val latitude = (this["latitude"] as? Number)?.toDouble() ?: return null
    val longitude = (this["longitude"] as? Number)?.toDouble() ?: return null
    return RoutePoint(latitude, longitude)
}

private fun Map<*, *>.toQuote(): AcceptedEarningsQuote? {
    fun number(key: String): Int? = (this[key] as? Number)?.toInt()
    return AcceptedEarningsQuote(
        approachMeters = number("approachMeters") ?: return null,
        deliveryMeters = number("deliveryMeters") ?: return null,
        chaskiCents = number("chaskiCents") ?: return null,
        tipCents = number("tipCents") ?: return null,
        totalCents = number("totalCents") ?: return null,
        pricingVersion = this["pricingVersion"] as? String ?: return null,
        distanceSource = this["distanceSource"] as? String ?: return null
    )
}
