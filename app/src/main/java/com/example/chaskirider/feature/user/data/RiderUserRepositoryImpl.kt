package com.example.chaskirider.feature.user.data

import android.content.Context
import android.net.Uri
import com.example.chaskirider.core.firebase.firebaseResult
import com.example.chaskirider.core.platform.FileProviderUris
import com.example.chaskirider.core.text.TextKey
import com.example.chaskirider.core.text.TextProvider
import com.example.chaskirider.feature.notifications.domain.NotificationsRepository
import com.example.chaskirider.feature.user.data.local.PortraitImageProcessor
import com.example.chaskirider.feature.user.data.local.RiderUserSessionStore
import com.example.chaskirider.feature.user.data.remote.RiderUserRemote
import com.example.chaskirider.feature.user.domain.RegistrationStatus
import com.example.chaskirider.feature.user.domain.RiderUserPolicy
import com.example.chaskirider.feature.user.domain.RiderUserService
import com.example.chaskirider.feature.user.domain.RiderUser
import com.example.chaskirider.feature.user.domain.RiderUserRepository
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

class RiderUserRepositoryImpl(
    context: Context,
    private val texts: TextProvider,
    private val remote: RiderUserRemote,
    private val session: RiderUserSessionStore,
    private val notifications: NotificationsRepository,
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val storage: FirebaseStorage = FirebaseStorage.getInstance()
) : RiderUserRepository {
    private val appContext = context.applicationContext
    override val user = session.user

    override suspend fun loginWithGoogle(idToken: String) = firebaseResult(texts) {
        auth.signInWithCredential(GoogleAuthProvider.getCredential(idToken, null)).await()
        remote.load()
    }

    override suspend fun loginWithEmail(email: String, password: String) = firebaseResult(texts) {
        auth.signInWithEmailAndPassword(email.trim(), password).await()
        remote.load()
    }

    override suspend fun getCurrentUser(): Result<RiderUser?> = firebaseResult(texts) {
        if (auth.currentUser == null) null else remote.load()
    }

    override suspend fun refresh(): Result<RiderUser> = firebaseResult(texts) { remote.load() }

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
                "phone" to RiderUserPolicy.normalizePhone(phone), "termsAccepted" to terms))
        }

    override suspend fun submitRegistration() = firebaseResult(texts) { remote.mutate("submit") }

    override suspend fun setAvailability(available: Boolean) = firebaseResult(texts) {
        remote.mutate("availability", mapOf("isAvailable" to available))
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
        require(current.id == uid && current.status == RegistrationStatus.APPROVED && current.isEnabled)
        if (current.profilePhotoPath.isNotBlank()) return@firebaseResult current
        val bytes = withContext(Dispatchers.IO) { PortraitImageProcessor(appContext).jpeg(uri) }
        val path = "riders/$uid/profilePhoto/${UUID.randomUUID()}"
        storage.reference.child(path).putBytes(bytes, StorageMetadata.Builder().setContentType("image/jpeg").build()).await()
        check(auth.currentUser?.uid == uid)
        remote.mutate("profilePhoto", mapOf("path" to path), expectedUid = uid)
    }

    override suspend fun loadProfilePhoto(user: RiderUser) = firebaseResult(texts) {
        require(auth.currentUser?.uid == user.id)
        require(user.profilePhotoPath.startsWith("riders/${user.id}/profilePhoto/"))
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
