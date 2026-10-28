package com.example.chaskirider

import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
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
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.util.UUID
import java.util.concurrent.TimeUnit

class OrderTrackingDeviceTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun trackingStartsAfterPickupContinuesInBackgroundAndStopsAtDelivery() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val user = FirebaseAuth.getInstance().currentUser
        assumeNotNull(user)
        val uid = user!!.uid
        val id = UUID.randomUUID().toString()
        val database = openOrderDatabase(context)
        val dao = database.orders()
        val db = FirebaseFirestore.getInstance()
        val remote = db.collection("riders").document(uid).collection("deliveryRecords").document(id)
        val tracking = remote.collection("tracking").document("current")
        val manager = WorkManager.getInstance(context)
        val name = "tracking-device-$id"
        val now = System.currentTimeMillis()
        val record = OrderRecord(id, uid, OrderStatus.ACCEPTED, "Prueba tecnica de seguimiento",
            RoutePoint(-12.046, -77.037), RoutePoint(-12.055, -77.030), now, null, null, null)
        suspend fun sync(record: OrderRecord, revision: Long) {
            dao.saveRecord(OrderRecordEntity(uid, id, Gson().toJson(record), revision, true))
            manager.enqueueUniqueWork(name, ExistingWorkPolicy.REPLACE,
                OneTimeWorkRequestBuilder<OrderSyncWorker>().setInputData(workDataOf("uid" to uid)).build())
                .result.get(10, TimeUnit.SECONDS)
            withTimeout(60_000) { while (dao.record(uid, id)!!.pending) delay(250) }
        }
        try {
            sync(record, 1)
            assertFalse(FirebaseOrderTracking(uid, id).publish(TrackingPosition(-12.0, -77.0, 5f, System.currentTimeMillis())))
            assertFalse(tracking.get(Source.SERVER).await().exists())
            val picked = record.copy(status = OrderStatus.ON_THE_WAY, arrivedAt = now + 1, pickedUpAt = now + 2)
            sync(picked, 2)
            compose.runOnUiThread { OrderTrackingService.start(context, uid, id) }
            var firstTime = 0L
            withTimeout(70_000) {
                while (firstTime == 0L) {
                    firstTime = tracking.get(Source.SERVER).await().getLong("recordedAt") ?: 0
                    delay(1000)
                }
            }
            InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand("input keyevent KEYCODE_HOME").close()
            withTimeout(60_000) {
                while ((tracking.get(Source.SERVER).await().getLong("recordedAt") ?: 0) <= firstTime) delay(1500)
            }
            val delivered = picked.copy(status = OrderStatus.DELIVERED, deliveredAt = System.currentTimeMillis())
            sync(delivered, 3)
            assertFalse(tracking.get(Source.SERVER).await().exists())
            assertFalse(FirebaseOrderTracking(uid, id).publish(TrackingPosition(-12.0, -77.0, 5f, System.currentTimeMillis())))
            delay(17_000)
            assertFalse(tracking.get(Source.SERVER).await().exists())
            File(context.getExternalFilesDir(null), "tracking-verification.json").writeText(
                """{"uid":"$uid","orderId":"$id","result":"PASSED"}""")
        } finally {
            OrderTrackingService.stop(context)
            manager.cancelUniqueWork(name).result.get(10, TimeUnit.SECONDS)
            database.close()
        }
    }
}
