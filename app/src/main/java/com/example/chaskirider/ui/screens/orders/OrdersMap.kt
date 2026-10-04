package com.example.chaskirider.ui.screens.orders

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.chaskirider.BuildConfig
import com.example.chaskirider.R
import com.example.chaskirider.domain.orders.OfferSnapshot
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.*

@Composable
internal fun OrdersMap(snapshot: OfferSnapshot?) {
    val quote = snapshot?.offer?.quote ?: snapshot?.batch
    val camera = rememberCameraPositionState()
    var loaded by remember { mutableStateOf(false) }
    val points = quote?.path?.map { LatLng(it.latitude, it.longitude) }.orEmpty()
    LaunchedEffect(loaded, points) {
        if (!loaded) return@LaunchedEffect
        if (points.size > 1) camera.animate(CameraUpdateFactory.newLatLngBounds(
            LatLngBounds.builder().apply { points.forEach { include(it) } }.build(), 70))
        else camera.move(CameraUpdateFactory.newLatLngZoom(LatLng(-12.0464, -77.0428), 13f))
    }
    if (BuildConfig.MAPS_CONFIGURED) GoogleMap(Modifier.fillMaxSize(), cameraPositionState = camera,
        onMapLoaded = { loaded = true }, contentPadding = PaddingValues(top = 72.dp, bottom = 330.dp),
        uiSettings = MapUiSettings(zoomControlsEnabled = false, mapToolbarEnabled = false)) {
        val merchantTitle = snapshot?.merchantName?.let { name ->
            snapshot?.merchantZone?.let { zone -> "$name · $zone" } ?: name
        } ?: stringResource(R.string.orders_store)
        snapshot?.merchant?.let { Marker(state = rememberUpdatedMarkerState(LatLng(it.latitude, it.longitude)),
            title = merchantTitle) }
        quote?.path?.firstOrNull()?.let { Marker(state = rememberUpdatedMarkerState(LatLng(it.latitude, it.longitude)),
            title = stringResource(R.string.orders_rider)) }
        quote?.stops?.forEach { stop -> key(stop.id) {
            Marker(state = rememberUpdatedMarkerState(LatLng(stop.point.latitude, stop.point.longitude)),
                title = stop.zone?.takeIf(String::isNotBlank)?.let { zone ->
                    stringResource(R.string.orders_delivery_zone, stop.id.uppercase(), zone) }
                    ?: stringResource(R.string.orders_delivery_marker, stop.id.uppercase()))
        } }
        // Línea de ejemplo, nunca presentada como navegación real por calles.
        if (points.size > 1) Polyline(points = points, color = Color(0xFFFF5722), width = 9f)
    } else Text(stringResource(R.string.orders_map_missing))
}
