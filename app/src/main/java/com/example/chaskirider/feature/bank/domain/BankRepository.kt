package com.example.chaskirider.feature.bank.domain

interface BankRepository {
    suspend fun load(): Result<BankInfo>
    suspend fun save(bank: BankInfo): Result<BankInfo>
}
