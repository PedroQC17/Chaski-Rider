package com.example.chaskirider.domain.model

data class RiderUser(
    val id: String = "",
    val name: String = "",
    val lastName: String = "",
    val dni: String = "",
    val phone: String = "",
    val email: String = "",
    val vehicleType: VehicleType = VehicleType.NONE,
    val status: RegistrationStatus = RegistrationStatus.INCOMPLETE,
    val rejectionReason: String = "",
    val currentStep: Int = 1,
    val termsAccepted: Boolean = false,
    val bankInfo: BankInfo = BankInfo(),
    val isAvailable: Boolean = false,
    val isEnabled: Boolean = true,
    // Document URLs in Storage
    val dniFrontUrl: String = "",
    val dniBackUrl: String = "",
    val driverLicenseUrl: String = "",
    val soatUrl: String = "",
    val bankStatementUrl: String = ""
)
