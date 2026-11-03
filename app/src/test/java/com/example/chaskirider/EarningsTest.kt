package com.example.chaskirider

import com.example.chaskirider.domain.earnings.*
import com.example.chaskirider.domain.orders.*
import com.example.chaskirider.domain.model.RiderUser
import com.example.chaskirider.domain.repository.RiderSession
import com.example.chaskirider.ui.screens.earnings.EarningsViewModel
import com.example.chaskirider.data.earnings.SettlementParser
import com.google.firebase.Timestamp
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class EarningsTest {
    private val dispatcher = StandardTestDispatcher()
    private val monday = EarningsCalendar.parseDay("2026-11-02")!!
    private val quote = AcceptedEarningsQuote(1234, 2345, 358, 250, 608, "PEN_1_KM_V1", "SIMULATED_GEODESIC")
    private fun order(id: String = "order", status: OrderStatus = OrderStatus.DELIVERED) = OrderRecord(
        id, "alice", status, "Local", RoutePoint(0.0, 0.0), RoutePoint(0.0, 0.0),
        monday, monday, monday, monday, cancelledAt = monday, earningsQuote = quote)
    @Before fun setup() { Dispatchers.setMain(dispatcher) }
    @After fun cleanup() { Dispatchers.resetMain() }

    @Test fun tariffRoundsCombinedDistanceToCentAndAddsNoBonus() {
        assertEquals(358, EarningsPolicy.routeCents(1234, 2345))
        assertEquals(1, EarningsPolicy.routeCents(4, 4))
        assertEquals(0, EarningsPolicy.routeCents(0, 0))
        assertEquals(1000, EarningsPolicy.routeCents(4000, 6000))
    }
    @Test fun completedUsesAcceptedQuoteAndCancellationEarnsZeroIncludingTip() {
        val done = EarningsPolicy.record(order())!!
        assertEquals(608, done.totalCents)
        assertEquals(250, done.tipCents)
        val cancelled = EarningsPolicy.record(order(status = OrderStatus.CANCELLED))!!
        assertEquals(0, cancelled.totalCents); assertEquals(0, cancelled.tipCents)
        assertNull(EarningsPolicy.record(order(status = OrderStatus.ON_THE_WAY)))
    }
    @Test fun legacyAndCorruptRecordsAreNotRepricedOrCountedAsZero() {
        val original = Gson().toJson(order().copy(earningsQuote = null))
        val restored = Gson().fromJson(original, OrderRecord::class.java)
        assertNull(EarningsPolicy.record(restored)!!.totalCents)
        assertNull(EarningsPolicy.record(order().copy(earningsQuote = quote.copy(totalCents = 1)))!!.totalCents)
        assertEquals(608, EarningsPolicy.record(Gson().fromJson(Gson().toJson(order()), OrderRecord::class.java))!!.totalCents)
    }
    @Test fun calendarUsesLimaMidnightAndMondayNotRollingHours() {
        val start = EarningsCalendar.parseDay("2026-11-01")!!
        assertEquals(5 * 3600_000L, start % 86400_000)
        assertEquals("2026-10-26", EarningsCalendar.day(EarningsCalendar.weekStart(monday - 1)))
        assertEquals(monday, EarningsCalendar.weekStart(monday))
        assertEquals(monday, EarningsCalendar.bounds(EarningsDateFilter.TODAY, monday + 100).first)
        assertEquals("2026-11-01", EarningsCalendar.day(EarningsCalendar.bounds(EarningsDateFilter.THIS_MONTH, monday).first))
        assertEquals("2026-12-01", EarningsCalendar.day(EarningsCalendar.bounds(EarningsDateFilter.THIS_MONTH, monday).second))
        assertNull(EarningsCalendar.parseDay("2026-02-30"))
    }
    @Test fun customRangeIncludesWholeLastDay() {
        val end = EarningsCalendar.parseDay("2026-11-05")!!
        assertEquals(end + 86400_000, EarningsCalendar.bounds(EarningsDateFilter.CUSTOM, monday, monday to end).second)
    }
    @Test fun settlementStaysPendingUntilManualPaymentAndDoesNotDuplicateCoveredOrders() {
        val done = EarningsPolicy.record(order())!!
        val unknown = done.copy(orderId = "legacy", totalCents = null)
        val cancelled = EarningsPolicy.record(order("cancelled", OrderStatus.CANCELLED))!!
        val pending = EarningsCalendar.weekly(listOf(done, unknown, cancelled), emptyList()).single()
        assertEquals(608L, pending.amountCents); assertEquals(SettlementStatus.PENDING, pending.status)
        val paid = pending.copy(id = "2026-11-02", status = SettlementStatus.PAID, paidAt = monday + 8 * 86400_000)
        assertEquals(listOf(paid), EarningsCalendar.weekly(listOf(done, unknown, cancelled), listOf(paid)))
        val extra = done.copy(orderId = "late")
        assertEquals(2, EarningsCalendar.weekly(listOf(done, extra), listOf(paid)).size)
    }
    @Test fun manualPaymentNeedsClosedWeekDateAndUniqueOrderIds() {
        val end = EarningsCalendar.weekEnd(monday)
        val data = mapOf("periodStart" to "2026-11-02", "status" to "PAID", "amountCents" to 608L,
            "paidAt" to Timestamp(java.util.Date(end)), "orderIds" to listOf("order"))
        assertNotNull(SettlementParser.parse("2026-11-02", data, end + 1))
        assertNotNull(SettlementParser.parse("2026-11-02-ajuste-1", data, end + 1))
        assertNull(SettlementParser.parse("2026-11-02", data, end - 1))
        assertNull(SettlementParser.parse("wrong", data, end + 1))
        assertNull(SettlementParser.parse("2026-11-02", data + ("orderIds" to listOf("order", "order")), end + 1))
        assertNull(SettlementParser.parse("2026-11-02", data + ("amountCents" to -1L), end + 1))
    }
    @Test fun modelUpdatesReactivelyClearsAccountsAndUsesVisibleTotals() = runTest(dispatcher) {
        val session = object : RiderSession { override val user = MutableStateFlow<RiderUser?>(RiderUser(id = "alice")) }
        val alice = MutableStateFlow(EarningsSnapshot())
        val bob = MutableStateFlow(EarningsSnapshot())
        val repository = object : EarningsRepository { override fun observe(uid: String) = if (uid == "alice") alice else bob }
        val model = EarningsViewModel(repository, session) { monday + 1000 }
        runCurrent()
        alice.value = EarningsSnapshot(records = listOf(EarningsPolicy.record(order())!!, EarningsPolicy.record(order("cancelled", OrderStatus.CANCELLED))!!))
        runCurrent(); assertEquals(608L, model.uiState.value.grandTotalCents)
        model.selectOrder(model.uiState.value.records.first())
        session.user.value = RiderUser(id = "bob")
        runCurrent(); assertTrue(model.uiState.value.records.isEmpty()); assertNull(model.uiState.value.selectedOrder)
        session.user.value = null
        runCurrent(); assertEquals(0L, model.uiState.value.grandTotalCents)
    }
    @Test fun repositoryFailureLeavesRetryableStateRatherThanSpinner() = runTest(dispatcher) {
        val session = object : RiderSession { override val user = MutableStateFlow<RiderUser?>(RiderUser(id = "alice")) }
        var broken = true
        val repository = object : EarningsRepository {
            override fun observe(uid: String): Flow<EarningsSnapshot> = flow { if (broken) error("offline") else emit(EarningsSnapshot()) }
        }
        val model = EarningsViewModel(repository, session) { monday }
        runCurrent(); assertFalse(model.uiState.value.isLoading); assertNotNull(model.uiState.value.warning)
        broken = false; model.refresh(); runCurrent(); assertNull(model.uiState.value.warning)
    }
}
