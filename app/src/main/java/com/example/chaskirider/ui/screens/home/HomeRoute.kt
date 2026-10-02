package com.example.chaskirider.ui.screens.home

import android.Manifest
import android.os.Build
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.chaskirider.R

@Composable
fun HomeRoute(viewModel: HomeViewModel, onOpenMenu: () -> Unit) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val latestState by rememberUpdatedState(state)
    var connectAfterPermission by remember { mutableStateOf(false) }
    val notifications = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            notifications.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
    val permissions = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        val granted = result.values.any { it }
        viewModel.setLocationPermission(granted)
        if (granted) {
            viewModel.locate()
            if (connectAfterPermission) viewModel.setAvailability(true)
        } else viewModel.reportError(context.getString(R.string.text_se_requiere_el_permiso_de_ubicacion_para))
        connectAfterPermission = false
    }
    DisposableEffect(lifecycleOwner, context) {
        fun refreshPermission() {
            val granted = listOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION)
                .any { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED }
            viewModel.setLocationPermission(granted)
            if (granted && latestState.location == null) viewModel.locate()
        }
        refreshPermission()
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) refreshPermission() }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    fun requestLocation(connect: Boolean) {
        connectAfterPermission = connect
        permissions.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
    }
    HomeScreen(state = state, onOpenMenu = onOpenMenu,
        onTogglePanel = viewModel::togglePanel,
        onToggleAvailability = {
            if (state.user?.isAvailable == true) viewModel.setAvailability(false)
            else if (state.hasLocationPermission) viewModel.setAvailability(true)
            else requestLocation(true)
        },
        onLocate = { if (state.hasLocationPermission) viewModel.locate() else requestLocation(false) },
        onDismissError = viewModel::clearError)
}
