package com.example.chaskirider

import com.example.chaskirider.data.orders.*
import com.example.chaskirider.domain.orders.*
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class DemoOrdersTest {
    private val first = DemoOrder("one", "Cocina", "Lima", "Recojo 1", RoutePoint(-12.046, -77.037),
        "Lima", "Entrega 1", RoutePoint(-12.055, -77.030), 100)
    private val second = first.copy(id = "two", pickup = RoutePoint(-12.050, -77.039))
    private var now = 1_000_000L
    private var uid: String? = "alice"
    private var available = true
    private val saved = mutableMapOf<String, DemoOrderState>()
    private val store = object : DemoOrderStore {
        override suspend fun load(uid: String) = saved[uid]
        override suspend fun save(uid: String, state: DemoOrderState) { saved[uid] = state }
    }
    private fun repository() = LocalDemoOfferRepository(store, { uid }, { listOf(first, second) }, { now }, { available })
    private suspend fun offer(repo: OfferRepository) = repo.execute(OfferAction.OFFER, lat = -12.046, lng = -77.037)

    @Test fun catalogCoversLimaWithDistinctPickupAndDelivery() {
        val file = listOf(File("src/main/assets/demo_orders_lima.json"), File("app/src/main/assets/demo_orders_lima.json")).first { it.exists() }
        val orders = DemoOrderCatalog.parse(file.readText())
        assertEquals(105, orders.size); assertEquals(35, orders.map { it.pickupZone }.distinct().size)
        assertTrue(orders.all { it.pickup != it.delivery })
    }
    @Test fun nearestUsesGpsAndRadiusWithoutDistantFallback() {
        assertEquals("one", DemoOrderCatalog.nearest(listOf(second, first), -12.046, -77.037, emptySet())?.id)
        assertEquals("two", DemoOrderCatalog.nearest(listOf(second, first), -12.046, -77.037, setOf("one"))?.id)
        assertNull(DemoOrderCatalog.nearest(listOf(first), -11.87, -77.07, emptySet()))
    }
    @Test fun refreshAndNewRepositoryKeepAcceptedOrder() = runTest {
        val repo = repository(); val pending = offer(repo)
        val accepted = repo.execute(OfferAction.ACCEPT, pending.offer!!.id)
        val restored = repository().execute(OfferAction.STATE)
        assertEquals(accepted, restored); assertNull(restored.offer)
        assertEquals(pending.offer.quote, restored.batch)
        assertEquals(first.pickup, restored.merchant)
    }
    @Test fun transitionsPreserveIdentityAndRequireCorrectOrder() = runTest {
        val repo = repository(); val pending = offer(repo)
        try { repo.execute(OfferAction.ACCEPT, "old-id"); fail() } catch (error: OfferRequestException) { assertEquals(409, error.status) }
        repo.execute(OfferAction.ACCEPT, pending.offer!!.id)
        try { repo.execute(OfferAction.DELIVER); fail() } catch (error: OfferRequestException) { assertEquals(409, error.status) }
        repo.execute(OfferAction.ARRIVE_MERCHANT); repo.execute(OfferAction.PICKUP)
        now += 3000
        val delivered = repo.execute(OfferAction.DELIVER)
        assertTrue(delivered.isDelivered); assertTrue(delivered.pickedUp)
        assertEquals(first.id, delivered.batch!!.stops.single().orderId)
        assertNotNull(delivered.arrivedAt); assertNotNull(delivered.pickedUpAt)
        assertEquals(delivered.deliveredAt, repo.execute(OfferAction.DELIVER).deliveredAt)
    }
    @Test fun rejectedExpiredAndCompletedOrdersDoNotImmediatelyRepeat() = runTest {
        val repo = repository(); val one = offer(repo)
        repo.execute(OfferAction.REJECT, one.offer!!.id)
        val two = offer(repo); assertEquals("two", two.offer!!.quote.stops.single().orderId)
        now += 45_001
        assertNull(repo.execute(OfferAction.STATE).offer)
        assertNull(offer(repo).offer)
        now += 300_000
        assertNotNull(offer(repo).offer)
    }
    @Test fun noAvailabilityAndNoGpsDoNotCreateAnOffer() = runTest {
        available = false
        assertNull(offer(repository()).offer)
        try { repository().execute(OfferAction.OFFER); fail() } catch (_: IllegalArgumentException) { }
        assertNull(repository().execute(OfferAction.STATE).offer)
    }
    @Test fun accountsAreIsolated() = runTest {
        val repo = repository(); val alice = offer(repo)
        repo.execute(OfferAction.ACCEPT, alice.offer!!.id)
        uid = "bob"; assertNull(repo.execute(OfferAction.STATE).batch)
        uid = "alice"; assertNotNull(repo.execute(OfferAction.STATE).batch)
        uid = null
        try { repo.execute(OfferAction.STATE); fail() } catch (error: OfferRequestException) { assertEquals(401, error.status) }
    }
    @Test fun paymentAddsUpAndWaitStopsAtPickup() = runTest {
        val repo = repository(); val pending = offer(repo)
        val quote = pending.offer!!.quote
        assertEquals(quote.totalCents, quote.baseCents + quote.approachCents + quote.deliveryCents + quote.guaranteeCents + quote.tipCents)
        repo.execute(OfferAction.ACCEPT, pending.offer.id); repo.execute(OfferAction.ARRIVE_MERCHANT)
        repo.execute(OfferAction.REPORT_NOT_READY); now += 360_000
        val picked = repo.execute(OfferAction.PICKUP); assertEquals(20, picked.waitingCompensationCents)
        now += 600_000
        assertEquals(20, repository().execute(OfferAction.STATE).waitingCompensationCents)
    }
    @Test fun failedSaveDoesNotConfirmAnAction() = runTest {
        val badStore = object : DemoOrderStore {
            override suspend fun load(uid: String) = saved[uid]
            override suspend fun save(uid: String, state: DemoOrderState) { error("disk full") }
        }
        val repo = repository(); val pending = offer(repo)
        val broken = LocalDemoOfferRepository(badStore, { uid }, { listOf(first) }, { now }, { true })
        try { broken.execute(OfferAction.ACCEPT, pending.offer!!.id); fail() } catch (_: IllegalStateException) { }
        assertNotNull(repo.execute(OfferAction.STATE).offer)
        assertNull(repo.execute(OfferAction.STATE).batch)
    }
}
