package com.example.chaskirider.feature.orders.domain

import kotlinx.coroutines.flow.Flow

interface OfferRepository {
    suspend fun resolveCustomerAbsence(orderId: String): OfferSnapshot = throw OfferRequestException(501)
    suspend fun reportProblem(reason: OrderProblemReason, detail: String, cancel: Boolean): OfferSnapshot =
        throw OfferRequestException(501)
    suspend fun execute(
        action: OfferAction,
        offerId: String? = null,
        lat: Double? = null,
        lng: Double? = null,
        evidenceUrl: String? = null,
        deliveryNote: String? = null,
        deliveryPin: String? = null
    ): OfferSnapshot
}

class OfferRequestException(val status: Int) : Exception()

data class OrderSyncState(
    val online: Boolean = true,
    val pendingActions: Int = 0,
    val syncing: Boolean = false,
    val needsAttention: Boolean = false
)

interface OrderSyncRepository {
    fun observe(uid: String): Flow<OrderSyncState>
    fun retry(uid: String)
}
