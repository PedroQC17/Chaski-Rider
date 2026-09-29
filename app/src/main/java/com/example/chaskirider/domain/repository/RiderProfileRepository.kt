package com.example.chaskirider.domain.repository

import com.example.chaskirider.domain.model.*

interface RiderProfileRepository {
    suspend fun savePersonalData(name: String, lastName: String, dni: String, phone: String, terms: Boolean): Result<RiderUser>
    suspend fun saveVehicle(vehicle: VehicleType): Result<RiderUser>
    suspend fun saveBankInfo(bank: BankInfo): Result<RiderUser>
    suspend fun submitRegistration(): Result<RiderUser>
    suspend fun setAvailability(available: Boolean): Result<RiderUser>
}
