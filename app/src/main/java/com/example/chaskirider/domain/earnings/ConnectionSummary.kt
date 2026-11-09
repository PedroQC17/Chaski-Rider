package com.example.chaskirider.domain.earnings

import java.util.Calendar

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
