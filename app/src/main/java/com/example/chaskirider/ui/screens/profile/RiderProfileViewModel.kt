package com.example.chaskirider.ui.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.example.chaskirider.domain.model.*
import com.example.chaskirider.domain.repository.RiderProfileRepository
import com.example.chaskirider.domain.repository.RiderSession

class RiderProfileViewModel(private val repository: RiderProfileRepository, session: RiderSession) : ViewModel() {
    private val _uiState = MutableStateFlow(RiderProfileUiState())
    val uiState = _uiState.asStateFlow()
    init { viewModelScope.launch { session.user.collect { acceptUser(it) } } }
    private fun acceptUser(user: RiderUser?) {
        if (user == null) _uiState.value = RiderProfileUiState()
        else _uiState.update { it.copy(currentUser = user) }
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
            action().onSuccess { acceptUser(it); _uiState.update { state -> state.copy(isLoading = false) }; onSuccess(it) }.onFailure { failure(it) }
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
    fun finishRegistration(bankName: String, holder: String, account: String, cci: String, onSuccess: () -> Unit) {
        val bank = BankInfo(bankName, holder, account, cci)
        RegistrationValidation.bankError(bank)?.let { reportError(it); return }
        val user = _uiState.value.currentUser ?: return
        val required = RegistrationValidation.requiredDocuments(user.vehicleType)
        if (required.any { RegistrationValidation.documentPaths(user)[it].isNullOrBlank() }) {
            reportError("Sube todos los documentos obligatorios antes de enviar"); return
        }
        userAction({
            val saved = repository.saveBankInfo(bank)
            if (saved.isFailure) Result.failure(saved.exceptionOrNull()!!) else repository.submitRegistration()
        }) { onSuccess() }
    }
}
