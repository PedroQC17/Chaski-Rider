package com.example.chaskirider.feature.orders.ui.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.example.chaskirider.feature.orders.domain.OrderProblemReason
import com.example.chaskirider.feature.orders.ui.routes.OrdersRoute
import com.example.chaskirider.feature.orders.ui.viewmodel.OrdersViewModel
import com.example.chaskirider.navigation.Screen

fun NavGraphBuilder.ordersGraph(
    workArea: Boolean,
    viewModel: OrdersViewModel,
    onMenu: () -> Unit,
    onSupport: () -> Unit,
    onOrderHelp: (OrderProblemReason) -> Unit,
    onTicket: (String) -> Unit,
) {
    composable(Screen.Orders.route) {
        if (workArea) OrdersRoute(
            viewModel = viewModel,
            onMenu = onMenu,
            onSupport = onSupport,
            onOrderHelp = onOrderHelp,
            onTicket = onTicket
        )
    }
}
