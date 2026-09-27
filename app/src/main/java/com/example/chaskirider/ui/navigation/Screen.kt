package com.example.chaskirider.ui.navigation

sealed class Screen(val route: String) {
    object Initial : Screen("initial")
    object Login : Screen("login")
    object Register : Screen("register")
    object Home : Screen("home")
}
