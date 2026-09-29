package com.example.chaskirider.ui.screens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.example.chaskirider.domain.model.*
import com.example.chaskirider.domain.repository.AuthRepository
import com.example.chaskirider.domain.repository.RiderSession

class AuthViewModel(private val repository: AuthRepository, session: RiderSession) : ViewModel() {
    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState = _uiState.asStateFlow()
    init {
        viewModelScope.launch { session.user.collect { user -> _uiState.update { it.copy(currentUser = user) } } }
        checkCurrentUser()
    }
    private fun acceptUser(user: RiderUser?) {
        _uiState.update { it.copy(currentUser = user, isLoading = false, initialized = true) }
    }
    fun checkCurrentUser() {
        if (_uiState.value.isLoading) return
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            repository.getCurrentUser().onSuccess { acceptUser(it) }.onFailure { failure(it); _uiState.update { s -> s.copy(initialized = true) } }
        }
    }
    private fun failure(error: Throwable) {
        _uiState.update { it.copy(isLoading = false, errorMessage = error.message ?: "No se pudo completar la operación") }
    }
    fun reportError(message: String) { _uiState.update { it.copy(errorMessage = message) } }
    fun clearError() { _uiState.update { it.copy(errorMessage = null, message = null) } }

    private fun userAction(action: suspend () -> Result<RiderUser>, onSuccess: (RiderUser) -> Unit = {}) {
        if (_uiState.value.isLoading) return
        _uiState.update { it.copy(isLoading = true, errorMessage = null, message = null) }
        viewModelScope.launch {
            action().onSuccess { acceptUser(it); onSuccess(it) }.onFailure { failure(it) }
        }
    }
    fun loginWithGoogle(token: String) = userAction({ repository.loginWithGoogle(token) })
    fun loginWithEmail(email: String, password: String) = userAction({ repository.loginWithEmail(email, password) })
    fun sendPasswordResetEmail(email: String) {
        if (_uiState.value.isLoading) return
        if (!email.matches(Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$"))) {
            reportError("Ingresa un correo válido"); return
        }
        _uiState.update { it.copy(isLoading = true, errorMessage = null, message = null) }
        viewModelScope.launch {
            repository.sendPasswordResetEmail(email).onSuccess {
                _uiState.update { it.copy(isLoading = false, message = "Si el correo corresponde a una cuenta, recibirás instrucciones para recuperar el acceso.") }
            }.onFailure { failure(it) }
        }
    }
    fun linkPassword(password: String, confirmation: String) {
        if (_uiState.value.isLoading) return
        if (password.length < 8 || password != confirmation) {
            reportError("Usa al menos 8 caracteres y confirma la misma contraseña"); return
        }
        _uiState.update { it.copy(isLoading = true, errorMessage = null, message = null) }
        viewModelScope.launch {
            repository.linkPassword(password).onSuccess {
                _uiState.update { it.copy(isLoading = false, message = "Contraseña configurada. Ya puedes ingresar con tu correo.") }
            }.onFailure { failure(it) }
        }
    }
    fun logout() {
        if (_uiState.value.isLoading) return
        viewModelScope.launch { repository.logout(); _uiState.value = AuthUiState(initialized = true) }
    }
}
