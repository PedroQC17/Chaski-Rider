package com.example.chaskirider.feature.user.domain

import kotlinx.coroutines.flow.StateFlow

interface RiderSession {
    val user: StateFlow<RiderUser?>
}

interface RiderUserRepository : RiderSession {
    suspend fun loginWithEmail(email: String, password: String): Result<RiderUser>
    suspend fun loginWithGoogle(idToken: String): Result<RiderUser>
    suspend fun sendPasswordResetEmail(email: String): Result<Unit>
    suspend fun linkPassword(password: String): Result<Unit>
    suspend fun logout()
    suspend fun getCurrentUser(): Result<RiderUser?>
    suspend fun refresh(): Result<RiderUser>

    suspend fun savePersonalData(name: String, lastName: String, dni: String, phone: String, terms: Boolean): Result<RiderUser>
    suspend fun submitRegistration(): Result<RiderUser>
    suspend fun setAvailability(available: Boolean): Result<RiderUser>

    suspend fun createProfilePhotoCapture(): Result<String>
    suspend fun saveProfilePhoto(source: String): Result<RiderUser>
    suspend fun loadProfilePhoto(user: RiderUser): Result<String>
}
