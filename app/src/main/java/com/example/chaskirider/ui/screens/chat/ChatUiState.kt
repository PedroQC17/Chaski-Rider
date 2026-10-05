package com.example.chaskirider.ui.screens.chat

import com.example.chaskirider.domain.chat.ChatChannel
import com.example.chaskirider.domain.chat.ChatMessage

data class ChatUiState(
    val isOpen: Boolean = false,
    val activeChannel: ChatChannel = ChatChannel.MERCHANT,
    val messages: List<ChatMessage> = emptyList(),
    val inputText: String = "",
    val isListeningSpeech: Boolean = false,
    val speechError: String? = null,
    val isMerchantChannelEnabled: Boolean = true,
    val isCustomerChannelEnabled: Boolean = true
)
