package com.example.chaskirider.feature.orders.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chaskirider.feature.orders.domain.*
import com.example.chaskirider.core.platform.location.LocationRepository
import com.example.chaskirider.feature.user.domain.RiderSession
import com.example.chaskirider.feature.orders.domain.CustomerContact
import com.example.chaskirider.feature.orders.domain.CustomerContactStore
import com.example.chaskirider.feature.orders.domain.CustomerAbsenceService
import com.example.chaskirider.feature.orders.domain.PhoneCallState
import com.example.chaskirider.feature.orders.domain.PhoneCalls
import com.example.chaskirider.feature.orders.domain.CallAttemptDetector
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

class CustomerContactViewModel(
    private val store: CustomerContactStore,
    private val session: RiderSession,
    private val locations: LocationRepository,
    private val now: () -> Long = System::currentTimeMillis,
    private val elapsed: () -> Long = android.os.SystemClock::elapsedRealtime,
    private val boot: () -> Int,
    private val service: CustomerAbsenceService? = null
) : ViewModel() {
    private val mutable = MutableStateFlow(CustomerContactUiState())
    val uiState = mutable.asStateFlow()
    private var owner: String? = null
    private var order: String? = null
    private var load: Job? = null
    private var action: Job? = null
    private var target: RoutePoint? = null

    init {
        viewModelScope.launch {
            session.user.map { it?.id }.distinctUntilChanged().collect {
                if (it != owner) bind(null)
            }
        }
        viewModelScope.launch {
            while (isActive) {
                tick()
                delay(1000)
            }
        }
    }

    fun bind(snapshot: OfferSnapshot?) {
        val uid = session.user.value?.id
        val stop = snapshot?.batch?.stops?.singleOrNull()
            ?.takeIf { snapshot.pickedUp && !snapshot.isCompleted }
        if (owner == uid && order == stop?.orderId && mutable.value.error == null) return
        load?.cancel(); action?.cancel()
        owner = uid; order = stop?.orderId; target = stop?.point
        mutable.value = CustomerContactUiState(loading = uid != null && stop != null)
        if (uid == null || stop == null) return
        load = viewModelScope.launch {
            try {
                val record = store.load(uid, stop.orderId)?.takeIf { service == null || it.serverRegistered }
                if (matches(uid, stop.orderId)) {
                    mutable.value = CustomerContactUiState(contact = record, loading = false)
                    tick()
                }
            } catch (error: CancellationException) { throw error }
            catch (_: Exception) { if (matches(uid, stop.orderId)) mutable.update {
                it.copy(loading = false, error = "No se pudo recuperar la espera. Cierra y vuelve a abrir el pedido.")
            } }
        }
    }

    private fun matches(uid: String, id: String) = owner == uid && order == id && session.user.value?.id == uid
    private fun tick() {
        val progress = mutable.value.contact ?: return
        mutable.update { it.copy(waitedMillis = progress.elapsedMillis(now(), elapsed(), boot())) }
    }

    private fun perform(block: suspend (String, String) -> Unit) {
        val uid = owner ?: return
        val id = order ?: return
        if (!matches(uid, id) || mutable.value.busy || mutable.value.loading) return
        mutable.update { it.copy(busy = true, error = null) }
        action = viewModelScope.launch {
            try { block(uid, id) }
            catch (error: CancellationException) { throw error }
            catch (error: Exception) { if (matches(uid, id)) mutable.update {
                it.copy(error = if (error is OfferRequestException && error.status == 412)
                    "Necesitas estar a menos de 100 metros del destino para registrar tu llegada."
                else if (error is com.google.firebase.functions.FirebaseFunctionsException &&
                    error.code == com.google.firebase.functions.FirebaseFunctionsException.Code.FAILED_PRECONDITION)
                    error.message ?: "Sincroniza el pedido y revisa los pasos pendientes."
                else "No se pudo registrar el paso. Revisa la ubicaciÃ³n, los permisos y la conexiÃ³n telefÃ³nica, y vuelve a intentar.")
            } }
            finally { if (matches(uid, id)) mutable.update { it.copy(busy = false) } }
        }
    }

    private suspend fun save(uid: String, id: String, contact: CustomerContact) {
        if (!matches(uid, id)) return
        store.save(uid, id, contact)
        if (matches(uid, id)) { mutable.update { it.copy(contact = contact) }; tick() }
    }

    fun arrive() = perform { uid, id ->
        if (mutable.value.contact != null) return@perform
        val point = locations.currentLocation().getOrThrow() ?: error("UbicaciÃ³n no disponible")
        val destination = target ?: return@perform
        OrderPolicy.requireNear(point.latitude, point.longitude, destination)
        service?.arrive(uid, id, point.latitude, point.longitude)
        save(uid, id, CustomerContact(arrivedAt = now(), serverRegistered = service != null))
    }

    fun call(calls: PhoneCalls, phone: String) = perform { uid, id ->
        val record = mutable.value.contact ?: return@perform
        val waited = record.elapsedMillis(now(), elapsed(), boot())
        if (!record.canCallAgain(waited)) return@perform
        coroutineScope {
            val detector = CallAttemptDetector()
            val ready = CompletableDeferred<Unit>()
            val observed = CompletableDeferred<Unit>()
            val listener = launch {
                calls.states().collect { state ->
                    if (!ready.isCompleted) {
                        check(state == PhoneCallState.IDLE) { "Ya hay una llamada en curso" }
                        detector.onState(state)
                        ready.complete(Unit)
                    } else if (detector.onState(state)) observed.complete(Unit)
                }
            }
            try {
                withTimeout(5000) { ready.await() }
                check(matches(uid, id))
                check(detector.arm())
                calls.start(phone)
                withTimeout(35_000) { observed.await() }
                val checkpoint = service?.call(uid, id, if (record.firstCallAt == null) 1 else 2)
                val next = if (record.firstCallAt == null) record.copy(
                    firstCallAt = checkpoint?.firstCallAt ?: now(), firstCallElapsed = elapsed(), boot = boot(), phone = phone)
                else record.copy(secondCallAt = checkpoint?.secondCallAt ?: now())
                save(uid, id, next)
            } finally { listener.cancelAndJoin() }
        }
    }

    fun acknowledgeReminder() = perform { uid, id ->
        val contact = mutable.value.contact ?: return@perform
        save(uid, id, contact.copy(reminderSeen = true))
    }

}
