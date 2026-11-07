package com.example.chaskirider.domain.earnings

enum class EarningsDateFilter { TODAY, THIS_WEEK, THIS_MONTH, CUSTOM }
enum class SettlementStatus { PENDING, PAID }

data class DeliveryEarningsRecord(
    val orderId: String,
    val merchantName: String,
    val completedAt: Long,
    val baseFareCents: Int?,
    val tipCents: Int?,
    val totalCents: Int?,
    val cancelled: Boolean = false,
    val quote: AcceptedEarningsQuote? = null,
    val acceptedAt: Long? = null
) {
    val hasKnownAmount: Boolean get() = baseFareCents != null && tipCents != null && totalCents != null &&
        baseFareCents >= 0 && tipCents >= 0 && totalCents.toLong() == baseFareCents.toLong() + tipCents
}

data class SettlementRecord(
    val id: String,
    val periodLabel: String,
    val amountCents: Long,
    val status: SettlementStatus,
    val paidAt: Long? = null,
    val orderIds: List<String> = emptyList(),
    val periodStart: Long = 0
)

data class EarningsSnapshot(
    val records: List<DeliveryEarningsRecord> = emptyList(),
    val settlements: List<SettlementRecord> = emptyList(),
    val warning: String? = null
)
