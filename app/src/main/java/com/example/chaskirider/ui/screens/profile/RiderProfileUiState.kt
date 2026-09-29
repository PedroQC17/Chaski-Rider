package com.example.chaskirider.ui.screens.profile

import com.example.chaskirider.domain.model.*

data class RiderProfileUiState(
    val currentUser: RiderUser? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val message: String? = null
)
