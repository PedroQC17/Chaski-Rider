package com.example.chaskirider.domain.model

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
