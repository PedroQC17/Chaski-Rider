package com.example.chaskirider.ui.screens.orders

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import com.example.chaskirider.BuildConfig
import com.example.chaskirider.R
import com.example.chaskirider.domain.model.GeoPoint
import com.example.chaskirider.domain.orders.OfferSnapshot
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.*
import com.google.maps.android.compose.*

@Composable
internal fun OrdersMap(snapshot: OfferSnapshot?, riderLocation: GeoPoint?, bottomInset: Dp) {
    if (!BuildConfig.MAPS_CONFIGURED) { Text(stringResource(R.string.orders_map_missing)); return }
    val quote = snapshot?.offer?.quote ?: snapshot?.batch
    val camera = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(-12.0464, -77.0428), 14f)
    }
    var loaded by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val style = remember(context) { MapStyleOptions.loadRawResourceStyle(context, R.raw.map_grayscale) }
    val pickupIcon = remember(loaded) { if (loaded) numberMarker("1", 0xFFFF5722.toInt()) else null }
    val deliveryIcon = remember(loaded) { if (loaded) numberMarker("2", 0xFF263238.toInt()) else null }
    val riderIcon = remember(loaded, context) { if (loaded) com.example.chaskirider.ui.components.riderMapMarker(context) else null }
    val pickup = snapshot?.merchant?.takeIf { quote != null }?.let { LatLng(it.latitude, it.longitude) }
    val destinations = quote?.stops?.map { LatLng(it.point.latitude, it.point.longitude) }.orEmpty()
    val points = listOfNotNull(pickup) + destinations
    LaunchedEffect(loaded, points, bottomInset) {
        if (!loaded) return@LaunchedEffect
        if (points.distinct().size > 1) camera.move(CameraUpdateFactory.newLatLngBounds(
            LatLngBounds.builder().apply { points.forEach { include(it) } }.build(), 90))
        else {
            val center = points.firstOrNull() ?: riderLocation?.let { LatLng(it.latitude, it.longitude) } ?: LatLng(-12.0464, -77.0428)
            camera.move(CameraUpdateFactory.newLatLngZoom(center, 15f))
        }
    }
    LaunchedEffect(loaded, riderLocation) {
        if (loaded && points.isEmpty() && riderLocation != null)
            camera.animate(CameraUpdateFactory.newLatLngZoom(LatLng(riderLocation.latitude, riderLocation.longitude), 15f))
    }
    GoogleMap(Modifier.fillMaxSize(), cameraPositionState = camera, onMapLoaded = { loaded = true },
        properties = MapProperties(mapStyleOptions = style),
        contentPadding = PaddingValues(top = 144.dp, bottom = bottomInset),
        uiSettings = MapUiSettings(zoomControlsEnabled = false, mapToolbarEnabled = false, compassEnabled = false)) {
        LaunchedEffect(Unit) { loaded = true }
        pickup?.let { Marker(state = rememberUpdatedMarkerState(it), icon = pickupIcon, zIndex = 2f,
            title = "1 · Recojo · ${snapshot?.merchantName.orEmpty()}", snippet = snapshot?.merchantAddress) }
        quote?.stops?.forEach { stop -> key(stop.id) {
            Marker(state = rememberUpdatedMarkerState(LatLng(stop.point.latitude, stop.point.longitude)),
                icon = deliveryIcon, zIndex = 2f, title = "2 · Entrega · ${stop.zone.orEmpty()}", snippet = stop.address)
        } }
        riderLocation?.let {
            Marker(state = rememberUpdatedMarkerState(LatLng(it.latitude, it.longitude)),
                title = stringResource(R.string.orders_rider),
                anchor = androidx.compose.ui.geometry.Offset(0.5f, 0.5f), zIndex = 1f,
                icon = riderIcon)
        }
    }
}

private fun numberMarker(label: String, color: Int): BitmapDescriptor {
    val bitmap = Bitmap.createBitmap(80, 90, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    paint.color = android.graphics.Color.WHITE; canvas.drawCircle(40f, 40f, 36f, paint)
    paint.color = color; canvas.drawCircle(40f, 40f, 31f, paint)
    paint.strokeWidth = 8f; canvas.drawLine(40f, 68f, 40f, 86f, paint)
    paint.color = android.graphics.Color.WHITE; paint.textSize = 38f; paint.textAlign = Paint.Align.CENTER
    paint.isFakeBoldText = true; canvas.drawText(label, 40f, 53f, paint)
    return BitmapDescriptorFactory.fromBitmap(bitmap)
}
