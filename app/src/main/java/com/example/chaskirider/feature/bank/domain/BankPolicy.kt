package com.example.chaskirider.feature.bank.domain

import com.example.chaskirider.core.text.TextKey

object BankPolicy {
    val supportedBanks = listOf("BCP", "INTERBANK", "BBVA")

    fun error(bank: BankInfo): TextKey? = when {
        bank.bankName !in supportedBanks || bank.holderName.isBlank() -> TextKey.TEXT_COMPLETA_EL_BANCO_Y_EL_TITULAR
        !bank.cci.matches(Regex("[0-9]{20}")) -> TextKey.TEXT_EL_CCI_DEBE_TENER_20_DIGITOS
        else -> null
    }
}
