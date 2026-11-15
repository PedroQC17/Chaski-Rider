package com.example.chaskirider.feature.chat.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Upsert
import com.example.chaskirider.feature.chat.domain.ChatDraft
import com.example.chaskirider.feature.chat.domain.ChatDraftStore
import com.example.chaskirider.feature.chat.domain.ChatSenderRole

enum class ChatMessageStatus { PENDING, SENT, FAILED }

@Entity(tableName = "chat_messages", primaryKeys = ["id"])
data class ChatMessageEntity(
    val id: String,
    val riderId: String,
    val orderId: String,
    val channel: String,
    val senderRole: String,
    val text: String,
    val createdAt: Long,
    val status: String
)

@Dao
interface ChatMessageDao {
    @Query("SELECT * FROM chat_messages WHERE riderId = :riderId AND orderId = :orderId AND channel = :channel AND status = 'PENDING' LIMIT 1")
    suspend fun pending(riderId: String, orderId: String, channel: String): ChatMessageEntity?

    @Upsert
    suspend fun save(message: ChatMessageEntity)

    @Query("DELETE FROM chat_messages WHERE id = :id AND text = :text")
    suspend fun delete(id: String, text: String)
}

class RoomChatMessageStore(private val dao: ChatMessageDao) : ChatDraftStore {
    override suspend fun load(key: String): ChatDraft? {
        val target = parse(key) ?: return null
        return dao.pending(target.first, target.second, target.third)?.let { ChatDraft(it.id, it.text) }
    }

    override suspend fun save(key: String, draft: ChatDraft) {
        val target = parse(key) ?: return
        dao.save(ChatMessageEntity(
            id = draft.id,
            riderId = target.first,
            orderId = target.second,
            channel = target.third,
            senderRole = ChatSenderRole.RIDER.name,
            text = draft.body,
            createdAt = System.currentTimeMillis(),
            status = ChatMessageStatus.PENDING.name
        ))
    }

    override suspend fun clear(key: String, draft: ChatDraft) = dao.delete(draft.id, draft.body)

    private fun parse(key: String): Triple<String, String, String>? {
        val parts = key.split("/")
        return if (parts.size >= 3) Triple(parts[0], parts[1], parts[2]) else null
    }
}
