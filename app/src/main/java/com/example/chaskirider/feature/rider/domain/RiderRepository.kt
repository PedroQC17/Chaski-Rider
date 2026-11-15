package com.example.chaskirider.feature.rider.domain

import kotlinx.coroutines.flow.StateFlow

interface RiderSession {
    val user: StateFlow<Rider?>
}

interface RiderRepository : RiderSession {
    suspend fun loginWithEmail(email: String, password: String): Result<Rider>
    suspend fun loginWithGoogle(idToken: String): Result<Rider>
    suspend fun sendPasswordResetEmail(email: String): Result<Unit>
    suspend fun linkPassword(password: String): Result<Unit>
    suspend fun logout()
    suspend fun getCurrentUser(): Result<Rider?>
    suspend fun refresh(): Result<Rider>

    suspend fun savePersonalData(name: String, lastName: String, dni: String, phone: String, terms: Boolean): Result<Rider>
    suspend fun submitRiderOnboarding(): Result<Rider>
    suspend fun setAvailability(available: Boolean): Result<Rider>

    suspend fun createProfilePhotoCapture(): Result<String>
    suspend fun saveProfilePhoto(source: String): Result<Rider>
    suspend fun loadProfilePhoto(user: Rider): Result<String>
}
