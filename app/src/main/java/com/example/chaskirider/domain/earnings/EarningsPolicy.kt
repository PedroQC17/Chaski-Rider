package com.example.chaskirider.domain.earnings

import com.example.chaskirider.domain.orders.DeliveryQuote
import com.example.chaskirider.domain.orders.OrderRecord
import com.example.chaskirider.domain.orders.OrderStatus
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

data class AcceptedEarningsQuote(
    val approachMeters: Int,
    val deliveryMeters: Int,
    val chaskiCents: Int,
    val tipCents: Int,
    val totalCents: Int,
    val pricingVersion: String,
    val distanceSource: String
) {
    fun valid() = approachMeters in 0..1_000_000 && deliveryMeters in 0..1_000_000 && chaskiCents >= 0 && tipCents >= 0 &&
        totalCents.toLong() == chaskiCents.toLong() + tipCents &&
        pricingVersion in listOf("PEN_1_KM_V1", "LEGACY_QUOTE") && distanceSource == "SIMULATED_GEODESIC" &&
        (pricingVersion != "PEN_1_KM_V1" || chaskiCents == EarningsPolicy.routeCents(approachMeters, deliveryMeters))

    companion object {
        fun from(quote: DeliveryQuote) = AcceptedEarningsQuote(
            quote.approachMeters, quote.deliveryMeters, quote.totalCents - quote.tipCents,
            quote.tipCents, quote.totalCents, quote.pricingVersion ?: "LEGACY_QUOTE", "SIMULATED_GEODESIC")
    }
}

object EarningsPolicy {
    fun routeCents(approachMeters: Int, deliveryMeters: Int): Int {
        require(approachMeters >= 0 && deliveryMeters >= 0)
        return Math.toIntExact((approachMeters.toLong() + deliveryMeters + 5) / 10)
    }

    fun record(order: OrderRecord): DeliveryEarningsRecord? {
        val cancelled = order.status == OrderStatus.CANCELLED
        val time = when (order.status) {
            OrderStatus.DELIVERED -> order.deliveredAt
            OrderStatus.CANCELLED -> order.cancelledAt
            else -> return null
        } ?: return null
        val quote = order.earningsQuote?.takeIf { it.valid() }
        return DeliveryEarningsRecord(order.orderId, order.merchantName, time,
            if (cancelled) 0 else quote?.chaskiCents,
            if (cancelled) 0 else quote?.tipCents,
            if (cancelled) 0 else quote?.totalCents, cancelled, quote, order.acceptedAt)
    }
}

object EarningsCalendar {
    val zone: TimeZone get() = TimeZone.getTimeZone("America/Lima")
    private fun calendar(time: Long) = Calendar.getInstance(zone).apply { timeInMillis = time }
    private fun Calendar.midnight() { set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }
    fun day(time: Long) = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { timeZone = zone }.format(Date(time))
    fun parseDay(day: String): Long? {
        if (!day.matches(Regex("[0-9]{4}-[0-9]{2}-[0-9]{2}"))) return null
        return try { SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { timeZone = zone; isLenient = false }.parse(day)?.time }
        catch (_: java.text.ParseException) { null }
    }
    fun weekStart(time: Long): Long = calendar(time).apply {
        midnight(); add(Calendar.DATE, -((get(Calendar.DAY_OF_WEEK) + 5) % 7))
    }.timeInMillis
    fun weekEnd(start: Long) = calendar(start).apply { add(Calendar.DATE, 7) }.timeInMillis
    fun bounds(filter: EarningsDateFilter, now: Long, range: Pair<Long, Long>? = null): Pair<Long, Long> {
        if (filter == EarningsDateFilter.CUSTOM && range != null) return range.first to calendar(range.second).apply { add(Calendar.DATE, 1) }.timeInMillis
        val start = calendar(now).apply {
            midnight()
            when (filter) {
                EarningsDateFilter.THIS_WEEK -> add(Calendar.DATE, -((get(Calendar.DAY_OF_WEEK) + 5) % 7))
                EarningsDateFilter.THIS_MONTH -> set(Calendar.DAY_OF_MONTH, 1)
                else -> Unit
            }
        }.timeInMillis
        val end = calendar(start).apply { when (filter) {
            EarningsDateFilter.THIS_WEEK -> add(Calendar.DATE, 7)
            EarningsDateFilter.THIS_MONTH -> add(Calendar.MONTH, 1)
            else -> add(Calendar.DATE, 1)
        } }.timeInMillis
        return start to end
    }
    fun weekly(records: List<DeliveryEarningsRecord>, payments: List<SettlementRecord>): List<SettlementRecord> {
        val paidOrders = payments.filter { it.status == SettlementStatus.PAID }.flatMap { it.orderIds }.toSet()
        val pending = records.filter { !it.cancelled && it.totalCents != null && it.orderId !in paidOrders }
            .groupBy { weekStart(it.completedAt) }.map { (start, orders) ->
                SettlementRecord("pending-${day(start)}", "${day(start)} — ${day(weekEnd(start) - 1)}",
                    orders.sumOf { it.totalCents!!.toLong() }, SettlementStatus.PENDING,
                    orderIds = orders.map { it.orderId }, periodStart = start)
            }
        return (payments.filter { it.status == SettlementStatus.PAID } + pending).sortedByDescending { it.periodStart }
    }
}
