package com.example.chaskirider.feature.earnings.domain

import com.example.chaskirider.feature.orders.domain.OrderRecord
import kotlinx.coroutines.flow.Flow

/** Fila persistida de un pedido, con [order] nulo si el contenido no pudo interpretarse. */
data class EarningsOrderRow(val orderId: String, val order: OrderRecord?)

/** Puerto que earnings necesita del almacenamiento de pedidos; lo implementa el feature orders. */
interface EarningsOrderSource {
    fun observe(uid: String): Flow<List<EarningsOrderRow>>
}
