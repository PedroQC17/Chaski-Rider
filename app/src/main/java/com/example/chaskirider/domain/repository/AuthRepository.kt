package com.example.chaskirider.domain.repository

import com.example.chaskirider.domain.model.RiderUser

interface AuthRepository {
    suspend fun loginWithEmail(email: String, pass: String): Result<RiderUser>
    suspend fun loginWithGoogle(idToken: String): Result<RiderUser>
    suspend fun sendPasswordResetEmail(email: String): Result<Unit>
    suspend fun linkPassword(password: String): Result<Unit>
    suspend fun logout()
    suspend fun getCurrentUser(): Result<RiderUser?>
}
