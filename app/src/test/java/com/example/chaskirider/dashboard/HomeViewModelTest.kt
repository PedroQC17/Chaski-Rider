package com.example.chaskirider.dashboard
import com.example.chaskirider.core.platform.location.GeoPoint
import com.example.chaskirider.feature.user.domain.RiderUser
import com.example.chaskirider.feature.bank.domain.BankInfo
import com.example.chaskirider.feature.vehicle.domain.VehicleType
import com.example.chaskirider.feature.orders.domain.*
import com.example.chaskirider.core.platform.location.LocationRepository
import com.example.chaskirider.feature.user.domain.RiderSession
import com.example.chaskirider.feature.user.domain.RiderUserRepository
import com.example.chaskirider.feature.user.FakeRiderUserRepository
import com.example.chaskirider.core.text.*
import com.example.chaskirider.dashboard.viewmodel.HomeViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val session = object : RiderSession {
        override val user = MutableStateFlow<RiderUser?>(RiderUser(id = "alice", isAvailable = true))
    }
    private var zoneGate: CompletableDeferred<OfferSnapshot>? = null
    private var locationGate: CompletableDeferred<GeoPoint?>? = null
    private val requests = mutableListOf<String>()
    private fun snapshot(id: String) = OfferSnapshot(1000, false, null, null, RoutePoint(-12.0, -77.0),
        demandZones = listOf(DemandZone(id, id, "HIGH", -12.0, -77.0, 100, 1000)))
    private val offers = object : OfferRepository {
        override suspend fun execute(action: OfferAction, offerId: String?, lat: Double?, lng: Double?,
            evidenceUrl: String?, deliveryNote: String?, deliveryPin: String?): OfferSnapshot {
            val owner = session.user.value!!.id
            requests += owner
            val gate = zoneGate
            return if (gate != null) withContext(NonCancellable) { gate.await() } else snapshot(owner)
        }
    }
    private val locations = object : LocationRepository {
        override suspend fun currentLocation(): Result<GeoPoint?> {
            val gate = locationGate
            return Result.success(if (gate == null) GeoPoint(-12.0, -77.0) else withContext(NonCancellable) { gate.await() })
        }
    }
    private val users = object : FakeRiderUserRepository() {
        override suspend fun setAvailability(available: Boolean) = Result.success(session.user.value!!.copy(isAvailable = available))
    }
    private val texts = object : TextProvider {
        override fun get(key: TextKey) = key.name
        override fun resolveError(message: String?, fallback: TextKey) = message ?: fallback.name
    }
    @Before fun setup() { Dispatchers.setMain(dispatcher) }
    @After fun cleanup() { Dispatchers.resetMain() }
    @Test fun accountChangeClearsLocationAndIgnoresLatePreviousZoneResponse() = runTest(dispatcher) {
        val vm = HomeViewModel(users, locations, texts, offers, session)
        runCurrent(); vm.setLocationPermission(true); vm.locate(); runCurrent()
        assertEquals("alice", vm.uiState.value.demandZones.single().id)
        assertNotNull(vm.uiState.value.location)
        val gate = CompletableDeferred<OfferSnapshot>()
        zoneGate = gate; vm.refreshZones(); runCurrent()
        zoneGate = null
        session.user.value = RiderUser(id = "bob", isAvailable = true)
        runCurrent()
        assertNull(vm.uiState.value.location)
        assertEquals("bob", vm.uiState.value.demandZones.single().id)
        gate.complete(snapshot("late-alice")); runCurrent()
        assertEquals("bob", vm.uiState.value.demandZones.single().id)
        session.user.value = null; runCurrent()
        assertTrue(vm.uiState.value.demandZones.isEmpty()); assertFalse(vm.uiState.value.zonesLoaded)
        session.user.value = RiderUser(id = "alice"); runCurrent()
        assertEquals("alice", requests.last())
        assertEquals("alice", vm.uiState.value.demandZones.single().id)
    }
    @Test fun revokedLocationPermissionCannotBeUndoneByLateGpsResult() = runTest(dispatcher) {
        val vm = HomeViewModel(users, locations, texts, offers, session)
        runCurrent(); vm.setLocationPermission(true)
        val gate = CompletableDeferred<GeoPoint?>()
        locationGate = gate; vm.locate(); runCurrent()
        vm.setLocationPermission(false)
        gate.complete(GeoPoint(-12.0, -77.0)); runCurrent()
        assertFalse(vm.uiState.value.hasLocationPermission)
        assertNull(vm.uiState.value.location); assertFalse(vm.uiState.value.isLocating)
    }
}
