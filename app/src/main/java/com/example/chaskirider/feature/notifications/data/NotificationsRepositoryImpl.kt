package com.example.chaskirider.feature.notifications.data
import com.example.chaskirider.feature.notifications.data.local.NotificationsLocal
import com.example.chaskirider.feature.notifications.domain.NotificationsRepository

class NotificationsRepositoryImpl : NotificationsRepository {
    override val notifications = NotificationsLocal.notifications
    override val unreadCount = NotificationsLocal.unreadCount
    override fun markSeen() = NotificationsLocal.markSeen()
    override fun clear() = NotificationsLocal.clear()
}
