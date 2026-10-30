package com.example.chaskirider.domain.earnings

enum class EarningsDateFilter {
    TODAY,
    THIS_WEEK,
    THIS_MONTH
}

enum class SettlementStatus {
    PENDING,
    PAID
}

data class DeliveryEarningsRecord(
    val orderId: String,
    val merchantName: String,
    val completedAt: Long,
    val baseFareCents: Int,
    val tipCents: Int = 0,
    val totalCents: Int = baseFareCents + tipCents
)

data class SettlementRecord(
    val id: String,
    val periodLabel: String,
    val amountCents: Int,
    val status: SettlementStatus,
    val paidAt: Long? = null
)
