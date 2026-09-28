
package com.example.chaskirider.ui.navigation

sealed class Screen(val route: String) {
    object Access : Screen("access")
    object EmailLogin : Screen("email_login")
    object OnboardingStep1 : Screen("onboarding_step_1")
    object OnboardingStep2 : Screen("onboarding_step_2")
    object OnboardingStep3 : Screen("onboarding_step_3")
    object RegistrationStatus : Screen("registration_status")
    object Home : Screen("home")
    object Profile : Screen("profile")
    object ProfilePersonalData : Screen("profile_personal_data")
    object ProfileVehicle : Screen("profile_vehicle")
    object ProfileDocuments : Screen("profile_documents")
    object Notifications : Screen("notifications")
    object Offer : Screen("offer")
    object Orders : Screen("orders")
}
