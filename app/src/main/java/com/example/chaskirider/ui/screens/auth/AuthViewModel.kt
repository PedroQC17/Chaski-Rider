// HU03 - Parte 2: se agrega updatePersonalData() para editar los datos personales
// desde Mi perfil (reutiliza la acción "personal" de la Cloud Function y la misma
// validación que el onboarding; se omite la validación de términos porque ya fue
// aceptada durante el registro).
// HU03 - Parte 3: saveStep2VehicleType ahora acepta CAR y se agrega updateVehicle()
// para cambiar el vehículo desde Mi perfil (acción "vehicle").
// HU04 - Parte 1: se agrega setAvailability() para conectar/desconectar al repartidor;
// valida que solo un repartidor aprobado y habilitado pueda activarse y persiste
// el estado con la acción "availability".
package com.example.chaskirider.ui.screens.auth

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chaskirider.di.AppContainer
import com.example.chaskirider.domain.model.*
import com.example.chaskirider.domain.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AuthUiState(
    val currentUser: RiderUser? = null,
    val isLoading: Boolean = false,
    val initialized: Boolean = false,
    val errorMessage: String? = null,
    val message: String? = null,
    val documentsMap: Map<String, DocumentFile> = emptyMap()
)

class AuthViewModel(private val repository: AuthRepository = AppContainer.authRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState = _uiState.asStateFlow()
    init { checkCurrentUser() }

    private fun acceptUser(user: RiderUser?) {
        val docs = user?.let { RegistrationValidation.documentPaths(it) }.orEmpty()
            .filterValues { it.isNotBlank() }.mapValues { (key, path) ->
                DocumentFile(id = key, name = key, url = path, uploadState = DocumentUploadState.UPLOADED)
            }
        _uiState.update { it.copy(currentUser = user, documentsMap = docs, isLoading = false, initialized = true) }
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
    fun saveStep1PersonalData(name: String, lastName: String, dni: String, phone: String, termsAccepted: Boolean, onSuccess: () -> Unit) {
        RegistrationValidation.personalError(name, lastName, dni, phone, termsAccepted)?.let { reportError(it); return }
        userAction({ repository.savePersonalData(name, lastName, dni, phone, termsAccepted) }) { onSuccess() }
    }
    fun updatePersonalData(name: String, lastName: String, dni: String, phone: String, onSuccess: () -> Unit) {
        RegistrationValidation.personalError(name, lastName, dni, phone, true)?.let { reportError(it); return }
        val terms = _uiState.value.currentUser?.termsAccepted ?: true
        userAction({ repository.savePersonalData(name, lastName, dni, phone, terms) }) { onSuccess() }
    }
    fun saveStep2VehicleType(vehicleType: VehicleType, onSuccess: () -> Unit) {
        if (vehicleType !in listOf(VehicleType.BICYCLE, VehicleType.MOTORCYCLE, VehicleType.CAR)) {
            reportError("Selecciona bicicleta, motocicleta o automóvil"); return
        }
        userAction({ repository.saveVehicle(vehicleType) }) { onSuccess() }
    }
    fun updateVehicle(vehicleType: VehicleType, onSuccess: () -> Unit) {
        if (vehicleType !in listOf(VehicleType.BICYCLE, VehicleType.MOTORCYCLE, VehicleType.CAR)) {
            reportError("Selecciona un vehículo"); return
        }
        userAction({ repository.saveVehicle(vehicleType) }) { onSuccess() }
    }
    fun setAvailability(available: Boolean, onSuccess: () -> Unit = {}) {
        val user = _uiState.value.currentUser
        if (user == null) { reportError("No hay una sesión activa"); return }
        RegistrationValidation.availabilityError(user, activating = available)?.let { reportError(it); return }
        userAction({ repository.setAvailability(available) }) { onSuccess() }
    }
    fun saveBank(bank: BankInfo) {
        RegistrationValidation.bankError(bank)?.let { reportError(it); return }
        userAction({ repository.saveBankInfo(bank) }) { _uiState.update { s -> s.copy(message = "Datos bancarios guardados") } }
    }
    fun uploadDocument(docType: String, uri: Uri) {
        if (_uiState.value.isLoading) return
        _uiState.update { it.copy(isLoading = true, errorMessage = null, documentsMap = it.documentsMap +
            (docType to DocumentFile(id = docType, uploadState = DocumentUploadState.UPLOADING))) }
        viewModelScope.launch {
            repository.uploadDocument(docType, uri).onSuccess { acceptUser(it) }.onFailure { error ->
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
    fun finishRegistration(bankName: String, holder: String, account: String, cci: String, onSuccess: () -> Unit) {
        val bank = BankInfo(bankName, holder, account, cci)
        RegistrationValidation.bankError(bank)?.let { reportError(it); return }
        val user = _uiState.value.currentUser ?: return
        val required = RegistrationValidation.requiredDocuments(user.vehicleType)
        if (required.any { _uiState.value.documentsMap[it]?.uploadState != DocumentUploadState.UPLOADED }) {
            reportError("Sube todos los documentos obligatorios antes de enviar"); return
        }
        userAction({
            val saved = repository.saveBankInfo(bank)
            if (saved.isFailure) Result.failure(saved.exceptionOrNull()!!) else repository.submitRegistration()
        }) { onSuccess() }
    }
    fun logout() {
        if (_uiState.value.isLoading) return
        viewModelScope.launch { repository.logout(); _uiState.value = AuthUiState(initialized = true) }
    }
}
