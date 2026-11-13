package com.example.chaskirider.feature.notifications.ui.navigation

import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.example.chaskirider.feature.notifications.ui.screens.NotificationsScreen
import com.example.chaskirider.feature.notifications.ui.viewmodel.NotificationsUiState
import com.example.chaskirider.feature.notifications.ui.viewmodel.NotificationsViewModel
import com.example.chaskirider.navigation.Screen

fun NavGraphBuilder.notificationsGraph(
    state: NotificationsUiState,
    viewModel: NotificationsViewModel,
    onOpenMenu: () -> Unit,
) {
    composable(Screen.Notifications.route) {
        LaunchedEffect(Unit) { viewModel.markSeen() }
        NotificationsScreen(state = state, onOpenMenu = onOpenMenu)
    }
}
