package com.example.chaskirider.domain.orders

enum class OrderStatus { ACCEPTED, AT_MERCHANT, ON_THE_WAY, DELIVERED, CANCELLED }

data class OrderRecord(
    val orderId: String,
    val riderId: String,
    val status: OrderStatus,
    val merchantName: String,
    val pickup: RoutePoint,
    val destination: RoutePoint,
    val acceptedAt: Long?,
    val arrivedAt: Long?,
    val pickedUpAt: Long?,
    val deliveredAt: Long?,
    val cancelledAt: Long? = null
) {
    companion object {
        fun from(uid: String, snapshot: OfferSnapshot): OrderRecord? {
            val stop = snapshot.batch?.stops?.singleOrNull() ?: return null
            val status = when {
                snapshot.isDelivered -> OrderStatus.DELIVERED
                snapshot.pickedUp -> OrderStatus.ON_THE_WAY
                snapshot.arrivedAt != null -> OrderStatus.AT_MERCHANT
                else -> OrderStatus.ACCEPTED
            }
            return OrderRecord(stop.orderId, uid, status, snapshot.merchantName.orEmpty(),
                snapshot.merchant, stop.point, snapshot.acceptedAt, snapshot.arrivedAt,
                snapshot.pickedUpAt, snapshot.deliveredAt)
        }
    }
}
