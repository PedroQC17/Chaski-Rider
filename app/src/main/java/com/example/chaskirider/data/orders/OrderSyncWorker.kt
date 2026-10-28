package com.example.chaskirider.data.orders

import android.content.Context
import androidx.work.*
import com.example.chaskirider.domain.orders.OrderRecord
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.FieldValue
import com.google.gson.Gson
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import java.util.concurrent.TimeUnit

class OrderSyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val uid = inputData.getString("uid") ?: return Result.failure()
        val auth = FirebaseAuth.getInstance()
        if (auth.currentUser?.uid != uid) return Result.success()
        val database = openOrderDatabase(applicationContext)
        try {
            val dao = database.orders()
            while (auth.currentUser?.uid == uid) {
                val pending = dao.pendingActions(uid)
                if (pending.isEmpty()) return Result.success()
                for (entity in pending) {
                    if (auth.currentUser?.uid != uid) return Result.success()
                    val record = Gson().fromJson(entity.content, OrderRecord::class.java)
                    val ref = FirebaseFirestore.getInstance().collection("riders").document(uid)
                        .collection("deliveryRecords").document(record.orderId)
                    withTimeout(20_000) {
                        FirebaseFirestore.getInstance().runTransaction { tx ->
                            val old = tx.get(ref)
                            val action = ref.collection("actions").document(entity.actionId)
                            val registeredAction = tx.get(action)
                            val incidents = record.problems.orEmpty().map { problem ->
                                val incidentRef = ref.collection("incidents").document(problem.id)
                                Triple(problem, incidentRef, tx.get(incidentRef).exists())
                            }
                            if ((old.getLong("revision") ?: 0) < entity.revision) {
                                tx.set(ref, mapOf(
                                "orderId" to record.orderId, "riderId" to uid, "status" to record.status.name,
                                "revision" to entity.revision, "merchantName" to record.merchantName,
                                "pickup" to mapOf("latitude" to record.pickup.latitude, "longitude" to record.pickup.longitude),
                                "destination" to mapOf("latitude" to record.destination.latitude, "longitude" to record.destination.longitude),
                                "acceptedAt" to record.acceptedAt, "arrivedAt" to record.arrivedAt,
                                "pickedUpAt" to record.pickedUpAt, "deliveredAt" to record.deliveredAt,
                                "cancelledAt" to record.cancelledAt, "updatedAt" to FieldValue.serverTimestamp()))
                                if (record.status in listOf(com.example.chaskirider.domain.orders.OrderStatus.DELIVERED,
                                        com.example.chaskirider.domain.orders.OrderStatus.CANCELLED))
                                    tx.delete(ref.collection("tracking").document("current"))
                            }
                            incidents.filterNot { it.third }.forEach { (problem, incidentRef, _) ->
                                tx.set(incidentRef, mapOf("orderId" to record.orderId, "riderId" to uid,
                                    "category" to problem.reason.name, "description" to problem.detail,
                                    "occurredAt" to problem.createdAt,
                                    "orderStatus" to (problem.orderStatus?.name ?: if (problem.pickedUp) "ON_THE_WAY" else "ACCEPTED"),
                                    "cancelled" to problem.cancelled, "createdAt" to FieldValue.serverTimestamp()))
                            }
                            if (!registeredAction.exists()) tx.set(action, mapOf(
                                "actionId" to entity.actionId, "orderId" to entity.orderId, "riderId" to uid,
                                "revision" to entity.revision, "kind" to entity.kind, "status" to record.status.name,
                                "waitStartedAt" to record.waitStartedAt, "createdAt" to FieldValue.serverTimestamp()))
                        }.await()
                    }
                    if (auth.currentUser?.uid != uid) return Result.success()
                    dao.acknowledge(entity)
                }
            }
            return Result.success()
        } catch (_: TimeoutCancellationException) { return Result.retry() }
        catch (error: CancellationException) { throw error }
        catch (error: FirebaseFirestoreException) {
            return if (error.code in listOf(FirebaseFirestoreException.Code.PERMISSION_DENIED,
                    FirebaseFirestoreException.Code.UNAUTHENTICATED, FirebaseFirestoreException.Code.INVALID_ARGUMENT))
                Result.failure() else Result.retry()
        } finally { database.close() }
    }

    companion object {
        fun schedule(context: Context, uid: String) {
            val request = OneTimeWorkRequestBuilder<OrderSyncWorker>()
                .setInputData(workDataOf("uid" to uid))
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS).build()
            WorkManager.getInstance(context).enqueueUniqueWork("order-sync-$uid", ExistingWorkPolicy.APPEND_OR_REPLACE, request)
        }
    }
}
