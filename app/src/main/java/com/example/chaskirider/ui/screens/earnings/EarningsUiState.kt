package com.example.chaskirider.ui.screens.earnings

import com.example.chaskirider.domain.earnings.*

/**
 * Estado simplificado de la UI de Ganancias.
 */
data class EarningsUiState(
    val selectedTab: Int = 0, // 0: Ingresos, 1: Liquidaciones
    val selectedFilter: EarningsDateFilter = EarningsDateFilter.THIS_WEEK,
    val records: List<DeliveryEarningsRecord> = emptyList(),
    val settlements: List<SettlementRecord> = emptyList(),
    val selectedOrder: DeliveryEarningsRecord? = null,
    val isLoading: Boolean = false
) {
    // Calculados automáticamente en tiempo real
    val chaskiTotalCents: Int get() = records.sumOf { it.baseFareCents }
    val tipsTotalCents: Int get() = records.sumOf { it.tipCents }
    val grandTotalCents: Int get() = records.sumOf { it.totalCents }
}
