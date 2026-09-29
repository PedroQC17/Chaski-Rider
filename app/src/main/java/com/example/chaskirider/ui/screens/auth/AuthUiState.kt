package com.example.chaskirider.ui.screens.auth

import com.example.chaskirider.domain.model.RiderUser

data class AuthUiState(
    val currentUser: RiderUser? = null,
    val isLoading: Boolean = false,
    val initialized: Boolean = false,
    val errorMessage: String? = null,
    val message: String? = null
)
