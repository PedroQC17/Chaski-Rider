package com.example.chaskirider.feature.user.ui.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.example.chaskirider.feature.user.ui.screens.AccessScreen
import com.example.chaskirider.feature.user.ui.screens.EmailLoginScreen
import com.example.chaskirider.feature.user.ui.screens.SessionLoadingScreen
import com.example.chaskirider.feature.user.ui.viewmodel.AuthUiState
import com.example.chaskirider.feature.user.ui.viewmodel.AuthViewModel
import com.example.chaskirider.navigation.Screen

fun NavGraphBuilder.authGraph(
    navController: NavController,
    state: AuthUiState,
    busy: Boolean,
    viewModel: AuthViewModel,
    onGoogleSignIn: () -> Unit,
    onAccountRecovery: () -> Unit,
) {
    composable(Screen.Startup.route) { SessionLoadingScreen(state, viewModel::checkCurrentUser) }
    composable(Screen.Access.route) {
        AccessScreen(
            onGoogleSignInClick = onGoogleSignIn,
            onEmailLoginClick = { viewModel.clearError(); navController.navigate(Screen.EmailLogin.route) },
            onAccountRecoveryClick = { viewModel.clearError(); onAccountRecovery() },
            isLoading = busy,
            errorMessage = state.errorMessage
        )
    }
    composable(Screen.EmailLogin.route) {
        EmailLoginScreen(onNavigateBack = { viewModel.clearError(); navController.popBackStack() },
            onLoginClick = viewModel::loginWithEmail,
            onPasswordResetRequest = viewModel::sendPasswordResetEmail,
            isLoading = state.isLoading, errorMessage = state.errorMessage,
            successMessage = state.message)
    }
}
