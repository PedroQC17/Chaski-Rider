package com.example.chaskirider.ui.screens.profile.photo

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chaskirider.domain.repository.ProfilePhotoRepository
import com.example.chaskirider.domain.repository.RiderSession
import com.example.chaskirider.domain.text.TextProvider
import com.example.chaskirider.domain.text.TextKey
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ProfilePhotoViewModel(private val repository: ProfilePhotoRepository, session: RiderSession,
    private val texts: TextProvider, private val saved: SavedStateHandle) : ViewModel() {
    private fun savedUri(key: String) = saved.get<String>(key)?.let(Uri::parse)
    private val state = MutableStateFlow(ProfilePhotoUiState(captureUri = savedUri("capture"), previewUri = savedUri("preview")))
    val uiState = state.asStateFlow()
    private var uid: String? = saved["owner"]
    private var loadedPath: String? = null
    private var saveJob: Job? = null
    private var loadJob: Job? = null
    init {
        viewModelScope.launch {
            session.user.collect { user ->
                if (uid != user?.id) {
                    saveJob?.cancel(); loadJob?.cancel(); loadedPath = null
                    uid = user?.id; saved["owner"] = uid
                    saved.remove<String>("capture"); saved.remove<String>("preview")
                    state.value = ProfilePhotoUiState()
                }
                if (user != null && user.profilePhotoPath.isNotBlank() && loadedPath != user.profilePhotoPath) {
                    loadedPath = user.profilePhotoPath
                    loadJob?.cancel()
                    loadJob = launch { repository.load(user).onSuccess { uri -> state.update { it.copy(profileUri = uri) } }
                        .onFailure { loadedPath = null } }
                }
            }
        }
    }
    fun error(message: String) { state.update { it.copy(isLoading = false, errorMessage = message) } }
    fun permissionDenied(message: String) { state.update { it.copy(permissionDenied = true, errorMessage = message) } }
    fun prepare(onReady: (Uri) -> Unit) {
        if (state.value.isLoading) return
        state.update { it.copy(isLoading = true, errorMessage = null, permissionDenied = false) }
        saveJob = viewModelScope.launch {
            repository.createCapture().onSuccess { uri ->
                saved["capture"] = uri.toString()
                state.update { it.copy(captureUri = uri, isLoading = false) }
                onReady(uri)
            }.onFailure { error(texts.get(TextKey.PROFILE_PHOTO_FAILURE)) }
        }
    }
    fun captured(success: Boolean) {
        if (success) {
            val uri = state.value.captureUri ?: return
            saved["preview"] = uri.toString()
            state.update { it.copy(previewUri = uri, errorMessage = null) }
        }
    }
    fun save() {
        if (state.value.isLoading) return
        val uri = state.value.previewUri ?: return
        state.update { it.copy(isLoading = true, errorMessage = null) }
        saveJob = viewModelScope.launch {
            repository.save(uri).onSuccess {
                saved.remove<String>("preview"); saved.remove<String>("capture")
                state.update { it.copy(isLoading = false, previewUri = null, captureUri = null) }
            }.onFailure { error(texts.get(TextKey.PROFILE_PHOTO_FAILURE)) }
        }
    }
}
