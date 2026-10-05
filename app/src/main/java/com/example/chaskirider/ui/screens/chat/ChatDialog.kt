package com.example.chaskirider.ui.screens.chat

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.chaskirider.domain.chat.ChatChannel
import com.example.chaskirider.domain.chat.ChatMessage
import com.example.chaskirider.domain.chat.ChatSenderRole
import com.example.chaskirider.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ChatDialog(
    state: ChatUiState,
    onClose: () -> Unit,
    onSelectChannel: (ChatChannel) -> Unit,
    onUpdateInputText: (String) -> Unit,
    onSpeechResult: (String) -> Unit,
    onSendMessage: () -> Unit
) {
    if (!state.isOpen) return

    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val matches = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val recognizedText = matches?.firstOrNull().orEmpty()
            if (recognizedText.isNotBlank()) {
                onSpeechResult(recognizedText)
            }
        }
    }

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        ChatDialogContent(
            state = state,
            onClose = onClose,
            onSelectChannel = onSelectChannel,
            onUpdateInputText = onUpdateInputText,
            onStartDictation = {
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es-PE")
                    putExtra(RecognizerIntent.EXTRA_PROMPT, "Dicta tu mensaje para la entrega...")
                }
                runCatching { speechLauncher.launch(intent) }
            },
            onSendMessage = onSendMessage
        )
    }
}

@Composable
fun ChatDialogContent(
    state: ChatUiState,
    onClose: () -> Unit,
    onSelectChannel: (ChatChannel) -> Unit,
    onUpdateInputText: (String) -> Unit,
    onStartDictation: () -> Unit,
    onSendMessage: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth(0.94f)
            .fillMaxHeight(0.86f),
        shape = RoundedCornerShape(24.dp),
        tonalElevation = 8.dp,
        color = BackgroundLight
    ) {
        Column(Modifier.fillMaxSize()) {
            // Header styled similar to ProfileHeaderCard / App Theme
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Orange, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    .padding(20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "💬 Chat del Pedido",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Comunicación en tiempo real",
                        fontSize = 13.sp,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                }
                IconButton(
                    onClick = onClose,
                    modifier = Modifier
                        .background(Color.White.copy(alpha = 0.2f), CircleShape)
                        .size(36.dp)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = Color.White)
                }
            }

            // HU10 Criterio 2: Diferenciación clara entre chat del cliente y del establecimiento
            TabRow(
                selectedTabIndex = state.activeChannel.ordinal,
                containerColor = Color.White,
                contentColor = Orange
            ) {
                Tab(
                    selected = state.activeChannel == ChatChannel.MERCHANT,
                    onClick = { onSelectChannel(ChatChannel.MERCHANT) },
                    text = {
                        Text(
                            text = "Establecimiento" + if (!state.isMerchantChannelEnabled) " (Lectura)" else "",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                )
                Tab(
                    selected = state.activeChannel == ChatChannel.CUSTOMER,
                    onClick = { onSelectChannel(ChatChannel.CUSTOMER) },
                    text = {
                        Text(
                            text = "Cliente" + if (!state.isCustomerChannelEnabled) " (Lectura)" else "",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                )
            }

            val filteredMessages = remember(state.messages, state.activeChannel) {
                state.messages.filter { it.channel == state.activeChannel }
            }

            // Messages list
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredMessages, key = { it.id }) { msg ->
                    ChatMessageItem(msg)
                }
            }

            val currentChannelEnabled = if (state.activeChannel == ChatChannel.MERCHANT) {
                state.isMerchantChannelEnabled
            } else {
                state.isCustomerChannelEnabled
            }

            if (!currentChannelEnabled) {
                Surface(
                    color = Color.White,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, BorderLight)
                ) {
                    Text(
                        text = if (state.activeChannel == ChatChannel.MERCHANT)
                            "🔒 El chat con el establecimiento finalizó al recoger el pedido."
                        else "🔒 El chat con el cliente finalizó al entregar el pedido.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            } else {
                // Input Bar with YouTube-style voice dictation button & Send button
                Surface(
                    tonalElevation = 4.dp,
                    color = Color.White,
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, BorderLight, RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                ) {
                    Row(
                        modifier = Modifier
                            .padding(12.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = state.inputText,
                            onValueChange = onUpdateInputText,
                            placeholder = { Text("Escribe o dicta un mensaje...") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(22.dp),
                            maxLines = 3
                        )

                        // Send button
                        IconButton(
                            onClick = onSendMessage,
                            enabled = state.inputText.isNotBlank(),
                            modifier = Modifier
                                .size(46.dp)
                                .clip(CircleShape)
                                .background(
                                    if (state.inputText.isNotBlank()) Orange
                                    else BorderLight
                                )
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Enviar",
                                tint = if (state.inputText.isNotBlank()) Color.White else TextMuted
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatMessageItem(message: ChatMessage) {
    val isRider = message.senderRole == ChatSenderRole.RIDER
    val alignment = if (isRider) Alignment.End else Alignment.Start
    val bubbleColor = if (isRider) Orange else Color.White
    val textColor = if (isRider) Color.White else TextDark

    val timeFormatter = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val timeStr = remember(message.timestamp) { timeFormatter.format(Date(message.timestamp)) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = alignment
    ) {
        // HU10 Criterio 3: Registro de remitente y hora por mensaje
        Text(
            text = "${message.senderName} • $timeStr",
            style = MaterialTheme.typography.labelSmall,
            color = TextMuted,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
        )

        Surface(
            color = bubbleColor,
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isRider) 16.dp else 4.dp,
                bottomEnd = if (isRider) 4.dp else 16.dp
            ),
            border = if (!isRider) BorderStroke(1.dp, BorderLight) else null,
            shadowElevation = 1.dp
        ) {
            Text(
                text = message.text,
                color = textColor,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 380, heightDp = 600)
@Composable
fun ChatDialogPreview() {
    val sampleState = ChatUiState(
        isOpen = true,
        activeChannel = ChatChannel.MERCHANT,
        inputText = "Estoy afuera del establecimiento, ya casi llego.",
        messages = listOf(
            ChatMessage(
                id = "1",
                orderId = "order_123",
                channel = ChatChannel.MERCHANT,
                senderName = "Restaurante Central",
                senderRole = ChatSenderRole.MERCHANT,
                text = "Hola repartidor, tu pedido está casi listo para empaquetar.",
                timestamp = System.currentTimeMillis() - 300_000
            ),
            ChatMessage(
                id = "2",
                orderId = "order_123",
                channel = ChatChannel.MERCHANT,
                senderName = "Tú (Repartidor)",
                senderRole = ChatSenderRole.RIDER,
                text = "Entendido, estoy a 2 minutos del local.",
                timestamp = System.currentTimeMillis() - 120_000
            )
        )
    )

    ChaskiRiderTheme {
        ChatDialogContent(
            state = sampleState,
            onClose = {},
            onSelectChannel = {},
            onUpdateInputText = {},
            onStartDictation = {},
            onSendMessage = {}
        )
    }
}
