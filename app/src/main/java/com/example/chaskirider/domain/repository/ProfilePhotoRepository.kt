package com.example.chaskirider.domain.repository
import android.net.Uri
import com.example.chaskirider.domain.model.RiderUser
interface ProfilePhotoRepository {
    suspend fun createCapture(): Result<Uri>
    suspend fun save(uri: Uri): Result<RiderUser>
    suspend fun load(user: RiderUser): Result<Uri>
}
