package com.example.chaskirider.ui.screens.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chaskirider.domain.model.RegistrationValidation
import com.example.chaskirider.domain.repository.AuthRepository
import com.example.chaskirider.domain.repository.RiderProfileRepository
import com.example.chaskirider.domain.repository.RiderSession
import com.example.chaskirider.domain.text.TextKey
import com.example.chaskirider.domain.text.TextProvider
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class PersonalRegistrationViewModel(
    private val auth: AuthRepository,
    private val profiles: RiderProfileRepository,
    session: RiderSession,
    private val texts: TextProvider
) : ViewModel() {
    private val state = MutableStateFlow(PersonalRegistrationUiState())
    val uiState = state.asStateFlow()
    private var uid: String? = null
    private var saveJob: Job? = null

    init {
        viewModelScope.launch {
            session.user.collect { user ->
                if (uid != user?.id) {
                    saveJob?.cancel()
                    uid = user?.id
                    state.value = if (user == null) PersonalRegistrationUiState() else PersonalRegistrationUiState(
                        name = user.name, lastName = user.lastName, dni = user.dni,
                        phone = user.phone.removePrefix("+51"), email = user.email,
                        termsAccepted = user.termsAccepted, needsPassword = !user.hasPassword)
                } else if (user?.hasPassword == true) {
                    state.update { it.copy(needsPassword = false, password = "", confirmation = "") }
                }
            }
        }
    }

    fun change(field: PersonalField, value: String) {
        if (state.value.isLoading) return
        state.update { current ->
            when (field) {
                PersonalField.NAME -> current.copy(name = value)
                PersonalField.LAST_NAME -> current.copy(lastName = value)
                PersonalField.DNI -> current.copy(dni = value.filter { it in '0'..'9' }.take(8))
                PersonalField.PHONE -> current.copy(phone = value.filter { it in '0'..'9' }.take(9))
                PersonalField.PASSWORD -> current.copy(password = value)
                PersonalField.CONFIRMATION -> current.copy(confirmation = value)
            }.copy(errorMessage = null)
        }
    }
    fun acceptTerms(value: Boolean) {
        if (!state.value.isLoading) state.update { it.copy(termsAccepted = value, errorMessage = null) }
    }
    fun save(onSuccess: () -> Unit) {
        val form = state.value
        if (form.isLoading || uid == null) return
        val error = RegistrationValidation.personalError(form.name, form.lastName, form.dni, form.phone, form.termsAccepted)
            ?: if (form.needsPassword && (form.password.length < 8 || form.password != form.confirmation))
                TextKey.TEXT_USA_AL_MENOS_8_CARACTERES_Y_CONFIRMA else null
        if (error != null) {
            state.update { it.copy(errorMessage = texts.get(error)) }; return
        }
        state.update { it.copy(isLoading = true, errorMessage = null) }
        saveJob = viewModelScope.launch {
            if (form.needsPassword) {
                val linked = auth.linkPassword(form.password)
                if (linked.isFailure) { failure(linked.exceptionOrNull()!!); return@launch }
                state.update { it.copy(needsPassword = false, password = "", confirmation = "") }
            }
            profiles.savePersonalData(form.name.trim(), form.lastName.trim(), form.dni, form.phone, form.termsAccepted)
                .onSuccess {
                    state.update { it.copy(isLoading = false) }
                    onSuccess()
                }.onFailure { failure(it) }
        }
    }
    private fun failure(error: Throwable) {
        state.update { it.copy(isLoading = false,
            errorMessage = texts.resolveError(error.message, TextKey.TEXT_NO_SE_PUDO_COMPLETAR_LA_OPERACION)) }
    }
}
