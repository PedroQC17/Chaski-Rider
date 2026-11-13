package com.example.chaskirider.feature.earnings.data

import com.example.chaskirider.feature.earnings.data.remote.EarningsRemote
import com.example.chaskirider.feature.earnings.domain.*
import com.example.chaskirider.feature.orders.data.local.DemoOrderDao
import com.example.chaskirider.feature.orders.domain.OrderRecord
import com.google.gson.Gson
import kotlinx.coroutines.flow.*

class EarningsRepositoryImpl(
    private val dao: DemoOrderDao,
    private val remote: EarningsRemote = EarningsRemote()
) : EarningsRepository {
    private val gson = Gson()

    override fun observe(uid: String): Flow<EarningsSnapshot> =
        combine(dao.observeRecords(uid), remote.payments(uid), remote.connections(uid)) { rows, feed, connection ->
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
                    (known.size != payment.orderIds.size || known.any { it.totalCents == null } ||
                        known.sumOf { it.totalCents!!.toLong() } == payment.amountCents)
                if (!matches) invalid = true
                matches
            }
            EarningsSnapshot(records, EarningsCalendar.weekly(records, paid), feed.warning ?: if (invalid)
                "Un pedido requiere revisión de soporte." else null, connection.sessions, connection.warning)
        }.flowOn(kotlinx.coroutines.Dispatchers.Default)
}
