package com.example.chaskirider.feature.orders.ui.viewmodel

import android.content.Context
import android.net.Uri
import android.os.SystemClock
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chaskirider.R
import com.example.chaskirider.di.AppContainer
import com.example.chaskirider.core.platform.location.GeoPoint
import com.example.chaskirider.feature.user.domain.RegistrationStatus
import com.example.chaskirider.feature.orders.domain.*
import com.example.chaskirider.core.platform.location.LocationRepository
import com.example.chaskirider.feature.user.domain.RiderSession
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

class OrdersViewModel(
    private val repository: OfferRepository,
    private val session: RiderSession,
    private val savedStateHandle: SavedStateHandle = SavedStateHandle(),
    private val locations: LocationRepository = AppContainer.locationRepository,
    private val automaticSearch: Boolean = true,
    private val elapsed: () -> Long = SystemClock::elapsedRealtime,
    private val synchronization: OrderSyncRepository? = null,
    private val searchSignals: Flow<Unit> = AppContainer.orderChanges
) : ViewModel() {
    private val draft = DeliveryDraft(savedStateHandle)

    init {
        draft.ensureOwner(session.user.value?.id)
    }

    private val mutable = MutableStateFlow(
        OrdersUiState(
            pendingCaptureUri = draft.pendingUri(),
            evidencePhotoUri = draft.evidenceUri(),
            deliveryNote = draft.note()
        )
    )
    val uiState = mutable.asStateFlow()
    private val riderLocation = RiderLocationTracker(viewModelScope, locations, elapsed, mutable)
    private var request: Job? = null
    private var ticker: Job? = null
    private var search: Job? = null
    private var uid: String? = session.user.value?.id
    private var foreground = false
    private var deadline = 0L

    init {
        viewModelScope.launch {
            session.user.map { it?.id }.distinctUntilChanged().collectLatest { owner ->
                mutable.update { it.copy(sync = OrderSyncState()) }
                if (owner != null) synchronization?.observe(owner)?.collect { state ->
                    if (session.user.value?.id == owner) mutable.update { it.copy(sync = state) }
                }
            }
        }
        viewModelScope.launch {
            session.user.collect { user ->
                if (user?.id != uid || user == null || user.status != RegistrationStatus.APPROVED || !user.isEnabled) {
                    draft.clear()
                    uid = user?.id
                    request?.cancel(); ticker?.cancel(); search?.cancel()
                    riderLocation.stop()
                    mutable.value = OrdersUiState()
                }
                mutable.update { it.copy(isAvailable = user?.isAvailable == true) }
                if (foreground && canWork() && mutable.value.snapshot == null) refresh()
                syncSearch()
                syncTracking()
            }
        }
    }

    fun setForeground(active: Boolean) {
        foreground = active
        if (active) refresh() else {
            search?.cancel(); ticker?.cancel()
            riderLocation.stop()
        }
        syncSearch()
        syncTracking()
    }

    private fun canWork(): Boolean = session.user.value?.let {
        it.id == uid && it.status == RegistrationStatus.APPROVED && it.isEnabled && it.profilePhotoPath.isNotBlank()
    } == true

    private fun syncSearch() {
        if (!automaticSearch || !foreground || !canWork() || !mutable.value.isAvailable) { search?.cancel(); return }
        if (search?.isActive == true) return
        search = viewModelScope.launch {
            searchSignals.collect { if (foreground && canWork()) refresh() }
        }
    }

    fun refresh() = execute(OfferAction.STATE)
    fun createOffer() = execute(OfferAction.OFFER)
    fun accept() { if (mutable.value.secondsLeft > 0) execute(OfferAction.ACCEPT) }
    fun reject() = execute(OfferAction.REJECT)
    fun clearError() { mutable.update { it.copy(error = null) } }
    fun retrySync() {
        val owner = uid ?: return
        if (session.user.value?.id != owner || !mutable.value.sync.online || mutable.value.sync.syncing) return
        try { synchronization?.retry(owner) }
        catch (_: RuntimeException) { mutable.update { it.copy(error = R.string.orders_sync_retry_error) } }
    }
    fun arriveAtMerchant() = execute(OfferAction.ARRIVE_MERCHANT)
    fun reportNotReady() = execute(OfferAction.REPORT_NOT_READY)
    fun pickup() = execute(OfferAction.PICKUP)


    //funciones para la validacion de finalizacion de entrega
    fun prepareEvidenceCapture(context: Context): Uri {
        val uri = draft.newCapture(context)
        mutable.update { it.copy(pendingCaptureUri = uri) }
        return uri
    }

    fun confirmEvidenceCapture(success: Boolean) {
        val evidence = if (success) draft.confirmPending(mutable.value.pendingCaptureUri) else null
        if (!success) draft.discardPending()
        mutable.update {
            if (evidence != null) it.copy(evidencePhotoUri = evidence, pendingCaptureUri = null)
            else it.copy(pendingCaptureUri = null)
        }
    }

    fun setEvidencePhoto(uri: Uri) {
        draft.saveEvidence(uri)
        mutable.update { it.copy(evidencePhotoUri = uri, pendingCaptureUri = null) }
    }

    fun clearEvidencePhoto() {
        draft.clearEvidence()
        mutable.update { it.copy(evidencePhotoUri = null, pendingCaptureUri = null) }
    }

    fun updateDeliveryNote(note: String) {
        val owner = uid ?: return
        val order = mutable.value.snapshot?.batch?.stops?.singleOrNull()?.orderId ?: return
        val trimmed = note.take(200)
        draft.saveNote(owner, order, trimmed)
        mutable.update { it.copy(deliveryNote = trimmed) }
    }
    fun updateDeliveryPin(value: String) { mutable.update { it.copy(deliveryPin = value.filter { c -> c in '0'..'9' }.take(4)) } }
    fun deliver() = execute(

        action=OfferAction.DELIVER,
        deliveryPin = mutable.value.deliveryPin,
        deliveryNote= mutable.value.deliveryNote?.ifBlank { null },
    )
    fun reset(): Job? { clearEvidencePhoto(); updateDeliveryPin(""); updateDeliveryNote(""); return execute(OfferAction.RESET) }

    fun openProblem() { mutable.update { it.copy(problemDialogOpen = true, problemReason = null, problemDetail = "") } }
    fun closeProblem() { if (!mutable.value.busy) mutable.update { it.copy(problemDialogOpen = false) } }
    fun selectProblem(reason: OrderProblemReason) { mutable.update { it.copy(problemReason = reason) } }
    fun updateProblemDetail(detail: String) { mutable.update { it.copy(problemDetail = detail.take(500)) } }
    fun confirmProblem() {
        val state = mutable.value
        val reason = state.problemReason ?: return
        val snapshot = state.snapshot ?: return
        if (!canWork() || state.busy || snapshot.batch == null || snapshot.isCompleted || state.problemDetail.trim().length < 10) return
        val requestUid = uid
        mutable.update { it.copy(busy = true, error = null) }
        request = viewModelScope.launch {
            try {
                val started = elapsed()
                val result = repository.reportProblem(reason, state.problemDetail, cancel = !snapshot.pickedUp)
                if (requestUid == uid) {
                    applySnapshot(result, started)
                    mutable.update { it.copy(problemDialogOpen = false, problemReason = null, problemDetail = "") }
                }
            } catch (error: CancellationException) { throw error }
            catch (_: Exception) { if (requestUid == uid) mutable.update { it.copy(error = R.string.orders_simulation_error) } }
            finally { if (requestUid == uid) mutable.update { it.copy(busy = false) } }
        }
    }

    fun updateRiderLocation(point: GeoPoint) = riderLocation.update(point)

    fun fetchCurrentLocation() {
        val requestUid = uid
        riderLocation.fetch { requestUid == uid }
    }

    private fun syncTracking() {
        riderLocation.restartTracking(
            foreground && canWork() && mutable.value.snapshot?.batch != null && mutable.value.snapshot?.isCompleted != true
        )
    }

    private fun applySnapshot(snapshot: OfferSnapshot, started: Long) {
        val order = snapshot.batch?.stops?.singleOrNull()?.orderId
        if (draft.boundOrderId() != order) {
            draft.clear()
            mutable.update { it.copy(deliveryNote = null, evidencePhotoUri = null, pendingCaptureUri = null, deliveryPin = "") }
        }
        if (order != null) {
            draft.bind(uid, order)
        }
        val remaining = ((snapshot.offer?.expiresAt ?: snapshot.serverTime) - snapshot.serverTime - (elapsed() - started)).coerceAtLeast(0)
        deadline = elapsed() + remaining
        mutable.update { it.copy(snapshot = snapshot, secondsLeft = ((remaining + 999) / 1000).toInt()) }
        riderLocation.recomputeDistances()
        syncTracking()
        ticker?.cancel()
        val waitStart = snapshot.waitStartedAt
        if (foreground && (snapshot.offer != null || (waitStart != null && !snapshot.pickedUp))) {
            ticker = viewModelScope.launch {
                while (isActive) {
                    val left = ((deadline - elapsed()).coerceAtLeast(0) + 999) / 1000
                    val wait = if (waitStart != null) ((snapshot.serverTime - waitStart + elapsed() - started) / 1000).coerceAtLeast(0).toInt() else 0
                    mutable.update { it.copy(secondsLeft = left.toInt(), elapsedWaitSeconds = wait) }
                    if (snapshot.offer != null && left == 0L && !mutable.value.busy) { refresh(); break }
                    delay(250)
                }
            }
        } else mutable.update { it.copy(elapsedWaitSeconds = 0) }
    }

    private fun execute(
        action: OfferAction,
        evidenceUrl: String? = null,
        deliveryPin: String? = null,
        deliveryNote: String? = null,
    ): Job? {
        if (!canWork() || mutable.value.busy) return null
        if (action == OfferAction.DELIVER && mutable.value.snapshot?.paidByCustomer == true && !mutable.value.sync.online) {
            mutable.update { it.copy(error = R.string.orders_pin_connection) }
            return null
        }
        val requestUid = uid
        val offerId = mutable.value.snapshot?.offer?.id
        if (action in listOf(OfferAction.ACCEPT, OfferAction.REJECT) && offerId == null) return null
        if (action in listOf(OfferAction.OFFER, OfferAction.ACCEPT) && !mutable.value.isAvailable) {
            mutable.update { it.copy(error = R.string.text_conectate_para_recibir_pedidos_en_tu_zona) }
            return null
        }
        mutable.update { it.copy(busy = true, error = null) }
        request = viewModelScope.launch {
            try {
                val requiresLocation = action in listOf(OfferAction.OFFER, OfferAction.ARRIVE_MERCHANT,
                    OfferAction.REPORT_NOT_READY, OfferAction.PICKUP, OfferAction.DELIVER)
                val point = if (requiresLocation) locations.currentLocation().getOrNull() else mutable.value.riderLocation
                if (requestUid != uid || !canWork()) return@launch
                if (requiresLocation && point == null) {
                    riderLocation.invalidate()
                    mutable.update { it.copy(error = R.string.orders_location_required) }
                    return@launch
                }
                if (requestUid != uid || (action == OfferAction.OFFER && session.user.value?.isAvailable != true)) return@launch
                if (requiresLocation && point != null) riderLocation.update(point)
                val started = elapsed()
                val snapshot = repository.execute(
                    action = action,
                    offerId = offerId,
                    lat = point?.latitude,
                    lng = point?.longitude,
                    evidenceUrl = evidenceUrl,
                    deliveryNote = deliveryNote,
                    deliveryPin = deliveryPin
                )
                if (requestUid != uid) return@launch
                if (action in listOf(OfferAction.ACCEPT, OfferAction.DELIVER)) { clearEvidencePhoto(); updateDeliveryPin(""); updateDeliveryNote("") }
                applySnapshot(snapshot, started)
                if (action == OfferAction.OFFER) mutable.update { it.copy(searched = true) }
            } catch (error: CancellationException) { throw error }
            catch (error: Exception) {
                if (requestUid == uid) mutable.update { it.copy(error = when ((error as? OfferRequestException)?.status) {
                    428 -> R.string.orders_pin_invalid
                    425 -> R.string.orders_pin_sync_required
                    503 -> R.string.orders_pin_connection
                    429 -> R.string.orders_pin_wait
                    401, 403 -> R.string.orders_access_error
                    409 -> R.string.orders_conflict
                    412 -> if (action == OfferAction.DELIVER) R.string.orders_near_delivery else R.string.orders_near_pickup
                    422 -> R.string.orders_location_required
                    else -> R.string.orders_simulation_error
                }) }
            } finally {
                if (requestUid == uid) mutable.update { it.copy(busy = false) }
            }
        }
        return request
    }
}
