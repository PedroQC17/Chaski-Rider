package com.example.chaskirider.data.repository

import com.example.chaskirider.domain.model.*
import com.example.chaskirider.domain.repository.AuthRepository
import com.google.firebase.auth.*
import kotlinx.coroutines.tasks.await

import com.example.chaskirider.data.remote.RiderProfileRemoteDataSource
import com.example.chaskirider.data.remote.firebaseResult
import com.example.chaskirider.data.session.RiderSessionStore
import com.example.chaskirider.data.notifications.NotificationsStore

class AuthRepositoryImpl(
    private val profiles: RiderProfileRemoteDataSource,
    private val session: RiderSessionStore,
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) : AuthRepository {
    override suspend fun loginWithGoogle(idToken: String) = firebaseResult {
        auth.signInWithCredential(GoogleAuthProvider.getCredential(idToken, null)).await()
        profiles.load()
    }
    override suspend fun loginWithEmail(email: String, pass: String) = firebaseResult {
        auth.signInWithEmailAndPassword(email.trim(), pass).await()
        profiles.load()
    }
    override suspend fun getCurrentUser(): Result<RiderUser?> = firebaseResult {
        if (auth.currentUser == null) null else profiles.load()
    }
    override suspend fun sendPasswordResetEmail(email: String) = firebaseResult {
        auth.sendPasswordResetEmail(email.trim()).await()
        Unit
    }
    override suspend fun linkPassword(password: String) = firebaseResult {
        val user = auth.currentUser ?: error("No hay una sesión activa")
        require(user.providerData.none { it.providerId == EmailAuthProvider.PROVIDER_ID }) {
            "Ya tienes una contraseña configurada. Usa la recuperación para cambiarla."
        }
        user.linkWithCredential(EmailAuthProvider.getCredential(
            user.email ?: error("La cuenta no tiene correo"), password)).await()
        Unit
    }
    override suspend fun logout() {
        auth.signOut()
        session.update(null)
        NotificationsStore.clear()
    }
}
