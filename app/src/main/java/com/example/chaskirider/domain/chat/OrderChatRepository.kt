package com.example.chaskirider.domain.chat

import kotlinx.coroutines.flow.Flow

data class ChatConversation(val messages: List<ChatMessage>, val fromCache: Boolean = false)
data class ChatDraft(val id: String, val body: String = "")

interface OrderChatRepository {
    fun observe(uid: String, orderId: String, channel: ChatChannel): Flow<ChatConversation>
    suspend fun send(uid: String, orderId: String, channel: ChatChannel, messageId: String, body: String)
}

interface ChatDraftStore {
    suspend fun load(key: String): ChatDraft?
    suspend fun save(key: String, draft: ChatDraft)
    suspend fun clear(key: String, draft: ChatDraft)
}
