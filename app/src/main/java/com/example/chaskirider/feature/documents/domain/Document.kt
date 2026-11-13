package com.example.chaskirider.feature.documents.domain

import android.net.Uri

data class DocumentFile(
    val id: String = "",
    val name: String = "",
    val url: String = "",
    val uploadState: DocumentUploadState = DocumentUploadState.NOT_SELECTED,
    val errorMessage: String? = null
)

enum class DocumentUploadState {
    NOT_SELECTED,
    SELECTED,
    UPLOADING,
    UPLOADED,
    ERROR
}

data class DocumentPage(val imageUri: Uri, val index: Int, val count: Int)

data class DocumentSnapshot(
    val paths: Map<String, String> = emptyMap(),
    val reviews: Map<String, DocumentReview> = emptyMap()
) {
    fun path(type: String): String = paths[type].orEmpty()
    fun exists(type: String): Boolean = path(type).isNotBlank()
    fun isObserved(type: String): Boolean = reviews[type]?.status == DocumentReviewStatus.OBSERVED
}

enum class DocumentReviewStatus { PENDING, APPROVED, OBSERVED }

data class DocumentReview(
    val status: DocumentReviewStatus = DocumentReviewStatus.PENDING,
    val reason: String = ""
)
