package com.example.chaskirider.dashboard.ui.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.example.chaskirider.dashboard.ui.routes.HomeRoute
import com.example.chaskirider.dashboard.ui.viewmodel.HomeViewModel
import com.example.chaskirider.navigation.Screen

fun NavGraphBuilder.homeGraph(
    workArea: Boolean,
    viewModel: HomeViewModel,
    onOpenMenu: () -> Unit,
) {
    composable(Screen.Home.route) {
        if (workArea) HomeRoute(viewModel, onOpenMenu = onOpenMenu)
    }
}
