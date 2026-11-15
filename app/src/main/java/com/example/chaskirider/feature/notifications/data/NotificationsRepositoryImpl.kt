package com.example.chaskirider.feature.notifications.data

import com.example.chaskirider.feature.notifications.data.local.NotificationDao
import com.example.chaskirider.feature.notifications.data.local.NotificationEntity
import com.example.chaskirider.feature.notifications.domain.ChaskiNotification
import com.example.chaskirider.feature.notifications.domain.NotificationsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

class NotificationsRepositoryImpl(private val dao: NotificationDao? = null) : NotificationsRepository {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutable = MutableStateFlow<List<ChaskiNotification>>(emptyList())
    override val notifications = mutable.asStateFlow()
    private val unread = MutableStateFlow(0)
    override val unreadCount = unread.asStateFlow()

    init {
        dao?.let { source ->
            scope.launch {
                source.observe().collect { rows ->
                    mutable.value = rows.map { ChaskiNotification(it.title, it.body, it.createdAt) }
                    unread.value = rows.count { !it.read }
                }
            }
        }
    }

    override fun add(notification: ChaskiNotification) {
        dao?.let { source ->
            scope.launch {
                source.insert(NotificationEntity(
                    id = UUID.randomUUID().toString(),
                    riderId = null,
                    title = notification.title,
                    body = notification.body,
                    createdAt = notification.timestamp,
                    read = false
                ))
            }
        }
    }

    override fun markSeen() {
        dao?.let { source -> scope.launch { source.markAllRead() } }
    }

    override fun clear() {
        dao?.let { source -> scope.launch { source.clear() } }
    }
}
