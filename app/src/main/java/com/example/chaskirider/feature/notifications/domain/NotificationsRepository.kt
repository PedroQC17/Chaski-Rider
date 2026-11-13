package com.example.chaskirider.feature.notifications.domain

import com.example.chaskirider.feature.notifications.domain.ChaskiNotification
import kotlinx.coroutines.flow.StateFlow

interface NotificationsRepository {
    val notifications: StateFlow<List<ChaskiNotification>>
    val unreadCount: StateFlow<Int>
    fun markSeen()
    fun clear()
}
