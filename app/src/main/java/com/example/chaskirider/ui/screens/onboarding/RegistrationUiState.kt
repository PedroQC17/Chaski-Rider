package com.example.chaskirider.ui.screens.onboarding

import com.example.chaskirider.domain.model.*

enum class DraftStatus { LOADING, SAVING, SAVED, ERROR }
enum class PersonalField { NAME, LAST_NAME, DNI, PHONE, PASSWORD, CONFIRMATION }
enum class BankField { BANK, HOLDER, ACCOUNT, CCI }

data class RegistrationUiState(
    val name: String = "", val lastName: String = "", val dni: String = "", val phone: String = "",
    val email: String = "", val termsAccepted: Boolean = false,
    val needsPassword: Boolean = true, val password: String = "", val confirmation: String = "",
    val vehicle: VehicleType = VehicleType.NONE, val bank: BankInfo = BankInfo(),
    val ready: Boolean = false, val editable: Boolean = false,
    val draftStatus: DraftStatus = DraftStatus.LOADING,
    val isLoading: Boolean = false, val errorMessage: String? = null
) {
    val controlsEnabled get() = ready && editable && !isLoading
}
