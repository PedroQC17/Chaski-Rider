package com.example.chaskirider.dashboard.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.example.chaskirider.dashboard.routes.HomeRoute
import com.example.chaskirider.dashboard.viewmodel.HomeViewModel
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
