package com.example.chaskirider.ui.screens.earnings

import com.example.chaskirider.domain.earnings.*

data class EarningsUiState(
    val selectedTab: Int = 0,
    val selectedFilter: EarningsDateFilter = EarningsDateFilter.THIS_WEEK,
    val range: Pair<Long, Long>? = null,
    val records: List<DeliveryEarningsRecord> = emptyList(),
    val settlements: List<SettlementRecord> = emptyList(),
    val selectedOrder: DeliveryEarningsRecord? = null,
    val isLoading: Boolean = false,
    val warning: String? = null
) {
    val chaskiTotalCents: Long get() = records.sumOf { it.baseFareCents?.toLong() ?: 0 }
    val tipsTotalCents: Long get() = records.sumOf { it.tipCents?.toLong() ?: 0 }
    val grandTotalCents: Long get() = records.sumOf { it.totalCents?.toLong() ?: 0 }
    val unknownCount: Int get() = records.count { it.totalCents == null }
}
