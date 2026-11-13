package com.example.chaskirider.feature.bank.data

import com.example.chaskirider.core.firebase.firebaseResult
import com.example.chaskirider.core.text.TextProvider
import com.example.chaskirider.feature.bank.data.remote.BankRemote
import com.example.chaskirider.feature.bank.domain.BankInfo
import com.example.chaskirider.feature.bank.domain.BankRepository

class BankRepositoryImpl(
    private val texts: TextProvider,
    private val remote: BankRemote,
    private val refreshUser: suspend () -> Unit
) : BankRepository {
    override suspend fun load() = firebaseResult(texts) { remote.load() }

    override suspend fun save(bank: BankInfo) = firebaseResult(texts) {
        remote.save(bank).also { refreshUser() }
    }
}
