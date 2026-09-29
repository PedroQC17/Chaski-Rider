package com.example.chaskirider.ui.screens.notifications

import com.example.chaskirider.domain.model.ChaskiNotification

data class NotificationsUiState(
    val notifications: List<ChaskiNotification> = emptyList(),
    val unreadCount: Int = 0
)
