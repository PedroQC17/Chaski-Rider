package com.example.chaskirider.feature.rider.ui.viewmodel

import com.example.chaskirider.feature.rider.domain.Rider

data class RiderProfileUiState(
    val changeNoticeVisible: Boolean = false,
    val currentUser: Rider? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val message: String? = null
)
