// HU03 - Parte 1: se agrega la ruta "profile" para la pantalla Mi perfil.
// HU03 - Parte 2: se agrega la ruta "profile_personal_data" para editar datos personales.
// HU03 - Parte 3: se agrega la ruta "profile_vehicle" para gestionar el vehículo.
// HU03 - Parte 4: se agrega la ruta "profile_documents" para gestionar documentos.
// HU04 - Parte 4: se agrega la ruta "notifications" para ver notificaciones.
// HU06 - Parte 3: se agrega la ruta "offer" para la pantalla de oferta.
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
}
