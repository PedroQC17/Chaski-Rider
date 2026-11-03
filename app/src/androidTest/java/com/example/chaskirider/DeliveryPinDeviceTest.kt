package com.example.chaskirider

import androidx.test.platform.app.InstrumentationRegistry
import androidx.work.*
import com.example.chaskirider.data.orders.*
import com.example.chaskirider.domain.orders.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import com.google.gson.Gson
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await
import org.junit.Assert.*
import org.junit.Assume.assumeNotNull
import org.junit.Test
import java.io.File
import java.util.UUID
import java.util.concurrent.TimeUnit

class DeliveryPinDeviceTest {
    @Test fun serverPinIsPrivateAndRequiredBeforePaidDeliverySyncs() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val user = FirebaseAuth.getInstance().currentUser
        assumeNotNull(user)
        val uid = user!!.uid
        val id = UUID.randomUUID().toString()
        val database = openOrderDatabase(context)
        val manager = WorkManager.getInstance(context)
        val name = "pin-device-$id"
        val now = System.currentTimeMillis()
        val point = RoutePoint(-12.04, -77.03)
        val record = OrderRecord(id, uid, OrderStatus.ON_THE_WAY, "Prueba técnica PIN", point, point,
            now - 3000, now - 2000, now - 1000, null, paidByCustomer = true)
        val remote = FirebaseFirestore.getInstance().document("riders/$uid/deliveryRecords/$id")
        val input = File(context.getExternalFilesDir(null), "pin-device-input.txt")
        val report = File(context.getExternalFilesDir(null), "pin-device-report.json")
        input.delete()
        report.writeText("""{"uid":"$uid","orderId":"$id","phase":"STARTED"}""")
        suspend fun sync(value: OrderRecord, revision: Long) {
            database.orders().saveRecord(OrderRecordEntity(uid, id, Gson().toJson(value), revision, true))
            manager.enqueueUniqueWork(name, ExistingWorkPolicy.REPLACE,
                OneTimeWorkRequestBuilder<OrderSyncWorker>().setInputData(workDataOf("uid" to uid)).build()).result.get(10, TimeUnit.SECONDS)
            withTimeout(90_000) { while (database.orders().record(uid,id)!!.pending) delay(250) }
        }
        try {
            sync(record, 1)
            try { FirebaseFirestore.getInstance().document("deliveryPins/${uid}_$id").get(Source.SERVER).await(); fail() }
            catch (_: com.google.firebase.firestore.FirebaseFirestoreException) { }
            report.writeText("""{"uid":"$uid","orderId":"$id","phase":"WAITING_PIN"}""")
            withTimeout(120_000) { while (!input.exists()) delay(500) }
            val code = input.readText().trim()
            assertTrue(code.matches(Regex("[0-9]{4}")))
            try { DeliveryPinRemote.verify(uid,id,if(code=="0000") "0001" else "0000"); fail() }
            catch (e: OfferRequestException) { assertEquals(428,e.status) }
            assertEquals("ON_THE_WAY",remote.get(Source.SERVER).await().getString("status"))
            DeliveryPinRemote.verify(uid,id,code)
            sync(record.copy(status=OrderStatus.DELIVERED,deliveredAt=System.currentTimeMillis()),2)
            assertEquals("DELIVERED",remote.get(Source.SERVER).await().getString("status"))
            assertFalse(remote.get(Source.SERVER).await().contains("evidencePhotoUrl"))
            report.writeText("""{"uid":"$uid","orderId":"$id","result":"PASSED"}""")
        } finally { input.delete(); manager.cancelUniqueWork(name).result.get(10,TimeUnit.SECONDS); database.close() }
    }
}
