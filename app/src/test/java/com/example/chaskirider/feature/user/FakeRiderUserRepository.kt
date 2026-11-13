package com.example.chaskirider.feature.user
import com.example.chaskirider.feature.user.domain.RiderUser
import com.example.chaskirider.feature.user.domain.RiderUserRepository
import kotlinx.coroutines.flow.MutableStateFlow

internal open class FakeRiderUserRepository : RiderUserRepository {
    override val user = MutableStateFlow<RiderUser?>(null)

    override suspend fun loginWithEmail(email: String, password: String): Result<RiderUser> = error("unused")
    override suspend fun loginWithGoogle(idToken: String): Result<RiderUser> = error("unused")
    override suspend fun sendPasswordResetEmail(email: String): Result<Unit> = error("unused")
    override suspend fun linkPassword(password: String): Result<Unit> = error("unused")
    override suspend fun logout() = error("unused")
    override suspend fun getCurrentUser(): Result<RiderUser?> = error("unused")
    override suspend fun refresh(): Result<RiderUser> = error("unused")

    override suspend fun savePersonalData(name: String, lastName: String, dni: String, phone: String, terms: Boolean): Result<RiderUser> =
        error("unused")
    override suspend fun submitRegistration(): Result<RiderUser> = error("unused")
    override suspend fun setAvailability(available: Boolean): Result<RiderUser> = error("unused")

    override suspend fun createProfilePhotoCapture(): Result<String> = error("unused")
    override suspend fun saveProfilePhoto(source: String): Result<RiderUser> = error("unused")
    override suspend fun loadProfilePhoto(user: RiderUser): Result<String> = error("unused")
}
