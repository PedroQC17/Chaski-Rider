package com.example.chaskirider.ui.screens.home

import com.example.chaskirider.domain.model.GeoPoint
import com.example.chaskirider.domain.model.RiderUser

data class HomeUiState(
    val user: RiderUser? = null,
    val isUpdatingAvailability: Boolean = false,
    val isLocating: Boolean = false,
    val hasLocationPermission: Boolean = false,
    val location: GeoPoint? = null,
    val cameraRequest: Int = 0,
    val errorMessage: String? = null,
    val locationUnavailable: Boolean = false
)
