package com.example.chaskirider.core.firebase

object FirebaseRoot {
    const val RIDERS = "riders"
    const val REGION = "us-central1"

    fun riderDoc(uid: String) = "$RIDERS/$uid"

    object Callables {
        const val RIDER_REGISTRATION = "riderRiderOnboarding"
        const val DELIVERY_PIN = "deliveryPin"
        const val CUSTOMER_ABSENCE = "customerAbsence"
        const val RIDER_ASSISTANT = "riderAssistant"
        const val MERCHANT_CHAT = "merchantChat"
    }
}
