package com.example.chaskirider.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chaskirider.domain.model.RegistrationValidation
import com.example.chaskirider.domain.orders.DemandZone
import com.example.chaskirider.domain.orders.OfferAction
import com.example.chaskirider.domain.orders.OfferRepository
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
    private val offers: OfferRepository,
    private val session: RiderSession
) : ViewModel() {
    private val state = MutableStateFlow(HomeUiState())
    val uiState = state.asStateFlow()
    private var updateJob: Job? = null
    private var locationJob: Job? = null
    private var zonesJob: Job? = null
    private var zonesRequestedFor: String? = null
    init {
        viewModelScope.launch {
            session.user.collect { user ->
                if (state.value.user?.id != user?.id) {
                    updateJob?.cancel(); locationJob?.cancel(); zonesJob?.cancel()
                    zonesRequestedFor = null
                    state.value = HomeUiState(user = user, hasLocationPermission = state.value.hasLocationPermission)
                } else state.update { it.copy(user = user) }
                // HU05: primera carga de zonas de demanda al iniciar sesión.
                if (user != null && zonesRequestedFor != user.id) {
                    zonesRequestedFor = user.id
                    refreshZones()
                }
            }
        }
    }
    fun clearError() { state.update { it.copy(errorMessage = null, locationUnavailable = false) } }
    fun reportError(message: String) { state.update { it.copy(errorMessage = message) } }
    fun setLocationPermission(granted: Boolean) {
        if (!granted) locationJob?.cancel()
        state.update { it.copy(hasLocationPermission = granted,
            location = if (granted) it.location else null,
            isLocating = if (granted) it.isLocating else false) }
    }
    fun locate() {
        if (state.value.user?.isAvailable != true) return
        if (!state.value.hasLocationPermission || state.value.isLocating) return
        state.update { it.copy(isLocating = true, locationUnavailable = false) }
        val owner = state.value.user?.id
        locationJob = viewModelScope.launch {
            val point = locations.currentLocation().getOrNull()
            if (session.user.value?.id == owner && state.value.hasLocationPermission) state.update {
                it.copy(isLocating = false, location = point, locationUnavailable = point == null, cameraRequest = it.cameraRequest + 1)
            }
        }
    }
    // HU05: carga o recarga las zonas de demanda; el servidor marca la actualización.
    fun refreshZones() {
        if (state.value.isRefreshingZones) return
        val owner = session.user.value?.id ?: return
        state.update { it.copy(isRefreshingZones = true) }
        zonesJob = viewModelScope.launch {
            try {
                val snapshot = offers.execute(OfferAction.STATE)
                if (session.user.value?.id == owner) state.update { s ->
                    s.copy(zonesLoaded = true, demandZones = snapshot.demandZones,
                        zonesUpdatedAt = snapshot.demandZones.maxOfOrNull { it.updatedAt } ?: snapshot.serverTime,
                        selectedZone = snapshot.demandZones.find { it.id == s.selectedZone?.id })
                }
            } catch (error: kotlinx.coroutines.CancellationException) { throw error }
            catch (_: Exception) { }
            finally { if (session.user.value?.id == owner) state.update { it.copy(isRefreshingZones = false) } }
        }
    }

    fun selectZone(zone: DemandZone) {
        state.update { it.copy(selectedZone = if (it.selectedZone?.id == zone.id) null else zone) }
    }
    fun clearZone() { state.update { it.copy(selectedZone = null) } }
    fun setAvailability(available: Boolean) {
        if (state.value.isUpdatingAvailability) return
        val user = state.value.user ?: return
        RegistrationValidation.availabilityError(user, available)?.let { reportError(texts.get(it)); return }
        if (available && !state.value.hasLocationPermission) {
            reportError(texts.get(TextKey.HOME_LOCATION_PERMISSION_REQUIRED)); return
        }
        state.update { it.copy(isUpdatingAvailability = true, errorMessage = null) }
        updateJob = viewModelScope.launch {
            profiles.setAvailability(available).onSuccess { updated ->
                if (session.user.value?.id != user.id) return@onSuccess
                state.update { it.copy(user = updated, isUpdatingAvailability = false) }
                if (available) locate()
            }.onFailure { error ->
                if (session.user.value?.id != user.id) return@onFailure
                state.update { it.copy(isUpdatingAvailability = false,
                    errorMessage = texts.resolveError(error.message, TextKey.TEXT_NO_SE_PUDO_COMPLETAR_LA_OPERACION)) }
            }
        }
    }
}
