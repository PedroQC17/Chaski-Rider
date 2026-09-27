package com.example.chaskirider.data.repository

import com.example.chaskirider.data.remote.ChaskiApiService
import com.example.chaskirider.domain.model.RiderUser
import com.example.chaskirider.domain.model.VehicleType
import com.example.chaskirider.domain.repository.AuthRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class AuthRepositoryImpl(
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val apiService: ChaskiApiService? = null
) : AuthRepository {

    override suspend fun login(email: String, pass: String): Result<RiderUser> {
        return try {
            val authResult = firebaseAuth.signInWithEmailAndPassword(email, pass).await()
            val userId = authResult.user?.uid ?: throw Exception("Usuario no encontrado")
            
            val doc = firestore.collection("riders").document(userId).get().await()
            val rider = doc.toObject(RiderUser::class.java) ?: RiderUser(id = userId, email = email)
            Result.success(rider)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun register(
        name: String,
        email: String,
        phone: String,
        pass: String
    ): Result<RiderUser> {
        return try {
            val authResult = firebaseAuth.createUserWithEmailAndPassword(email, pass).await()
            val userId = authResult.user?.uid ?: throw Exception("Error al crear usuario")
            
            val rider = RiderUser(
                id = userId,
                name = name,
                email = email,
                phone = phone
            )
            firestore.collection("riders").document(userId).set(rider).await()
            Result.success(rider)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun logout() {
        firebaseAuth.signOut()
    }

    override suspend fun getCurrentUser(): RiderUser? {
        val currentUser = firebaseAuth.currentUser ?: return null
        return try {
            val doc = firestore.collection("riders").document(currentUser.uid).get().await()
            doc.toObject(RiderUser::class.java) ?: RiderUser(id = currentUser.uid, email = currentUser.email ?: "")
        } catch (e: Exception) {
            RiderUser(id = currentUser.uid, email = currentUser.email ?: "")
        }
    }

    override suspend fun updateVehicle(vehicleType: VehicleType): Result<Unit> {
        val uid = firebaseAuth.currentUser?.uid ?: return Result.failure(Exception("Sin sesión activa"))
        return try {
            firestore.collection("riders").document(uid).update("vehicleType", vehicleType.name).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateAvailability(isAvailable: Boolean): Result<Unit> {
        val uid = firebaseAuth.currentUser?.uid ?: return Result.failure(Exception("Sin sesión activa"))
        return try {
            firestore.collection("riders").document(uid).update("isAvailable", isAvailable).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
