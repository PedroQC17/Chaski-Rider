package com.example.chaskirider.data.chat

import androidx.room.*
import com.example.chaskirider.domain.chat.*

@Entity(tableName = "chat_drafts")
data class ChatDraftEntity(@PrimaryKey val key: String, val messageId: String, val body: String)
@Dao
interface ChatDraftDao {
    @Query("SELECT * FROM chat_drafts WHERE `key` = :key")
    suspend fun load(key: String): ChatDraftEntity?
    @Upsert suspend fun save(draft: ChatDraftEntity)
    @Query("DELETE FROM chat_drafts WHERE `key` = :key AND messageId = :id AND body = :body")
    suspend fun delete(key: String, id: String, body: String)
}
@Database(entities = [ChatDraftEntity::class], version = 1, exportSchema = true)
abstract class ChatDraftDatabase : RoomDatabase() { abstract fun drafts(): ChatDraftDao }

class RoomChatDraftStore(private val dao: ChatDraftDao) : ChatDraftStore {
    override suspend fun load(key: String) = dao.load(key)?.let { ChatDraft(it.messageId, it.body) }
    override suspend fun save(key: String, draft: ChatDraft) = dao.save(ChatDraftEntity(key, draft.id, draft.body))
    override suspend fun clear(key: String, draft: ChatDraft) = dao.delete(key, draft.id, draft.body)
}
