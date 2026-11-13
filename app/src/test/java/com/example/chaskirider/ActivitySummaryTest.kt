package com.example.chaskirider

import com.example.chaskirider.feature.earnings.domain.*
import com.example.chaskirider.feature.user.domain.RiderUser
import com.example.chaskirider.feature.user.domain.RiderSession
import com.example.chaskirider.feature.earnings.ui.viewmodel.EarningsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class ActivitySummaryTest {
    private val dispatcher = StandardTestDispatcher()
    private val monday = EarningsCalendar.parseDay("2026-11-02")!!
    private val day = 86_400_000L
    private fun delivered(id: String, time: Long, minutes: Long) = DeliveryEarningsRecord(
        id, "Local", time, 300, 250, 550, acceptedAt = time - minutes * 60_000)
    @Before fun setup() { Dispatchers.setMain(dispatcher) }
    @After fun cleanup() { Dispatchers.resetMain() }

    @Test fun summaryCountsCancellationsSeparatelyAndAveragesOnlyValidCompletedTimes() {
        val good = delivered("one", monday + 3_600_000, 10)
        val two = delivered("two", monday + day, 20)
        val cancelled = good.copy(orderId = "cancel", cancelled = true, baseFareCents = 999, tipCents = 999, totalCents = 1998)
        val missingTime = good.copy(orderId = "old", acceptedAt = null)
        val negativeTime = good.copy(orderId = "bad", acceptedAt = good.completedAt + 1)
        val invalidStart = good.copy(orderId = "epoch", acceptedAt = 0)
        val summary = ActivitySummary.from(listOf(good, two, cancelled, missingTime, negativeTime, invalidStart))
        assertEquals(5, summary.delivered); assertEquals(1, summary.cancelled)
        assertEquals(2, summary.timedDeliveries)
        assertEquals(900_000.0, summary.averageDurationMillis!!, 0.0)
        assertEquals(2750L, summary.totalCents)
        assertEquals(1250L, summary.tipCents)
    }
    @Test fun unknownOrInconsistentMoneyIsNotCountedButDeliveryAndValidTimeAre() {
        val good = delivered("one", monday + 3_600_000, 10)
        val legacy = good.copy(orderId = "old", totalCents = null)
        val corrupt = good.copy(orderId = "bad", totalCents = 999)
        val summary = ActivitySummary.from(listOf(good, legacy, corrupt))
        assertEquals(3, summary.delivered); assertEquals(3, summary.timedDeliveries)
        assertEquals(2, summary.unknownAmounts); assertEquals(550L, summary.totalCents)
        val points = ActivityChart.points(listOf(good, legacy, corrupt), monday, monday + 7 * day)
        assertEquals(summary.totalCents, points.sumOf { it.amountCents })
    }
    @Test fun emptyActivityHasNoInventedAverageAndSevenZeroDays() {
        val summary = ActivitySummary.from(emptyList())
        assertEquals(0, summary.delivered); assertEquals(0, summary.cancelled)
        assertEquals(0L, summary.totalCents); assertNull(summary.averageDurationMillis)
        val points = ActivityChart.points(emptyList(), monday, monday + 7 * day)
        assertEquals(7, points.size); assertTrue(points.all { it.amountCents == 0L })
    }
    @Test fun chartKeepsZeroDaysAndUsesLimaCompletionDate() {
        val before = delivered("before", monday - 1, 1)
        val first = delivered("one", monday, 10)
        val second = delivered("two", monday + 2 * day, 20)
        val cancelled = first.copy(orderId = "cancel", cancelled = true)
        val nextWeek = delivered("next", monday + 7 * day, 1)
        val points = ActivityChart.points(listOf(before, first, second, cancelled, nextWeek), monday, monday + 7 * day)
        assertEquals(listOf(550L, 0L, 550L, 0L, 0L, 0L, 0L), points.map { it.amountCents })
        assertEquals("02/11", points.first().label); assertEquals("08/11", points.last().label)
        assertTrue(points.none { it.monthly })
    }
    @Test fun longerRangesUseMonthsWithoutLosingPartialMonthTotals() {
        val start = EarningsCalendar.parseDay("2026-10-20")!!
        val end = EarningsCalendar.parseDay("2026-12-03")!!
        val records = listOf(delivered("one", start, 10), delivered("two", monday, 10), delivered("three", end - 1, 10), delivered("outside", end, 1))
        val points = ActivityChart.points(records, start, end)
        assertEquals(listOf("10/26", "11/26", "12/26"), points.map { it.label })
        assertEquals(1650L, points.sumOf { it.amountCents })
        assertTrue(points.all { it.monthly })
        assertTrue(ActivityChart.points(records, end, start).isEmpty())
    }
    @Test fun modelChangesAllMetricsAndChartTogetherAndDeduplicatedRoomSnapshotDoesNotAccumulate() = runTest(dispatcher) {
        val session = object : RiderSession { override val user = MutableStateFlow<RiderUser?>(RiderUser(id = "alice")) }
        val feed = MutableStateFlow(EarningsSnapshot())
        val repository = object : EarningsRepository { override fun observe(uid: String) = feed }
        val now = monday + day + 12 * 3_600_000
        val model = EarningsViewModel(repository, session) { now }
        runCurrent()
        val one = delivered("one", monday + 3_600_000, 10)
        val two = delivered("two", monday + day + 3_600_000, 20)
        val cancel = one.copy(orderId = "cancel", cancelled = true, totalCents = 0, baseFareCents = 0, tipCents = 0)
        feed.value = EarningsSnapshot(records = listOf(one, two, cancel, delivered("future", now + 1, 1)))
        runCurrent()
        assertEquals(2, model.uiState.value.activity.delivered); assertEquals(1, model.uiState.value.activity.cancelled)
        assertEquals(1100L, model.uiState.value.grandTotalCents)
        assertEquals(7, model.uiState.value.chartPoints.size)
        model.selectFilter(EarningsDateFilter.TODAY)
        assertEquals(1, model.uiState.value.activity.delivered); assertEquals(0, model.uiState.value.activity.cancelled)
        assertEquals(1200_000.0, model.uiState.value.activity.averageDurationMillis!!, 0.0)
        assertEquals(1, model.uiState.value.chartPoints.size)
        assertEquals(550L, model.uiState.value.chartPoints.sumOf { it.amountCents })
        model.selectFilter(EarningsDateFilter.THIS_WEEK)
        feed.value = EarningsSnapshot(records = listOf(one, two, cancel))
        runCurrent(); model.refresh(); runCurrent()
        assertEquals(2, model.uiState.value.activity.delivered)
        assertEquals(model.uiState.value.grandTotalCents, model.uiState.value.chartPoints.sumOf { it.amountCents })
    }
}
