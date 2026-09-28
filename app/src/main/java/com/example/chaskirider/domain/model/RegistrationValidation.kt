
package com.example.chaskirider.domain.model

object RegistrationValidation {
    const val MAX_FILE_BYTES = 10L * 1024 * 1024
    val mimeTypes = arrayOf("application/pdf", "image/jpeg", "image/png")
    fun normalizePhone(phone: String): String {
        val clean = phone.replace(Regex("[\\s()-]"), "")
        return if (clean.matches(Regex("9[0-9]{8}"))) "+51$clean" else clean
    }
    fun personalError(name: String, last: String, dni: String, phone: String, terms: Boolean): String? = when {
        name.isBlank() || last.isBlank() -> "Completa tus nombres y apellidos"
        !dni.matches(Regex("[0-9]{8}")) -> "El DNI debe tener 8 dígitos"
        !normalizePhone(phone).matches(Regex("\\+[1-9][0-9]{7,14}")) -> "Ingresa un celular válido con código de país"
        !terms -> "Debes aceptar los términos y la política de privacidad"
        else -> null
    }
    fun bankError(bank: BankInfo): String? = when {
        bank.bankName.isBlank() || bank.holderName.isBlank() -> "Completa el banco y el titular"
        bank.accountNumber.isBlank() && bank.cci.isBlank() -> "Ingresa un número de cuenta o CCI"
        bank.cci.isNotBlank() && !bank.cci.matches(Regex("[0-9]{20}")) -> "El CCI debe tener 20 dígitos"
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
    fun availabilityError(user: RiderUser, activating: Boolean): String? = when {
        !activating -> null
        user.status != RegistrationStatus.APPROVED || !user.isEnabled ->
            "Solo los repartidores habilitados pueden activarse"
        else -> null
    }
    fun nextStep(user: RiderUser): Int = when {
        personalError(user.name, user.lastName, user.dni, user.phone, user.termsAccepted) != null -> 1
        user.vehicleType !in listOf(VehicleType.BICYCLE, VehicleType.MOTORCYCLE, VehicleType.CAR) -> 2
        else -> 3
    }
}
