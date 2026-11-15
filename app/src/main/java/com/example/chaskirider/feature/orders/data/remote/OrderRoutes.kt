package com.example.chaskirider.feature.orders.data.remote

import com.example.chaskirider.core.firebase.FirebaseRoot

object OrderRoutes {
    fun records(uid: String) = "${FirebaseRoot.riderDoc(uid)}/deliveryRecords"
    fun order(uid: String, orderId: String) = "${records(uid)}/$orderId"
    fun incidents(uid: String, orderId: String) = "${order(uid, orderId)}/incidents"
    fun actions(uid: String, orderId: String) = "${order(uid, orderId)}/actions"
    fun tracking(uid: String, orderId: String) = "${order(uid, orderId)}/tracking/current"
}
