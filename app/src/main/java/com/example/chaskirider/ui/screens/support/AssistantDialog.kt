package com.example.chaskirider.ui.screens.support

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.chaskirider.domain.support.SupportSender

@Composable
fun AssistantDialog(state: AssistantUiState, onBody: (String) -> Unit, onSend: () -> Unit,
    onHuman: () -> Unit, onResolve: () -> Unit, onTicket: (String) -> Unit, onClose: () -> Unit) {
    val list = rememberLazyListState()
    LaunchedEffect(state.messages.size) { if (state.messages.isNotEmpty()) list.animateScrollToItem(state.messages.lastIndex) }
    OrderHelpPage("Asistente Chaski", onClose) {
            Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("No compartas tu PIN ni contraseñas.", style = MaterialTheme.typography.bodySmall)
                if (state.loading || state.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
                state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                LazyColumn(Modifier.fillMaxWidth().weight(1f), state = list, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (state.messages.isEmpty()) item { Text("¿En qué puedo ayudarte? Puedes consultar los pasos pendientes o pedir un asesor humano.") }
                    items(state.messages, key = { it.id }) { message ->
                        Surface(color = if (message.sender == SupportSender.RIDER) MaterialTheme.colorScheme.surfaceVariant
                            else MaterialTheme.colorScheme.secondaryContainer) {
                            Column(Modifier.fillMaxWidth().padding(10.dp)) {
                                Text(if (message.sender == SupportSender.RIDER) "Tú" else if (message.mode == "GEMINI") "Gemini · Asistente Chaski" else "Soporte", fontWeight = FontWeight.Bold)
                                Text(message.body)
                            }
                        }
                    }
                }
                if (state.canResolve && !state.needsHuman && !state.closed) {
                    Text("Si el cliente sigue ausente, puedes finalizar el caso.")
                    Button(onClick = onResolve, enabled = !state.busy) { Text("El cliente sigue ausente · Finalizar") }
                }
                if (!state.closed && !state.needsHuman) {
                    OutlinedTextField(value = state.draft?.body.orEmpty(), onValueChange = onBody,
                        enabled = !state.busy && !state.loading, label = { Text("Escribe tu consulta") },
                        minLines = 1, maxLines = 3, modifier = Modifier.fillMaxWidth())
                    Button(onClick = onSend, enabled = !state.busy && !state.loading && state.draft?.body?.isNotBlank() == true) { Text("Enviar") }
                    TextButton(onClick = onHuman, enabled = !state.busy && !state.loading) { Text("Solicitar asesor humano") }
                } else Text(if (state.closed) "Caso cerrado. Consulta la resolución en Mis tickets."
                    else "El caso está pendiente de un asesor humano. Continúa la conversación en Mis tickets.")
                state.ticketId?.let { id -> TextButton(onClick = { onTicket(id) }, enabled = !state.busy) { Text("Ver ticket") } }
            }
        }
}
