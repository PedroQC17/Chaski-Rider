package com.example.chaskirider.data.orders

import com.example.chaskirider.domain.orders.*
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

internal interface OfferApi {
    @POST("riderOfferDemo")
    suspend fun execute(@Header("Authorization") token: String, @Body body: OfferRequest): SnapshotDto
}
internal data class OfferRequest(
    val action: String,
    val offerId: String?,
    val requestId: String,
    val latitude: Double? = null,
    val longitude: Double? = null
)
internal data class PointDto(val latitude: Double, val longitude: Double) {
    fun toDomain() = RoutePoint(latitude, longitude)
}
internal data class StopDto(val id: String, val orderId: String, val zone: String?, val point: PointDto) {
    fun toDomain() = DeliveryStop(id, orderId, zone, point.toDomain())
}
internal data class QuoteDto(val stops: List<StopDto>, val approachMeters: Int, val deliveryMeters: Int,
    val durationSeconds: Int, val baseCents: Int, val approachCents: Int, val deliveryCents: Int,
    val tipCents: Int, val guaranteeCents: Int, val totalCents: Int, val additionalCents: Int, val path: List<PointDto>) {
    fun toDomain() = DeliveryQuote(stops.map { it.toDomain() }, approachMeters, deliveryMeters,
        durationSeconds, baseCents, approachCents, deliveryCents, tipCents, guaranteeCents, totalCents,
        additionalCents, path.map { it.toDomain() })
}
internal data class OfferDto(val id: String, val expiresAt: Long, val quote: QuoteDto,
    val reason: String?, val reasonMeters: Int?) {
    fun toDomain() = DeliveryOffer(id, expiresAt, quote.toDomain(), reason, reasonMeters)
}
internal data class MerchantDto(val latitude: Double, val longitude: Double, val name: String?, val zone: String?) {
    fun toPoint() = RoutePoint(latitude, longitude)
}
internal data class DemandZoneDto(val id: String, val name: String, val level: String,
    val latitude: Double, val longitude: Double, val radiusMeters: Int, val updatedAt: Long) {
    fun toDomain() = DemandZone(id, name, level, latitude, longitude, radiusMeters, updatedAt)
}
internal data class SnapshotDto(val serverTime: Long, val pickedUp: Boolean,
    val batch: QuoteDto?, val offer: OfferDto?, val merchant: MerchantDto,
    val demandZones: List<DemandZoneDto>?,
    val arrivedAt: Long? = null,
    val waitStartedAt: Long? = null,
    val isNotReadyReported: Boolean? = null,
    val waitingCompensationCents: Int? = null,
    val pickedUpAt: Long? = null,
    val deliveredAt: Long? = null,
    val isDelivered: Boolean? = null) {
    fun toDomain() = OfferSnapshot(serverTime, pickedUp, batch?.toDomain(), offer?.toDomain(),
        merchant.toPoint(), merchant.name, merchant.zone, demandZones.orEmpty().map { it.toDomain() },
        arrivedAt, waitStartedAt, isNotReadyReported ?: false, waitingCompensationCents ?: 0,
        pickedUpAt, deliveredAt, isDelivered ?: false)
}
