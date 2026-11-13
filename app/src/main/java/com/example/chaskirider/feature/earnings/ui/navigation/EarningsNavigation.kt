package com.example.chaskirider.feature.earnings.ui.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.example.chaskirider.feature.earnings.ui.screens.EarningsRoute
import com.example.chaskirider.feature.earnings.ui.viewmodel.EarningsViewModel
import com.example.chaskirider.navigation.Screen

fun NavGraphBuilder.earningsGraph(
    workArea: Boolean,
    viewModel: EarningsViewModel,
    onMenu: () -> Unit,
) {
    composable(Screen.Earnings.route) {
        if (workArea) EarningsRoute(viewModel = viewModel, onMenu = onMenu)
    }
}
