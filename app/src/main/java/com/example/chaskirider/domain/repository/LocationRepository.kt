package com.example.chaskirider.domain.repository

import android.location.Location

interface LocationRepository {
    suspend fun getCurrentLocation(): Location?
    fun hasLocationPermission(): Boolean
}
