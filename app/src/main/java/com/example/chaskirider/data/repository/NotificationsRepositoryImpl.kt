package com.example.chaskirider.data.repository

import com.example.chaskirider.data.notifications.NotificationsStore
import com.example.chaskirider.domain.repository.NotificationsRepository

class NotificationsRepositoryImpl : NotificationsRepository {
    override val notifications = NotificationsStore.notifications
    override val unreadCount = NotificationsStore.unreadCount
    override fun markSeen() = NotificationsStore.markSeen()
}
