package com.example.chaskirider.feature.notifications.domain

import kotlinx.coroutines.flow.StateFlow

interface NotificationsRepository {
    val notifications: StateFlow<List<ChaskiNotification>>
    val unreadCount: StateFlow<Int>
    fun add(notification: ChaskiNotification)
    fun markSeen()
    fun clear()
}
