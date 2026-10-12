package com.example.chaskirider.domain.repository

import android.net.Uri
import com.example.chaskirider.domain.model.RiderUser

interface DocumentRepository {
    suspend fun createCapture(): Result<Uri>
    suspend fun hasCapture(uri: Uri): Result<Boolean>
    suspend fun deleteCapture(uri: Uri): Result<Unit>
    suspend fun uploadDocument(docType: String, uri: Uri): Result<RiderUser>
    suspend fun getDocument(docType: String): Result<Uri>
}
