package com.example.chaskirider.feature.orders.data.remote

import com.example.chaskirider.feature.orders.domain.OfferRequestException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException
import kotlinx.coroutines.tasks.await
import com.example.chaskirider.feature.orders.data.local.DemoOrderDao
import com.example.chaskirider.feature.orders.domain.ContactCheckpoint
import com.example.chaskirider.feature.orders.domain.CustomerAbsenceService
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeout
import com.example.chaskirider.core.platform.location.TrackingPosition
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue

object DeliveryPinRemote {
    suspend fun prepare(orderId: String) {
        FirebaseFunctions.getInstance("us-central1").getHttpsCallable("deliveryPin")
            .call(mapOf("action" to "prepare", "orderId" to orderId)).await()
    }
    suspend fun verify(uid: String, orderId: String, pin: String) {
        if (FirebaseAuth.getInstance().currentUser?.uid != uid) throw OfferRequestException(401)
        try {
            val response = kotlinx.coroutines.withTimeout(15_000) {
                FirebaseFunctions.getInstance("us-central1").getHttpsCallable("deliveryPin")
                    .call(mapOf("action" to "verify", "orderId" to orderId, "pin" to pin)).await().data as Map<*, *>
            }
            if (response["verified"] != true) throw OfferRequestException(428)
        } catch (_: kotlinx.coroutines.TimeoutCancellationException) { throw OfferRequestException(503) }
        catch (e: FirebaseFunctionsException) {
            throw OfferRequestException(when (e.code) {
                FirebaseFunctionsException.Code.RESOURCE_EXHAUSTED -> 429
                FirebaseFunctionsException.Code.FAILED_PRECONDITION -> 425
                FirebaseFunctionsException.Code.UNAUTHENTICATED -> 401
                FirebaseFunctionsException.Code.PERMISSION_DENIED, FirebaseFunctionsException.Code.INVALID_ARGUMENT -> 428
                else -> 503
            })
        }
        if (FirebaseAuth.getInstance().currentUser?.uid != uid) throw OfferRequestException(401)
    }
}

class FirebaseCustomerAbsence(private val dao: DemoOrderDao, private val schedule: (String) -> Unit) : CustomerAbsenceService {
    suspend fun awaitSync(uid: String, orderId: String) {
        check(FirebaseAuth.getInstance().currentUser?.uid == uid) { "La sesión cambió" }
        if (dao.record(uid, orderId)?.pending != false) {
            schedule(uid)
            withTimeout(30_000) { dao.observeRecord(uid, orderId).first { it?.pending == false } }
        }
        check(FirebaseAuth.getInstance().currentUser?.uid == uid)
    }
    private suspend fun request(uid: String, orderId: String, action: String, extra: Map<String, Any> = emptyMap()): Map<*, *> {
        awaitSync(uid, orderId)
        val result = withTimeout(30_000) {
            FirebaseFunctions.getInstance("us-central1").getHttpsCallable("customerAbsence")
                .call(mapOf("action" to action, "orderId" to orderId) + extra).await().data as Map<*, *>
        }
        check(FirebaseAuth.getInstance().currentUser?.uid == uid)
        return result
    }
    override suspend fun arrive(uid: String, orderId: String, latitude: Double, longitude: Double) {
        request(uid, orderId, "arrive", mapOf("latitude" to latitude, "longitude" to longitude))
    }
    override suspend fun call(uid: String, orderId: String, attempt: Int): ContactCheckpoint {
        val result = request(uid, orderId, "call", mapOf("attempt" to attempt))
        return ContactCheckpoint((result["firstCallAt"] as? Number)?.toLong(), (result["secondCallAt"] as? Number)?.toLong())
    }
    suspend fun resolve(uid: String, orderId: String): Long =
        (request(uid, orderId, "resolve")["completedAt"] as? Number)?.toLong() ?: error("Resolución incompleta")
}

class FirebaseOrderTracking(private val uid: String, private val orderId: String) {
    suspend fun publish(position: TrackingPosition): Boolean = withTimeout(15_000) {
        val auth = FirebaseAuth.getInstance()
        if (auth.currentUser?.uid != uid || !position.isRecent(System.currentTimeMillis())) return@withTimeout false
        val db = FirebaseFirestore.getInstance()
        val order = db.collection("riders").document(uid).collection("deliveryRecords").document(orderId)
        db.runTransaction { tx ->
            val record = tx.get(order)
            if (auth.currentUser?.uid != uid || record.getString("status") != "ON_THE_WAY" ||
                !position.isRecent(System.currentTimeMillis())) false
            else {
                val target = order.collection("tracking").document("current")
                val previous = tx.get(target)
                if ((previous.getLong("recordedAt") ?: 0) >= position.recordedAt) return@runTransaction true
                tx.set(target, mapOf(
                    "orderId" to orderId, "riderId" to uid, "latitude" to position.latitude,
                    "longitude" to position.longitude, "accuracyMeters" to position.accuracyMeters,
                    "recordedAt" to position.recordedAt, "updatedAt" to FieldValue.serverTimestamp()))
                true
            }
        }.await()
    }
}
