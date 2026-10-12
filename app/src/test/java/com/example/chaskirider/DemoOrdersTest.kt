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
        assertTrue(orders.size >= 105); assertTrue(orders.map { it.pickupZone }.distinct().size >= 20)
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
        try { repo.execute(OfferAction.DELIVER, lat = first.delivery.latitude, lng = first.delivery.longitude); fail() } catch (error: OfferRequestException) { assertEquals(409, error.status) }
        repo.execute(OfferAction.ARRIVE_MERCHANT, lat = first.pickup.latitude, lng = first.pickup.longitude); repo.execute(OfferAction.PICKUP, lat = first.pickup.latitude, lng = first.pickup.longitude)
        now += 3000
        val delivered = repo.execute(OfferAction.DELIVER, lat = first.delivery.latitude, lng = first.delivery.longitude)
        assertTrue(delivered.isDelivered); assertTrue(delivered.pickedUp)
        assertEquals(first.id, delivered.batch!!.stops.single().orderId)
        assertNotNull(delivered.arrivedAt); assertNotNull(delivered.pickedUpAt)
        assertEquals(delivered.deliveredAt, repo.execute(OfferAction.DELIVER, lat = first.delivery.latitude, lng = first.delivery.longitude).deliveredAt)
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
        repo.execute(OfferAction.ACCEPT, pending.offer.id); repo.execute(OfferAction.ARRIVE_MERCHANT, lat = first.pickup.latitude, lng = first.pickup.longitude)
        repo.execute(OfferAction.REPORT_NOT_READY, lat = first.pickup.latitude, lng = first.pickup.longitude); now += 360_000
        val picked = repo.execute(OfferAction.PICKUP, lat = first.pickup.latitude, lng = first.pickup.longitude); assertEquals(20, picked.waitingCompensationCents)
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

    @Test fun proximityUsesExactDistanceAndRejectsInvalidCoordinates() {
        val target = RoutePoint(0.0, 0.0)
        fun latitude(meters: Double) = Math.toDegrees(meters / 6371000.0)
        assertTrue(OrderLocationRules.isNear(latitude(99.99), 0.0, target))
        assertFalse(OrderLocationRules.isNear(latitude(100.4), 0.0, target))
        assertFalse(OrderLocationRules.isNear(null, 0.0, target))
        assertFalse(OrderLocationRules.isNear(Double.NaN, 0.0, target))
        assertFalse(OrderLocationRules.isNear(91.0, 0.0, target))
    }

    @Test fun arrivalPickupAndDeliveryCannotAdvanceFromAnotherLocation() = runTest {
        val repo = repository()
        val pending = offer(repo)
        val accepted = repo.execute(OfferAction.ACCEPT, pending.offer!!.id)
        suspend fun blocked(action: OfferAction, point: RoutePoint?, status: Int) {
            val before = repo.execute(OfferAction.STATE)
            try { repo.execute(action, lat = point?.latitude, lng = point?.longitude); fail() }
            catch (error: OfferRequestException) { assertEquals(status, error.status) }
            assertEquals(before, repo.execute(OfferAction.STATE))
        }
        blocked(OfferAction.ARRIVE_MERCHANT, null, 422)
        blocked(OfferAction.ARRIVE_MERCHANT, first.delivery, 412)
        assertNull(accepted.arrivedAt)
        repo.execute(OfferAction.ARRIVE_MERCHANT, lat = first.pickup.latitude, lng = first.pickup.longitude)
        blocked(OfferAction.PICKUP, first.delivery, 412)
        blocked(OfferAction.REPORT_NOT_READY, first.delivery, 412)
        repo.execute(OfferAction.PICKUP, lat = first.pickup.latitude, lng = first.pickup.longitude)
        blocked(OfferAction.DELIVER, first.pickup, 412)
        blocked(OfferAction.DELIVER, null, 422)
        assertTrue(repo.execute(OfferAction.DELIVER, lat = first.delivery.latitude, lng = first.delivery.longitude).isDelivered)
    }
    @Test fun cancellationPersistsReasonAndCannotResetAnActiveOrder() = runTest {
        val repo = repository(); val pending = offer(repo)
        repo.execute(OfferAction.ACCEPT, pending.offer!!.id)
        try { repo.execute(OfferAction.RESET); fail() }
        catch (error: OfferRequestException) { assertEquals(409, error.status) }
        try { repo.reportProblem(OrderProblemReason.OTHER, "short", true); fail() }
        catch (error: OfferRequestException) { assertEquals(400, error.status) }
        assertNotNull(repository().execute(OfferAction.STATE).batch)
        now += 400_000
        val cancelled = repo.reportProblem(OrderProblemReason.LOCAL_CLOSED, "El local tiene las puertas cerradas", true)
        assertNull(cancelled.batch)
        val history = saved["alice"]!!.problems!!.single()
        assertTrue(history.cancelled); assertFalse(history.pickedUp)
        assertEquals(first.id, history.orderId)
        assertEquals(OrderProblemReason.LOCAL_CLOSED, history.reason)
        assertNull(repository().execute(OfferAction.STATE).batch)
        assertEquals("two", offer(repository()).offer!!.quote.stops.single().orderId)
        assertEquals(history, saved["alice"]!!.problems!!.single())
    }

    @Test fun problemAfterPickupKeepsOrderAndCannotCancelOrDuplicateReport() = runTest {
        val repo = repository(); val pending = offer(repo)
        repo.execute(OfferAction.ACCEPT, pending.offer!!.id)
        repo.execute(OfferAction.ARRIVE_MERCHANT, lat = first.pickup.latitude, lng = first.pickup.longitude)
        repo.execute(OfferAction.PICKUP, lat = first.pickup.latitude, lng = first.pickup.longitude)
        try { repo.reportProblem(OrderProblemReason.OTHER, "No puedo continuar con el pedido", true); fail() }
        catch (error: OfferRequestException) { assertEquals(409, error.status) }
        val reported = repo.reportProblem(OrderProblemReason.CUSTOMER_UNREACHABLE, "El cliente no responde al contacto", false)
        assertTrue(reported.pickedUp); assertFalse(reported.isDelivered)
        assertNotNull(reported.batch); assertFalse(reported.problem!!.cancelled)
        assertEquals(reported, repository().execute(OfferAction.STATE))
        repo.reportProblem(OrderProblemReason.OTHER, "Otro intento del mismo reporte", false)
        assertEquals(1, saved["alice"]!!.problems!!.size)
        uid = "bob"; assertNull(repository().execute(OfferAction.STATE).problem)
        uid = "alice"
        assertTrue(repo.execute(OfferAction.DELIVER, lat = first.delivery.latitude, lng = first.delivery.longitude).isDelivered)
        repo.execute(OfferAction.RESET)
        assertEquals(1, saved["alice"]!!.problems!!.size)
    }

    @Test fun failedCancellationSaveKeepsAcceptedOrderAndHistoryUnchanged() = runTest {
        val repo = repository(); val pending = offer(repo)
        val accepted = repo.execute(OfferAction.ACCEPT, pending.offer!!.id)
        val badStore = object : DemoOrderStore {
            override suspend fun load(uid: String) = saved[uid]
            override suspend fun save(uid: String, state: DemoOrderState) { error("disk full") }
        }
        val broken = LocalDemoOfferRepository(badStore, { uid }, { listOf(first) }, { now }, { true })
        try { broken.reportProblem(OrderProblemReason.LOCAL_CLOSED, "El local tiene las puertas cerradas", true); fail() }
        catch (_: IllegalStateException) { }
        assertEquals(accepted, repository().execute(OfferAction.STATE))
        assertTrue(saved["alice"]!!.problems.orEmpty().isEmpty())
    }

}
