package com.example.chaskirider.feature.bank.data

import com.example.chaskirider.core.firebase.firebaseResult
import com.example.chaskirider.core.text.TextProvider
import com.example.chaskirider.feature.bank.data.local.BankAccountStore
import com.example.chaskirider.feature.bank.data.remote.BankRemote
import com.example.chaskirider.feature.bank.domain.BankAccount
import com.example.chaskirider.feature.bank.domain.BankRepository

class BankRepositoryImpl(
    private val texts: TextProvider,
    private val remote: BankRemote,
    private val store: BankAccountStore,
    private val currentUid: () -> String?,
    private val refreshUser: suspend () -> Unit
) : BankRepository {
    override suspend fun load(): Result<BankAccount> {
        val uid = currentUid()
        val cached = uid?.let { runCatching { store.load(it) }.getOrNull() }
        return firebaseResult(texts) { remote.load() }.fold(
            onSuccess = { value ->
                uid?.let { runCatching { store.save(it, value) } }
                Result.success(value)
            },
            onFailure = { error -> cached?.let { Result.success(it) } ?: Result.failure(error) }
        )
    }

    override suspend fun save(bank: BankAccount): Result<BankAccount> = firebaseResult(texts) {
        remote.save(bank).also { saved ->
            currentUid()?.let { runCatching { store.save(it, saved) } }
            refreshUser()
        }
    }
}
