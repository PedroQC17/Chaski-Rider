package com.example.chaskirider.feature.notifications.data.remote

import com.example.chaskirider.platform.ChaskiFirebaseMessagingService
import android.content.Context
import com.google.firebase.messaging.FirebaseMessaging

class NotificationsInitializer {
    fun initialize(context: Context) {
        ChaskiFirebaseMessagingService.ensureChannel(context)
        FirebaseMessaging.getInstance().subscribeToTopic(ChaskiFirebaseMessagingService.TOPIC_RIDERS)
    }
}

class RiderTopicSubscription(private val messaging: FirebaseMessaging = FirebaseMessaging.getInstance()) {
    private var currentUid: String? = null
    fun bind(uid: String?) {
        if (currentUid == uid) return
        currentUid?.let { messaging.unsubscribeFromTopic("rider_$it") }
        currentUid = uid
        uid?.let { messaging.subscribeToTopic("rider_$it") }
    }
}
