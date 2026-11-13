package com.example.chaskirider

import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.chaskirider.core.platform.location.GeoPoint
import com.example.chaskirider.feature.user.domain.RiderUser
import com.example.chaskirider.feature.orders.domain.*
import com.example.chaskirider.core.platform.location.LocationRepository
import com.example.chaskirider.feature.user.domain.RiderSession
import com.example.chaskirider.feature.orders.domain.CustomerContact
import com.example.chaskirider.feature.orders.domain.CustomerContactStore
import com.example.chaskirider.feature.orders.domain.PhoneCallState
import com.example.chaskirider.feature.orders.domain.PhoneCalls
import com.example.chaskirider.feature.orders.ui.viewmodel.CustomerContactViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CustomerContactViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val session = object : RiderSession { override val user = MutableStateFlow<RiderUser?>(RiderUser(id = "alice")) }
    private val records = mutableMapOf<Pair<String, String>, CustomerContact>()
    private val store = object : CustomerContactStore {
        override suspend fun load(uid: String, orderId: String) = records[uid to orderId]
        override suspend fun save(uid: String, orderId: String, contact: CustomerContact) { records[uid to orderId] = contact }
    }
    private var point: GeoPoint? = GeoPoint(-12.0, -77.0)
    private val locations = object : LocationRepository { override suspend fun currentLocation() = Result.success(point) }
    private var time = 1000L
    private val stores = mutableListOf<ViewModelStore>()
    private fun vm(): CustomerContactViewModel {
        val holder = ViewModelStore().also { stores += it }
        return ViewModelProvider(holder, viewModelFactory { initializer {
            CustomerContactViewModel(store, session, locations, now = { time }, elapsed = { time }, boot = { 1 })
        } })[CustomerContactViewModel::class.java]
    }
    private fun snapshot(id: String = "order") = OfferSnapshot(1000, true,
        DeliveryQuote(listOf(DeliveryStop("stop", id, null, RoutePoint(-12.0, -77.0))),
            100, 100, 60, 0, 0, 0, 0, 0, 100, 0, emptyList()), null, RoutePoint(-12.01, -77.01))

    @Before fun setup() { Dispatchers.setMain(dispatcher) }
    @After fun cleanup() { stores.forEach { it.clear() }; Dispatchers.resetMain() }
    private fun runModelTest(block: suspend TestScope.() -> Unit) = runTest {
        try { block() } finally { stores.forEach { it.clear() } }
    }

    @Test fun arrivalRequiresLocationAndNeverStartsWaitingByItself() = runModelTest {
        val vm = vm(); runCurrent(); vm.bind(snapshot()); runCurrent()
        point = GeoPoint(0.0, 0.0)
        vm.arrive(); runCurrent()
        assertNull(vm.uiState.value.contact)
        point = GeoPoint(-12.0, -77.0)
        vm.arrive(); runCurrent()
        assertNotNull(vm.uiState.value.contact)
        assertNull(vm.uiState.value.contact?.firstCallAt)
        assertEquals(0L, vm.uiState.value.waitedMillis)
    }

    @Test fun observedCallsPersistAndAnotherAccountOrOrderCannotReuseThem() = runModelTest {
        val vm = vm(); runCurrent(); vm.bind(snapshot()); runCurrent(); vm.arrive(); runCurrent()
        val phone = object : PhoneCalls {
            val events = MutableStateFlow(PhoneCallState.IDLE)
            var calls = 0
            override fun states(): Flow<PhoneCallState> = events
            override fun start(number: String) { calls++; events.value = PhoneCallState.OFFHOOK }
        }
        vm.call(phone, "test"); runCurrent()
        assertEquals(1000L, records["alice" to "order"]?.firstCallAt)
        assertEquals("test", records["alice" to "order"]?.phone)
        phone.events.value = PhoneCallState.IDLE
        time += 179_999
        vm.call(phone, "test"); runCurrent()
        assertEquals(1, phone.calls)
        time++
        vm.call(phone, "test"); runCurrent()
        assertEquals(2, phone.calls)
        val restored = vm(); runCurrent(); restored.bind(snapshot()); runCurrent()
        assertNotNull(restored.uiState.value.contact?.secondCallAt)
        restored.bind(snapshot("other")); runCurrent()
        assertNull(restored.uiState.value.contact)
        session.user.value = RiderUser(id = "bob"); runCurrent()
        restored.bind(snapshot()); runCurrent()
        assertNull(restored.uiState.value.contact)
    }

    @Test fun failedCallLaunchDoesNotStartCounter() = runModelTest {
        val vm = vm(); runCurrent(); vm.bind(snapshot()); runCurrent(); vm.arrive(); runCurrent()
        val phone = object : PhoneCalls {
            override fun states() = flowOf(PhoneCallState.IDLE)
            override fun start(number: String) { error("No SIM") }
        }
        vm.call(phone, "test"); runCurrent()
        assertNull(vm.uiState.value.contact?.firstCallAt)
        assertNotNull(vm.uiState.value.error)
    }
}
