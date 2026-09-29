package com.example.chaskirider.data.notifications

import android.content.Context
import com.google.firebase.messaging.FirebaseMessaging

class NotificationsInitializer {
    fun initialize(context: Context) {
        ChaskiFirebaseMessagingService.ensureChannel(context)
        FirebaseMessaging.getInstance().subscribeToTopic(ChaskiFirebaseMessagingService.TOPIC_RIDERS)
    }
}
