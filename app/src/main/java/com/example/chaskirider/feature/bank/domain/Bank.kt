package com.example.chaskirider.feature.bank.domain

data class BankInfo(
    val bankName: String = "",
    val holderName: String = "",
    val accountNumber: String = "",
    val cci: String = "",
    val statementDocumentUrl: String = ""
)
