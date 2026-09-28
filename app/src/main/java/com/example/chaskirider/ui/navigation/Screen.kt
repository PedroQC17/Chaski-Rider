// HU03 - Parte 1: se agrega la ruta "profile" para la pantalla Mi perfil.
// HU03 - Parte 2: se agrega la ruta "profile_personal_data" para editar datos personales.
// HU03 - Parte 3: se agrega la ruta "profile_vehicle" para gestionar el vehículo.
// Parte 4 agregará la ruta de documentos.
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
}
