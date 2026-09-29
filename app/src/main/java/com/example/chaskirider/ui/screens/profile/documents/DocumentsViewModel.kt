package com.example.chaskirider.ui.screens.profile.documents

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.example.chaskirider.domain.model.*
import com.example.chaskirider.domain.repository.DocumentRepository
import com.example.chaskirider.domain.repository.RiderSession

class DocumentsViewModel(private val repository: DocumentRepository, session: RiderSession) : ViewModel() {
    private val _uiState = MutableStateFlow(DocumentsUiState())
    val uiState = _uiState.asStateFlow()
    init { viewModelScope.launch { session.user.collect { acceptUser(it) } } }
    private fun acceptUser(user: RiderUser?) {
        val docs = user?.let { RegistrationValidation.documentPaths(it) }.orEmpty()
            .filterValues { it.isNotBlank() }.mapValues { (key, path) ->
                DocumentFile(id = key, name = key, url = path, uploadState = DocumentUploadState.UPLOADED)
            }
        if (user == null) _uiState.value = DocumentsUiState()
        else _uiState.update { it.copy(documentsMap = docs) }
    }
    private fun failure(error: Throwable) {
        _uiState.update { it.copy(isLoading = false, errorMessage = error.message ?: "No se pudo completar la operación") }
    }
    fun reportError(message: String) { _uiState.update { it.copy(errorMessage = message) } }
    fun clearError() { _uiState.update { it.copy(errorMessage = null, message = null) } }

    fun prepareCapture(onReady: (Uri) -> Unit) {
        if (_uiState.value.isLoading) return
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            repository.createCaptureUri().onSuccess { uri ->
                _uiState.update { it.copy(isLoading = false) }
                onReady(uri)
            }.onFailure { failure(it) }
        }
    }

    fun uploadDocument(docType: String, uri: Uri) {
        if (_uiState.value.isLoading) return
        _uiState.update { it.copy(isLoading = true, errorMessage = null, documentsMap = it.documentsMap +
            (docType to DocumentFile(id = docType, uploadState = DocumentUploadState.UPLOADING))) }
        viewModelScope.launch {
            repository.uploadDocument(docType, uri).onSuccess { acceptUser(it); _uiState.update { state -> state.copy(isLoading = false) } }.onFailure { error ->
                failure(error)
                _uiState.update { it.copy(documentsMap = it.documentsMap +
                    (docType to DocumentFile(id = docType, uploadState = DocumentUploadState.ERROR, errorMessage = error.message))) }
            }
        }
    }
    fun openDocument(docType: String, onReady: (Uri) -> Unit) {
        if (_uiState.value.isLoading) return
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            repository.getDocument(docType).onSuccess { uri ->
                _uiState.update { it.copy(isLoading = false) }; onReady(uri)
            }.onFailure { failure(it) }
        }
    }
}
