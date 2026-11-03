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
    private data class Payments(val records: List<SettlementRecord> = emptyList(), val warning: String? = null)

    override fun observe(uid: String): Flow<EarningsSnapshot> = combine(dao.observeRecords(uid), payments(uid)) { rows, feed ->
        var invalid = false
        val records = rows.mapNotNull { row ->
            try {
                val order = Gson().fromJson(row.content, OrderRecord::class.java)
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
            "Hay registros inconsistentes. Contacta con soporte para revisarlos." else null)
    }

    private fun payments(uid: String): Flow<Payments> = callbackFlow {
        val listener = firestore.collection("riders").document(uid).collection("settlements")
            .addSnapshotListener(com.google.firebase.firestore.MetadataChanges.INCLUDE) { snapshot, error ->
                if (error != null) {
                    trySend(Payments(warning = "No se pudieron consultar las liquidaciones. Reintenta para confirmar sus estados."))
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
                    invalid -> "Hay liquidaciones incompletas o inconsistentes. Solicita su revisión a soporte."
                    snapshot?.metadata?.isFromCache == true -> "Liquidaciones guardadas: pendiente de actualizar con Firebase."
                    else -> null
                }))
            }
        awaitClose { listener.remove() }
    }.onStart { emit(Payments(warning = "Consultando confirmaciones de pago…")) }
}
