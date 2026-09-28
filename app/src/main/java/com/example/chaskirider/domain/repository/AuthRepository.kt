
package com.example.chaskirider.domain.repository

import android.net.Uri
import com.example.chaskirider.domain.model.*

interface AuthRepository {
    suspend fun loginWithEmail(email: String, pass: String): Result<RiderUser>
    suspend fun loginWithGoogle(idToken: String): Result<RiderUser>
    suspend fun sendPasswordResetEmail(email: String): Result<Unit>
    suspend fun linkPassword(password: String): Result<Unit>
    suspend fun savePersonalData(name: String, lastName: String, dni: String, phone: String, terms: Boolean): Result<RiderUser>
    suspend fun saveVehicle(vehicle: VehicleType): Result<RiderUser>
    suspend fun saveBankInfo(bank: BankInfo): Result<RiderUser>
    suspend fun submitRegistration(): Result<RiderUser>
    suspend fun uploadDocument(docType: String, uri: Uri): Result<RiderUser>
    suspend fun getDocument(docType: String): Result<Uri>
    suspend fun logout()
    suspend fun getCurrentUser(): Result<RiderUser?>
    suspend fun setAvailability(available: Boolean): Result<RiderUser>
}
