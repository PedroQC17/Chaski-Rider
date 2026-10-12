package com.example.chaskirider.ui.screens.profile.documents

import com.example.chaskirider.domain.text.TextProvider
import com.example.chaskirider.domain.text.TextKey
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
import com.example.chaskirider.domain.repository.RegistrationDraftRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job

class DocumentsViewModel(
    private val texts: TextProvider,
    private val repository: DocumentRepository, session: RiderSession,
    private val previews: com.example.chaskirider.domain.repository.DocumentPreviewRepository,
    private val drafts: RegistrationDraftRepository) : ViewModel() {
    private var uploadJob: Job? = null
    private var captureJob: Job? = null
    private var pendingCapture: Uri? = null
    private var previewJob: kotlinx.coroutines.Job? = null
    private var sessionUid: String? = null
    private val _uiState = MutableStateFlow(DocumentsUiState())
    val uiState = _uiState.asStateFlow()
    init { viewModelScope.launch { session.user.collect { acceptUser(it) } } }
    private fun acceptUser(user: RiderUser?) {
        if (sessionUid != user?.id) {
            previewJob?.cancel(); uploadJob?.cancel(); captureJob?.cancel()
            pendingCapture = null
            _uiState.value = DocumentsUiState(); sessionUid = user?.id
            if (user != null) recoverCapture()
        }
        val docs = user?.let { RegistrationValidation.documentPaths(it) }.orEmpty()
            .filterValues { it.isNotBlank() }.mapValues { (key, path) ->
                DocumentFile(id = key, name = key, url = path, uploadState = DocumentUploadState.UPLOADED)
            }
        if (user == null) _uiState.value = DocumentsUiState()
        else _uiState.update { it.copy(documentsMap = docs) }
    }
    private fun failure(error: Throwable) {
        _uiState.update { it.copy(isLoading = false, errorMessage = texts.resolveError(error.message, TextKey.TEXT_NO_SE_PUDO_COMPLETAR_LA_OPERACION)) }
    }
    fun reportError(message: String) { _uiState.update { it.copy(errorMessage = message) } }
    fun clearError() { _uiState.update { it.copy(errorMessage = null, message = null) } }

    fun uploadDocument(docType: String, uri: Uri) {
        if (_uiState.value.isLoading) return
        val owner = sessionUid ?: return
        _uiState.update { it.copy(isLoading = true, errorMessage = null, documentsMap = it.documentsMap +
            (docType to DocumentFile(id = docType, uploadState = DocumentUploadState.UPLOADING))) }
        uploadJob = viewModelScope.launch {
            repository.uploadDocument(docType, uri).onSuccess {
                if (sessionUid != owner) return@launch
                acceptUser(it)
                if (pendingCapture == uri) {
                    try { clearCapture(owner) }
                    catch (e: CancellationException) { throw e }
                    catch (e: Exception) { failure(e) }
                }
                _uiState.update { state -> state.copy(isLoading = false) }
            }.onFailure { error ->
                if (sessionUid != owner) return@launch
                failure(error)
                _uiState.update { it.copy(documentsMap = it.documentsMap +
                    (docType to DocumentFile(id = docType, uploadState = DocumentUploadState.ERROR, errorMessage = texts.resolveError(error.message, TextKey.TEXT_NO_SE_PUDO_COMPLETAR_LA_OPERACION)))) }
            }
        }
    }
    fun dismissPreview() {
        previewJob?.cancel()
        _uiState.update { it.copy(preview = null) }
    }

    fun recoverCapture() {
        val owner = sessionUid ?: return
        if (captureJob?.isActive == true || _uiState.value.isLoading) return
        captureJob = viewModelScope.launch {
            try {
                val capture = drafts.loadCapture(owner) ?: return@launch
                val uri = Uri.parse(capture.uri)
                val exists = repository.hasCapture(uri).getOrThrow()
                if (sessionUid != owner) return@launch
                pendingCapture = uri
                _uiState.update { it.copy(captureType = if (exists) capture.type else null,
                    captureUri = if (exists) uri else null) }
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { failure(e) }
        }
    }

    private suspend fun clearCapture(owner: String) {
        val capture = drafts.loadCapture(owner)
        capture?.let { repository.deleteCapture(Uri.parse(it.uri)).getOrThrow() }
        drafts.clearCapture(owner)
        if (sessionUid == owner) {
            pendingCapture = null
            _uiState.update { it.copy(captureType = null, captureUri = null) }
        }
    }

    // HU03: captura con cámara; la foto se confirma en vista previa antes de subirla.
    fun prepareCapture(docType: String, onReady: (Uri) -> Unit) {
        if (captureJob?.isActive == true || _uiState.value.captureType != null || _uiState.value.isLoading) return
        val owner = sessionUid ?: return
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        captureJob = viewModelScope.launch {
            try {
                clearCapture(owner)
                val uri = repository.createCapture().getOrThrow()
                drafts.saveCapture(PendingDocumentCapture(owner, docType, uri.toString()))
                if (sessionUid != owner) return@launch
                pendingCapture = uri
                _uiState.update { it.copy(captureType = docType, isLoading = false) }
                onReady(uri)
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { if (sessionUid == owner) failure(e) }
        }
    }
    fun captured(success: Boolean) {
        // ActivityResult can arrive after process recreation, before Room has finished loading.
        val previous = captureJob
        viewModelScope.launch {
            previous?.join()
            if (success) recoverCapture() else discardCapture()
        }
    }
    fun discardCapture() {
        if (_uiState.value.isLoading) return
        val owner = sessionUid ?: return
        val previous = captureJob
        _uiState.update { it.copy(isLoading = true) }
        captureJob = viewModelScope.launch {
            previous?.join()
            try { clearCapture(owner) }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { if (sessionUid == owner) failure(e) }
            finally { if (sessionUid == owner) _uiState.update { it.copy(isLoading = false) } }
        }
    }
    fun confirmCapture() {
        val type = _uiState.value.captureType ?: return
        val uri = pendingCapture ?: return
        uploadDocument(type, uri)
    }
    fun openDocument(docType: String) {
        previewJob?.cancel()
        _uiState.update { it.copy(preview = DocumentPreviewUiState(docType)) }
        previewJob = viewModelScope.launch {
            repository.getDocument(docType).onSuccess { uri ->
                _uiState.update { it.copy(preview = it.preview?.copy(sourceUri = uri)) }
                render(uri, 0)
            }.onFailure { _uiState.update { it.copy(preview = it.preview?.copy(loading = false, error = true)) } }
        }
    }
    fun showPage(index: Int) {
        val current = _uiState.value.preview ?: return
        if (current.loading) return
        val uri = current.sourceUri ?: run { openDocument(current.type); return }
        if (index < 0 || (current.page != null && index >= current.page.count)) return
        previewJob?.cancel()
        _uiState.update { it.copy(preview = it.preview?.copy(loading = true, error = false)) }
        previewJob = viewModelScope.launch { render(uri, index) }
    }
    private suspend fun render(uri: Uri, index: Int) {
        previews.render(uri, index).onSuccess { page ->
            _uiState.update { it.copy(preview = it.preview?.copy(page = page, loading = false, error = false)) }
        }.onFailure { _uiState.update { it.copy(preview = it.preview?.copy(loading = false, error = true)) } }
    }
}
