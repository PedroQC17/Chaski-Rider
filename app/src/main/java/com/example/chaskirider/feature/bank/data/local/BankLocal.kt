package com.example.chaskirider.feature.bank.data.local

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Upsert
import com.example.chaskirider.feature.bank.domain.BankInfo

@Entity(tableName = "bank_cache")
data class BankInfoEntity(
    @PrimaryKey val uid: String,
    val bankName: String,
    val holderName: String,
    val accountNumber: String,
    val cci: String,
    val statementDocumentUrl: String
)

@Dao
interface BankCacheDao {
    @Query("SELECT * FROM bank_cache WHERE uid = :uid")
    suspend fun load(uid: String): BankInfoEntity?

    @Upsert
    suspend fun save(entity: BankInfoEntity)

    @Query("DELETE FROM bank_cache WHERE uid = :uid")
    suspend fun clear(uid: String)
}

/** Caché local de los datos bancarios para mostrarlos aunque Firestore no responda. */
interface BankLocal {
    suspend fun load(uid: String): BankInfo?
    suspend fun save(uid: String, bank: BankInfo)
    suspend fun clear(uid: String)
}

class RoomBankLocal(private val dao: BankCacheDao) : BankLocal {
    override suspend fun load(uid: String) = dao.load(uid)?.let {
        BankInfo(it.bankName, it.holderName, it.accountNumber, it.cci, it.statementDocumentUrl)
    }

    override suspend fun save(uid: String, bank: BankInfo) = dao.save(
        BankInfoEntity(uid, bank.bankName, bank.holderName, bank.accountNumber, bank.cci,
            bank.statementDocumentUrl)
    )

    override suspend fun clear(uid: String) = dao.clear(uid)
}
