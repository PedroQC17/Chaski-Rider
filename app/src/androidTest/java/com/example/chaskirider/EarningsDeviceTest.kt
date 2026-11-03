package com.example.chaskirider

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.example.chaskirider.data.orders.*
import com.example.chaskirider.domain.earnings.*
import com.example.chaskirider.domain.model.RiderUser
import com.example.chaskirider.domain.orders.*
import com.example.chaskirider.domain.repository.RiderSession
import com.example.chaskirider.ui.screens.earnings.*
import com.example.chaskirider.ui.theme.ChaskiRiderTheme
import com.google.gson.Gson
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class EarningsDeviceTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private val quote = DeliveryQuote(listOf(DeliveryStop("1", "earnings-test", "Lima", RoutePoint(-12.01, -77.01))),
        1000, 2000, 600, 0, 100, 200, 250, 0, 550, 0, emptyList(), "PEN_1_KM_V1")

    @Test fun acceptedQuoteAndSingleEarningSurviveRoomReopenWithoutCrossingAccounts() = runBlocking {
        val name = "earnings-restore-test.db"
        context.deleteDatabase(name)
        var db = Room.databaseBuilder(context, DemoOrderDatabase::class.java, name).build()
        val gson = Gson()
        val now = System.currentTimeMillis()
        val accepted = OfferSnapshot(now, false, quote, null, RoutePoint(-12.0, -77.0), merchantName = "Local", acceptedAt = now)
        try {
            RoomDemoOrderStore(db.orders()).save("alice", DemoOrderState(accepted))
            db.close()
            db = Room.databaseBuilder(context, DemoOrderDatabase::class.java, name).build()
            val store = RoomDemoOrderStore(db.orders())
            val restored = store.load("alice")!!.snapshot
            val done = restored.copy(arrivedAt = now + 1, pickedUp = true, pickedUpAt = now + 2,
                isDelivered = true, deliveredAt = now + 3,
                batch = quote.copy(totalCents = 9999, tipCents = 5000))
            store.save("alice", DemoOrderState(done))
            store.save("alice", DemoOrderState(done))
            val rows = db.orders().observeRecords("alice").first()
            assertEquals(1, rows.size)
            val record = gson.fromJson(rows.single().content, OrderRecord::class.java)
            assertEquals(550, EarningsPolicy.record(record)!!.totalCents)
            assertEquals(250, EarningsPolicy.record(record)!!.tipCents)
            assertTrue(db.orders().observeRecords("bob").first().isEmpty())
            assertEquals(2, db.orders().actions("alice", "earnings-test").size)
            db.close()
            db = Room.databaseBuilder(context, DemoOrderDatabase::class.java, name).build()
            assertEquals(550, EarningsPolicy.record(gson.fromJson(db.orders().record("alice", "earnings-test")!!.content, OrderRecord::class.java))!!.totalCents)
        } finally { db.close(); context.deleteDatabase(name) }
    }

    @Test fun cancelledOrderPersistsZeroAndUnknownOldOrderStaysUnknown() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, DemoOrderDatabase::class.java).build()
        val gson = Gson()
        val now = System.currentTimeMillis()
        try {
            val store = RoomDemoOrderStore(db.orders())
            val accepted = OfferSnapshot(now, false, quote, null, RoutePoint(-12.0, -77.0), merchantName = "Local", acceptedAt = now)
            store.save("alice", DemoOrderState(accepted))
            val problem = OrderProblem("cancel", "earnings-test", OrderProblemReason.LOCAL_CLOSED, "El local se encuentra cerrado", now + 1, false, true, OrderStatus.ACCEPTED)
            store.save("alice", DemoOrderState(OfferSnapshot(now + 1, false, null, null, RoutePoint(0.0, 0.0)), problems = listOf(problem)))
            val record = gson.fromJson(db.orders().record("alice", "earnings-test")!!.content, OrderRecord::class.java)
            assertEquals(OrderStatus.CANCELLED, record.status)
            assertEquals(0, EarningsPolicy.record(record)!!.totalCents)
            val legacy = record.copy(orderId = "legacy", earningsQuote = null, status = OrderStatus.DELIVERED, deliveredAt = now)
            db.orders().putRecord(OrderRecordEntity("alice", "legacy", gson.toJson(legacy), 1, false))
            assertNull(EarningsPolicy.record(gson.fromJson(db.orders().record("alice", "legacy")!!.content, OrderRecord::class.java))!!.totalCents)
        } finally { db.close() }
    }

    @Test fun earningsScreenShowsRealBreakdownAndRemovesDataOnAccountChange() {
        val now = System.currentTimeMillis()
        val record = DeliveryEarningsRecord("one", "Negocio de prueba", now, 300, 250, 550,
            quote = AcceptedEarningsQuote.from(quote))
        val alice = MutableStateFlow(EarningsSnapshot(records = listOf(record)))
        val session = object : RiderSession { override val user = MutableStateFlow<RiderUser?>(RiderUser(id = "alice")) }
        val repo = object : EarningsRepository { override fun observe(uid: String) = if (uid == "alice") alice else flowOf(EarningsSnapshot()) }
        val model = EarningsViewModel(repo, session)
        compose.setContent { ChaskiRiderTheme { EarningsRoute(model, {}) } }
        compose.onNodeWithText("Negocio de prueba").performScrollTo().performClick()
        compose.onNodeWithText("Detalle de ganancias").assertIsDisplayed()
        compose.onNodeWithText("Hasta el local: 1.00 km").assertExists()
        compose.onNodeWithText("Cerrar").performClick()
        compose.runOnIdle { session.user.value = RiderUser(id = "bob") }
        compose.waitForIdle()
        compose.onNodeWithText("Negocio de prueba").assertDoesNotExist()
        compose.onNodeWithText("Aún no hay pedidos finalizados en estas fechas.").performScrollTo().assertIsDisplayed()
    }
}
