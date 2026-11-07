package com.example.chaskirider.domain.orders

import kotlinx.coroutines.flow.Flow

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
