package com.example.chaskirider.feature.earnings.domain

import com.example.chaskirider.feature.orders.domain.AcceptedEarningsQuote
import com.example.chaskirider.feature.orders.domain.OrderRecord
import com.example.chaskirider.feature.orders.domain.OrderStatus
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object EarningsPolicy {

    fun record(order: OrderRecord): DeliveryEarningsRecord? {
        val cancelled = order.status == OrderStatus.CANCELLED
        val time = when (order.status) {
            OrderStatus.DELIVERED -> order.deliveredAt
            OrderStatus.CANCELLED -> order.cancelledAt
            OrderStatus.COMPLETED_UNSUCCESSFUL -> order.completedAt
            else -> return null
        } ?: return null
        val quote = order.earningsQuote?.takeIf { it.valid() }
        val unsuccessful = order.status == OrderStatus.COMPLETED_UNSUCCESSFUL
        val base = if (cancelled) 0 else quote?.chaskiCents
        val tip = if (cancelled || unsuccessful) 0 else quote?.tipCents
        val total = if (base != null && tip != null) base + tip else null
        return DeliveryEarningsRecord(order.orderId, order.merchantName, time,
            base, tip, total, cancelled, quote, order.acceptedAt, unsuccessful)
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
