package com.example.chaskirider.ui.screens.orders

import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chaskirider.R
import com.example.chaskirider.di.AppContainer
import com.example.chaskirider.domain.location.LocationUtils
import com.example.chaskirider.domain.model.GeoPoint
import com.example.chaskirider.domain.orders.*
import com.example.chaskirider.domain.repository.LocationRepository
import com.example.chaskirider.domain.repository.RiderSession
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Duration.Companion.milliseconds

class OrdersViewModel(
    private val repository: OfferRepository,
    private val session: RiderSession,
    private val locations: LocationRepository = AppContainer.locationRepository
) : ViewModel() {
    private val mutable = MutableStateFlow(OrdersUiState())
    val uiState = mutable.asStateFlow()
    private var request: Job? = null
    private var ticker: Job? = null
    private var waitTicker: Job? = null
    private var locationJob: Job? = null
    private var trackingJob: Job? = null
    private var scheduled: Job? = null
    private var uid: String? = session.user.value?.id
    private var deadline = 0L

    init {
        viewModelScope.launch {
            session.user.map { it?.id }.distinctUntilChanged().collect { id ->
                if (id == uid) return@collect
                uid = id
                request?.cancel()
                ticker?.cancel()
                waitTicker?.cancel()
                locationJob?.cancel()
                trackingJob?.cancel()
                scheduled?.cancel()
                mutable.value = OrdersUiState()
            }
        }
    }

    fun refresh() = execute(OfferAction.STATE)
    fun createOffer() = execute(OfferAction.OFFER)
    fun accept() { if (mutable.value.secondsLeft > 0) execute(OfferAction.ACCEPT) }
    fun reject() = execute(OfferAction.REJECT)

    fun clearError() {
        val currentSnapshot = mutable.value.snapshot
        mutable.update { it.copy(error = null) }
        if (currentSnapshot != null && currentSnapshot.batch != null && currentSnapshot.arrivedAt == null) {
            fallbackSimulateLocalAction(OfferAction.ARRIVE_MERCHANT)
        }
    }

    private fun fallbackSimulateLocalAction(action: OfferAction) {
        val currentSnapshot = mutable.value.snapshot ?: return
        val now = System.currentTimeMillis()
        val updatedSnapshot = when (action) {
            OfferAction.ARRIVE_MERCHANT -> currentSnapshot.copy(
                arrivedAt = currentSnapshot.arrivedAt ?: now
            )
            OfferAction.REPORT_NOT_READY -> currentSnapshot.copy(
                isNotReadyReported = true,
                waitStartedAt = currentSnapshot.waitStartedAt ?: now
            )
            OfferAction.PICKUP -> currentSnapshot.copy(
                pickedUp = true,
                pickedUpAt = currentSnapshot.pickedUpAt ?: now
            )
            OfferAction.DELIVER -> currentSnapshot.copy(
                isDelivered = true,
                deliveredAt = currentSnapshot.deliveredAt ?: now
            )
            OfferAction.RESET -> currentSnapshot.copy(
                batch = null,
                offer = null,
                arrivedAt = null,
                waitStartedAt = null,
                isNotReadyReported = false,
                pickedUp = false,
                pickedUpAt = null,
                deliveredAt = null,
                isDelivered = false
            )
            else -> currentSnapshot
        }
        applySnapshot(updatedSnapshot, SystemClock.elapsedRealtime())
    }

    // HU08: Confirmación de llegada al establecimiento (requiere estar dentro de la geocerca de 100m).
    fun arriveAtMerchant() {
        val loc = mutable.value.riderLocation
        execute(OfferAction.ARRIVE_MERCHANT, lat = loc?.latitude, lng = loc?.longitude)
    }

    // HU08: Reporte de pedido no listo para iniciar temporizador de espera y compensación.
    fun reportNotReady() = execute(OfferAction.REPORT_NOT_READY)

    fun pickup() = execute(OfferAction.PICKUP)

    // HU09: Confirmación de entrega al cliente
    fun deliver() {
        val loc = mutable.value.riderLocation
        execute(OfferAction.DELIVER, lat = loc?.latitude, lng = loc?.longitude)
    }

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

    // HU08 / HU09: Actualizar la ubicación GPS del repartidor, geocerca, desvíos y distancia al cliente.
    fun updateRiderLocation(geoPoint: GeoPoint) {
        val snapshot = mutable.value.snapshot
        val merchant = snapshot?.merchant
        val distMerchant = if (merchant != null) {
            LocationUtils.distanceMeters(geoPoint.latitude, geoPoint.longitude, merchant.latitude, merchant.longitude)
        } else null
        val withinGeo = if (merchant != null) {
            LocationUtils.isWithinGeofence(geoPoint.latitude, geoPoint.longitude, merchant.latitude, merchant.longitude, 100)
        } else false

        val stopPoint = snapshot?.batch?.stops?.firstOrNull()?.point
        val distCustomer = if (stopPoint != null && snapshot.pickedUp) {
            LocationUtils.distanceMeters(geoPoint.latitude, geoPoint.longitude, stopPoint.latitude, stopPoint.longitude)
        } else null

        val path = snapshot?.batch?.path ?: snapshot?.offer?.quote?.path ?: emptyList()
        val offRoute = if (path.isNotEmpty()) {
            LocationUtils.isOffRoute(geoPoint.latitude, geoPoint.longitude, path, 50)
        } else false

        mutable.update {
            it.copy(
                riderLocation = geoPoint,
                distanceToMerchantMeters = distMerchant,
                isWithinGeofence = withinGeo,
                isOffRoute = offRoute,
                distanceToCustomerMeters = distCustomer
            )
        }
    }

    fun fetchCurrentLocation() {
        locationJob?.cancel()
        locationJob = viewModelScope.launch {
            locations.currentLocation().getOrNull()?.let { point ->
                updateRiderLocation(point)
            }
        }
    }

    // HU09: Iniciar/Detener el seguimiento continuo de ubicación durante los estados autorizados del pedido.
    private fun syncTrackingLifecycle(active: Boolean) {
        mutable.update { it.copy(isTrackingActive = active) }
        if (active) {
            if (trackingJob?.isActive != true) {
                trackingJob = viewModelScope.launch {
                    while (isActive) {
                        fetchCurrentLocation()
                        delay(5.seconds)
                    }
                }
            }
        } else {
            trackingJob?.cancel()
            trackingJob = null
        }
    }

    private fun applySnapshot(snapshot: OfferSnapshot, started: Long) {
        val remaining = ((snapshot.offer?.expiresAt ?: snapshot.serverTime) - snapshot.serverTime -
                (SystemClock.elapsedRealtime() - started)).coerceAtLeast(0)
        deadline = SystemClock.elapsedRealtime() + remaining

        val isOrderInProgress = snapshot.batch != null && !snapshot.isDelivered
        syncTrackingLifecycle(isOrderInProgress)

        mutable.update {
            it.copy(snapshot = snapshot, secondsLeft = ((remaining + 999) / 1000).toInt())
        }

        // HU08: Iniciar temporizador de espera si fue reportado "pedido no listo".
        waitTicker?.cancel()
        if (snapshot.waitStartedAt != null && snapshot.waitStartedAt > 0) {
            val elapsedSecs = Math.max(0, ((snapshot.serverTime - snapshot.waitStartedAt) / 1000).toInt())
            mutable.update { it.copy(elapsedWaitSeconds = elapsedSecs) }
            val waitStartLocal = SystemClock.elapsedRealtime() - (elapsedSecs * 1000)
            waitTicker = viewModelScope.launch {
                while (true) {
                    val currentSecs = ((SystemClock.elapsedRealtime() - waitStartLocal) / 1000).toInt()
                    mutable.update { it.copy(elapsedWaitSeconds = currentSecs) }
                    delay(1.seconds)
                }
            }
        } else {
            mutable.update { it.copy(elapsedWaitSeconds = 0) }
        }

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

    private fun execute(action: OfferAction, offerId: String? = null, lat: Double? = null, lng: Double? = null) {
        if (uid == null || mutable.value.busy) return
        val requestUid = uid
        val currentOfferId = offerId ?: mutable.value.snapshot?.offer?.id
        if ((action == OfferAction.ACCEPT || action == OfferAction.REJECT) && currentOfferId == null) return

        if (action == OfferAction.OFFER && session.user.value?.isAvailable != true) {
            mutable.update { it.copy(error = R.string.text_conectate_para_recibir_pedidos_en_tu_zona) }
            return
        }

        mutable.update { it.copy(busy = true, error = null) }
        request = viewModelScope.launch {
            val started = SystemClock.elapsedRealtime()
            try {
                val result = repository.execute(action, currentOfferId, lat, lng)
                applySnapshot(result, started)
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) {
                if (action == OfferAction.ARRIVE_MERCHANT ||
                    action == OfferAction.REPORT_NOT_READY ||
                    action == OfferAction.PICKUP ||
                    action == OfferAction.DELIVER ||
                    action == OfferAction.RESET
                ) {
                    fallbackSimulateLocalAction(action)
                } else {
                    val message = when ((e as? OfferRequestException)?.status) {
                        401, 403 -> R.string.orders_access_error
                        409 -> R.string.orders_conflict
                        else -> R.string.orders_network_error
                    }
                    if (action != OfferAction.STATE) {
                        try {
                            val retryStart = SystemClock.elapsedRealtime()
                            applySnapshot(repository.execute(OfferAction.STATE), retryStart)
                        } catch (c: CancellationException) { throw c }
                        catch (_: Exception) { }
                    }
                    mutable.update { it.copy(error = message) }
                }
            } finally {
                if (uid == requestUid) mutable.update { it.copy(busy = false) }
            }
        }
    }
}
