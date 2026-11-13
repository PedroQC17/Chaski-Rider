package com.example.chaskirider.feature.earnings.data.remote

import com.example.chaskirider.feature.earnings.data.local.SettlementParser
import com.example.chaskirider.feature.earnings.domain.ConnectionSession
import com.example.chaskirider.feature.earnings.domain.SettlementRecord
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.MetadataChanges
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*

data class Payments(val records: List<SettlementRecord> = emptyList(), val warning: String? = null)
data class Connections(val sessions: List<ConnectionSession> = emptyList(), val warning: String? = null)

class EarningsRemote(private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()) {
    fun payments(uid: String): Flow<Payments> = callbackFlow {
        val listener = firestore.collection("riders").document(uid).collection("settlements")
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
}
