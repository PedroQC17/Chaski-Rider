package com.example.chaskirider

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.example.chaskirider.data.support.*
import com.example.chaskirider.domain.model.*
import com.example.chaskirider.domain.orders.*
import com.example.chaskirider.domain.repository.*
import com.example.chaskirider.domain.support.*
import com.example.chaskirider.ui.screens.support.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.util.UUID
import java.util.concurrent.atomic.AtomicLong

class CustomerContactDeviceTest {
    @get:Rule val compose = createComposeRule()
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test fun roomRestoresProgressAfterReopeningAndSeparatesAccountsAndOrders() = runBlocking {
        val name = "verify-contact-${UUID.randomUUID()}.db"
        fun open() = Room.databaseBuilder(context, CustomerContactDatabase::class.java, name).build()
        val record = CustomerContact(1000, 2000, 3000, 1, 182000, true, "000000000")
        var database = open()
        try {
            RoomCustomerContactStore(database.contacts()).save("alice", "order", record)
            database.close()
            database = open()
            val store = RoomCustomerContactStore(database.contacts())
            assertEquals(record, store.load("alice", "order"))
            assertNull(store.load("bob", "order"))
            assertNull(store.load("alice", "other"))
        } finally { database.close(); context.deleteDatabase(name) }
    }

    @Test fun reminderAcknowledgementPersistsAndSupportUnlocksAfterTenMinutes() = runBlocking {
        val database = Room.inMemoryDatabaseBuilder(context, CustomerContactDatabase::class.java).build()
        val store = RoomCustomerContactStore(database.contacts())
        val holder = ViewModelStore()
        val time = AtomicLong(481000)
        val session = object : RiderSession { override val user = MutableStateFlow<RiderUser?>(RiderUser(id = "alice")) }
        val locations = object : LocationRepository { override suspend fun currentLocation() = Result.success<GeoPoint?>(null) }
        var opened = false
        try {
            store.save("alice", "order", CustomerContact(500, 1000, 1000, 1, 181000))
            lateinit var vm: CustomerContactViewModel
            compose.runOnIdle {
                vm = ViewModelProvider(holder, viewModelFactory { initializer {
                    CustomerContactViewModel(store, session, locations, { time.get() }, { time.get() }, { 1 })
                } })[CustomerContactViewModel::class.java]
                vm.bind(OfferSnapshot(1000, true, DeliveryQuote(
                    listOf(DeliveryStop("stop", "order", null, RoutePoint(-12.0, -77.0))),
                    100, 100, 60, 0, 0, 0, 0, 0, 100, 0, emptyList()), null, RoutePoint(-12.01, -77.01)))
            }
            compose.setContent { CustomerContactDialog(vm, "order", null, {}, { opened = true }) }
            compose.waitUntil(10000) { !vm.uiState.value.loading }
            compose.onNodeWithText("Intenta también por WhatsApp").assertIsDisplayed()
            compose.onNodeWithText("Entendido").performClick()
            compose.waitUntil(10000) { vm.uiState.value.contact?.reminderSeen == true }
            compose.onNodeWithText("Hablar con soporte").performScrollTo().assertIsNotEnabled()
            assertTrue(store.load("alice", "order")!!.reminderSeen)
            time.set(601000)
            compose.waitUntil(10000) { vm.uiState.value.waitedMillis >= 600000 }
            compose.onNodeWithText("Hablar con soporte").performScrollTo().assertIsEnabled().performClick()
            assertTrue(opened)
        } finally {
            compose.runOnIdle { holder.clear() }
            database.close()
        }
    }
}
