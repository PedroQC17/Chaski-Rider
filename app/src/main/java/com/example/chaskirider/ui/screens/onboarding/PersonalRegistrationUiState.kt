package com.example.chaskirider.ui.screens.onboarding

data class PersonalRegistrationUiState(
    val name: String = "",
    val lastName: String = "",
    val dni: String = "",
    val phone: String = "",
    val email: String = "",
    val termsAccepted: Boolean = false,
    val needsPassword: Boolean = true,
    val password: String = "",
    val confirmation: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

enum class PersonalField { NAME, LAST_NAME, DNI, PHONE, PASSWORD, CONFIRMATION }
