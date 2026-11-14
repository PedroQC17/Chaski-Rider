package com.example.chaskirider.ui.screens.home.components

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalContext
import com.google.android.gms.maps.model.MapStyleOptions
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.chaskirider.BuildConfig
import com.example.chaskirider.R
import com.example.chaskirider.ui.screens.home.HomeUiState
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*

@Composable
fun WorkMap(state: HomeUiState, bottomInset: androidx.compose.ui.unit.Dp,
    onZoneTap: (com.example.chaskirider.domain.orders.DemandZone) -> Unit = {}) {
    Log.d("WorkMap", "Rendering WorkMap. MAPS_CONFIGURED=${BuildConfig.MAPS_CONFIGURED}, isInspection=${LocalInspectionMode.current}")
    if (!BuildConfig.MAPS_CONFIGURED || LocalInspectionMode.current) {
        Box(Modifier.fillMaxSize().background(Color(0xFFF0F2EF)), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Place, null, tint = Color(0xFF829085), modifier = Modifier.size(36.dp))
                Spacer(Modifier.height(12.dp))
                Text(stringResource(R.string.home_map_pending), color = Color(0xFF59665D))
            }
        }
        return
    }
    val camera = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(-12.0464, -77.0428), 12f)
    }
    var loaded by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val mapStyle = remember(context) { MapStyleOptions.loadRawResourceStyle(context, R.raw.map_grayscale) }
    val riderIcon = remember(loaded, context) { if (loaded) com.example.chaskirider.ui.components.riderMapMarker(context) else null }
    LaunchedEffect(loaded, state.cameraRequest, state.location) {
        val point = state.location
        Log.d("WorkMap", "LaunchedEffect: loaded=$loaded, location=$point, cameraRequest=${state.cameraRequest}")
        if (loaded && point != null) {
            Log.d("WorkMap", "Animating camera to: ${point.latitude}, ${point.longitude}")
            camera.animate(
                CameraUpdateFactory.newLatLngZoom(LatLng(point.latitude, point.longitude), 16f))
        }
    }
    GoogleMap(
        modifier = Modifier.fillMaxSize(), cameraPositionState = camera,
        properties = MapProperties(mapStyleOptions = mapStyle),
        uiSettings = MapUiSettings(zoomControlsEnabled = false, myLocationButtonEnabled = false,
            mapToolbarEnabled = false, compassEnabled = false, rotationGesturesEnabled = false),
        contentPadding = PaddingValues(top = 96.dp, bottom = bottomInset),
        onMapLoaded = {
            Log.d("WorkMap", "GoogleMap onMapLoaded triggered")
            loaded = true
        }
    ) {
        LaunchedEffect(Unit) { loaded = true }
        state.location?.takeIf { state.hasLocationPermission }?.let { point ->
            Marker(state = rememberUpdatedMarkerState(LatLng(point.latitude, point.longitude)),
                title = stringResource(R.string.orders_rider), icon = riderIcon,
                anchor = androidx.compose.ui.geometry.Offset(0.5f, 0.5f), zIndex = 1f)
        }
        if (state.user?.isAvailable == true) {
            state.demandZones.forEach { zone ->
                val current = demandColor(zone.level, forecast = false)
                val forecast = demandColor(zone.forecastLevel, forecast = true)
                val fill = current ?: forecast ?: return@forEach
                if (zone.radiusMeters <= 0) return@forEach
                val dy = zone.radiusMeters / 111_320.0
                val dx = dy / kotlin.math.cos(Math.toRadians(zone.latitude))
                Polygon(
                    points = listOf(
                        LatLng(zone.latitude + dy, zone.longitude),
                        LatLng(zone.latitude, zone.longitude + dx),
                        LatLng(zone.latitude - dy, zone.longitude),
                        LatLng(zone.latitude, zone.longitude - dx)
                    ),
                    fillColor = fill.copy(alpha = 0.55f),
                    strokeColor = forecast ?: fill,
                    strokeWidth = if (state.selectedZone?.id == zone.id) 7f else 4f,
                    clickable = true,
                    onClick = { onZoneTap(zone) }
                )
            }
        }
    }
}

private fun demandColor(level: String?, forecast: Boolean): Color? = when (level) {
    "HIGH" -> if (forecast) Color(0xFF1243A3) else Color(0xFFB11226)
    "MEDIUM" -> if (forecast) Color(0xFF4285D5) else Color(0xFFE35353)
    "LOW" -> if (forecast) Color(0xFF94C4F3) else Color(0xFFF6AAAA)
    else -> null
}
