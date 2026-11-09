package com.example.chaskirider.data.earnings

import com.example.chaskirider.data.orders.DemoOrderDao
import com.example.chaskirider.domain.earnings.*
import com.example.chaskirider.domain.orders.OrderRecord
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*

class LocalEarningsRepository(
    private val dao: DemoOrderDao,
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : EarningsRepository {
    private val gson = Gson()
    private data class Payments(val records: List<SettlementRecord> = emptyList(), val warning: String? = null)

    override fun observe(uid: String): Flow<EarningsSnapshot> = combine(dao.observeRecords(uid), payments(uid), connections(uid)) { rows, feed, connection ->
        var invalid = false
        val records = rows.mapNotNull { row ->
            try {
                val order = gson.fromJson(row.content, OrderRecord::class.java)
                if (order.riderId != uid || order.orderId != row.orderId) { invalid = true; null }
                else EarningsPolicy.record(order)
            } catch (_: RuntimeException) { invalid = true; null }
        }.sortedByDescending { it.completedAt }
        val paid = feed.records.filter { payment ->
            val known = records.filter { it.orderId in payment.orderIds }
            val matches = known.all { !it.cancelled && EarningsCalendar.weekStart(it.completedAt) == payment.periodStart } &&
                (known.size != payment.orderIds.size || known.any { it.totalCents == null } || known.sumOf { it.totalCents!!.toLong() } == payment.amountCents)
            if (!matches) invalid = true
            matches
        }
        EarningsSnapshot(records, EarningsCalendar.weekly(records, paid), feed.warning ?: if (invalid)
            "Un pedido requiere revisión de soporte." else null, connection.sessions, connection.warning)
    }.flowOn(kotlinx.coroutines.Dispatchers.Default)

    private data class Connections(val sessions: List<ConnectionSession> = emptyList(), val warning: String? = null)
    private fun connections(uid: String): Flow<Connections> {
        val profile = callbackFlow {
            val listener = firestore.document("riders/$uid").addSnapshotListener { doc, error ->
                trySend(if (error != null) null else doc?.getLong("connectionStartedAt"))
            }
            awaitClose { listener.remove() }
        }
        val history = callbackFlow {
            val listener = firestore.collection("riders/$uid/connectionSessions").addSnapshotListener { snapshot, error ->
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

    private fun payments(uid: String): Flow<Payments> = callbackFlow {
        val listener = firestore.collection("riders").document(uid).collection("settlements")
            .addSnapshotListener(com.google.firebase.firestore.MetadataChanges.INCLUDE) { snapshot, error ->
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
}
