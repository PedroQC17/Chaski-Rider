package com.example.chaskirider

import com.example.chaskirider.feature.earnings.domain.*
import org.junit.Assert.*
import org.junit.Test

class ConnectionSummaryTest {
 private val day = EarningsCalendar.parseDay("2026-11-02")!!
 private val hour = 3600000L
 @Test fun averageUsesActiveDaysAndCombinesSessions() {
  val sessions = listOf(ConnectionSession(day, day+4*hour), ConnectionSession(day+5*hour,day+11*hour),ConnectionSession(day+24*hour,day+29*hour))
  val result = ConnectionSummary.from(sessions,day,day+168*hour,day+168*hour)
  assertEquals(15*hour,result.totalMillis)
  assertEquals(2,result.days)
  assertEquals(15*hour/2,result.averageDailyMillis)
 }
 @Test fun activeConnectionSplitsAtLimaMidnightWithoutDoubleCounting() {
  val result = ConnectionSummary.from(listOf(ConnectionSession(day+23*hour),ConnectionSession(day+24*hour)),day,day+168*hour,day+26*hour)
  assertEquals(3*hour,result.totalMillis)
  assertEquals(2,result.days)
 }
 @Test fun clipsRangeAndIgnoresFutureSessions() {
  val result=ConnectionSummary.from(listOf(ConnectionSession(day-hour,day+hour),ConnectionSession(day+4*hour)),day,day+24*hour,day+2*hour)
  assertEquals(hour,result.totalMillis)
  assertEquals(1,result.days)
  assertEquals(0,ConnectionSummary.from(emptyList(),day,day+hour,day).days)
 }
}
