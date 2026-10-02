package com.example.chaskirider.ui.screens.profile.photo

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.chaskirider.R

@Composable
fun ProfilePhotoRoute(viewModel: ProfilePhotoViewModel, onLogout: () -> Unit) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    BackHandler { }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture(), viewModel::captured)
    fun openCamera() {
        viewModel.prepare { uri ->
            try { camera.launch(uri) }
            catch (_: android.content.ActivityNotFoundException) { viewModel.error(context.getString(R.string.profile_photo_camera_error)) }
            catch (_: SecurityException) { viewModel.error(context.getString(R.string.profile_photo_camera_error)) }
        }
    }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) openCamera() else viewModel.permissionDenied(context.getString(R.string.profile_photo_permission))
    }
    ProfilePhotoScreen(state, onCapture = {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) openCamera()
        else permission.launch(Manifest.permission.CAMERA)
    }, onSave = viewModel::save, onLogout = onLogout, onOpenSettings = {
        context.startActivity(android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            android.net.Uri.parse("package:${context.packageName}")))
    })
}
