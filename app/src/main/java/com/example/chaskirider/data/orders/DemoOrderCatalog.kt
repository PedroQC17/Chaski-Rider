package com.example.chaskirider.data.orders

import com.example.chaskirider.domain.location.LocationUtils
import com.example.chaskirider.domain.orders.RoutePoint
import com.google.gson.Gson

data class DemoOrder(
    val id: String, val merchantName: String, val pickupZone: String, val pickupAddress: String,
    val pickup: RoutePoint, val deliveryZone: String, val deliveryAddress: String,
    val delivery: RoutePoint, val tipCents: Int
)

object DemoOrderCatalog {
    const val SEARCH_RADIUS_METERS = 5000
    fun parse(json: String): List<DemoOrder> {
        val orders = Gson().fromJson(json, Array<DemoOrder>::class.java).toList()
        require(orders.isNotEmpty() && orders.map { it.id }.distinct().size == orders.size)
        require(orders.all { order ->
            order.id.isNotBlank() && order.merchantName.isNotBlank() && order.tipCents >= 0 &&
                order.pickupZone.isNotBlank() && order.deliveryZone.isNotBlank() &&
                order.pickupAddress.isNotBlank() && order.deliveryAddress.isNotBlank() &&
                listOf(order.pickup, order.delivery).all {
                    it.latitude.isFinite() && it.longitude.isFinite() &&
                        it.latitude in -12.5..-11.5 && it.longitude in -77.3..-76.5
                }
        })
        return orders
    }
    fun nearest(orders: List<DemoOrder>, lat: Double, lng: Double, excluded: Set<String>): DemoOrder? =
        orders.filter { it.id !in excluded }.map { it to
            LocationUtils.distanceMeters(lat, lng, it.pickup.latitude, it.pickup.longitude)
        }.filter { it.second <= SEARCH_RADIUS_METERS }.minByOrNull { it.second }?.first
}
