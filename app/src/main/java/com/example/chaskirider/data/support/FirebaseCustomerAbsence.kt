package com.example.chaskirider.data.support

import com.example.chaskirider.data.orders.DemoOrderDao
import com.example.chaskirider.domain.support.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout

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
