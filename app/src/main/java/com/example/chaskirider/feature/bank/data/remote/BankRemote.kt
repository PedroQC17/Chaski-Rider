package com.example.chaskirider.feature.bank.data.remote

import com.example.chaskirider.core.firebase.FirebaseRoot
import com.example.chaskirider.core.firebase.getWithCacheFallback
import com.example.chaskirider.core.text.TextKey
import com.example.chaskirider.core.text.TextProvider
import com.example.chaskirider.feature.bank.domain.BankAccount
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.tasks.await

class BankRemote(
    private val texts: TextProvider,
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val functions: FirebaseFunctions = FirebaseFunctions.getInstance(FirebaseRoot.REGION)
) {
    suspend fun load(): BankAccount {
        val snapshot = riderDocument()
        val info = snapshot.get("bankInfo", BankAccount::class.java) ?: BankAccount()
        return info.copy(bankName = info.bankName.uppercase())
    }

    suspend fun save(bank: BankAccount): BankAccount {
        requireUid()
        functions.getHttpsCallable(FirebaseRoot.Callables.RIDER_REGISTRATION).call(mapOf(
            "action" to "bank",
            "bankInfo" to mapOf("bankName" to bank.bankName.trim().uppercase(),
                "holderName" to bank.holderName.trim(),
                "accountNumber" to bank.accountNumber.trim(),
                "cci" to bank.cci.trim())
        )).await()
        return load()
    }

    private suspend fun riderDocument(): com.google.firebase.firestore.DocumentSnapshot {
        val uid = requireUid()
        return db.document(FirebaseRoot.riderDoc(uid)).getWithCacheFallback()
    }

    private fun requireUid(): String =
        auth.currentUser?.uid ?: error(texts.get(TextKey.TEXT_NO_HAY_UNA_SESION_ACTIVA))
}
