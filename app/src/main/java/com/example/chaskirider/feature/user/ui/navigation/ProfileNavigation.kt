package com.example.chaskirider.feature.user.ui.navigation

import android.net.Uri
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.example.chaskirider.feature.documents.ui.screens.ProfileDocumentsScreen
import com.example.chaskirider.feature.documents.ui.viewmodel.DocumentsUiState
import com.example.chaskirider.feature.documents.ui.viewmodel.DocumentsViewModel
import com.example.chaskirider.feature.user.domain.RiderUser
import com.example.chaskirider.feature.user.ui.screens.ProfilePersonalDataScreen
import com.example.chaskirider.feature.user.ui.screens.ProfileScreen
import com.example.chaskirider.feature.user.ui.viewmodel.RiderProfileViewModel
import com.example.chaskirider.feature.vehicle.domain.VehicleType
import com.example.chaskirider.feature.vehicle.ui.screens.ProfileVehicleScreen
import com.example.chaskirider.navigation.Screen

fun NavGraphBuilder.profileGraph(
    navController: NavController,
    user: RiderUser?,
    photoUri: Uri?,
    profileViewModel: RiderProfileViewModel,
    documentsState: DocumentsUiState,
    documentsViewModel: DocumentsViewModel,
    onOpenMenu: () -> Unit,
    onLogout: () -> Unit,
    onSupport: () -> Unit,
    onPasswordSetup: () -> Unit,
) {
    composable(Screen.Profile.route) {
        user?.let {
            ProfileScreen(user = it, photoUri = photoUri,
                onOpenMenu = onOpenMenu,
                onPersonalDataClick = { profileViewModel.clearError(); navController.navigate(Screen.ProfilePersonalData.route) },
                onVehicleClick = { profileViewModel.clearError(); navController.navigate(Screen.ProfileVehicle.route) },
                onDocumentsClick = { profileViewModel.clearError(); navController.navigate(Screen.ProfileDocuments.route) },
                onSupportClick = onSupport,
                onLogoutClick = onLogout)
        }
    }
    composable(Screen.ProfilePersonalData.route) {
        user?.let {
            ProfilePersonalDataScreen(user = it,
                onNavigateBack = { profileViewModel.clearError(); navController.popBackStack() },
                onRequestChange = profileViewModel::showChangeNotice,
                onConfigurePassword = onPasswordSetup)
        }
    }
    composable(Screen.ProfileVehicle.route) {
        user?.let {
            ProfileVehicleScreen(user = it,
                onNavigateBack = { profileViewModel.clearError(); navController.popBackStack() },
                onRequestChange = profileViewModel::showChangeNotice)
        }
    }
    composable(Screen.ProfileDocuments.route) {
        user?.let {
            ProfileDocumentsScreen(vehicleType = VehicleType.fromId(it.vehicleTypeId),
                paths = documentsState.documentsMap.mapValues { (_, file) -> file.url },
                onNavigateBack = { profileViewModel.clearError(); documentsViewModel.clearError(); navController.popBackStack() },
                onDocumentView = documentsViewModel::openDocument,
                onRequestChange = profileViewModel::showChangeNotice)
        }
    }
}
