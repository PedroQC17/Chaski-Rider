package com.example.chaskirider.feature.chat.ui.viewmodel

import com.example.chaskirider.feature.chat.domain.ChatChannel
import com.example.chaskirider.feature.chat.domain.ChatMessage

data class ChatUiState(
    val isOpen: Boolean = false,
    val activeChannel: ChatChannel = ChatChannel.MERCHANT,
    val messages: List<ChatMessage> = emptyList(),
    val inputText: String = "",
    val isListeningSpeech: Boolean = false,
    val speechError: String? = null,
    val isMerchantChannelEnabled: Boolean = false,
    val isCustomerChannelEnabled: Boolean = false,
    val loading: Boolean = false,
    val busy: Boolean = false,
    val fromCache: Boolean = false,
    val error: String? = null
)
