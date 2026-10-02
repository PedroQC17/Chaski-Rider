package com.example.chaskirider.data.orders

import com.example.chaskirider.domain.orders.*
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

internal interface OfferApi {
    @POST("riderOfferDemo")
    suspend fun execute(@Header("Authorization") token: String, @Body body: OfferRequest): SnapshotDto
}
internal data class OfferRequest(val action: String, val offerId: String?, val requestId: String)
internal data class PointDto(val latitude: Double, val longitude: Double) {
    fun toDomain() = RoutePoint(latitude, longitude)
}
internal data class StopDto(val id: String, val orderId: String, val point: PointDto) {
    fun toDomain() = DeliveryStop(id, orderId, point.toDomain())
}
internal data class QuoteDto(val stops: List<StopDto>, val approachMeters: Int, val deliveryMeters: Int,
    val durationSeconds: Int, val approachCents: Int, val deliveryCents: Int,
    val guaranteeCents: Int, val totalCents: Int, val additionalCents: Int, val path: List<PointDto>) {
    fun toDomain() = DeliveryQuote(stops.map { it.toDomain() }, approachMeters, deliveryMeters,
        durationSeconds, approachCents, deliveryCents, guaranteeCents, totalCents, additionalCents, path.map { it.toDomain() })
}
internal data class OfferDto(val id: String, val expiresAt: Long, val quote: QuoteDto) {
    fun toDomain() = DeliveryOffer(id, expiresAt, quote.toDomain())
}
internal data class SnapshotDto(val serverTime: Long, val pickedUp: Boolean,
    val batch: QuoteDto?, val offer: OfferDto?, val merchant: PointDto) {
    fun toDomain() = OfferSnapshot(serverTime, pickedUp, batch?.toDomain(), offer?.toDomain(), merchant.toDomain())
}
