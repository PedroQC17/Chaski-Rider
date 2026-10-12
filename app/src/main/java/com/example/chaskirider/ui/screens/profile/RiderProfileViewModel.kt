package com.example.chaskirider.ui.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.example.chaskirider.domain.model.*
import com.example.chaskirider.domain.repository.RiderSession

class RiderProfileViewModel(session: RiderSession) : ViewModel() {
    private val _uiState = MutableStateFlow(RiderProfileUiState())
    val uiState = _uiState.asStateFlow()
    init { viewModelScope.launch { session.user.collect { acceptUser(it) } } }
    private fun acceptUser(user: RiderUser?) {
        if (user == null) _uiState.value = RiderProfileUiState()
        else _uiState.update { it.copy(currentUser = user) }
    }
    fun showChangeNotice() { _uiState.update { it.copy(changeNoticeVisible = true) } }
    fun dismissChangeNotice() { _uiState.update { it.copy(changeNoticeVisible = false) } }
    fun clearError() { _uiState.update { it.copy(errorMessage = null, message = null) } }

}
