// HU06 - Parte 2: ViewModel del módulo de pedidos.
// Máquina de estados: Idle -> Searching (esperando oferta mock) ->
// OfferActive (cuenta regresiva de 30 s con Aceptar/Rechazar) -> Accepted.
// - Solo busca oferta si el rider está Disponible (bindUser lo decide).
// - Al rechazar (o expirar la cuenta regresiva) vuelve a Searching y agenda
//   la siguiente oferta; al aceptar queda en Accepted y no se ofrecen más.
// - simulateOffer() permite disparar una oferta al instante para pruebas.
// La fuente real es OrderRepository (mock hoy; backend `riderOrders` después).
package com.example.chaskirider.ui.screens.orders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chaskirider.domain.model.RideOffer
import com.example.chaskirider.domain.model.RiderUser
import com.example.chaskirider.domain.repository.OrderRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class OrderUiStatus { IDLE, SEARCHING, OFFER_ACTIVE, ACCEPTED }

data class OrdersUiState(
    val status: OrderUiStatus = OrderUiStatus.IDLE,
    val offer: RideOffer? = null,
    val acceptedOffer: RideOffer? = null,
    val secondsLeft: Int = 0,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val message: String? = null
)

class OrdersViewModel(private val repository: OrderRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(OrdersUiState())
    val uiState: StateFlow<OrdersUiState> = _uiState.asStateFlow()
    val acceptedOrder: StateFlow<RideOffer?> = repository.acceptedOrder

    private var currentUser: RiderUser? = null
    private var wasAvailable = false
    private var searchJob: Job? = null
    private var countdownJob: Job? = null

    // Debe llamarse cada vez que cambie el usuario (AppNavigation).
    fun bindUser(user: RiderUser?) {
        currentUser = user
        if (user == null) {
            reset()
            return
        }
        val available = user.isAvailable
        if (!available && wasAvailable) {
            // Se desconectó: cancela búsqueda y oferta vigente si la hubiera.
            wasAvailable = false
            searchJob?.cancel()
            countdownJob?.cancel()
            _uiState.value.offer?.let { offer ->
                viewModelScope.launch { repository.reject(offer.id) }
            }
            if (_uiState.value.status != OrderUiStatus.ACCEPTED) reset() else clearOffer()
        } else if (available && !wasAvailable) {
            wasAvailable = true
            if (_uiState.value.status != OrderUiStatus.ACCEPTED) {
                _uiState.update { it.copy(status = OrderUiStatus.SEARCHING, message = "Buscando pedidos cerca de ti…") }
                scheduleOffer(FIRST_OFFER_DELAY_MS)
            }
        }
    }

    // Dispara una oferta inmediata (botón de prueba en Home).
    fun simulateOffer() {
        if (_uiState.value.status != OrderUiStatus.SEARCHING) return
        searchJob?.cancel()
        fetchOffer()
    }

    fun accept() {
        val offer = _uiState.value.offer ?: return
        countdownJob?.cancel()
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            repository.accept(offer.id)
                .onSuccess {
                    _uiState.update {
                        it.copy(status = OrderUiStatus.ACCEPTED, offer = null, acceptedOffer = offer,
                            secondsLeft = 0, isLoading = false,
                            message = "Pedido aceptado. Ya está asignado a ti.")
                    }
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = e.message ?: "No se pudo aceptar el pedido")
                    }
                    backToSearching()
                }
        }
    }

    fun reject() {
        val offer = _uiState.value.offer ?: return
        countdownJob?.cancel()
        viewModelScope.launch {
            repository.reject(offer.id)
                .onSuccess {
                    _uiState.update { it.copy(status = OrderUiStatus.SEARCHING, offer = null, secondsLeft = 0) }
                    scheduleOffer(NEXT_OFFER_DELAY_MS)
                }
                .onFailure { e ->
                    _uiState.update { it.copy(errorMessage = e.message ?: "No se pudo rechazar el pedido") }
                    backToSearching()
                }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null, message = null) }
    }

    private fun fetchOffer() {
        val user = currentUser ?: return
        if (!user.isAvailable || _uiState.value.status != OrderUiStatus.SEARCHING) return
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            repository.nextOffer(user)
                .onSuccess { offer ->
                    _uiState.update {
                        it.copy(status = OrderUiStatus.OFFER_ACTIVE, offer = offer,
                            secondsLeft = offer.expiresInSeconds, isLoading = false, message = null)
                    }
                    startCountdown()
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = e.message ?: "No se pudo recibir la oferta")
                    }
                }
        }
    }

    private fun startCountdown() {
        countdownJob?.cancel()
        countdownJob = viewModelScope.launch {
            while (_uiState.value.secondsLeft > 0 && _uiState.value.status == OrderUiStatus.OFFER_ACTIVE) {
                delay(1000)
                _uiState.update { it.copy(secondsLeft = it.secondsLeft - 1) }
            }
            if (_uiState.value.status == OrderUiStatus.OFFER_ACTIVE) {
                // Criterio HU06: al expirar se rechaza y el rider sigue buscando.
                _uiState.value.offer?.let { repository.reject(it.id) }
                _uiState.update {
                    it.copy(status = OrderUiStatus.SEARCHING, offer = null, secondsLeft = 0,
                        message = "La oferta expiró. Sigues buscando pedidos.")
                }
                scheduleOffer(NEXT_OFFER_DELAY_MS)
            }
        }
    }

    private fun backToSearching() {
        countdownJob?.cancel()
        clearOffer()
        if (currentUser?.isAvailable == true) {
            _uiState.update { it.copy(status = OrderUiStatus.SEARCHING) }
            scheduleOffer(NEXT_OFFER_DELAY_MS)
        } else {
            reset()
        }
    }

    private fun scheduleOffer(delayMs: Long) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(delayMs)
            if (currentUser?.isAvailable == true && _uiState.value.status == OrderUiStatus.SEARCHING) fetchOffer()
        }
    }

    private fun clearOffer() {
        _uiState.update { it.copy(offer = null, secondsLeft = 0) }
    }

    private fun reset() {
        searchJob?.cancel()
        countdownJob?.cancel()
        _uiState.value = OrdersUiState()
    }

    companion object {
        // Mock: la primera oferta llega a los 5 s de activarse; las
        // siguientes, 6 s después de cada rechazo/expiración.
        const val FIRST_OFFER_DELAY_MS = 5_000L
        const val NEXT_OFFER_DELAY_MS = 6_000L
    }
}
