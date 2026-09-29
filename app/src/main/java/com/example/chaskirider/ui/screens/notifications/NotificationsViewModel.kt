package com.example.chaskirider.ui.screens.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chaskirider.domain.repository.NotificationsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

class NotificationsViewModel(private val repository: NotificationsRepository) : ViewModel() {
    val uiState = combine(repository.notifications, repository.unreadCount) { items, unread ->
        NotificationsUiState(items, unread)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), NotificationsUiState())
    fun markSeen() = repository.markSeen()
}
