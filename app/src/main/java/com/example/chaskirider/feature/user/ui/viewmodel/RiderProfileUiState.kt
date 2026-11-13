package com.example.chaskirider.feature.user.ui.viewmodel

import com.example.chaskirider.feature.user.domain.RiderUser

data class RiderProfileUiState(
    val changeNoticeVisible: Boolean = false,
    val currentUser: RiderUser? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val message: String? = null
)
