package com.example.chaskirider.feature.earnings.domain

import java.util.Calendar

data class IncomeSplit(
    val chaskiCents: Long,
    val tipsCents: Long
)

data class ActivityBreakdown(
    val successful: Int,
    val unsuccessful: Int,
    val cancelled: Int
)

data class ConnectionDay(
    val label: String,
    val millis: Long
)

data class EarningsDashboard(
    val earnings: List<ActivityChartPoint>,
    val income: IncomeSplit,
    val activity: ActivityBreakdown,
    val connection: List<ConnectionDay>
)

object EarningsService {
    fun build(
        records: List<DeliveryEarningsRecord>,
        sessions: List<ConnectionSession>,
        from: Long,
        until: Long,
        now: Long
    ): EarningsDashboard {
        val inRange = records.filter { it.completedAt in from until until }
        val summary = ActivitySummary.from(inRange)

        return EarningsDashboard(
            earnings = ActivityChart.points(inRange, from, until),
            income = IncomeSplit(summary.chaskiCents, summary.tipCents),
            activity = ActivityBreakdown(summary.delivered, summary.unsuccessful, summary.cancelled),
            connection = connectionDays(sessions, from, until, now)
        )
    }

    private fun connectionDays(
        sessions: List<ConnectionSession>,
        from: Long,
        until: Long,
        now: Long
    ): List<ConnectionDay> {
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
        return daily.entries.sortedBy { it.key }.map { (start, millis) ->
            ConnectionDay(EarningsCalendar.day(start), millis)
        }
    }
}
