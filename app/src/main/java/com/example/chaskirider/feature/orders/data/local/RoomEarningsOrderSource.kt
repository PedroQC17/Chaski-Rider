package com.example.chaskirider.feature.orders.data.local

import com.example.chaskirider.feature.earnings.domain.EarningsOrderRow
import com.example.chaskirider.feature.earnings.domain.EarningsOrderSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Adaptador que expone los pedidos persistidos como el puerto que consume earnings. */
class RoomEarningsOrderSource(private val dao: OrdersDao) : EarningsOrderSource {
    override fun observe(uid: String): Flow<List<EarningsOrderRow>> =
        dao.observeRecords(uid).map { rows ->
            rows.map { EarningsOrderRow(it.orderId, it.toDomain()) }
        }
}
