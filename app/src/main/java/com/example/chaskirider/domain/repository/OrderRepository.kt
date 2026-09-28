// HU06 - Parte 1: contrato del módulo de pedidos.
// nextOffer/accept/reject son las operaciones de la HU06 (recibir, aceptar,
// rechazar). acceptedOrder expone el pedido aceptado para la pestaña Pedidos.
// NOTA: la implementación actual (OrderRepositoryImpl) es MOCK local;
// cuando exista el backend se reemplaza por una que llame a la función
// propuesta `riderOrders` (acciones "accept"/"reject") sin cambiar esta interfaz.
package com.example.chaskirider.domain.repository

import com.example.chaskirider.domain.model.RideOffer
import com.example.chaskirider.domain.model.RiderUser
import kotlinx.coroutines.flow.StateFlow

interface OrderRepository {
    val acceptedOrder: StateFlow<RideOffer?>
    suspend fun nextOffer(user: RiderUser): Result<RideOffer>
    suspend fun accept(offerId: String): Result<RideOffer>
    suspend fun reject(offerId: String): Result<Unit>
}
