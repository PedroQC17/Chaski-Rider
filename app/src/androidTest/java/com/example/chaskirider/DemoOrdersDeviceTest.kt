package com.example.chaskirider

import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.example.chaskirider.data.orders.*
import com.example.chaskirider.domain.model.*
import com.example.chaskirider.domain.orders.*
import com.example.chaskirider.domain.repository.*
import com.example.chaskirider.ui.screens.orders.*
import com.example.chaskirider.ui.theme.ChaskiRiderTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File

class DemoOrdersDeviceTest {
    @Test fun databaseMigrationKeepsExistingState() = runBlocking {
        val name = "order-migration-test.db"
        context.deleteDatabase(name)
        val oldState = DemoOrderState(OfferSnapshot(1000, false, null, null, RoutePoint(-12.0, -77.0)))
        val file = context.getDatabasePath(name)
        file.parentFile!!.mkdirs()
        android.database.sqlite.SQLiteDatabase.openOrCreateDatabase(file, null).use { old ->
            old.execSQL("CREATE TABLE demo_orders (uid TEXT NOT NULL PRIMARY KEY, state TEXT NOT NULL)")
            old.execSQL("INSERT INTO demo_orders(uid, state) VALUES (?, ?)", arrayOf("alice", com.google.gson.Gson().toJson(oldState)))
            old.version = 1
        }
        val migrated = Room.databaseBuilder(context, DemoOrderDatabase::class.java, name)
            .addMigrations(DemoOrderDatabase.MIGRATION_1_2).build()
        try {
            assertEquals(oldState, RoomDemoOrderStore(migrated.orders()).load("alice"))
            assertTrue(migrated.orders().pending("alice").isEmpty())
        } finally { migrated.close(); context.deleteDatabase(name) }
    }

    @Test fun orderOutboxPreservesNewerChangesAndCompletedHistory() = runBlocking {
        val database = Room.inMemoryDatabaseBuilder(context, DemoOrderDatabase::class.java).build()
        try {
            val dao = database.orders()
            val repo = LocalDemoOfferRepository(RoomDemoOrderStore(dao), { "alice" }, { catalog() }, hasOffer = { true })
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
            repo.execute(OfferAction.RESET)
            assertNotNull(dao.record("alice", id))
            assertTrue(dao.pending("bob").isEmpty())
            dao.confirm("alice", id, cancelled.revision)
            assertTrue(dao.pending("alice").isEmpty())
        } finally { database.close() }
    }

    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private fun catalog() = context.assets.open("demo_orders_lima.json").bufferedReader().use { DemoOrderCatalog.parse(it.readText()) }
    private fun screenshot(name: String) {
        val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
        File(context.getExternalFilesDir(null), name).outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    @Test fun roomRestoresOrderAfterDatabaseReopensAndIsolatesAccounts() = runBlocking {
        val name = "demo-orders-device-test.db"
        context.deleteDatabase(name)
        var database = Room.databaseBuilder(context, DemoOrderDatabase::class.java, name).build()
        try {
            var uid = "alice"
            var repo = LocalDemoOfferRepository(RoomDemoOrderStore(database.orders()), { uid }, { catalog() }, hasOffer = { true })
            val offer = repo.execute(OfferAction.OFFER, lat = -12.046, lng = -77.037)
            assertNotNull(offer.offer)
            repo.execute(OfferAction.ACCEPT, offer.offer!!.id)
            val picked = repo.execute(OfferAction.ARRIVE_MERCHANT, lat = offer.merchant.latitude, lng = offer.merchant.longitude)
            database.close()
            database = Room.databaseBuilder(context, DemoOrderDatabase::class.java, name).build()
            repo = LocalDemoOfferRepository(RoomDemoOrderStore(database.orders()), { uid }, { catalog() }, hasOffer = { true })
            val restored = repo.execute(OfferAction.STATE)
            assertEquals(picked.batch, restored.batch)
            assertEquals(picked.merchant, restored.merchant)
            assertEquals(picked.arrivedAt, restored.arrivedAt)
            assertEquals(offer.merchant, restored.arrivalLocation)
            uid = "bob"; assertNull(repo.execute(OfferAction.STATE).batch)
        } finally { database.close(); context.deleteDatabase(name) }
    }

    @Test fun riderCanAcceptPickupAndDeliverSameSimulatedOrder() {
        val database = Room.inMemoryDatabaseBuilder(context, DemoOrderDatabase::class.java).build()
        val session = object : RiderSession {
            override val user = MutableStateFlow<RiderUser?>(RiderUser(id = "device-rider", status = RegistrationStatus.APPROVED,
                isAvailable = true, profilePhotoPath = "photo"))
        }
        var gps = GeoPoint(-12.046, -77.037)
        val locations = object : LocationRepository {
            override suspend fun currentLocation(): Result<GeoPoint?> = Result.success(gps)
        }
        val repo = LocalDemoOfferRepository(RoomDemoOrderStore(database.orders()), { session.user.value?.id }, { catalog() }, hasOffer = { true })
        val vm = OrdersViewModel(repo, session, locations)
        try {
            compose.setContent { ChaskiRiderTheme { OrdersRoute(vm, {}, isDemo = true) } }
            compose.waitUntil(10_000) { vm.uiState.value.snapshot != null && !vm.uiState.value.busy }
            compose.runOnUiThread { vm.createOffer() }
            compose.waitUntil(10_000) { vm.uiState.value.snapshot?.offer != null && !vm.uiState.value.busy }
            val orderId = vm.uiState.value.snapshot!!.offer!!.quote.stops.single().orderId
            compose.onNodeWithText("RECOJO").assertExists()
            compose.onNodeWithText("ENTREGA").assertExists()
            screenshot("orders-offer.png")
            compose.onNodeWithText(context.getString(R.string.orders_accept)).performClick()
            compose.waitUntil(10_000) { vm.uiState.value.snapshot?.batch != null && !vm.uiState.value.busy }
            screenshot("orders-accepted.png")
            compose.onNodeWithText("Navegar al local").assertExists()
            assertEquals("google.navigation:q=${vm.uiState.value.snapshot!!.merchant.latitude},${vm.uiState.value.snapshot!!.merchant.longitude}",
                orderNavigationIntent(vm.uiState.value.snapshot!!.merchant).dataString)
            compose.onNodeWithText("Llegué al local").assertIsNotEnabled()
            val pickup = vm.uiState.value.snapshot!!.merchant
            gps = GeoPoint(pickup.latitude, pickup.longitude)
            compose.runOnUiThread { vm.fetchCurrentLocation() }
            compose.waitUntil(10_000) { vm.uiState.value.isWithinGeofence }
            compose.onNodeWithText("Llegué al local").performClick()
            compose.waitUntil(10_000) { vm.uiState.value.snapshot?.arrivedAt != null && !vm.uiState.value.busy }
            val destination = vm.uiState.value.snapshot!!.batch!!.stops.single().point
            gps = GeoPoint(destination.latitude, destination.longitude)
            compose.runOnUiThread { vm.fetchCurrentLocation() }
            compose.waitUntil(10_000) { !vm.uiState.value.isWithinGeofence }
            compose.onNodeWithText("Confirmar recojo").assertIsNotEnabled()
            gps = GeoPoint(pickup.latitude, pickup.longitude)
            compose.runOnUiThread { vm.fetchCurrentLocation() }
            compose.waitUntil(10_000) { vm.uiState.value.isWithinGeofence }
            compose.onNodeWithText("Confirmar recojo").performClick()
            compose.waitUntil(10_000) { vm.uiState.value.snapshot?.pickedUp == true && !vm.uiState.value.busy }
            compose.onNodeWithText("Navegar al cliente").assertExists()
            compose.onNodeWithText("Navegar al local").assertDoesNotExist()
            compose.onNodeWithText("Confirmar entrega").assertIsNotEnabled()
            val delivery = vm.uiState.value.snapshot!!.batch!!.stops.single().point
            gps = GeoPoint(delivery.latitude, delivery.longitude)
            compose.runOnUiThread { vm.fetchCurrentLocation() }
            compose.waitUntil(10_000) { vm.uiState.value.isNearCustomer }
            compose.onNodeWithText("Confirmar entrega").performClick()
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
