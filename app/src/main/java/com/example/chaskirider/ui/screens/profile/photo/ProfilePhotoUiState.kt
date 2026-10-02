package com.example.chaskirider.ui.screens.profile.photo
import android.net.Uri
data class ProfilePhotoUiState(
    val captureUri: Uri? = null,
    val previewUri: Uri? = null,
    val profileUri: Uri? = null,
    val isLoading: Boolean = false,
    val permissionDenied: Boolean = false,
    val errorMessage: String? = null
)
