package com.example.chaskirider.feature.user.ui.viewmodel

import com.example.chaskirider.feature.user.domain.RiderUser

data class AuthUiState(
    val currentUser: RiderUser? = null,
    val isLoading: Boolean = false,
    val initialized: Boolean = false,
    val errorMessage: String? = null,
    val message: String? = null
)
