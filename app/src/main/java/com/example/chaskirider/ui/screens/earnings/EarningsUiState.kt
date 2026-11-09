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
    val warning: String? = null,
    val chartPoints: List<ActivityChartPoint> = emptyList(),
    val connection: ConnectionSummary = ConnectionSummary(),
    val connectionWarning: String? = null
) {
    val activity = ActivitySummary.from(records)
    val chaskiTotalCents: Long get() = activity.chaskiCents
    val tipsTotalCents: Long get() = activity.tipCents
    val grandTotalCents: Long get() = activity.totalCents
    val unknownCount: Int get() = activity.unknownAmounts
}
