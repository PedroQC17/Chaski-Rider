package com.example.chaskirider.data.repository

import android.net.Uri
import com.example.chaskirider.data.documents.RiderDocumentDataSource
import com.example.chaskirider.domain.repository.DocumentRepository

class DocumentRepositoryImpl(private val documents: RiderDocumentDataSource) : DocumentRepository {
    override suspend fun createCapture() = documents.createCapture()
    override suspend fun hasCapture(uri: Uri) = documents.hasCapture(uri)
    override suspend fun deleteCapture(uri: Uri) = documents.deleteCapture(uri)
    override suspend fun uploadDocument(docType: String, uri: Uri) = documents.uploadDocument(docType, uri)
    override suspend fun getDocument(docType: String) = documents.getDocument(docType)
}
