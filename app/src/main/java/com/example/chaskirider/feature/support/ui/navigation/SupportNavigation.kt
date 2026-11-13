package com.example.chaskirider.feature.support.ui.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.example.chaskirider.feature.support.ui.screens.SupportRoute
import com.example.chaskirider.feature.support.ui.viewmodel.SupportViewModel
import com.example.chaskirider.feature.user.domain.RiderUser
import com.example.chaskirider.navigation.Screen

fun NavGraphBuilder.supportGraph(
    user: RiderUser?,
    viewModel: SupportViewModel,
    onBack: () -> Unit,
) {
    composable(Screen.Support.route) {
        if (user != null) SupportRoute(viewModel, onBack = onBack)
    }
}
