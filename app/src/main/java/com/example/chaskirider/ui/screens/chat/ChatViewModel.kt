package com.example.chaskirider.ui.screens.chat

import androidx.lifecycle.ViewModel
import com.example.chaskirider.domain.chat.ChatChannel
import com.example.chaskirider.domain.chat.ChatMessage
import com.example.chaskirider.domain.chat.ChatSenderRole
import com.example.chaskirider.domain.orders.OfferSnapshot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.UUID

class ChatViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState = _uiState.asStateFlow()

    private var currentOrderId: String? = null

    fun openChat() {
        _uiState.update { it.copy(isOpen = true) }
    }

    fun closeChat() {
        _uiState.update { it.copy(isOpen = false, isListeningSpeech = false) }
    }

    fun selectChannel(channel: ChatChannel) {
        _uiState.update { it.copy(activeChannel = channel) }
    }

    fun updateInputText(text: String) {
        _uiState.update { it.copy(inputText = text) }
    }

    // HU10: Dictado por voz inserta el texto en la caja para revisión previa, sin enviar automáticamente.
    fun onSpeechRecognized(recognizedText: String) {
        if (recognizedText.isBlank()) return
        _uiState.update {
            val combined = if (it.inputText.isBlank()) recognizedText else "${it.inputText} $recognizedText"
            it.copy(inputText = combined, isListeningSpeech = false, speechError = null)
        }
    }

    fun setListeningSpeech(listening: Boolean) {
        _uiState.update { it.copy(isListeningSpeech = listening, speechError = null) }
    }

    fun setSpeechError(error: String) {
        _uiState.update { it.copy(isListeningSpeech = false, speechError = error) }
    }

    // HU10: Envío explícito de mensajes por acción deliberada del repartidor.
    fun sendMessage() {
        val text = _uiState.value.inputText.trim()
        if (text.isBlank()) return

        val orderId = currentOrderId ?: "demo_order"
        val channel = _uiState.value.activeChannel

        val newMessage = ChatMessage(
            id = UUID.randomUUID().toString(),
            orderId = orderId,
            channel = channel,
            senderName = "Tú (Repartidor)",
            senderRole = ChatSenderRole.RIDER,
            text = text,
            timestamp = System.currentTimeMillis()
        )

        _uiState.update {
            it.copy(
                messages = it.messages + newMessage,
                inputText = ""
            )
        }
    }

    // HU10: Sincronización de reglas de negocio por fase de pedido.
    fun syncOrderState(snapshot: OfferSnapshot?) {
        if (snapshot == null) {
            _uiState.update { ChatUiState() }
            return
        }

        val orderId = snapshot.batch?.stops?.firstOrNull()?.orderId ?: "demo_order"
        if (currentOrderId != orderId) {
            currentOrderId = orderId
            val merchantName = snapshot.merchantName ?: "Establecimiento"
            val initialMessages = listOf(
                ChatMessage(
                    id = "init_merchant",
                    orderId = orderId,
                    channel = ChatChannel.MERCHANT,
                    senderName = merchantName,
                    senderRole = ChatSenderRole.MERCHANT,
                    text = "Hola repartidor, el pedido está en preparación en nuestro local.",
                    timestamp = snapshot.serverTime - 120_000
                ),
                ChatMessage(
                    id = "init_customer",
                    orderId = orderId,
                    channel = ChatChannel.CUSTOMER,
                    senderName = "Cliente",
                    senderRole = ChatSenderRole.CUSTOMER,
                    text = "Hola! Por favor tocar el timbre al llegar, gracias.",
                    timestamp = snapshot.serverTime - 60_000
                )
            )
            _uiState.update { it.copy(messages = initialMessages) }
        }

        // HU10 Criterio 4: Chat del establecimiento solo permite mensajes durante el recojo (!pickedUp)
        val merchantEnabled = !snapshot.pickedUp && !snapshot.isDelivered
        // HU10 Criterio 5: Chat del cliente pasa a solo lectura al terminar el periodo (!isDelivered)
        val customerEnabled = !snapshot.isDelivered

        _uiState.update {
            val targetChannel = if (!merchantEnabled && it.activeChannel == ChatChannel.MERCHANT) ChatChannel.CUSTOMER else it.activeChannel
            it.copy(
                isMerchantChannelEnabled = merchantEnabled,
                isCustomerChannelEnabled = customerEnabled,
                activeChannel = targetChannel
            )
        }
    }
}
