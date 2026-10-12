package com.example.chaskirider.domain.model

/** Editable data only. Passwords, approval and availability never belong in a draft. */
data class RegistrationDraft(
    val uid: String,
    val name: String = "",
    val lastName: String = "",
    val dni: String = "",
    val phone: String = "",
    val termsAccepted: Boolean = false,
    val vehicle: VehicleType = VehicleType.NONE,
    val bank: BankInfo = BankInfo(),
    val personalDirty: Boolean = false,
    val vehicleDirty: Boolean = false,
    val bankDirty: Boolean = false
) {
    fun mergeWith(user: RiderUser) = copy(
        name = if (personalDirty) name else user.name,
        lastName = if (personalDirty) lastName else user.lastName,
        dni = if (personalDirty) dni else user.dni,
        phone = if (personalDirty) phone else user.phone.removePrefix("+51"),
        termsAccepted = if (personalDirty) termsAccepted else user.termsAccepted,
        vehicle = if (vehicleDirty) vehicle else user.vehicleType,
        bank = if (bankDirty) bank else user.bankInfo
    )
}

data class PendingDocumentCapture(val uid: String, val type: String, val uri: String)
