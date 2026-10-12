package com.example.chaskirider

import com.example.chaskirider.domain.model.*
import com.example.chaskirider.domain.orders.*
import com.example.chaskirider.domain.repository.*
import com.example.chaskirider.ui.screens.orders.OrdersViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class OrdersViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val session = object : RiderSession {
        override val user = MutableStateFlow<RiderUser?>(RiderUser(id = "alice", status = RegistrationStatus.APPROVED,
            isAvailable = true, profilePhotoPath = "photo"))
    }
    private val empty = OfferSnapshot(1000, false, null, null, RoutePoint(0.0, 0.0))
    private var result = empty
    private var failure: Exception? = null
    private var point: GeoPoint? = GeoPoint(-12.046, -77.037)
    private var locationGate: CompletableDeferred<Unit>? = null
    private val calls = mutableListOf<OfferAction>()
    private var requestedPoint: GeoPoint? = null
    private val repository = object : OfferRepository {
        override suspend fun execute(action: OfferAction, offerId: String?, lat: Double?, lng: Double?): OfferSnapshot {
            calls += action
            requestedPoint = if (lat != null && lng != null) GeoPoint(lat, lng) else null
            failure?.let { throw it }
            return result
        }
    }
    private val locations = object : LocationRepository {
        override suspend fun currentLocation(): Result<GeoPoint?> { locationGate?.await(); return Result.success(point) }
    }
    private fun model() = OrdersViewModel(repository, session, locations) { dispatcher.scheduler.currentTime }
    @Before fun setup() { Dispatchers.setMain(dispatcher) }
    @After fun cleanup() { Dispatchers.resetMain() }

    @Test fun searchUsesFreshGpsAndMissingGpsDoesNotCallRepository() = runTest(dispatcher) {
        val vm = model(); runCurrent()
        vm.createOffer(); advanceUntilIdle()
        assertEquals(point, requestedPoint); assertEquals(listOf(OfferAction.OFFER), calls)
        point = null; vm.createOffer(); advanceUntilIdle()
        assertEquals(1, calls.size); assertEquals(R.string.orders_location_required, vm.uiState.value.error)
    }
    @Test fun clearingErrorCannotConfirmArrival() = runTest(dispatcher) {
        val quote = DeliveryQuote(emptyList(), 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, emptyList())
        result = empty.copy(batch = quote)
        val vm = model(); runCurrent(); vm.refresh(); advanceUntilIdle()
        failure = OfferRequestException(409)
        vm.arriveAtMerchant(); advanceUntilIdle(); vm.clearError()
        assertNull(vm.uiState.value.snapshot!!.arrivedAt)
        assertNull(vm.uiState.value.error); assertFalse(vm.uiState.value.busy)
    }
    @Test fun accountSwitchCancelsGpsSearchAndRemovesOldOrder() = runTest(dispatcher) {
        val vm = model(); runCurrent(); locationGate = CompletableDeferred()
        vm.createOffer(); runCurrent()
        session.user.value = session.user.value!!.copy(id = "bob")
        runCurrent(); locationGate!!.complete(Unit); advanceUntilIdle()
        assertTrue(calls.isEmpty()); assertNull(vm.uiState.value.snapshot); assertFalse(vm.uiState.value.busy)
    }
    @Test fun disconnectDuringGpsSearchDoesNotGenerateOrder() = runTest(dispatcher) {
        val vm = model(); runCurrent(); locationGate = CompletableDeferred()
        vm.createOffer(); runCurrent(); session.user.value = session.user.value!!.copy(isAvailable = false)
        runCurrent(); locationGate!!.complete(Unit); advanceUntilIdle()
        assertTrue(calls.isEmpty())
    }
    @Test fun automaticSearchRunsOnlyConnectedAndInForeground() = runTest(dispatcher) {
        val vm = model(); runCurrent(); vm.setForeground(true); runCurrent()
        advanceTimeBy(5001); runCurrent(); assertTrue(calls.contains(OfferAction.OFFER))
        vm.setForeground(false); runCurrent(); val count = calls.size
        advanceTimeBy(60_000); runCurrent(); assertEquals(count, calls.size)
        session.user.value = session.user.value!!.copy(isAvailable = false); runCurrent()
        vm.setForeground(true); runCurrent(); val restored = calls.size
        advanceTimeBy(30_000); runCurrent(); assertEquals(restored, calls.size)
        vm.setForeground(false)
    }
    @Test fun anotherAccountRestoresItsStateWhileAppRemainsOpen() = runTest(dispatcher) {
        val vm = model(); runCurrent(); vm.setForeground(true); runCurrent()
        val initialReads = calls.count { it == OfferAction.STATE }
        session.user.value = session.user.value!!.copy(id = "bob"); runCurrent()
        assertEquals(initialReads + 1, calls.count { it == OfferAction.STATE })
        assertNotNull(vm.uiState.value.snapshot)
        vm.setForeground(false)
    }
}
