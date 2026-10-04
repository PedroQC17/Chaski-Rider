package com.example.chaskirider.ui.screens.profile.documents

import android.net.Uri
import com.example.chaskirider.domain.model.DocumentFile

data class DocumentsUiState(
    val documentsMap: Map<String, DocumentFile> = emptyMap(),
    val preview: DocumentPreviewUiState? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val message: String? = null,
    // Captura de cámara pendiente de confirmación antes de subirla.
    val captureType: String? = null,
    val captureUri: Uri? = null
)
