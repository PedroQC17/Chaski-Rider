package com.example.chaskirider.data.earnings

import com.example.chaskirider.domain.earnings.*
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Repositorio de ganancias dinámico.
 * Contiene entregas base de la semana y suma dinámicamente cada nuevo pedido completado en la app.
 */
class LocalEarningsRepository(
    private val clock: () -> Long = System::currentTimeMillis
) : EarningsRepository {

    private val dynamicRecords = MutableStateFlow<List<DeliveryEarningsRecord>>(emptyList())

    init {
        val now = clock()
        val hour = 3_600_000L
        val day = 86_400_000L
        dynamicRecords.value = listOf(
            DeliveryEarningsRecord("PV-102-21", "Plaza Vea Magnolias", now - 2 * hour, 1200, 300),
            DeliveryEarningsRecord("BM-204-10", "Bembos San Isidro", now - 5 * hour, 1500, 500),
            DeliveryEarningsRecord("KFC-501", "KFC La Molina", now - 1 * day, 1800, 400),
            DeliveryEarningsRecord("SS-302", "Siete Sopas Surco", now - 2 * day, 2200, 800),
            DeliveryEarningsRecord("STB-101", "Starbucks Jockey", now - 3 * day, 1100, 200)
        )
    }

    override suspend fun getEarnings(filter: EarningsDateFilter): List<DeliveryEarningsRecord> {
        val now = clock()
        val day = 86_400_000L
        val all = dynamicRecords.value

        return when (filter) {
            EarningsDateFilter.TODAY -> all.filter { (now - it.completedAt) <= day }
            EarningsDateFilter.THIS_WEEK -> all.filter { (now - it.completedAt) <= 7 * day }
            EarningsDateFilter.THIS_MONTH -> all
        }
    }

    override suspend fun getSettlements(): List<SettlementRecord> {
        val now = clock()
        val day = 86_400_000L
        return listOf(
            SettlementRecord("S-101", "21 Oct - 27 Oct 2026", 18500, SettlementStatus.PENDING),
            SettlementRecord("S-100", "14 Oct - 20 Oct 2026", 24200, SettlementStatus.PAID, paidAt = now - 7 * day)
        )
    }

    /**
     * Agrega dinámicamente un nuevo pedido entregado en la app al historial de ganancias.
     */
    fun recordCompletedOrder(orderId: String, merchantName: String, baseFareCents: Int, tipCents: Int) {
        val record = DeliveryEarningsRecord(
            orderId = orderId,
            merchantName = merchantName,
            completedAt = clock(),
            baseFareCents = baseFareCents,
            tipCents = tipCents
        )
        dynamicRecords.value = listOf(record) + dynamicRecords.value
    }
}
