package com.example.chaskirider.domain.repository

import com.example.chaskirider.domain.model.RiderUser
import com.example.chaskirider.domain.model.VehicleType

interface AuthRepository {
    suspend fun login(email: String, pass: String): Result<RiderUser>
    suspend fun register(name: String, email: String, phone: String, pass: String): Result<RiderUser>
    suspend fun logout()
    suspend fun getCurrentUser(): RiderUser?
    suspend fun updateVehicle(vehicleType: VehicleType): Result<Unit>
    suspend fun updateAvailability(isAvailable: Boolean): Result<Unit>
}
