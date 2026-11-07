package com.example.chaskirider.data.orders

import com.example.chaskirider.domain.orders.OfferRequestException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException
import kotlinx.coroutines.tasks.await

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
