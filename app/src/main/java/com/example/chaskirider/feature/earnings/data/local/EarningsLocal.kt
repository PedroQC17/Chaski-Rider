package com.example.chaskirider.feature.earnings.data.local

import com.example.chaskirider.feature.earnings.domain.*
import com.google.firebase.Timestamp

object SettlementParser {
    fun parse(id: String, data: Map<String, Any?>, now: Long): SettlementRecord? {
        val start = (data["periodStart"] as? String)?.let(EarningsCalendar::parseDay) ?: return null
        val end = EarningsCalendar.weekEnd(start)
        val amount = data["amountCents"] as? Long ?: return null
        val paidAt = (data["paidAt"] as? Timestamp)?.toDate()?.time ?: return null
        val ids = (data["orderIds"] as? List<*>)?.map { it as? String ?: return null } ?: return null
        val validId = id.matches(Regex("${EarningsCalendar.day(start)}(?:-[A-Za-z0-9_-]{1,40})?"))
        if (data["status"] != "PAID" || !validId || start != EarningsCalendar.weekStart(start) ||
            amount < 0 || end > now || paidAt < end || paidAt > now || ids.isEmpty() || ids.any { it.isBlank() } || ids.distinct().size != ids.size) return null
        return SettlementRecord(id, "${EarningsCalendar.day(start)} — ${EarningsCalendar.day(end - 1)}",
            amount, SettlementStatus.PAID, paidAt, ids, start)
    }
}
