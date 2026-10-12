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
            val picked = repo.execute(OfferAction.ARRIVE_MERCHANT)
            database.close()
            database = Room.databaseBuilder(context, DemoOrderDatabase::class.java, name).build()
            repo = LocalDemoOfferRepository(RoomDemoOrderStore(database.orders()), { uid }, { catalog() }, hasOffer = { true })
            val restored = repo.execute(OfferAction.STATE)
            assertEquals(picked.batch, restored.batch)
            assertEquals(picked.merchant, restored.merchant)
            assertEquals(picked.arrivedAt, restored.arrivedAt)
            uid = "bob"; assertNull(repo.execute(OfferAction.STATE).batch)
        } finally { database.close(); context.deleteDatabase(name) }
    }

    @Test fun riderCanAcceptPickupAndDeliverSameSimulatedOrder() {
        val database = Room.inMemoryDatabaseBuilder(context, DemoOrderDatabase::class.java).build()
        val session = object : RiderSession {
            override val user = MutableStateFlow<RiderUser?>(RiderUser(id = "device-rider", status = RegistrationStatus.APPROVED,
                isAvailable = true, profilePhotoPath = "photo"))
        }
        val locations = object : LocationRepository {
            override suspend fun currentLocation(): Result<GeoPoint?> = Result.success(GeoPoint(-12.046, -77.037))
        }
        val repo = LocalDemoOfferRepository(RoomDemoOrderStore(database.orders()), { session.user.value?.id }, { catalog() }, hasOffer = { true })
        val vm = OrdersViewModel(repo, session, locations)
        try {
            compose.setContent { ChaskiRiderTheme { OrdersRoute(vm, {}, isDemo = true) } }
            compose.waitUntil(10_000) { vm.uiState.value.snapshot != null && !vm.uiState.value.busy }
            compose.runOnUiThread { vm.createOffer() }
            compose.waitUntil(10_000) { vm.uiState.value.snapshot?.offer != null && !vm.uiState.value.busy }
            val orderId = vm.uiState.value.snapshot!!.offer!!.quote.stops.single().orderId
            compose.onNodeWithText("1 · Recojo").assertExists()
            compose.onNodeWithText("2 · Entrega · Cercado de Lima").assertExists()
            screenshot("orders-offer.png")
            compose.onNodeWithText(context.getString(R.string.orders_accept)).performClick()
            compose.waitUntil(10_000) { vm.uiState.value.snapshot?.batch != null && !vm.uiState.value.busy }
            compose.onNodeWithContentDescription("Minimizar").performScrollTo().performClick()
            screenshot("orders-accepted.png")
            compose.onNodeWithContentDescription("Expandir").performClick()
            compose.onNodeWithText("Simular llegada al recojo").performClick()
            compose.waitUntil(10_000) { vm.uiState.value.snapshot?.arrivedAt != null && !vm.uiState.value.busy }
            compose.onNodeWithText("Simular pedido recogido").performClick()
            compose.waitUntil(10_000) { vm.uiState.value.snapshot?.pickedUp == true && !vm.uiState.value.busy }
            compose.onNodeWithText("Simular entrega al cliente").performClick()
            compose.waitUntil(10_000) { vm.uiState.value.snapshot?.isDelivered == true && !vm.uiState.value.busy }
            assertEquals(orderId, vm.uiState.value.snapshot!!.batch!!.stops.single().orderId)
            compose.onNodeWithText("Siguiente Pedido").assertExists()
            screenshot("orders-delivered.png")
        } finally {
            compose.runOnUiThread { vm.setForeground(false) }
            database.close()
        }
    }
}
