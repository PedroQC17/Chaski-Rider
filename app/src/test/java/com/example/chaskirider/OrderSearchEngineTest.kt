package com.example.chaskirider

import com.example.chaskirider.data.orders.OrderSearchEngine
import com.example.chaskirider.domain.model.*
import com.example.chaskirider.domain.orders.*
import com.example.chaskirider.domain.repository.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class OrderSearchEngineTest {
    private val session = object : RiderSession {
        override val user = MutableStateFlow<RiderUser?>(RiderUser(id = "alice", status = RegistrationStatus.APPROVED,
            isAvailable = true, profilePhotoPath = "photo"))
    }
    private val calls = mutableListOf<OfferAction>()
    private var snapshot = OfferSnapshot(1000, false, null, null, RoutePoint(0.0, 0.0))
    private var point: GeoPoint? = GeoPoint(-12.1, -77.03)
    private var gate: CompletableDeferred<Unit>? = null
    private val locations = object : LocationRepository {
        override suspend fun currentLocation(): Result<GeoPoint?> { gate?.await(); return Result.success(point) }
    }
    private val repository = object : OfferRepository {
        override suspend fun execute(action: OfferAction, offerId: String?, lat: Double?, lng: Double?): OfferSnapshot {
            calls += action
            if (action == OfferAction.OFFER) { assertEquals(point!!.latitude, lat!!, 0.0); assertEquals(point!!.longitude, lng!!, 0.0) }
            return snapshot
        }
    }
    private fun engine() = OrderSearchEngine(repository, session, locations)

    @Test fun connectedRiderCanSearchWithoutAnActivityOrViewModel() = runTest {
        engine().poll(true)
        assertEquals(listOf(OfferAction.STATE, OfferAction.OFFER), calls)
    }
    @Test fun disconnectedOrUnapprovedRiderCannotSearch() = runTest {
        val rider = session.user.value!!
        for (user in listOf(null, rider.copy(isAvailable = false), rider.copy(isEnabled = false),
            rider.copy(status = RegistrationStatus.PENDING_REVIEW), rider.copy(profilePhotoPath = ""))) {
            session.user.value = user
            assertNull(engine().poll(true))
        }
        assertTrue(calls.isEmpty())
    }
    @Test fun missingGpsDoesNotInventAPosition() = runTest {
        point = null
        engine().poll(true)
        assertEquals(listOf(OfferAction.STATE), calls)
    }
    @Test fun existingOrderAndRefreshOnlyDoNotCreateAnotherOffer() = runTest {
        engine().poll(false)
        val quote = DeliveryQuote(emptyList(), 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, emptyList())
        snapshot = snapshot.copy(batch = quote)
        engine().poll(true)
        assertEquals(listOf(OfferAction.STATE, OfferAction.STATE), calls)
    }
    @Test fun disconnectWhileGpsIsPendingPreventsANewOffer() = runTest {
        gate = CompletableDeferred()
        val request = async { engine().poll(true) }
        yield()
        session.user.value = session.user.value!!.copy(isAvailable = false)
        gate!!.complete(Unit)
        assertNull(request.await())
        assertEquals(listOf(OfferAction.STATE), calls)
    }
    @Test fun accountChangeWhileGpsIsPendingCannotSendTheOldPositionToAnotherAccount() = runTest {
        gate = CompletableDeferred()
        val request = async { engine().poll(true) }
        yield()
        session.user.value = session.user.value!!.copy(id = "bob")
        gate!!.complete(Unit)
        assertNull(request.await())
        assertEquals(listOf(OfferAction.STATE), calls)
    }
}
