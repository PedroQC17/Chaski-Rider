package com.example.chaskirider.feature.earnings.domain

import com.example.chaskirider.feature.orders.domain.AcceptedEarningsQuote
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

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
    val acceptedAt: Long? = null,
    val unsuccessful: Boolean = false
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
    val warning: String? = null,
    val connections: List<ConnectionSession> = emptyList(),
    val connectionWarning: String? = null
)

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

data class ConnectionSession(val startedAt: Long, val endedAt: Long? = null)
data class ConnectionSummary(val totalMillis: Long = 0, val days: Int = 0) {
    val averageDailyMillis: Long get() = if (days == 0) 0 else totalMillis / days
    companion object {
        fun from(sessions: List<ConnectionSession>, from: Long, until: Long, now: Long): ConnectionSummary {
            val daily = mutableMapOf<Long, Long>()
            var coveredUntil = from
            for (session in sessions.sortedBy { it.startedAt }) {
                var cursor = maxOf(from, session.startedAt, coveredUntil)
                val end = minOf(until, now, session.endedAt ?: now)
                if (end <= cursor) continue
                while (cursor < end) {
                    val day = Calendar.getInstance(EarningsCalendar.zone).apply {
                        timeInMillis = cursor
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                    val key = day.timeInMillis
                    day.add(Calendar.DATE, 1)
                    val next = minOf(end, day.timeInMillis)
                    daily[key] = (daily[key] ?: 0) + next - cursor
                    cursor = next
                }
                coveredUntil = end
            }
            return ConnectionSummary(daily.values.sum(), daily.size)
        }
    }
}
