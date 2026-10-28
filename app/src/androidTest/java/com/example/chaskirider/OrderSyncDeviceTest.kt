package com.example.chaskirider

import androidx.test.platform.app.InstrumentationRegistry
import androidx.work.*
import com.example.chaskirider.data.orders.*
import com.example.chaskirider.domain.orders.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import com.google.gson.Gson
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import org.junit.Assert.*
import org.junit.Assume.assumeNotNull
import org.junit.Test
import java.io.File
import java.util.UUID
import java.util.concurrent.TimeUnit

class OrderSyncDeviceTest {
    @Test fun realWorkerUploadsAndRecoversAfterConnectionLoss() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val user = FirebaseAuth.getInstance().currentUser
        assumeNotNull(user)
        val uid = user!!.uid
        fun shell(command: String): String = android.os.ParcelFileDescriptor.AutoCloseInputStream(
            InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(command))
            .bufferedReader().use { it.readText().trim() }
        val wifi = shell("settings get global wifi_on")
        val data = shell("settings get global mobile_data")
        fun restoreNetwork() {
            shell("svc wifi ${if (wifi == "1") "enable" else "disable"}")
            shell("svc data ${if (data == "1") "enable" else "disable"}")
        }
        val id = UUID.randomUUID().toString()
        val name = "verify-order-sync-$id"
        val database = openOrderDatabase(context)
        val firestore = FirebaseFirestore.getInstance()
        val manager = WorkManager.getInstance(context)
        val dao = database.orders()
        val gson = Gson()
        val now = System.currentTimeMillis()
        val record = OrderRecord(id, uid, OrderStatus.ACCEPTED, "Prueba tecnica de sincronizacion",
            RoutePoint(-12.046, -77.037), RoutePoint(-12.055, -77.030), now, null, null, null,
            problems = listOf(OrderProblem(UUID.randomUUID().toString(), id, OrderProblemReason.VEHICLE_PROBLEM,
                "Prueba técnica de incidencia sincronizada", now, false, false, OrderStatus.ACCEPTED)))
        val remote = firestore.collection("riders").document(uid).collection("deliveryRecords").document(id)
        val report = File(context.getExternalFilesDir(null), "order-sync-verification.json")
        report.writeText("""{"uid":"$uid","orderId":"$id","result":"STARTED"}""")
        suspend fun enqueue(): UUID {
            val work = OneTimeWorkRequestBuilder<OrderSyncWorker>().setInputData(workDataOf("uid" to uid)).build()
            manager.enqueueUniqueWork(name, ExistingWorkPolicy.REPLACE, work).result.get(10, TimeUnit.SECONDS)
            return work.id
        }
        suspend fun awaitUpload() = withTimeout(90_000) {
            while (dao.record(uid, id)?.pending != false) delay(250)
        }
        try {
            dao.saveRecord(OrderRecordEntity(uid, id, gson.toJson(record), 1, true))
            enqueue()
            awaitUpload()
            val accepted = remote.get(Source.SERVER).await()
            assertEquals("ACCEPTED", accepted.getString("status"))
            assertEquals(1L, accepted.getLong("revision"))
            assertFalse(accepted.contains("latitude"))
            val incident = remote.collection("incidents").document(record.problems!!.single().id).get(Source.SERVER).await()
            assertEquals("VEHICLE_PROBLEM", incident.getString("category"))
            assertEquals("ACCEPTED", incident.getString("orderStatus"))

            shell("svc wifi disable")
            shell("svc data disable")
            val connectivity = context.getSystemService(android.net.ConnectivityManager::class.java)
            withTimeout(20_000) { while (connectivity.activeNetwork != null) delay(250) }
            val picked = record.copy(status = OrderStatus.ON_THE_WAY, arrivedAt = now + 1, pickedUpAt = now + 2)
            dao.saveRecord(OrderRecordEntity(uid, id, gson.toJson(picked), 2, true))
            val offlineWork = enqueue()
            withTimeout(55_000) {
                while (manager.getWorkInfoById(offlineWork).get(10, TimeUnit.SECONDS)!!.runAttemptCount < 1) delay(250)
                delay(22_000)
            }
            assertTrue(dao.record(uid, id)!!.pending)
            restoreNetwork()
            withTimeout(45_000) { while (connectivity.activeNetwork == null) delay(250) }
            awaitUpload()
            val pickedRemote = remote.get(Source.SERVER).await()
            assertEquals("ON_THE_WAY", pickedRemote.getString("status"))
            assertEquals(2L, pickedRemote.getLong("revision"))

            val delivered = picked.copy(status = OrderStatus.DELIVERED, deliveredAt = now + 3)
            dao.saveRecord(OrderRecordEntity(uid, id, gson.toJson(delivered), 3, true))
            enqueue()
            awaitUpload()
            assertEquals("DELIVERED", remote.get(Source.SERVER).await().getString("status"))
            assertFalse(dao.record(uid, id)!!.pending)
            report.writeText(
                """{"uid":"$uid","orderId":"$id","result":"PASSED"}""")
        } finally {
            restoreNetwork()
            firestore.enableNetwork().await()
            manager.cancelUniqueWork(name).result.get(10, TimeUnit.SECONDS)
            database.close()
        }
    }
}
