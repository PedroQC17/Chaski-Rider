package com.example.chaskirider.data.repository

import com.example.chaskirider.domain.text.TextProvider
import com.example.chaskirider.domain.text.TextKey
import com.example.chaskirider.domain.model.*
import com.example.chaskirider.domain.repository.AuthRepository
import com.google.firebase.auth.*
import kotlinx.coroutines.tasks.await

import com.example.chaskirider.data.remote.RiderProfileRemoteDataSource
import com.example.chaskirider.data.remote.firebaseResult
import com.example.chaskirider.data.session.RiderSessionStore
import com.example.chaskirider.data.notifications.NotificationsStore

class AuthRepositoryImpl(
    private val texts: TextProvider,
    private val profiles: RiderProfileRemoteDataSource,
    private val session: RiderSessionStore,
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) : AuthRepository {
    override suspend fun loginWithGoogle(idToken: String) = firebaseResult(texts) {
        auth.signInWithCredential(GoogleAuthProvider.getCredential(idToken, null)).await()
        profiles.load()
    }
    override suspend fun loginWithEmail(email: String, pass: String) = firebaseResult(texts) {
        auth.signInWithEmailAndPassword(email.trim(), pass).await()
        profiles.load()
    }
    override suspend fun getCurrentUser(): Result<RiderUser?> = firebaseResult(texts) {
        if (auth.currentUser == null) null else profiles.load()
    }
    override suspend fun sendPasswordResetEmail(email: String) = firebaseResult(texts) {
        auth.sendPasswordResetEmail(email.trim()).await()
        Unit
    }
    override suspend fun linkPassword(password: String) = firebaseResult(texts) {
        val user = auth.currentUser ?: error(texts.get(TextKey.TEXT_NO_HAY_UNA_SESION_ACTIVA))
        if (user.providerData.any { it.providerId == EmailAuthProvider.PROVIDER_ID }) {
            user.updatePassword(password).await()
        } else {
            user.linkWithCredential(EmailAuthProvider.getCredential(
                user.email ?: error(texts.get(TextKey.TEXT_LA_CUENTA_NO_TIENE_CORREO)), password)).await()
        }
        session.update(session.user.value?.copy(hasPassword = true))
        Unit
    }
    override suspend fun logout() {
        auth.signOut()
        session.update(null)
        NotificationsStore.clear()
    }
}
