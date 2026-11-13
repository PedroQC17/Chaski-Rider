package com.example.chaskirider.feature.vehicle.domain

interface VehicleRepository {
    suspend fun load(): Result<VehicleType>
    suspend fun save(vehicle: VehicleType): Result<VehicleType>
}
