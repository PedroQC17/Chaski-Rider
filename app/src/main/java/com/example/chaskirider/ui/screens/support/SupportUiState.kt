package com.example.chaskirider.ui.screens.support

import com.example.chaskirider.domain.support.*

data class SupportUiState(
    val loading: Boolean = true,
    val busy: Boolean = false,
    val category: SupportCategory? = null,
    val topic: SupportTopic? = null,
    val formOpen: Boolean = false,
    val draft: SupportDraft? = null,
    val availableOrder: SupportOrderContext? = null,
    val tickets: List<SupportTicket> = emptyList(),
    val selectedTicketId: String? = null,
    val reply: SupportDraft? = null,
    val replyLoading: Boolean = false,
    val conversation: List<SupportMessage> = emptyList(),
    val error: String? = null,
    val notice: String? = null
)
