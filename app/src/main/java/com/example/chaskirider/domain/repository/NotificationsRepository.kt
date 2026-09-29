package com.example.chaskirider.domain.repository

import com.example.chaskirider.domain.model.ChaskiNotification
import kotlinx.coroutines.flow.StateFlow

interface NotificationsRepository {
    val notifications: StateFlow<List<ChaskiNotification>>
    val unreadCount: StateFlow<Int>
    fun markSeen()
}
