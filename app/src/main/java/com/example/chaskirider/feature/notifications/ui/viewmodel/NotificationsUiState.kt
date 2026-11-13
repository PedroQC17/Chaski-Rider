package com.example.chaskirider.feature.notifications.ui.viewmodel

import com.example.chaskirider.feature.notifications.domain.ChaskiNotification

data class NotificationsUiState(
    val notifications: List<ChaskiNotification> = emptyList(),
    val unreadCount: Int = 0
)
