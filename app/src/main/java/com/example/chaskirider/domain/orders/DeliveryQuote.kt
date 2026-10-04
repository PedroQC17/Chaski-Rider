package com.example.chaskirider.domain.orders

data class RoutePoint(val latitude: Double, val longitude: Double)
data class DeliveryStop(val id: String, val orderId: String, val zone: String?, val point: RoutePoint)
data class DeliveryQuote(
    val stops: List<DeliveryStop>, val approachMeters: Int, val deliveryMeters: Int,
    val durationSeconds: Int, val baseCents: Int, val approachCents: Int, val deliveryCents: Int,
    val tipCents: Int, val guaranteeCents: Int, val totalCents: Int, val additionalCents: Int,
    val path: List<RoutePoint>
)
