// Modelo del documento riders/{uid}. Firestore deriva el nombre del getter: isAvailable()
// se vuelve "available" y el campo privado isAvailable no matchea (se descarta en silencio).
// @get:PropertyName fija el nombre real del documento y habilita el campo privado.
package com.example.chaskirider.domain.model

import com.google.firebase.firestore.PropertyName

data class RiderUser(
    val id: String = "",
    val name: String = "",
    val lastName: String = "",
    val dni: String = "",
    val phone: String = "",
    val email: String = "",
    val profilePhotoPath: String = "",
    val hasPassword: Boolean = false,
    val vehicleType: VehicleType = VehicleType.NONE,
    val status: RegistrationStatus = RegistrationStatus.INCOMPLETE,
    val rejectionReason: String = "",
    val currentStep: Int = 1,
    val termsAccepted: Boolean = false,
    val bankInfo: BankInfo = BankInfo(),
    @get:PropertyName("isAvailable")
    val isAvailable: Boolean = false,
    @get:PropertyName("isEnabled")
    val isEnabled: Boolean = true,

    val dniFrontUrl: String = "",
    val dniBackUrl: String = "",
    val driverLicenseUrl: String = "",
    val soatUrl: String = "",
    val bankStatementUrl: String = ""
)
