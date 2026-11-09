package com.example.chaskirider.domain.support

import kotlinx.coroutines.flow.Flow

data class AssistantReply(val mode: String, val needsHuman: Boolean, val canResolve: Boolean, val ticketId: String)
interface AssistantRepository {
    fun messages(uid: String, orderId: String): Flow<List<SupportMessage>>
    fun ticket(uid: String, orderId: String): Flow<SupportTicket?>
    suspend fun send(uid: String, orderId: String, messageId: String, body: String, human: Boolean): AssistantReply
}
