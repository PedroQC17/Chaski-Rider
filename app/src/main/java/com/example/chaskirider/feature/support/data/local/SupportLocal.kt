package com.example.chaskirider.feature.support.data.local

import androidx.room.*
import com.example.chaskirider.feature.support.domain.SupportDraft
import com.example.chaskirider.feature.support.domain.SupportDraftStore
import com.google.gson.Gson

@Entity(tableName = "support_drafts")
data class SupportDraftEntity(@PrimaryKey val uid: String, val content: String)

@Dao
interface SupportDraftDao {
    @Query("SELECT * FROM support_drafts WHERE uid = :uid")
    suspend fun load(uid: String): SupportDraftEntity?
    @Upsert suspend fun save(draft: SupportDraftEntity)
    @Query("DELETE FROM support_drafts WHERE uid = :uid AND content = :content")
    suspend fun delete(uid: String, content: String)
}


class RoomSupportDraftStore(private val dao: SupportDraftDao) : SupportDraftStore {
    private val gson = Gson()
    override suspend fun load(uid: String) = dao.load(uid)?.let { gson.fromJson(it.content, SupportDraft::class.java) }
    override suspend fun save(uid: String, draft: SupportDraft) = dao.save(SupportDraftEntity(uid, gson.toJson(draft)))
    override suspend fun clear(uid: String, draftId: String) {
        val saved = dao.load(uid) ?: return
        if (gson.fromJson(saved.content, SupportDraft::class.java).id == draftId) dao.delete(uid, saved.content)
    }
}
