
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
