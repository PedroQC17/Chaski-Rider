package com.example.chaskirider.data.profile

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.chaskirider.data.remote.RiderProfileRemoteDataSource
import com.example.chaskirider.data.remote.firebaseResult
import com.example.chaskirider.domain.model.*
import com.example.chaskirider.domain.repository.ProfilePhotoRepository
import com.example.chaskirider.domain.text.TextProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

class ProfilePhotoRepositoryImpl(private val context: Context, private val texts: TextProvider,
    private val profiles: RiderProfileRemoteDataSource,
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val storage: FirebaseStorage = FirebaseStorage.getInstance()) : ProfilePhotoRepository {
    override suspend fun createCapture() = firebaseResult(texts) {
        withContext(Dispatchers.IO) {
            val dir = File(context.cacheDir, "captures").apply { mkdirs() }
            FileProvider.getUriForFile(context, "${context.packageName}.files", File.createTempFile("portrait-", ".jpg", dir))
        }
    }
    override suspend fun save(uri: Uri) = firebaseResult(texts) {
        val uid = requireNotNull(auth.currentUser?.uid)
        require(uri.scheme == "content" && uri.authority == "${context.packageName}.files")
        val user = profiles.load()
        require(user.id == uid && user.status == RegistrationStatus.APPROVED && user.isEnabled)
        if (user.profilePhotoPath.isNotBlank()) return@firebaseResult user
        val bytes = withContext(Dispatchers.IO) { PortraitImageProcessor(context).jpeg(uri) }
        val path = "riders/$uid/profilePhoto/${UUID.randomUUID()}"
        storage.reference.child(path).putBytes(bytes, StorageMetadata.Builder().setContentType("image/jpeg").build()).await()
        check(auth.currentUser?.uid == uid)
        profiles.mutate("profilePhoto", mapOf("path" to path))
    }
    override suspend fun load(user: RiderUser) = firebaseResult(texts) {
        require(auth.currentUser?.uid == user.id)
        require(user.profilePhotoPath.startsWith("riders/${user.id}/profilePhoto/"))
        val dir = File(context.cacheDir, "documents").apply { mkdirs() }
        val file = File(dir, "portrait-${user.profilePhotoPath.substringAfterLast('/')}.jpg")
        if (!file.exists() || file.length() == 0L) {
            val bytes = storage.reference.child(user.profilePhotoPath).getBytes(5L * 1024 * 1024).await()
            withContext(Dispatchers.IO) { file.writeBytes(bytes) }
        }
        check(auth.currentUser?.uid == user.id)
        FileProvider.getUriForFile(context, "${context.packageName}.files", file)
    }
}
