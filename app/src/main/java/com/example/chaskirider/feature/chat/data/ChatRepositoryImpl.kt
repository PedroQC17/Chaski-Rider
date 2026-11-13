package com.example.chaskirider.feature.chat.data

import com.example.chaskirider.feature.chat.data.remote.ChatRemote
import com.example.chaskirider.feature.chat.domain.*
import kotlinx.coroutines.flow.map

class ChatRepositoryImpl(private val remote: ChatRemote = ChatRemote()) : OrderChatRepository {
    override fun observe(uid: String, orderId: String, channel: ChatChannel) =
        remote.observe(uid, orderId, channel).map { snapshot ->
            val result = snapshot.documents.map { doc ->
                val role = ChatSenderRole.valueOf(requireNotNull(doc.getString("senderRole")))
                check(doc.getString("orderId") == orderId && doc.getString("channel") == channel.name)
                ChatMessage(doc.id, orderId, channel, when (role) {
                    ChatSenderRole.RIDER -> "Tú"
                    ChatSenderRole.MERCHANT -> "Establecimiento"
                    ChatSenderRole.CUSTOMER -> "Cliente"
                }, role, requireNotNull(doc.getString("text")), doc.getTimestamp("createdAt")?.toDate()?.time ?: 0)
            }
            ChatConversation(result, snapshot.metadata.isFromCache)
        }

    override suspend fun send(uid: String, orderId: String, channel: ChatChannel, messageId: String, body: String) {
        require(body.trim().length in 1..2000)
        remote.send(uid, orderId, channel, messageId, body)
    }
}
