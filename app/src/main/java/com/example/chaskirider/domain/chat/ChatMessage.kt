package com.example.chaskirider.domain.chat


//clase que detemrina si se conversa con el establecimiento o con el cliente
enum class ChatChannel {
    MERCHANT,
    CUSTOMER
}

enum class ChatSenderRole {
    RIDER,
    MERCHANT,
    CUSTOMER
}

data class ChatMessage(
    val id: String,
    val orderId: String,
    val channel: ChatChannel,
    val senderName: String,
    val senderRole: ChatSenderRole,
    val text: String,
    val timestamp: Long
)
