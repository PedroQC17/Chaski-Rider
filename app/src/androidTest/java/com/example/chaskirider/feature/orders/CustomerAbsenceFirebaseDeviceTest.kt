package com.example.chaskirider.feature.orders
import androidx.test.platform.app.InstrumentationRegistry
import androidx.work.*
import com.example.chaskirider.feature.orders.data.local.DemoOrderState
import com.example.chaskirider.feature.orders.data.local.OrderRecordEntity
import com.example.chaskirider.feature.orders.data.local.RoomDemoOrderStore
import com.example.chaskirider.feature.orders.data.local.openOrderDatabase
import com.example.chaskirider.feature.orders.data.OrderRepositoryImpl
import com.example.chaskirider.platform.OrderSyncWorker
import com.example.chaskirider.feature.orders.data.remote.FirebaseCustomerAbsence
import com.example.chaskirider.feature.support.data.FirebaseAssistantRepository
import com.example.chaskirider.feature.support.data.SupportRepositoryImpl
import com.example.chaskirider.feature.orders.domain.*
import com.example.chaskirider.feature.earnings.domain.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Source
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.util.UUID
import java.util.concurrent.TimeUnit

class CustomerAbsenceFirebaseDeviceTest {
    @Test fun serverResolutionClosesTicketPreservesPayAndReleasesLocalOrder() = runBlocking {
        org.junit.Assume.assumeTrue("Prueba real opcional; requiere preparar los tiempos del pedido tÃ©cnico",
            InstrumentationRegistry.getArguments().getString("testAbsenceFixture") == "true")
        val context=InstrumentationRegistry.getInstrumentation().targetContext
        val uid=FirebaseAuth.getInstance().currentUser?.uid
        assertNotNull("Se requiere una sesiÃ³n de pruebas aprobada",uid)
        val owner=uid!!
        val id=UUID.randomUUID().toString()
        val name="verify-absence-$id.db"
        var database=openOrderDatabase(context,name)
        val report=File(context.getExternalFilesDir(null),"absence-device-report.json")
        val input=File(context.getExternalFilesDir(null),"absence-device-ready.txt")
        val now=System.currentTimeMillis()
        val point=RoutePoint(-12.04,-77.03)
        val quote=DeliveryQuote(listOf(DeliveryStop("1",id,null,point)),1000,2000,300,0,100,200,50,0,350,0,emptyList(),"PEN_1_KM_V1")
        val snapshot=OfferSnapshot(now,true,quote,null,point,merchantName="Prueba tÃ©cnica cliente ausente",
            arrivedAt=now-8000,pickedUpAt=now-5000,acceptedAt=now-10000,paidByCustomer=true)
        val remote=FirebaseFirestore.getInstance().document("riders/$owner/deliveryRecords/$id")
        try {
            input.delete()
            RoomDemoOrderStore(database.orders()).save(owner,DemoOrderState(snapshot))
            database.orders().confirm(owner,id,1)
            remote.set(mapOf("orderId" to id,"riderId" to owner,"status" to "ON_THE_WAY","revision" to 1,
                "merchantName" to snapshot.merchantName,"pickup" to mapOf("latitude" to point.latitude,"longitude" to point.longitude),
                "destination" to mapOf("latitude" to point.latitude,"longitude" to point.longitude),"acceptedAt" to snapshot.acceptedAt,
                "arrivedAt" to snapshot.arrivedAt,"pickedUpAt" to snapshot.pickedUpAt,"deliveredAt" to null,"cancelledAt" to null,
                "paidByCustomer" to true,"earningsQuote" to mapOf("approachMeters" to 1000,"deliveryMeters" to 2000,
                    "chaskiCents" to 300,"tipCents" to 50,"totalCents" to 350,"pricingVersion" to "PEN_1_KM_V1","distanceSource" to "SIMULATED_GEODESIC"),
                "updatedAt" to FieldValue.serverTimestamp())).await()
            val service=FirebaseCustomerAbsence(database.orders()) { error("El pedido tÃ©cnico ya estÃ¡ sincronizado") }
            service.arrive(owner,id,point.latitude,point.longitude)
            service.call(owner,id,1)
            report.writeText("""{"uid":"$owner","orderId":"$id","phase":"WAITING_FIXTURE"}""")
            withTimeout(120000) { while (!input.exists()) delay(500) }
            service.call(owner,id,2)
            val withGemini=InstrumentationRegistry.getArguments().getString("testAbsenceGemini")=="true"
            if(withGemini) {
                val assistant=FirebaseAssistantRepository(SupportRepositoryImpl())
                val answer=assistant.send(owner,id,UUID.randomUUID().toString(),
                    "El cliente sigue ausente. Ya registrÃ© las llamadas y cumplÃ­ la espera. Revisa los requisitos del servidor para finalizar.",false)
                assertEquals("GEMINI",answer.mode);assertTrue(answer.canResolve);assertFalse(answer.needsHuman)
            }
            val repo=OrderRepositoryImpl(RoomDemoOrderStore(database.orders()),{owner},{emptyList()},resolveAbsence=service::resolve)
            val completed=repo.resolveCustomerAbsence(id)
            assertTrue(completed.isCompleted);assertFalse(completed.isDelivered);assertNull(completed.deliveredAt)
            assertEquals(completed,repo.resolveCustomerAbsence(id))
            val record=OrderRecord.from(owner,completed)!!
            val earning=EarningsPolicy.record(record)!!
            assertTrue(earning.unsuccessful);assertFalse(earning.cancelled);assertEquals(300,earning.totalCents)
            assertEquals(0,ActivitySummary.from(listOf(earning)).delivered)
            assertEquals(1,ActivitySummary.from(listOf(earning)).unsuccessful)
            val saved=remote.get(Source.SERVER).await()
            assertEquals("COMPLETED_UNSUCCESSFUL",saved.getString("status"))
            assertNull(saved.getLong("deliveredAt"));assertEquals(350L,saved.getLong("earningsQuote.totalCents"))
            val ticket=FirebaseFirestore.getInstance().document("riders/$owner/supportTickets/absence_$id")
            assertEquals("CLOSED",ticket.get(Source.SERVER).await().getString("status"))
            assertEquals(if(withGemini) 3 else 1,ticket.collection("messages").get(Source.SERVER).await().size())
            val mainDatabase=openOrderDatabase(context)
            val manager=WorkManager.getInstance(context)
            val workName="verify-absence-sync-$id"
            try {
                mainDatabase.orders().saveRecord(OrderRecordEntity(owner,id,com.google.gson.Gson().toJson(record),2,true))
                manager.enqueueUniqueWork(workName,ExistingWorkPolicy.REPLACE,
                    OneTimeWorkRequestBuilder<OrderSyncWorker>().setInputData(workDataOf("uid" to owner)).build())
                    .result.get(10,TimeUnit.SECONDS)
                withTimeout(60000) { while (mainDatabase.orders().record(owner,id)?.pending!=false) delay(250) }
                assertEquals("COMPLETED_UNSUCCESSFUL",remote.get(Source.SERVER).await().getString("status"))
                assertEquals(2L,remote.get(Source.SERVER).await().getLong("revision"))
            } finally {
                manager.cancelUniqueWork(workName).result.get(10,TimeUnit.SECONDS)
                mainDatabase.openHelper.writableDatabase.execSQL("DELETE FROM order_actions WHERE uid = ? AND orderId = ?",arrayOf(owner,id))
                mainDatabase.openHelper.writableDatabase.execSQL("DELETE FROM order_records WHERE uid = ? AND orderId = ?",arrayOf(owner,id))
                mainDatabase.close()
            }
            database.close();database=openOrderDatabase(context,name)
            val restored=OrderRepositoryImpl(RoomDemoOrderStore(database.orders()),{owner},{emptyList()})
            assertTrue(restored.execute(OfferAction.STATE).isCompleted)
            restored.execute(OfferAction.RESET)
            assertNull(restored.execute(OfferAction.STATE).batch)
            report.writeText("""{"uid":"$owner","orderId":"$id","result":"PASSED"}""")
        } finally {
            input.delete();database.close();context.deleteDatabase(name)
        }
    }
}
