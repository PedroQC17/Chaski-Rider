package com.example.chaskirider.feature.rider
import com.example.chaskirider.feature.rider.domain.Rider
import com.example.chaskirider.feature.rider.domain.RiderRepository
import kotlinx.coroutines.flow.MutableStateFlow

internal open class FakeRiderRepository : RiderRepository {
    override val user = MutableStateFlow<Rider?>(null)

    override suspend fun loginWithEmail(email: String, password: String): Result<Rider> = error("unused")
    override suspend fun loginWithGoogle(idToken: String): Result<Rider> = error("unused")
    override suspend fun sendPasswordResetEmail(email: String): Result<Unit> = error("unused")
    override suspend fun linkPassword(password: String): Result<Unit> = error("unused")
    override suspend fun logout() = error("unused")
    override suspend fun getCurrentUser(): Result<Rider?> = error("unused")
    override suspend fun refresh(): Result<Rider> = error("unused")

    override suspend fun savePersonalData(name: String, lastName: String, dni: String, phone: String, terms: Boolean): Result<Rider> =
        error("unused")
    override suspend fun submitRiderOnboarding(): Result<Rider> = error("unused")
    override suspend fun setAvailability(available: Boolean): Result<Rider> = error("unused")

    override suspend fun createProfilePhotoCapture(): Result<String> = error("unused")
    override suspend fun saveProfilePhoto(source: String): Result<Rider> = error("unused")
    override suspend fun loadProfilePhoto(user: Rider): Result<String> = error("unused")
}
