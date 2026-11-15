package com.example.chaskirider.feature.chat.data.remote

import com.example.chaskirider.core.firebase.FirebaseRoot

object ChatRoutes {
    fun order(uid: String, orderId: String) = "${FirebaseRoot.riderDoc(uid)}/deliveryRecords/$orderId"
    fun messages(uid: String, orderId: String, channel: String) =
        "${order(uid, orderId)}/chats/$channel/messages"
}
