package com.example.chaskirider.feature.bank.domain

interface BankRepository {
    suspend fun load(): Result<BankAccount>
    suspend fun save(bank: BankAccount): Result<BankAccount>
}
