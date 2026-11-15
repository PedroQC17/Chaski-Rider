package com.example.chaskirider.feature.rider.ui.viewmodel

import com.example.chaskirider.feature.rider.domain.Rider

data class AuthUiState(
    val currentUser: Rider? = null,
    val isLoading: Boolean = false,
    val initialized: Boolean = false,
    val errorMessage: String? = null,
    val message: String? = null
)
