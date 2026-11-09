package com.example.chaskirider.domain.earnings

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class ActivitySummary(
    val delivered: Int = 0,
    val cancelled: Int = 0,
    val chaskiCents: Long = 0,
    val tipCents: Long = 0,
    val unknownAmounts: Int = 0,
    val averageDurationMillis: Double? = null,
    val timedDeliveries: Int = 0,
    val unsuccessful: Int = 0
) {
    val totalCents get() = chaskiCents + tipCents

    companion object {
        fun from(records: List<DeliveryEarningsRecord>): ActivitySummary {
            val completed = records.filterNot { it.cancelled }
            val delivered = completed.filterNot { it.unsuccessful }
            val known = completed.filter { it.hasKnownAmount }
            val durations = delivered.mapNotNull { record ->
                record.acceptedAt?.takeIf { it > 0 && it <= record.completedAt }?.let { record.completedAt - it }
            }
            return ActivitySummary(delivered.size, records.count { it.cancelled },
                known.sumOf { it.baseFareCents!!.toLong() },
                known.sumOf { it.tipCents!!.toLong() },
                completed.size - known.size,
                durations.takeIf { it.isNotEmpty() }?.map { it.toDouble() }?.average(), durations.size,
                completed.count { it.unsuccessful })
        }
    }
}

data class ActivityChartPoint(val periodStart: Long, val label: String, val amountCents: Long, val monthly: Boolean = false)

object ActivityChart {
    fun points(records: List<DeliveryEarningsRecord>, from: Long, until: Long): List<ActivityChartPoint> {
        if (until <= from) return emptyList()
        val monthly = until - from > 31L * 86_400_000
        val format = SimpleDateFormat(if (monthly) "MM/yy" else "dd/MM", Locale.US).apply { timeZone = EarningsCalendar.zone }
        fun start(time: Long): Long = Calendar.getInstance(EarningsCalendar.zone).apply {
            timeInMillis = time
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            if (monthly) set(Calendar.DAY_OF_MONTH, 1)
        }.timeInMillis
        val totals = records.filter { !it.cancelled && it.hasKnownAmount && it.completedAt >= from && it.completedAt < until }
            .groupBy { start(it.completedAt) }.mapValues { (_, orders) -> orders.sumOf { it.totalCents?.toLong() ?: 0 } }
        val cursor = Calendar.getInstance(EarningsCalendar.zone).apply { timeInMillis = start(from) }
        return buildList {
            while (cursor.timeInMillis < until) {
                val time = cursor.timeInMillis
                add(ActivityChartPoint(time, format.format(Date(time)), totals[time] ?: 0, monthly))
                cursor.add(if (monthly) Calendar.MONTH else Calendar.DATE, 1)
            }
        }
    }
}
