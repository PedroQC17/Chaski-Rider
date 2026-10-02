package com.example.chaskirider.domain.orders

data class DeliveryOffer(val id: String, val expiresAt: Long, val quote: DeliveryQuote)
data class OfferSnapshot(val serverTime: Long, val pickedUp: Boolean,
    val batch: DeliveryQuote?, val offer: DeliveryOffer?, val merchant: RoutePoint)
enum class OfferAction { STATE, OFFER, ACCEPT, REJECT, PICKUP, RESET }
interface OfferRepository {
    suspend fun execute(action: OfferAction, offerId: String? = null): OfferSnapshot
}
class OfferRequestException(val status: Int) : Exception()
