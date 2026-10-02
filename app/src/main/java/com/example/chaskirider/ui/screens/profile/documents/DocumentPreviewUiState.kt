package com.example.chaskirider.ui.screens.profile.documents
import android.net.Uri
import com.example.chaskirider.domain.model.DocumentPage
data class DocumentPreviewUiState(
    val type: String,
    val sourceUri: Uri? = null,
    val page: DocumentPage? = null,
    val loading: Boolean = true,
    val error: Boolean = false
)
