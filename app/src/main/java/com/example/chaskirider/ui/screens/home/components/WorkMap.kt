package com.example.chaskirider.ui.screens.home.components

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
    LaunchedEffect(loaded, state.cameraRequest, state.location) {
        val point = state.location
        if (loaded && point != null) camera.animate(
            CameraUpdateFactory.newLatLngZoom(LatLng(point.latitude, point.longitude), 16f))
    }
    GoogleMap(
        modifier = Modifier.fillMaxSize(), cameraPositionState = camera,
        properties = MapProperties(isMyLocationEnabled = state.hasLocationPermission && state.user?.isAvailable == true),
        uiSettings = MapUiSettings(zoomControlsEnabled = false, myLocationButtonEnabled = false,
            mapToolbarEnabled = false, compassEnabled = false, rotationGesturesEnabled = false),
        contentPadding = PaddingValues(top = 96.dp, bottom = bottomInset),
        onMapLoaded = { loaded = true }
    ) {
        // HU05: capa de zonas de demanda; al tocar una zona se selecciona.
        state.demandZones.forEach { zone ->
            val selected = state.selectedZone?.id == zone.id
            val color = demandLevelColor(zone.level)
            Circle(
                center = LatLng(zone.latitude, zone.longitude),
                radius = zone.radiusMeters.toDouble(),
                strokeColor = color,
                fillColor = color.copy(alpha = if (selected) 0.35f else 0.15f),
                strokeWidth = if (selected) 5f else 3f,
                clickable = true,
                onClick = { onZoneTap(zone); true }
            )
        }
    }
}

private fun demandLevelColor(level: String) = when (level) {
    "HIGH" -> Color(0xFFE53935)
    "MEDIUM" -> Color(0xFFFB8C00)
    "LOW" -> Color(0xFF43A047)
    else -> Color(0xFF4285F4)
}
