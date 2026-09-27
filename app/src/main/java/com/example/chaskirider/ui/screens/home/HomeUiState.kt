package com.example.chaskirider.ui.screens.home

import android.location.Location
import com.example.chaskirider.domain.model.RiderUser

data class HomeUiState(
    val user: RiderUser? = null,
    val isAvailable: Boolean = false,
    val hasLocationPermission: Boolean = false,
    val currentLocation: Location? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)
