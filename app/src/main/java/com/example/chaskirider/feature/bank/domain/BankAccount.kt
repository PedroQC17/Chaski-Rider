package com.example.chaskirider.feature.bank.domain

data class BankAccount(
    val bankName: String = "",
    val holderName: String = "",
    val accountNumber: String = "",
    val cci: String = "",
    val statementDocumentUrl: String = ""
)
