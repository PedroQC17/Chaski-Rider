
package com.example.chaskirider.data.notifications

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class ChaskiNotification(
    val title: String,
    val body: String,
    val timestamp: Long = System.currentTimeMillis()
)

object NotificationsStore {
    private const val MAX_NOTIFICATIONS = 50
    private val _notifications = MutableStateFlow<List<ChaskiNotification>>(emptyList())
    val notifications: StateFlow<List<ChaskiNotification>> = _notifications.asStateFlow()

    private val _unreadCount = MutableStateFlow(0)
    val unreadCount: StateFlow<Int> = _unreadCount.asStateFlow()

    fun add(notification: ChaskiNotification) {
        _notifications.update { (listOf(notification) + it).take(MAX_NOTIFICATIONS) }
        _unreadCount.update { it + 1 }
    }

    fun markSeen() {
        _unreadCount.value = 0
    }

    fun clear() {
        _notifications.value = emptyList()
        _unreadCount.value = 0
    }
}
