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
fun WorkMap(state: HomeUiState, bottomInset: androidx.compose.ui.unit.Dp) {
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
    )
}
