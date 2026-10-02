package com.example.chaskirider.ui.screens.profile.documents

import com.example.chaskirider.domain.model.DocumentFile

data class DocumentsUiState(
    val documentsMap: Map<String, DocumentFile> = emptyMap(),
    val previewUri: android.net.Uri? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val message: String? = null
)
