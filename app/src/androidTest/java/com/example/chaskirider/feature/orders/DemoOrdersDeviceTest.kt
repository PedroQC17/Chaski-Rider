package com.example.chaskirider.feature.orders
import com.example.chaskirider.R
import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.example.chaskirider.feature.orders.data.local.DemoOrderCatalog
import com.example.chaskirider.feature.orders.data.local.DemoOrderState
import com.example.chaskirider.data.local.ChaskiDatabase
import com.example.chaskirider.data.local.openChaskiDatabase
import com.example.chaskirider.feature.orders.data.local.OrderRecordEntity
import com.example.chaskirider.feature.orders.data.local.RoomDemoOrderStore
import com.example.chaskirider.feature.orders.data.OrderRepositoryImpl
import com.example.chaskirider.core.platform.location.GeoPoint
import com.example.chaskirider.feature.user.domain.RiderUser
import com.example.chaskirider.feature.user.domain.RegistrationStatus
import com.example.chaskirider.feature.orders.domain.*
import com.example.chaskirider.core.platform.location.LocationRepository
import com.example.chaskirider.feature.user.domain.RiderSession
import com.example.chaskirider.feature.orders.ui.routes.OrdersRoute
import com.example.chaskirider.feature.orders.ui.viewmodel.OrdersViewModel
import com.example.chaskirider.feature.orders.ui.components.orderNavigationIntent
import com.example.chaskirider.feature.chat.domain.*
import com.example.chaskirider.feature.chat.ui.viewmodel.ChatViewModel
import com.example.chaskirider.ui.theme.ChaskiRiderTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File

class DemoOrdersDeviceTest {
    @Test fun offlineActionsSurviveReopenAndOldAcknowledgementKeepsNewerStatePending() = runBlocking {
        val name = "actions-restore-test.db"
        context.deleteDatabase(name)
        var db = openChaskiDatabase(context, name, importLegacy = false)
        try {
            var dao = db.orders()
            val gson = com.google.gson.Gson()
            val point = RoutePoint(-12.0, -77.0)
            val first = OrderRecord("order", "alice", OrderStatus.ACCEPTED, "Local", point, point, 1000, null, null, null)
            dao.saveRecord(OrderRecordEntity("alice", "order", gson.toJson(first), 1, true))
            val arrival = first.copy(status = OrderStatus.AT_MERCHANT, arrivedAt = 2000)
            dao.saveRecord(OrderRecordEntity("alice", "order", gson.toJson(arrival), 2, true))
            dao.saveRecord(OrderRecordEntity("alice", "order", gson.toJson(arrival.copy(waitStartedAt = 3000)), 3, true))
            db.close()
            db = openChaskiDatabase(context, name, importLegacy = false)
            dao = db.orders()
            val pending = dao.pendingActions("alice")
            assertEquals(listOf("ACCEPTED", "AT_MERCHANT", "WAIT_STARTED"), pending.map { it.kind })
            assertEquals(3, pending.map { it.actionId }.distinct().size)
            assertTrue(dao.pendingActions("bob").isEmpty())
            dao.acknowledge(pending.first())
            assertTrue(dao.record("alice", "order")!!.pending)
            dao.acknowledge(pending.first())
            assertEquals(2, dao.pendingActions("alice").size)
            pending.drop(1).forEach { dao.acknowledge(it) }
            assertFalse(dao.record("alice", "order")!!.pending)
            assertEquals(3, dao.actions("alice", "order").size)
        } finally { db.close(); context.deleteDatabase(name) }
    }

    @Test fun orderOutboxPreservesNewerChangesAndCompletedHistory() = runBlocking {
        val database = Room.inMemoryDatabaseBuilder(context, ChaskiDatabase::class.java).build()
        try {
            val dao = database.orders()
            val repo = OrderRepositoryImpl(RoomDemoOrderStore(dao), { "alice" }, { catalog() }, hasOffer = { true })
            val offer = repo.execute(OfferAction.OFFER, lat = -12.046, lng = -77.037)
            val accepted = repo.execute(OfferAction.ACCEPT, offer.offer!!.id)
            val id = accepted.batch!!.stops.single().orderId
            val first = dao.pending("alice").single()
            assertEquals(1L, first.revision)
            repo.execute(OfferAction.STATE)
            assertEquals(1L, dao.pending("alice").single().revision)
            repo.execute(OfferAction.ARRIVE_MERCHANT, lat = offer.merchant.latitude, lng = offer.merchant.longitude)
            dao.confirm("alice", id, first.revision)
            assertEquals(2L, dao.pending("alice").single().revision)
            repo.reportProblem(OrderProblemReason.LOCAL_CLOSED, "El negocio no puede entregar el pedido", true)
            val cancelled = dao.pending("alice").single()
            assertEquals(OrderStatus.CANCELLED, com.google.gson.Gson().fromJson(cancelled.content, OrderRecord::class.java).status)
            val incident = com.google.gson.Gson().fromJson(cancelled.content, OrderRecord::class.java).problems!!.single()
            assertEquals(OrderStatus.AT_MERCHANT, incident.orderStatus)
            assertEquals(OrderProblemReason.LOCAL_CLOSED, incident.reason)
            assertTrue(incident.cancelled)
            repo.execute(OfferAction.RESET)
            assertNotNull(dao.record("alice", id))
            assertTrue(dao.pending("bob").isEmpty())
            dao.confirm("alice", id, cancelled.revision)
            assertTrue(dao.pending("alice").isEmpty())
        } finally { database.close() }
    }

    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private fun catalog() = context.assets.open("demo_orders_lima.json").bufferedReader().use {
        DemoOrderCatalog.parse(it.readText()).map { it.copy(paidByCustomer = false) }
    }
    private fun screenshot(name: String) {
        val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
        File(context.getExternalFilesDir(null), name).outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    @Test fun roomRestoresOrderAfterDatabaseReopensAndIsolatesAccounts() = runBlocking {
        val name = "demo-orders-device-test.db"
        context.deleteDatabase(name)
        var database = openChaskiDatabase(context, name, importLegacy = false)
        try {
            var uid = "alice"
            var repo = OrderRepositoryImpl(RoomDemoOrderStore(database.orders()), { uid }, { catalog() }, hasOffer = { true })
            val offer = repo.execute(OfferAction.OFFER, lat = -12.046, lng = -77.037)
            assertNotNull(offer.offer)
            repo.execute(OfferAction.ACCEPT, offer.offer!!.id)
            val picked = repo.execute(OfferAction.ARRIVE_MERCHANT, lat = offer.merchant.latitude, lng = offer.merchant.longitude)
            database.close()
            database = openChaskiDatabase(context, name, importLegacy = false)
            repo = OrderRepositoryImpl(RoomDemoOrderStore(database.orders()), { uid }, { catalog() }, hasOffer = { true })
            val restored = repo.execute(OfferAction.STATE)
            assertEquals(picked.batch, restored.batch)
            assertEquals(picked.merchant, restored.merchant)
            assertEquals(picked.arrivedAt, restored.arrivedAt)
            assertEquals(offer.merchant, restored.arrivalLocation)
            uid = "bob"; assertNull(repo.execute(OfferAction.STATE).batch)
        } finally { database.close(); context.deleteDatabase(name) }
    }

    @Test fun riderCanAcceptPickupAndDeliverSameSimulatedOrder() {
        val database = Room.inMemoryDatabaseBuilder(context, ChaskiDatabase::class.java).build()
        val session = object : RiderSession {
            override val user = MutableStateFlow<RiderUser?>(RiderUser(id = "device-rider", status = RegistrationStatus.APPROVED,
                isAvailable = true, profilePhotoPath = "photo"))
        }
        var gps = GeoPoint(-12.046, -77.037)
        val locations = object : LocationRepository {
            override suspend fun currentLocation(): Result<GeoPoint?> = Result.success(gps)
        }
        val repo = OrderRepositoryImpl(RoomDemoOrderStore(database.orders()), { session.user.value?.id }, { catalog() }, hasOffer = { true })
        val vm = OrdersViewModel(repo, session, locations = locations, automaticSearch = false)
        try {
            val chat = ChatViewModel(object : OrderChatRepository {
                override fun observe(uid: String, orderId: String, channel: ChatChannel) = kotlinx.coroutines.flow.flowOf(ChatConversation(emptyList()))
                override suspend fun send(uid: String, orderId: String, channel: ChatChannel, messageId: String, body: String) { }
            }, object : ChatDraftStore {
                override suspend fun load(key: String): ChatDraft? = null
                override suspend fun save(key: String, draft: ChatDraft) { }
                override suspend fun clear(key: String, draft: ChatDraft) { }
            }, session)
            compose.setContent { ChaskiRiderTheme { OrdersRoute(vm, {}, chatViewModel = chat) } }
            compose.waitUntil(10_000) { vm.uiState.value.snapshot != null && !vm.uiState.value.busy }
            compose.runOnUiThread { vm.createOffer() }
            compose.waitUntil(10_000) { vm.uiState.value.snapshot?.offer != null && !vm.uiState.value.busy }
            val orderId = vm.uiState.value.snapshot!!.offer!!.quote.stops.single().orderId
            compose.onNodeWithText("RECOJO").assertExists()
            compose.onNodeWithText("ENTREGA").assertExists()
            compose.onNodeWithText(context.getString(R.string.orders_accept)).assertIsEnabled()
            compose.onNodeWithText(context.getString(R.string.orders_accept)).performScrollTo().performClick()
            try {
                compose.waitUntil(10_000) { vm.uiState.value.snapshot?.batch != null && !vm.uiState.value.busy }
            } catch (error: Throwable) {
                throw AssertionError("AceptaciÃ³n: busy=${vm.uiState.value.busy}, error=${vm.uiState.value.error}, segundos=${vm.uiState.value.secondsLeft}", error)
            }
            screenshot("orders-accepted.png")
            compose.onNodeWithText("Navegar al local").assertExists()
            assertEquals("google.navigation:q=${vm.uiState.value.snapshot!!.merchant.latitude},${vm.uiState.value.snapshot!!.merchant.longitude}",
                orderNavigationIntent(vm.uiState.value.snapshot!!.merchant).dataString)
            compose.onNodeWithText("LleguÃ© al local").assertIsNotEnabled()
            val pickup = vm.uiState.value.snapshot!!.merchant
            gps = GeoPoint(pickup.latitude, pickup.longitude)
            compose.runOnUiThread { vm.fetchCurrentLocation() }
            compose.waitUntil(10_000) { vm.uiState.value.isWithinGeofence }
            compose.onNodeWithText("LleguÃ© al local").performScrollTo().performClick()
            compose.waitUntil(10_000) { vm.uiState.value.snapshot?.arrivedAt != null && !vm.uiState.value.busy }
            val destination = vm.uiState.value.snapshot!!.batch!!.stops.single().point
            gps = GeoPoint(destination.latitude, destination.longitude)
            compose.runOnUiThread { vm.fetchCurrentLocation() }
            compose.waitUntil(10_000) { !vm.uiState.value.isWithinGeofence }
            compose.onNodeWithText("Confirmar recojo").assertIsNotEnabled()
            gps = GeoPoint(pickup.latitude, pickup.longitude)
            compose.runOnUiThread { vm.fetchCurrentLocation() }
            compose.waitUntil(10_000) { vm.uiState.value.isWithinGeofence }
            compose.onNodeWithText("Confirmar recojo").performScrollTo().performClick()
            compose.waitUntil(10_000) { vm.uiState.value.snapshot?.pickedUp == true && !vm.uiState.value.busy }
            compose.onNodeWithText("Navegar al cliente").assertExists()
            compose.onNodeWithText("Navegar al local").assertDoesNotExist()
            compose.onNodeWithText("Confirmar entrega").assertIsNotEnabled()
            val delivery = vm.uiState.value.snapshot!!.batch!!.stops.single().point
            gps = GeoPoint(delivery.latitude, delivery.longitude)
            compose.runOnUiThread { vm.fetchCurrentLocation() }
            compose.waitUntil(10_000) { vm.uiState.value.isNearCustomer }
            compose.onNodeWithText("Confirmar entrega").performScrollTo().performClick()
            compose.waitUntil(10_000) { vm.uiState.value.snapshot?.isDelivered == true && !vm.uiState.value.busy }
            assertEquals(orderId, vm.uiState.value.snapshot!!.batch!!.stops.single().orderId)
            compose.onNodeWithText("Siguiente Pedido").assertExists()
            compose.onNodeWithText("Navegar al cliente").assertDoesNotExist()
            screenshot("orders-delivered.png")
        } finally {
            compose.runOnUiThread { vm.setForeground(false) }
            database.close()
        }
    }
}
