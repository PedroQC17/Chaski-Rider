package com.example.chaskirider.ui.screens.orders

import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chaskirider.R
import com.example.chaskirider.domain.orders.*
import com.example.chaskirider.domain.repository.RiderSession
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Duration.Companion.milliseconds

class OrdersViewModel(private val repository: OfferRepository, private val session: RiderSession) : ViewModel() {
    private val mutable = MutableStateFlow(OrdersUiState())
    val uiState = mutable.asStateFlow()
    private var request: Job? = null
    private var ticker: Job? = null
    private var scheduled: Job? = null
    private var uid: String? = session.user.value?.id
    private var deadline = 0L

    init {
        viewModelScope.launch {
            session.user.map { it?.id }.distinctUntilChanged().collect { id ->
                if (id == uid) return@collect
                uid = id; request?.cancel(); ticker?.cancel(); scheduled?.cancel()
                mutable.value = OrdersUiState()
                // Cargar únicamente al entrar a la demostración.
            }
        }
    }
    fun refresh() = execute(OfferAction.STATE)
    fun createOffer() = execute(OfferAction.OFFER)
    fun accept() { if (mutable.value.secondsLeft > 0) execute(OfferAction.ACCEPT) }
    fun reject() = execute(OfferAction.REJECT)
    fun pickup() = execute(OfferAction.PICKUP)
    fun reset() = execute(OfferAction.RESET)
    fun scheduleOffer() {
        if (mutable.value.busy || mutable.value.scheduling || mutable.value.error != null) return
        mutable.update { it.copy(scheduling = true) }
        scheduled = viewModelScope.launch {
            delay(5.seconds)
            mutable.update { it.copy(scheduling = false) }
            createOffer()
        }
    }
    private fun applySnapshot(snapshot: OfferSnapshot, started: Long) {
        // Restar todo el viaje de red es conservador: nunca ampliar el plazo del servidor.
        val remaining = ((snapshot.offer?.expiresAt ?: snapshot.serverTime) - snapshot.serverTime -
            (SystemClock.elapsedRealtime() - started)).coerceAtLeast(0)
        deadline = SystemClock.elapsedRealtime() + remaining
        mutable.update { it.copy(snapshot = snapshot, secondsLeft = ((remaining + 999) / 1000).toInt()) }
        ticker?.cancel()
        if (snapshot.offer != null) ticker = viewModelScope.launch {
            while (true) {
                val left = ((deadline - SystemClock.elapsedRealtime()).coerceAtLeast(0) + 999) / 1000
                mutable.update { it.copy(secondsLeft = left.toInt()) }
                if (left == 0L) {
                    if (!mutable.value.busy) { refresh(); break }
                }
                delay(250.milliseconds)
            }
        }
    }
    private fun execute(action: OfferAction) {
        if (uid == null || mutable.value.busy || (action != OfferAction.STATE && mutable.value.error != null)) return
        val requestUid = uid
        val offerId = mutable.value.snapshot?.offer?.id
        if ((action == OfferAction.ACCEPT || action == OfferAction.REJECT) && offerId == null) return
        // HU04: sin disponibilidad activa no se piden ofertas nuevas.
        if (action == OfferAction.OFFER && session.user.value?.isAvailable != true) {
            mutable.update { it.copy(error = R.string.text_conectate_para_recibir_pedidos_en_tu_zona) }
            return
        }
        mutable.update { it.copy(busy = true, error = null) }
        request = viewModelScope.launch {
            val started = SystemClock.elapsedRealtime()
            try { applySnapshot(repository.execute(action, offerId), started) }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) {
                val message = when ((e as? OfferRequestException)?.status) {
                    401, 403 -> R.string.orders_access_error
                    409 -> R.string.orders_conflict
                    else -> R.string.orders_network_error
                }
                // Una respuesta perdida puede ocultar una aceptación ya confirmada. Reconciliar antes de continuar.
                if (action != OfferAction.STATE) {
                    try { val retryStart = SystemClock.elapsedRealtime(); applySnapshot(repository.execute(OfferAction.STATE), retryStart) }
                    catch (c: CancellationException) { throw c }
                    catch (_: Exception) { }
                }
                mutable.update { it.copy(error = message) }
            } finally { if (uid == requestUid) mutable.update { it.copy(busy = false) } }
        }
    }
}
