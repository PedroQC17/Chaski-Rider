package com.example.chaskirider.feature.support.ui.viewmodel

import com.example.chaskirider.feature.support.domain.SupportCategory
import com.example.chaskirider.feature.support.domain.SupportTopic
import com.example.chaskirider.feature.support.domain.SupportOrderContext
import com.example.chaskirider.feature.support.domain.SupportDraft
import com.example.chaskirider.feature.support.domain.SupportMessage
import com.example.chaskirider.feature.support.domain.SupportTicket

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
