package com.example.chaskirider.feature.rider.domain

interface VehicleRepository {
    suspend fun load(): Result<VehicleType>
    suspend fun save(vehicle: VehicleType): Result<VehicleType>
}
