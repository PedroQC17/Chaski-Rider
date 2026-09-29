package com.example.chaskirider.data.notifications

import com.google.firebase.messaging.FirebaseMessaging

class RiderTopicSubscription(private val messaging: FirebaseMessaging = FirebaseMessaging.getInstance()) {
    private var currentUid: String? = null
    fun bind(uid: String?) {
        if (currentUid == uid) return
        currentUid?.let { messaging.unsubscribeFromTopic("rider_$it") }
        currentUid = uid
        uid?.let { messaging.subscribeToTopic("rider_$it") }
    }
}
