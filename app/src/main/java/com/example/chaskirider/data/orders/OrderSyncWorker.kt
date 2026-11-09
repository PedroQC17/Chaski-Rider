package com.example.chaskirider.data.orders

import android.content.Context
import androidx.work.*
import com.example.chaskirider.domain.orders.OrderRecord
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.SetOptions
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
            var blocked = false
            for (orderId in dao.pendingOrderIds(uid)) {
                var stopOrder = false
                while (!stopOrder) {
                    if (auth.currentUser?.uid != uid) return Result.success()
                    val pending = dao.pendingOrderActions(uid, orderId)
                    if (pending.isEmpty()) break
                    for (entity in pending) {
                        if (auth.currentUser?.uid != uid) return Result.success()
                        try {
                            upload(entity, uid)
                            if (auth.currentUser?.uid != uid) return Result.success()
                            dao.acknowledge(entity)
                        } catch (_: TimeoutCancellationException) { return Result.retry() }
                        catch (error: CancellationException) { throw error }
                        catch (error: com.google.firebase.functions.FirebaseFunctionsException) {
                            android.util.Log.w("OrderSync", "No se pudo preparar PIN: ${error.code}")
                            if (error.code !in listOf(com.google.firebase.functions.FirebaseFunctionsException.Code.PERMISSION_DENIED,
                                com.google.firebase.functions.FirebaseFunctionsException.Code.UNAUTHENTICATED,
                                com.google.firebase.functions.FirebaseFunctionsException.Code.INVALID_ARGUMENT)) return Result.retry()
                            blocked = true; stopOrder = true; break
                        } catch (error: FirebaseFirestoreException) {
                            android.util.Log.w("OrderSync", "No se pudo sincronizar: ${error.code}")
                            if (error.code !in listOf(FirebaseFirestoreException.Code.PERMISSION_DENIED,
                                FirebaseFirestoreException.Code.UNAUTHENTICATED, FirebaseFirestoreException.Code.INVALID_ARGUMENT)) return Result.retry()
                            blocked = true; stopOrder = true; break
                        } catch (error: RuntimeException) {
                            android.util.Log.w("OrderSync", "Acción local no válida: ${error.javaClass.simpleName}")
                            blocked = true; stopOrder = true; break
                        }
                    }
                }
            }
            return if (blocked) Result.failure() else Result.success()
        } finally { database.close() }
    }

    private suspend fun upload(entity: OrderActionEntity, uid: String) {
        val record = Gson().fromJson(entity.content, OrderRecord::class.java)
        require(record.orderId == entity.orderId && record.riderId == uid)
        require(record.status in com.example.chaskirider.domain.orders.OrderStatus.entries)
        val ref = FirebaseFirestore.getInstance().collection("riders").document(uid)
            .collection("deliveryRecords").document(record.orderId)
        var remotelyCompleted = false
        withTimeout(20_000) {
            FirebaseFirestore.getInstance().runTransaction { tx ->
                val old = tx.get(ref)
                if (old.getString("status") == "COMPLETED_UNSUCCESSFUL") {
                    remotelyCompleted = true
                    return@runTransaction
                }
                val action = ref.collection("actions").document(entity.actionId)
                val registeredAction = tx.get(action)
                val incidents = record.problems.orEmpty().map { problem ->
                    val incidentRef = ref.collection("incidents").document(problem.id)
                    Triple(problem, incidentRef, tx.get(incidentRef).exists())
                }
                if ((old.getLong("revision") ?: 0) < entity.revision) {
                    val dataMap = mutableMapOf<String, Any?>(
                        "orderId" to record.orderId,
                        "riderId" to uid,
                        "status" to record.status.name,
                        "revision" to entity.revision,
                        "merchantName" to record.merchantName,
                        "pickup" to mapOf("latitude" to record.pickup.latitude, "longitude" to record.pickup.longitude),
                        "destination" to mapOf("latitude" to record.destination.latitude, "longitude" to record.destination.longitude),
                        "acceptedAt" to record.acceptedAt,
                        "arrivedAt" to record.arrivedAt,
                        "pickedUpAt" to record.pickedUpAt,
                        "deliveredAt" to record.deliveredAt,
                        "cancelledAt" to record.cancelledAt,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                    if (record.paidByCustomer) dataMap["paidByCustomer"] = true
                    record.earningsQuote?.let { quote ->
                        dataMap["earningsQuote"] = mapOf(
                            "approachMeters" to quote.approachMeters, "deliveryMeters" to quote.deliveryMeters,
                            "chaskiCents" to quote.chaskiCents, "tipCents" to quote.tipCents,
                            "totalCents" to quote.totalCents, "pricingVersion" to quote.pricingVersion,
                            "distanceSource" to quote.distanceSource)
                    }
                    if (record.evidencePhotoUrl != null) dataMap["evidencePhotoUrl"] = record.evidencePhotoUrl
                    if (record.deliveryNote != null) dataMap["deliveryNote"] = record.deliveryNote
                    tx.set(ref, dataMap, SetOptions.merge())
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
        if (FirebaseAuth.getInstance().currentUser?.uid != uid) throw kotlinx.coroutines.CancellationException("La sesión cambió")
        if (!remotelyCompleted && record.paidByCustomer && record.status !in listOf(com.example.chaskirider.domain.orders.OrderStatus.DELIVERED, com.example.chaskirider.domain.orders.OrderStatus.CANCELLED, com.example.chaskirider.domain.orders.OrderStatus.COMPLETED_UNSUCCESSFUL))
            withTimeout(20_000) { DeliveryPinRemote.prepare(record.orderId) }
    }

    companion object {
        fun schedule(context: Context, uid: String, force: Boolean = false) {
            val request = OneTimeWorkRequestBuilder<OrderSyncWorker>()
                .setInputData(workDataOf("uid" to uid))
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS).build()
            WorkManager.getInstance(context).enqueueUniqueWork("order-sync-$uid", if (force) ExistingWorkPolicy.REPLACE else ExistingWorkPolicy.APPEND_OR_REPLACE, request)
        }
    }
}
