package com.example.chaskirider.feature.orders.ui.viewmodel

import com.example.chaskirider.feature.orders.domain.OrderProblemReason
import com.example.chaskirider.feature.orders.domain.OrderSyncState
import android.net.Uri
import com.example.chaskirider.core.platform.location.GeoPoint
import com.example.chaskirider.feature.orders.domain.OfferSnapshot

data class OrdersUiState(
    val snapshot: OfferSnapshot? = null,
    val busy: Boolean = false,
    val deliveryPin: String = "",
    val problemDialogOpen: Boolean = false,
    val problemReason: com.example.chaskirider.feature.orders.domain.OrderProblemReason? = null,
    val problemDetail: String = "",
    val secondsLeft: Int = 0,
    val error: Int? = null,
    val isAvailable: Boolean = false,
    val searched: Boolean = false,
    // HU08: estado de geocerca, ubicación GPS y tiempo de espera
    val riderLocation: GeoPoint? = null,
    val distanceToMerchantMeters: Int? = null,
    val isWithinGeofence: Boolean = false,
    val isNearCustomer: Boolean = false,
    val elapsedWaitSeconds: Int = 0,
    // HU09: tracking hacia el cliente y entrega
    val distanceToCustomerMeters: Int? = null,
    val isTrackingActive: Boolean = false,
    // HU11: foto de evidencia
    val pendingCaptureUri: Uri? = null,
    val evidencePhotoUri: Uri? = null,
    val deliveryNote: String? = null,
    val isUploadingEvidence: Boolean = false,
    val sync: com.example.chaskirider.feature.orders.domain.OrderSyncState = com.example.chaskirider.feature.orders.domain.OrderSyncState(),

)
