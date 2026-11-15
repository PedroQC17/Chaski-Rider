package com.example.chaskirider.feature.rider.data

import android.content.Context
import android.net.Uri
import com.example.chaskirider.core.firebase.firebaseResult
import com.example.chaskirider.core.platform.FileProviderUris
import com.example.chaskirider.core.text.TextKey
import com.example.chaskirider.core.text.TextProvider
import com.example.chaskirider.feature.notifications.domain.NotificationsRepository
import com.example.chaskirider.feature.rider.data.local.RiderStore
import com.example.chaskirider.feature.rider.data.local.PortraitImageProcessor
import com.example.chaskirider.feature.rider.data.local.RiderSessionStore
import com.example.chaskirider.feature.rider.data.remote.RiderRemote
import com.example.chaskirider.feature.rider.data.remote.RiderRoutes
import com.example.chaskirider.feature.rider.domain.RiderStatus
import com.example.chaskirider.feature.rider.domain.RiderPolicy
import com.example.chaskirider.feature.rider.domain.RiderOnboardingService
import com.example.chaskirider.feature.rider.domain.Rider
import com.example.chaskirider.feature.rider.domain.RiderRepository
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

class RiderRepositoryImpl(
    context: Context,
    private val texts: TextProvider,
    private val remote: RiderRemote,
    private val session: RiderSessionStore,
    private val notifications: NotificationsRepository,
    private val riders: RiderStore,
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val storage: FirebaseStorage = FirebaseStorage.getInstance()
) : RiderRepository {
    private val appContext = context.applicationContext
    override val user = session.user

    override suspend fun loginWithGoogle(idToken: String) = firebaseResult(texts) {
        auth.signInWithCredential(GoogleAuthProvider.getCredential(idToken, null)).await()
        remote.load().also { riders.save(it) }
    }

    override suspend fun loginWithEmail(email: String, password: String) = firebaseResult(texts) {
        auth.signInWithEmailAndPassword(email.trim(), password).await()
        remote.load().also { riders.save(it) }
    }

    override suspend fun getCurrentUser(): Result<Rider?> = firebaseResult(texts) {
        if (auth.currentUser == null) null else remote.load().also { riders.save(it) }
    }

    override suspend fun refresh(): Result<Rider> = firebaseResult(texts) {
        remote.load().also { riders.save(it) }
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
        check(auth.currentUser?.uid == user.uid && session.user.value?.id == user.uid) {
            texts.get(TextKey.TEXT_LA_SESION_CAMBIO_VUELVE_A_INGRESAR)
        }
        session.update(session.user.value?.copy(hasPassword = true))
        Unit
    }

    override suspend fun logout() {
        auth.signOut()
        session.update(null)
        notifications.clear()
    }

    override suspend fun savePersonalData(name: String, lastName: String, dni: String, phone: String, terms: Boolean) =
        firebaseResult(texts) {
            remote.mutate("personal", mapOf("name" to name.trim(), "lastName" to lastName.trim(), "dni" to dni.trim(),
                "phone" to RiderPolicy.normalizePhone(phone), "termsAccepted" to terms)).also { riders.save(it) }
        }

    override suspend fun submitRiderOnboarding() = firebaseResult(texts) {
        remote.mutate("submit").also { riders.save(it) }
    }

    override suspend fun setAvailability(available: Boolean) = firebaseResult(texts) {
        val previous = session.user.value
        // Al desconectar, el estado local cambia de inmediato para detener búsqueda y aceptación
        // de ofertas mientras el backend confirma la operación.
        if (!available && previous != null) session.update(previous.copy(isAvailable = false))
        try {
            remote.mutate("availability", mapOf("isAvailable" to available)).also { riders.save(it) }
        } catch (error: Exception) {
            if (!available && previous != null && session.user.value?.id == previous.id) session.update(previous)
            throw error
        }
    }

    override suspend fun createProfilePhotoCapture() = firebaseResult(texts) {
        withContext(Dispatchers.IO) {
            val dir = File(appContext.cacheDir, "captures").apply { mkdirs() }
            FileProviderUris.uriFor(appContext, File.createTempFile("portrait-", ".jpg", dir)).toString()
        }
    }

    override suspend fun saveProfilePhoto(source: String) = firebaseResult(texts) {
        val uri = Uri.parse(source)
        val uid = requireNotNull(auth.currentUser?.uid)
        require(FileProviderUris.belongsTo(appContext, uri))
        val current = remote.load()
        require(current.id == uid && current.status == RiderStatus.APPROVED && current.isEnabled)
        if (current.profilePhotoPath.isNotBlank()) return@firebaseResult current.also { riders.save(it) }
        val bytes = withContext(Dispatchers.IO) { PortraitImageProcessor(appContext).jpeg(uri) }
        val path = RiderRoutes.profilePhoto(uid, UUID.randomUUID().toString())
        storage.reference.child(path).putBytes(bytes, StorageMetadata.Builder().setContentType("image/jpeg").build()).await()
        check(auth.currentUser?.uid == uid)
        remote.mutate("profilePhoto", mapOf("path" to path), expectedUid = uid).also { riders.save(it) }
    }

    override suspend fun loadProfilePhoto(user: Rider) = firebaseResult(texts) {
        require(auth.currentUser?.uid == user.id)
        require(user.profilePhotoPath.startsWith(RiderRoutes.profilePhotoPrefix(user.id)))
        val dir = File(appContext.cacheDir, "documents").apply { mkdirs() }
        val file = File(dir, "portrait-${user.profilePhotoPath.substringAfterLast('/')}.jpg")
        if (!file.exists() || file.length() == 0L) {
            val bytes = storage.reference.child(user.profilePhotoPath).getBytes(5L * 1024 * 1024).await()
            withContext(Dispatchers.IO) { file.writeBytes(bytes) }
        }
        check(auth.currentUser?.uid == user.id)
        FileProviderUris.uriFor(appContext, file).toString()
    }
}
