package com.example.chaskirider.feature.bank.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Upsert
import com.example.chaskirider.feature.bank.domain.BankAccount

@Entity(tableName = "bank_accounts")
data class BankAccountEntity(
    @PrimaryKey val riderId: String,
    val bankName: String,
    val holderName: String,
    val accountNumber: String,
    val cci: String,
    val statementDocumentUrl: String
)

@Dao
interface BankAccountDao {
    @Query("SELECT * FROM bank_accounts WHERE riderId = :riderId")
    suspend fun load(riderId: String): BankAccountEntity?

    @Upsert
    suspend fun save(entity: BankAccountEntity)
}

class BankAccountStore(private val dao: BankAccountDao) {
    suspend fun load(riderId: String): BankAccount? = dao.load(riderId)?.toDomain()
    suspend fun save(riderId: String, bank: BankAccount) = dao.save(bank.toEntity(riderId))
}

fun BankAccountEntity.toDomain() = BankAccount(bankName, holderName, accountNumber, cci, statementDocumentUrl)

fun BankAccount.toEntity(riderId: String) =
    BankAccountEntity(riderId, bankName, holderName, accountNumber, cci, statementDocumentUrl)
