package com.example.chaskirider.feature.documents.ui.viewmodel

import android.net.Uri
import com.example.chaskirider.feature.documents.domain.DocumentPage

data class DocumentPreviewUiState(
    val type: String,
    val sourceUri: Uri? = null,
    val page: DocumentPage? = null,
    val loading: Boolean = true,
    val error: Boolean = false
)
