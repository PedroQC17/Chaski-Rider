package com.example.chaskirider.data.orders

import com.example.chaskirider.domain.orders.TrackingPosition
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout

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
