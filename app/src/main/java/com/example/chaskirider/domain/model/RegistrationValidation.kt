
package com.example.chaskirider.domain.model

import com.example.chaskirider.domain.text.TextKey

object RegistrationValidation {
    const val MAX_FILE_BYTES = 10L * 1024 * 1024
    val mimeTypes = arrayOf("application/pdf", "image/jpeg", "image/png")
    fun normalizePhone(phone: String): String {
        val clean = phone.replace(Regex("[\\s()-]"), "")
        return if (clean.matches(Regex("9[0-9]{8}"))) "+51$clean" else clean
    }
    fun personalError(name: String, last: String, dni: String, phone: String, terms: Boolean): TextKey? = when {
        name.isBlank() || last.isBlank() -> TextKey.TEXT_COMPLETA_TUS_NOMBRES_Y_APELLIDOS
        !dni.matches(Regex("[0-9]{8}")) -> TextKey.TEXT_EL_DNI_DEBE_TENER_8_DIGITOS
        !normalizePhone(phone).matches(Regex("\\+[1-9][0-9]{7,14}")) -> TextKey.TEXT_INGRESA_UN_CELULAR_VALIDO_CON_CODIGO_DE
        !terms -> TextKey.TEXT_DEBES_ACEPTAR_LOS_TERMINOS_Y_LA_POLITICA
        else -> null
    }
    fun bankError(bank: BankInfo): TextKey? = when {
        bank.bankName !in listOf("BCP", "INTERBANK", "BBVA") || bank.holderName.isBlank() -> TextKey.TEXT_COMPLETA_EL_BANCO_Y_EL_TITULAR
        !bank.cci.matches(Regex("[0-9]{20}")) -> TextKey.TEXT_EL_CCI_DEBE_TENER_20_DIGITOS
        else -> null
    }
    fun requiredDocuments(vehicle: VehicleType) =
        listOf("dniFront", "dniBack", "bankStatement") +
            if (vehicle == VehicleType.MOTORCYCLE) listOf("driverLicense", "soat") else emptyList()
    fun documentPaths(user: RiderUser) = mapOf(
        "dniFront" to user.dniFrontUrl, "dniBack" to user.dniBackUrl,
        "bankStatement" to user.bankStatementUrl, "driverLicense" to user.driverLicenseUrl,
        "soat" to user.soatUrl
    )
    fun availabilityError(user: RiderUser, activating: Boolean): TextKey? = when {
        !activating -> null
        user.profilePhotoPath.isBlank() -> TextKey.PROFILE_PHOTO_REQUIRED
        user.status != RegistrationStatus.APPROVED || !user.isEnabled ->
            TextKey.TEXT_SOLO_LOS_REPARTIDORES_HABILITADOS_PUEDEN_ACTIVARSE
        else -> null
    }
    fun nextStep(user: RiderUser): Int = when {
        !user.hasPassword -> 1
        personalError(user.name, user.lastName, user.dni, user.phone, user.termsAccepted) != null -> 1
        user.vehicleType !in listOf(VehicleType.BICYCLE, VehicleType.MOTORCYCLE) -> 2
        else -> 3
    }
}
