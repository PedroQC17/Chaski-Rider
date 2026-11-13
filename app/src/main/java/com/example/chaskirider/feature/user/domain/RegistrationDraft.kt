package com.example.chaskirider.feature.user.domain

import com.example.chaskirider.feature.bank.domain.BankInfo
import com.example.chaskirider.feature.vehicle.domain.VehicleType

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
    fun mergeWith(user: RiderUser, bankInfo: BankInfo) = copy(
        name = if (personalDirty) name else user.name,
        lastName = if (personalDirty) lastName else user.lastName,
        dni = if (personalDirty) dni else user.dni,
        phone = if (personalDirty) phone else user.phone.removePrefix("+51"),
        termsAccepted = if (personalDirty) termsAccepted else user.termsAccepted,
        vehicle = if (vehicleDirty) vehicle else VehicleType.fromId(user.vehicleTypeId),
        bank = if (bankDirty) bank else bankInfo
    )
}

data class PendingDocumentCapture(val uid: String, val type: String, val uri: String)

interface RegistrationDraftRepository {
    suspend fun load(uid: String): RegistrationDraft?
    suspend fun save(draft: RegistrationDraft)
    suspend fun clear(uid: String)
    suspend fun loadCapture(uid: String): PendingDocumentCapture?
    suspend fun saveCapture(capture: PendingDocumentCapture)
    suspend fun clearCapture(uid: String)
}
