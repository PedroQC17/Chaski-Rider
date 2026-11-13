package com.example.chaskirider.feature.earnings.data

import com.example.chaskirider.feature.earnings.data.remote.EarningsRemote
import com.example.chaskirider.feature.earnings.domain.*
import kotlinx.coroutines.flow.*

class EarningsRepositoryImpl(
    private val orders: EarningsOrderSource,
    private val remote: EarningsRemote = EarningsRemote()
) : EarningsRepository {
    override fun observe(uid: String): Flow<EarningsSnapshot> =
        combine(orders.observe(uid), remote.payments(uid), remote.connections(uid)) { rows, feed, connection ->
            var invalid = false
            val records = rows.mapNotNull { row ->
                val order = row.order
                if (order == null || order.riderId != uid || order.orderId != row.orderId) {
                    invalid = true
                    null
                } else EarningsPolicy.record(order)
            }.sortedByDescending { it.completedAt }
            val paid = feed.records.filter { payment ->
                val known = records.filter { it.orderId in payment.orderIds }
                val matches = known.all { !it.cancelled && EarningsCalendar.weekStart(it.completedAt) == payment.periodStart } &&
                    (known.size != payment.orderIds.size || known.any { it.totalCents == null } ||
                        known.sumOf { it.totalCents!!.toLong() } == payment.amountCents)
                if (!matches) invalid = true
                matches
            }
            EarningsSnapshot(records, EarningsCalendar.weekly(records, paid), feed.warning ?: if (invalid)
                "Un pedido requiere revisión de soporte." else null, connection.sessions, connection.warning)
        }.flowOn(kotlinx.coroutines.Dispatchers.Default)
}
