package com.example.chaskirider.feature.user.domain

import com.example.chaskirider.core.text.TextKey

object RiderUserPolicy {
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

    fun availabilityError(user: RiderUser, activating: Boolean): TextKey? = when {
        !activating -> null
        user.profilePhotoPath.isBlank() -> TextKey.PROFILE_PHOTO_REQUIRED
        user.status != RegistrationStatus.APPROVED || !user.isEnabled ->
            TextKey.TEXT_SOLO_LOS_REPARTIDORES_HABILITADOS_PUEDEN_ACTIVARSE
        else -> null
    }
}
