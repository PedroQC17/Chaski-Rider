package com.example.chaskirider.data.repository

import com.example.chaskirider.domain.model.*
import com.example.chaskirider.domain.repository.RiderProfileRepository
import com.example.chaskirider.data.remote.RiderProfileRemoteDataSource
import com.example.chaskirider.data.remote.firebaseResult

class RiderProfileRepositoryImpl(
    private val profiles: RiderProfileRemoteDataSource
) : RiderProfileRepository {
    override suspend fun savePersonalData(name: String, lastName: String, dni: String, phone: String, terms: Boolean) = firebaseResult {
        profiles.mutate("personal", mapOf("name" to name.trim(), "lastName" to lastName.trim(), "dni" to dni.trim(),
            "phone" to RegistrationValidation.normalizePhone(phone), "termsAccepted" to terms))
    }
    override suspend fun saveVehicle(vehicle: VehicleType) = firebaseResult { profiles.mutate("vehicle", mapOf("vehicleType" to vehicle.name)) }
    override suspend fun saveBankInfo(bank: BankInfo) = firebaseResult {
        profiles.mutate("bank", mapOf("bankInfo" to mapOf("bankName" to bank.bankName.trim(),
            "holderName" to bank.holderName.trim(), "accountNumber" to bank.accountNumber.trim(), "cci" to bank.cci.trim())))
    }
    override suspend fun submitRegistration() = firebaseResult { profiles.mutate("submit") }

    override suspend fun setAvailability(available: Boolean) = firebaseResult {
        profiles.mutate("availability", mapOf("isAvailable" to available))
    }
}
