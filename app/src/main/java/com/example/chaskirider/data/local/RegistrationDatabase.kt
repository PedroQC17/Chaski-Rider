package com.example.chaskirider.data.local

import androidx.room.*
import com.example.chaskirider.domain.model.*
import com.example.chaskirider.domain.repository.RegistrationDraftRepository

@Entity(tableName = "registration_drafts")
data class RegistrationDraftEntity(
    @PrimaryKey val uid: String,
    val name: String, val lastName: String, val dni: String, val phone: String,
    val termsAccepted: Boolean, val vehicle: String,
    val bankName: String, val holderName: String, val accountNumber: String, val cci: String,
    val personalDirty: Boolean, val vehicleDirty: Boolean, val bankDirty: Boolean
)

@Entity(tableName = "document_captures")
data class DocumentCaptureEntity(@PrimaryKey val uid: String, val type: String, val uri: String)

@Dao
interface RegistrationDraftDao {
    @Query("SELECT * FROM registration_drafts WHERE uid = :uid")
    suspend fun load(uid: String): RegistrationDraftEntity?
    @Upsert suspend fun save(entity: RegistrationDraftEntity)
    @Query("DELETE FROM registration_drafts WHERE uid = :uid") suspend fun clear(uid: String)
    @Query("SELECT * FROM document_captures WHERE uid = :uid") suspend fun loadCapture(uid: String): DocumentCaptureEntity?
    @Upsert suspend fun saveCapture(entity: DocumentCaptureEntity)
    @Query("DELETE FROM document_captures WHERE uid = :uid") suspend fun clearCapture(uid: String)
}

@Database(entities = [RegistrationDraftEntity::class, DocumentCaptureEntity::class], version = 1, exportSchema = true)
abstract class RegistrationDatabase : RoomDatabase() {
    abstract fun drafts(): RegistrationDraftDao
}

class RoomRegistrationDraftRepository(private val dao: RegistrationDraftDao) : RegistrationDraftRepository {
    override suspend fun load(uid: String) = dao.load(uid)?.let {
        RegistrationDraft(it.uid, it.name, it.lastName, it.dni, it.phone, it.termsAccepted,
            VehicleType.entries.firstOrNull { v -> v.name == it.vehicle } ?: VehicleType.NONE,
            BankInfo(it.bankName, it.holderName, it.accountNumber, it.cci),
            it.personalDirty, it.vehicleDirty, it.bankDirty)
    }
    override suspend fun save(draft: RegistrationDraft) {
        dao.save(RegistrationDraftEntity(draft.uid, draft.name, draft.lastName, draft.dni, draft.phone,
            draft.termsAccepted, draft.vehicle.name, draft.bank.bankName, draft.bank.holderName,
            draft.bank.accountNumber, draft.bank.cci, draft.personalDirty, draft.vehicleDirty, draft.bankDirty))
    }
    override suspend fun clear(uid: String) = dao.clear(uid)
    override suspend fun loadCapture(uid: String) = dao.loadCapture(uid)?.let { PendingDocumentCapture(it.uid, it.type, it.uri) }
    override suspend fun saveCapture(capture: PendingDocumentCapture) =
        dao.saveCapture(DocumentCaptureEntity(capture.uid, capture.type, capture.uri))
    override suspend fun clearCapture(uid: String) = dao.clearCapture(uid)
}
