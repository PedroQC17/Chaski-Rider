package com.example.chaskirider.domain.earnings

/**
 * Interfaz sencilla para consultar ganancias y liquidaciones.
 */
interface EarningsRepository {
    suspend fun getEarnings(filter: EarningsDateFilter): List<DeliveryEarningsRecord>
    suspend fun getSettlements(): List<SettlementRecord>
}
