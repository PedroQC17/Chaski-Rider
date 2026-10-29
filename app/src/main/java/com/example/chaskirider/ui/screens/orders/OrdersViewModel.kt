package com.example.chaskirider.ui.screens.orders

import android.content.Context
import android.net.Uri
import android.os.SystemClock
import androidx.core.content.FileProvider
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chaskirider.R
import com.example.chaskirider.di.AppContainer
import com.example.chaskirider.domain.location.LocationUtils
import com.example.chaskirider.domain.model.GeoPoint
import com.example.chaskirider.domain.model.RegistrationStatus
import com.example.chaskirider.domain.orders.*
import com.example.chaskirider.domain.repository.LocationRepository
import com.example.chaskirider.domain.repository.RiderSession
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.File

class OrdersViewModel(
    private val repository: OfferRepository,
    private val session: RiderSession,
    private val savedStateHandle: SavedStateHandle = SavedStateHandle(),
    private val locations: LocationRepository = AppContainer.locationRepository,
    private val automaticSearch: Boolean = true,
    private val elapsed: () -> Long = SystemClock::elapsedRealtime
) : ViewModel() {
    private fun savedUri(key: String) = savedStateHandle.get<String>(key)?.let(Uri::parse)

    private val mutable = MutableStateFlow(
        OrdersUiState(
            pendingCaptureUri = savedUri("pending_capture"),
            evidencePhotoUri = savedUri("evidence_photo"),
            deliveryNote = savedStateHandle.get<String>("delivery_note")
        )
    )
    val uiState = mutable.asStateFlow()
    private var request: Job? = null
    private var ticker: Job? = null
    private var search: Job? = null
    private var tracking: Job? = null
    private var locationJob: Job? = null
    private var uid: String? = session.user.value?.id
    private var foreground = false
    private var deadline = 0L
    private var locationConfirmedAt: Long? = null

    init {
        viewModelScope.launch {
            session.user.collect { user ->
                if (user?.id != uid || user == null || user.status != RegistrationStatus.APPROVED || !user.isEnabled) {
                    uid = user?.id
                    request?.cancel(); ticker?.cancel(); search?.cancel(); tracking?.cancel(); locationJob?.cancel()
                    mutable.value = OrdersUiState()
                    locationConfirmedAt = null
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
            search?.cancel(); tracking?.cancel(); ticker?.cancel(); locationJob?.cancel()
            invalidateLocation()
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
            delay(5_000)
            while (isActive) {
                val snapshot = mutable.value.snapshot
                if (snapshot == null) refresh()
                else if (snapshot.offer == null && snapshot.batch == null) createOffer()
                delay(20_000)
            }
        }
    }

    fun refresh() = execute(OfferAction.STATE)
    fun createOffer() = execute(OfferAction.OFFER)
    fun accept() { if (mutable.value.secondsLeft > 0) execute(OfferAction.ACCEPT) }
    fun reject() = execute(OfferAction.REJECT)
    fun clearError() { mutable.update { it.copy(error = null) } }
    fun arriveAtMerchant() = execute(OfferAction.ARRIVE_MERCHANT)
    fun reportNotReady() = execute(OfferAction.REPORT_NOT_READY)
    fun pickup() = execute(OfferAction.PICKUP)


    //funciones para la validacion de finalizacion de entrega
    fun prepareEvidenceCapture(context: Context): Uri {
        val dir = File(context.cacheDir, "captures").apply { mkdirs() }
        val tempFile = File(dir, "evidence_${System.currentTimeMillis()}.jpg")
        val contentUri = FileProvider.getUriForFile(context, "${context.packageName}.files", tempFile)
        savedStateHandle["pending_capture"] = contentUri.toString()
        mutable.update { it.copy(pendingCaptureUri = contentUri) }
        return contentUri
    }

    fun confirmEvidenceCapture(success: Boolean) {
        val pending = mutable.value.pendingCaptureUri ?: savedUri("pending_capture")
        if (success && pending != null) {
            savedStateHandle["evidence_photo"] = pending.toString()
            savedStateHandle.remove<String>("pending_capture")
            mutable.update { it.copy(evidencePhotoUri = pending, pendingCaptureUri = null) }
        } else {
            savedStateHandle.remove<String>("pending_capture")
            mutable.update { it.copy(pendingCaptureUri = null) }
        }
    }

    fun setEvidencePhoto(uri: Uri){
        savedStateHandle["evidence_photo"] = uri.toString()
        savedStateHandle.remove<String>("pending_capture")
        mutable.update{it.copy(evidencePhotoUri = uri, pendingCaptureUri = null)}
    }

    fun clearEvidencePhoto(){
        savedStateHandle.remove<String>("pending_capture")
        savedStateHandle.remove<String>("evidence_photo")
        mutable.update{it.copy(evidencePhotoUri = null, pendingCaptureUri = null)}
    }

    fun updateDeliveryNote(note: String){
        val trimmed = note.take(200)
        savedStateHandle["delivery_note"] = trimmed
        mutable.update{it.copy(deliveryNote = trimmed)}
    }
    fun deliver() = execute(

        action=OfferAction.DELIVER,
        evidenceUrl= mutable.value.evidencePhotoUri.toString(),
        deliveryNote= mutable.value.deliveryNote?.ifBlank { null },
    )
    fun reset() = execute(OfferAction.RESET)

    fun openProblem() { mutable.update { it.copy(problemDialogOpen = true, problemReason = null, problemDetail = "") } }
    fun closeProblem() { if (!mutable.value.busy) mutable.update { it.copy(problemDialogOpen = false) } }
    fun selectProblem(reason: OrderProblemReason) { mutable.update { it.copy(problemReason = reason) } }
    fun updateProblemDetail(detail: String) { mutable.update { it.copy(problemDetail = detail.take(500)) } }
    fun confirmProblem() {
        val state = mutable.value
        val reason = state.problemReason ?: return
        val snapshot = state.snapshot ?: return
        if (!canWork() || state.busy || snapshot.batch == null || snapshot.isDelivered || state.problemDetail.trim().length < 10) return
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

    fun updateRiderLocation(point: GeoPoint) {
        if (!point.latitude.isFinite() || !point.longitude.isFinite() || point.latitude !in -90.0..90.0 || point.longitude !in -180.0..180.0) {
            invalidateLocation(); return
        }
        locationConfirmedAt = elapsed()
        mutable.update { it.copy(riderLocation = point) }
        updateDistances()
    }

    private fun invalidateLocation() {
        locationConfirmedAt = null
        mutable.update { it.copy(distanceToMerchantMeters = null, distanceToCustomerMeters = null,
            isWithinGeofence = false, isNearCustomer = false) }
    }

    private fun updateDistances() {
        val point = mutable.value.riderLocation ?: return
        if (locationConfirmedAt?.let { elapsed() - it in 0..30_000 } != true) { invalidateLocation(); return }
        val snapshot = mutable.value.snapshot
        val active = snapshot?.offer != null || snapshot?.batch != null
        val merchant = snapshot?.merchant?.takeIf { active }
        val customer = snapshot?.batch?.stops?.firstOrNull()?.point
        val merchantDistance = merchant?.let { LocationUtils.distanceMeters(point.latitude, point.longitude, it.latitude, it.longitude) }
        val customerDistance = customer?.let { LocationUtils.distanceMeters(point.latitude, point.longitude, it.latitude, it.longitude) }
        mutable.update { it.copy(distanceToMerchantMeters = merchantDistance,
            distanceToCustomerMeters = customerDistance,
            isWithinGeofence = merchant?.let { target -> OrderLocationRules.isNear(point.latitude, point.longitude, target) } == true,
            isNearCustomer = customer?.let { target -> OrderLocationRules.isNear(point.latitude, point.longitude, target) } == true) }
    }

    fun fetchCurrentLocation() {
        val requestUid = uid
        locationJob?.cancel()
        locationJob = viewModelScope.launch {
            val point = locations.currentLocation().getOrNull()
            if (requestUid == uid) {
                if (point != null) updateRiderLocation(point) else invalidateLocation()
            }
        }
    }

    private fun syncTracking() {
        val active = foreground && canWork() && mutable.value.snapshot?.batch != null && mutable.value.snapshot?.isDelivered != true
        mutable.update { it.copy(isTrackingActive = active) }
        if (!active) { tracking?.cancel(); return }
        if (tracking?.isActive == true) return
        tracking = viewModelScope.launch {
            while (isActive) {
                val point = locations.currentLocation().getOrNull()
                if (point != null) updateRiderLocation(point) else invalidateLocation()
                delay(5_000)
            }
        }
    }

    private fun applySnapshot(snapshot: OfferSnapshot, started: Long) {
        val remaining = ((snapshot.offer?.expiresAt ?: snapshot.serverTime) - snapshot.serverTime - (elapsed() - started)).coerceAtLeast(0)
        deadline = elapsed() + remaining
        mutable.update { it.copy(snapshot = snapshot, secondsLeft = ((remaining + 999) / 1000).toInt()) }
        updateDistances()
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
        deliveryNote: String? = null,
    ): Job? {
        if (!canWork() || mutable.value.busy) return null
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
                    invalidateLocation()
                    mutable.update { it.copy(error = R.string.orders_location_required) }
                    return@launch
                }
                if (requestUid != uid || (action == OfferAction.OFFER && session.user.value?.isAvailable != true)) return@launch
                if (requiresLocation && point != null) updateRiderLocation(point)
                val started = elapsed()
                val snapshot = repository.execute(
                    action = action,
                    offerId = offerId,
                    lat = point?.latitude,
                    lng = point?.longitude,
                    evidenceUrl = evidenceUrl,
                    deliveryNote = deliveryNote
                )
                if (requestUid != uid) return@launch
                applySnapshot(snapshot, started)
                if (action == OfferAction.OFFER) mutable.update { it.copy(searched = true) }
            } catch (error: CancellationException) { throw error }
            catch (error: Exception) {
                if (requestUid == uid) mutable.update { it.copy(error = when ((error as? OfferRequestException)?.status) {
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
