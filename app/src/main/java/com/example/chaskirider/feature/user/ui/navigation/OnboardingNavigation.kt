package com.example.chaskirider.feature.user.ui.navigation

import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.example.chaskirider.feature.documents.ui.viewmodel.DocumentsUiState
import com.example.chaskirider.feature.documents.ui.viewmodel.DocumentsViewModel
import com.example.chaskirider.feature.user.domain.RegistrationStatus
import com.example.chaskirider.feature.user.domain.RiderUser
import com.example.chaskirider.feature.user.ui.routes.ProfilePhotoRoute
import com.example.chaskirider.feature.user.ui.screens.OnboardingStep1Screen
import com.example.chaskirider.feature.user.ui.screens.OnboardingStep2Screen
import com.example.chaskirider.feature.user.ui.screens.OnboardingStep3Screen
import com.example.chaskirider.feature.user.ui.screens.RegistrationStatusScreen
import com.example.chaskirider.feature.user.ui.viewmodel.ProfilePhotoViewModel
import com.example.chaskirider.feature.user.ui.viewmodel.RegistrationReviewUiState
import com.example.chaskirider.feature.user.ui.viewmodel.RegistrationReviewViewModel
import com.example.chaskirider.feature.user.ui.viewmodel.RegistrationUiState
import com.example.chaskirider.feature.user.ui.viewmodel.RegistrationViewModel
import com.example.chaskirider.feature.vehicle.domain.VehicleType
import com.example.chaskirider.navigation.Screen

fun NavGraphBuilder.onboardingGraph(
    navController: NavController,
    user: RiderUser?,
    personalState: RegistrationUiState,
    reviewState: RegistrationReviewUiState,
    documentsState: DocumentsUiState,
    personalViewModel: RegistrationViewModel,
    documentsViewModel: DocumentsViewModel,
    reviewViewModel: RegistrationReviewViewModel,
    photoViewModel: ProfilePhotoViewModel,
    needsPhoto: Boolean,
    onLogout: () -> Unit,
    onDocumentCamera: (String) -> Unit,
    onSupport: () -> Unit,
    onGoHome: () -> Unit,
) {
    composable(Screen.OnboardingStep1.route) {
        user?.let {
            OnboardingStep1Screen(state = personalState,
                onChange = personalViewModel::change, onTermsChange = personalViewModel::acceptTerms,
                onNavigateBack = onLogout,
                onRetryDraft = personalViewModel::retryDraft,
                onContinue = { personalViewModel.savePersonal { navController.navigate(Screen.OnboardingStep2.route) } })
        }
    }
    composable(Screen.OnboardingStep2.route) {
        user?.let {
            OnboardingStep2Screen(state = personalState,
                onVehicleChange = personalViewModel::selectVehicle,
                onRetryDraft = personalViewModel::retryDraft,
                onNavigateBack = { personalViewModel.clearError(); navController.navigate(Screen.OnboardingStep1.route) { launchSingleTop = true } },
                onContinueClick = { personalViewModel.saveVehicle { navController.navigate(Screen.OnboardingStep3.route) } })
        }
    }
    composable(Screen.OnboardingStep3.route) {
        user?.let {
            OnboardingStep3Screen(vehicleType = VehicleType.fromId(it.vehicleTypeId), state = personalState,
                onBankChange = personalViewModel::changeBank,
                onRetryDraft = personalViewModel::retryDraft,
                onNavigateBack = { personalViewModel.clearError(); documentsViewModel.clearError(); navController.navigate(Screen.OnboardingStep2.route) { launchSingleTop = true } },
                onDocumentPick = documentsViewModel::uploadDocument,
                onDocumentCamera = onDocumentCamera,
                onDocumentView = documentsViewModel::openDocument,
                onFinishRegistrationClick = { personalViewModel.submit {
                    navController.navigate(Screen.RegistrationStatus.route) { popUpTo(navController.graph.id) { inclusive = true }; launchSingleTop = true }
                } },
                documentsMap = documentsState.documentsMap, isLoading = personalState.isLoading || documentsState.isLoading,
                errorMessage = documentsState.errorMessage ?: personalState.errorMessage)
        }
    }
    composable(Screen.RegistrationStatus.route) {
        user?.let {
            LifecycleResumeEffect(it.id) {
                reviewViewModel.refresh()
                onPauseOrDispose { }
            }
            RegistrationStatusScreen(state = reviewState,
                onResumeRegistrationClick = { if (reviewState.canEdit) {
                    personalViewModel.clearError(); navController.navigate(Screen.OnboardingStep1.route) { launchSingleTop = true }
                } },
                onGoToHomeClick = { if (it.status == RegistrationStatus.APPROVED && it.isEnabled) onGoHome() },
                onLogoutClick = onLogout,
                onRefresh = reviewViewModel::refresh,
                onSupportClick = onSupport)
        }
    }
    composable(Screen.ProfilePhoto.route) {
        if (needsPhoto) ProfilePhotoRoute(photoViewModel, onLogout = onLogout)
    }
}
