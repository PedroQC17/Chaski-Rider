package com.example.chaskirider

import com.example.chaskirider.core.text.AndroidTextProvider
import com.example.chaskirider.feature.user.data.remote.RiderUserRemote
import com.example.chaskirider.feature.user.data.local.RiderUserSessionStore
import androidx.test.platform.app.InstrumentationRegistry
import com.example.chaskirider.feature.orders.data.local.OrderRecordEntity
import com.example.chaskirider.feature.orders.data.local.openOrderDatabase
import com.example.chaskirider.feature.orders.domain.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import com.google.gson.Gson
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.tasks.await
import org.junit.Assert.*
import org.junit.Assume.assumeNotNull
import org.junit.Test

class OfflineRecoveryDeviceTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test fun workerAcknowledgementUpdatesObserversAcrossRoomInstances() = runBlocking {
        val name = "sync-observer-test.db"
        context.deleteDatabase(name)
        val ui = openOrderDatabase(context, name)
        val worker = openOrderDatabase(context, name)
        try {
            val ready = CompletableDeferred<Unit>()
            val observed = async {
                withTimeout(10_000) {
                    ui.orders().observePendingCount("alice").onEach { if (it == 0) ready.complete(Unit) }.first { it == 1 }
                }
            }
            ready.await()
            val point = RoutePoint(-12.0, -77.0)
            val record = OrderRecord("one", "alice", OrderStatus.ACCEPTED, "Local", point, point, 1000, null, null, null)
            worker.orders().saveRecord(OrderRecordEntity("alice", "one", Gson().toJson(record), 1, true))
            assertEquals(1, observed.await())
            val cleared = async { withTimeout(10_000) { ui.orders().observePendingCount("alice").first { it == 0 } } }
            worker.orders().acknowledge(worker.orders().pendingActions("alice").single())
            assertEquals(0, cleared.await())
        } finally { ui.close(); worker.close(); context.deleteDatabase(name) }
    }

    @Test fun cachedProfileCanRestoreSameAccountWithoutServer() = runBlocking {
        val current = FirebaseAuth.getInstance().currentUser
        assumeNotNull(current)
        val uid = current!!.uid
        val firestore = FirebaseFirestore.getInstance()
        val server = firestore.document("riders/$uid").get(Source.SERVER).await()
        assertTrue(server.exists())
        val session = com.example.chaskirider.feature.user.data.local.RiderUserSessionStore()
        val source = com.example.chaskirider.feature.user.data.remote.RiderUserRemote(
            com.example.chaskirider.core.text.AndroidTextProvider(context), session, online = { false })
        firestore.disableNetwork().await()
        try {
            val cached = withTimeout(5_000) { source.load() }
            assertEquals(uid, cached.id)
            assertEquals(uid, session.user.value?.id)
            assertEquals(server.getString("status"), cached.status.name)
        } finally { firestore.enableNetwork().await() }
    }
}
