package com.example.chaskirider.feature.documents.domain

import android.net.Uri
import com.example.chaskirider.feature.documents.domain.DocumentPage

interface DocumentRepository {
    suspend fun load(): Result<DocumentSnapshot>
    suspend fun createCapture(): Result<Uri>
    suspend fun hasCapture(uri: Uri): Result<Boolean>
    suspend fun deleteCapture(uri: Uri): Result<Unit>
    suspend fun uploadDocument(docType: String, uri: Uri): Result<Unit>
    suspend fun getDocument(docType: String): Result<Uri>
}

interface DocumentPreviewRepository {
    suspend fun render(uri: Uri, page: Int): Result<DocumentPage>
}
