package com.example.chaskirider.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chaskirider.domain.model.RegistrationValidation
import com.example.chaskirider.domain.repository.LocationRepository
import com.example.chaskirider.domain.repository.RiderProfileRepository
import com.example.chaskirider.domain.repository.RiderSession
import com.example.chaskirider.domain.text.TextKey
import com.example.chaskirider.domain.text.TextProvider
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class HomeViewModel(
    private val profiles: RiderProfileRepository,
    private val locations: LocationRepository,
    private val texts: TextProvider,
    session: RiderSession
) : ViewModel() {
    private val state = MutableStateFlow(HomeUiState())
    val uiState = state.asStateFlow()
    private var updateJob: Job? = null
    private var locationJob: Job? = null
    init {
        viewModelScope.launch {
            session.user.collect { user ->
                if (state.value.user?.id != user?.id) {
                    updateJob?.cancel(); locationJob?.cancel()
                    state.value = HomeUiState(user = user)
                } else state.update { it.copy(user = user) }
            }
        }
    }
    fun togglePanel() { state.update { it.copy(panelExpanded = !it.panelExpanded) } }
    fun clearError() { state.update { it.copy(errorMessage = null, locationUnavailable = false) } }
    fun reportError(message: String) { state.update { it.copy(errorMessage = message) } }
    fun setLocationPermission(granted: Boolean) {
        if (!granted) locationJob?.cancel()
        state.update { it.copy(hasLocationPermission = granted,
            location = if (granted) it.location else null,
            isLocating = if (granted) it.isLocating else false) }
    }
    fun locate() {
        if (!state.value.hasLocationPermission || state.value.isLocating) return
        state.update { it.copy(isLocating = true, locationUnavailable = false) }
        locationJob = viewModelScope.launch {
            val point = locations.currentLocation().getOrNull()
            state.update { it.copy(isLocating = false, location = point,
                locationUnavailable = point == null, cameraRequest = it.cameraRequest + 1) }
        }
    }
    fun setAvailability(available: Boolean) {
        if (state.value.isUpdatingAvailability) return
        val user = state.value.user ?: return
        RegistrationValidation.availabilityError(user, available)?.let { reportError(texts.get(it)); return }
        if (available && !state.value.hasLocationPermission) {
            reportError(texts.get(TextKey.HOME_LOCATION_PERMISSION_REQUIRED)); return
        }
        state.update { it.copy(isUpdatingAvailability = true, errorMessage = null) }
        updateJob = viewModelScope.launch {
            profiles.setAvailability(available).onSuccess { user ->
                state.update { it.copy(user = user, isUpdatingAvailability = false) }
            }.onFailure { error ->
                state.update { it.copy(isUpdatingAvailability = false,
                    errorMessage = texts.resolveError(error.message, TextKey.TEXT_NO_SE_PUDO_COMPLETAR_LA_OPERACION)) }
            }
        }
    }
}
