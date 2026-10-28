package com.example.chaskirider.ui.screens.support

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.chaskirider.domain.support.*
import com.example.chaskirider.ui.components.AppHeader
import java.text.DateFormat
import java.util.Date

@Composable
fun SupportRoute(viewModel: SupportViewModel, onBack: () -> Unit) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    fun back() { if (!viewModel.back()) onBack() }
    BackHandler(onBack = ::back)
    SupportScreen(state, ::back, viewModel::selectCategory, viewModel::selectTopic,
        viewModel::openForm, viewModel::changeBody, viewModel::linkOrder,
        viewModel::createTicket, viewModel::discardDraft, viewModel::openTicket,
        viewModel::changeReply, viewModel::sendReply, viewModel::refresh, viewModel::prepareEmergency)
}

@Composable
fun SupportScreen(state: SupportUiState, onBack: () -> Unit,
    onCategory: (SupportCategory) -> Unit, onTopic: (SupportTopic) -> Unit,
    onForm: () -> Unit, onBody: (String) -> Unit, onLink: (Boolean) -> Unit,
    onCreate: () -> Unit, onDiscard: () -> Unit, onTicket: (String) -> Unit,
    onReply: (String) -> Unit, onSend: () -> Unit, onRefresh: () -> Unit, onEmergency: () -> Unit = {}) {
    var discardConfirmation by remember { mutableStateOf(false) }
    var emergencyConfirmation by remember { mutableStateOf(false) }
    val scroll = rememberScrollState()
    LaunchedEffect(state.category, state.topic, state.formOpen, state.selectedTicketId) { scroll.scrollTo(0) }
    val ticket = state.tickets.firstOrNull { it.id == state.selectedTicketId }
    val title = when {
        state.selectedTicketId != null -> "Mi ticket"
        state.formOpen -> "Contactar a soporte"
        state.topic != null -> state.topic.title
        state.category != null -> state.category.title
        else -> "Ayuda y soporte"
    }
    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().imePadding().padding(horizontal = 20.dp)) {
        AppHeader(title, onBack)
        Column(Modifier.weight(1f).verticalScroll(scroll).padding(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)) {
            if (state.loading || state.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
            state.error?.let {
                SupportNotice(it, error = true)
                TextButton(onClick = onRefresh, enabled = !state.busy) { Text("Volver a intentar") }
            }
            state.notice?.let { SupportNotice(it) }
            when {
                state.loading -> Text("Recuperando tu información…")
                state.selectedTicketId != null -> {
                    if (ticket == null) Text("Esperando los datos del ticket…")
                    else {
                        Text(ticket.topic.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(ticketStatus(ticket), color = MaterialTheme.colorScheme.primary)
                        Text(formatTime(ticket.createdAt), style = MaterialTheme.typography.bodySmall)
                        ticket.order?.let { OrderContextCard(it) }
                        ticket.location?.let { Text("Ubicación adjunta: ${it.latitude}, ${it.longitude}") }
                        Text("Abrir un ticket no cancela ni completa un pedido.", style = MaterialTheme.typography.bodySmall)
                        (ticket.messages + state.conversation).forEach { message ->
                            Surface(color = if (message.sender == SupportSender.SUPPORT) MaterialTheme.colorScheme.secondaryContainer
                                else MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
                                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(if (message.sender == SupportSender.SUPPORT) "Soporte" else "Tú", fontWeight = FontWeight.Bold)
                                    Text(message.body)
                                    Text(if (message.pending) "Pendiente de envío" else formatTime(message.createdAt), style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                        if (ticket.status == SupportTicketStatus.CLOSED) Text("Este ticket está cerrado. Puedes abrir otro desde Ayuda si necesitas asistencia.")
                        else {
                            OutlinedTextField(value = state.reply?.body.orEmpty(), onValueChange = onReply,
                                enabled = !state.busy && !state.replyLoading && state.reply != null,
                                label = { Text("Añadir información") }, minLines = 2, maxLines = 5,
                                supportingText = { Text("Hasta 2000 caracteres") }, modifier = Modifier.fillMaxWidth().testTag("support_reply"))
                            Button(onClick = onSend, enabled = !state.busy && SupportValidation.validReply(state.reply?.body.orEmpty()),
                                modifier = Modifier.fillMaxWidth()) { Text("Enviar mensaje") }
                        }
                    }
                }
                state.formOpen -> {
                    val draft = state.draft
                    Text(draft?.topic?.title.orEmpty(), fontWeight = FontWeight.Bold)
                    Text("Explica qué ocurrió y qué ayuda necesitas. No incluyas contraseñas ni datos bancarios completos.")
                    if (draft != null && draft.topic?.category in listOf(SupportCategory.ORDER, SupportCategory.SAFETY)) {
                        if (draft.order != null) OrderContextCard(draft.order)
                        if (state.availableOrder != null && draft.order?.problemId == null && draft.topic?.category == SupportCategory.ORDER) {
                            Row {
                                Checkbox(checked = draft.order != null, onCheckedChange = onLink, enabled = !state.busy)
                                Text("Vincular al pedido activo", Modifier.padding(top = 12.dp))
                            }
                        }
                    }
                    if (draft?.topic == SupportTopic.EMERGENCY) {
                        Text("Caso urgente · El pedido permanece activo", color = MaterialTheme.colorScheme.error)
                        Text(if (draft.location != null) "Ubicación adjunta (${formatTime(draft.location.capturedAt)})"
                            else "Sin ubicación adjunta. Indica dónde estás en la descripción.")
                        TextButton(onClick = { emergencyConfirmation = true }, enabled = !state.busy) { Text("Actualizar ubicación") }
                    }
                    OutlinedTextField(value = draft?.body.orEmpty(), onValueChange = onBody,
                        enabled = !state.busy && draft != null, label = { Text("Describe tu problema") },
                        supportingText = { Text("Entre 10 y 2000 caracteres. Tu borrador se guarda en este dispositivo.") },
                        minLines = 4, maxLines = 8, modifier = Modifier.fillMaxWidth().testTag("support_body"))
                    Button(onClick = onCreate, enabled = !state.busy && draft?.topic != null && SupportValidation.validBody(draft.body),
                        modifier = Modifier.fillMaxWidth()) { Text(if (draft?.topic == SupportTopic.EMERGENCY) "Enviar caso urgente" else "Crear ticket") }
                    TextButton(onClick = { discardConfirmation = true }, enabled = !state.busy) { Text("Descartar borrador") }
                }
                state.topic != null -> {
                    Text(state.topic.guidance, style = MaterialTheme.typography.bodyLarge)
                    Button(onClick = { if (state.topic == SupportTopic.EMERGENCY) emergencyConfirmation = true else onForm() },
                        enabled = state.draft != null && !state.busy, modifier = Modifier.fillMaxWidth()) {
                        Text(if (state.topic == SupportTopic.EMERGENCY) "Informar a soporte" else "Necesito más ayuda") }
                }
                state.category != null -> {
                    Text("Selecciona lo que necesitas resolver.")
                    SupportTopic.entries.filter { it.category == state.category }.forEach { topic ->
                        OutlinedButton(onClick = { onTopic(topic) }, modifier = Modifier.fillMaxWidth()) { Text(topic.title) }
                    }
                }
                else -> {
                    Text("¿En qué podemos ayudarte?", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    SupportCategory.entries.forEach { category ->
                        OutlinedButton(onClick = { onCategory(category) }, modifier = Modifier.fillMaxWidth()) { Text(category.title) }
                    }
                    if (state.draft?.body?.isNotBlank() == true) {
                        TextButton(onClick = { state.draft.topic?.let(onTopic); onForm() }) { Text("Continuar mi borrador") }
                    }
                    HorizontalDivider()
                    Text("Mis tickets", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Conéctate para enviar tickets y recibir respuestas.", style = MaterialTheme.typography.bodySmall)
                    if (state.tickets.isEmpty()) Text("Tus tickets registrados aparecerán aquí.", style = MaterialTheme.typography.bodyMedium)
                    state.tickets.forEach { item ->
                        OutlinedCard(onClick = { onTicket(item.id) }, modifier = Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(item.topic.title, fontWeight = FontWeight.Bold)
                                Text(ticketStatus(item), color = MaterialTheme.colorScheme.primary)
                                Text(formatTime(item.createdAt), style = MaterialTheme.typography.bodySmall)
                                item.messages.lastOrNull()?.let { Text(it.body, maxLines = 2) }
                            }
                        }
                    }
                }
            }
        }
    }
    if (discardConfirmation) AlertDialog(onDismissRequest = { discardConfirmation = false },
        title = { Text("¿Descartar el borrador?") }, text = { Text("Se eliminará el texto que todavía no enviaste.") },
        confirmButton = { TextButton(onClick = { onDiscard(); discardConfirmation = false }) { Text("Descartar") } },
        dismissButton = { TextButton(onClick = { discardConfirmation = false }) { Text("Conservar") } })
    if (emergencyConfirmation) AlertDialog(onDismissRequest = { emergencyConfirmation = false },
        title = { Text("¿Adjuntar tu ubicación al caso?") },
        text = { Text("Consultaremos tu ubicación actual para compartirla con soporte al enviar el caso. No se enviará hasta que revises la descripción y pulses Enviar caso urgente. No cancela el pedido ni llama a los servicios de emergencia.") },
        confirmButton = { TextButton(onClick = { emergencyConfirmation = false; onEmergency() }) { Text("Continuar") } },
        dismissButton = { TextButton(onClick = { emergencyConfirmation = false }) { Text("Volver") } })
}

@Composable
private fun SupportNotice(text: String, error: Boolean = false) {
    Surface(color = if (error) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.secondaryContainer,
        shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) { Text(text, Modifier.padding(12.dp)) }
}

@Composable
private fun OrderContextCard(order: SupportOrderContext) {
    Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Pedido vinculado", fontWeight = FontWeight.Bold)
            Text(order.merchantName)
            Text("Recojo: ${order.pickupAddress}", style = MaterialTheme.typography.bodySmall)
            Text("Entrega: ${order.deliveryAddress}", style = MaterialTheme.typography.bodySmall)
            Text("Estado: " + when (order.orderStatus ?: if (order.pickedUp)
                com.example.chaskirider.domain.orders.OrderStatus.ON_THE_WAY else com.example.chaskirider.domain.orders.OrderStatus.ACCEPTED) {
                com.example.chaskirider.domain.orders.OrderStatus.ACCEPTED -> "Aceptado"
                com.example.chaskirider.domain.orders.OrderStatus.AT_MERCHANT -> "En el local"
                com.example.chaskirider.domain.orders.OrderStatus.ON_THE_WAY -> "En camino al cliente"
                com.example.chaskirider.domain.orders.OrderStatus.DELIVERED -> "Entregado"
                com.example.chaskirider.domain.orders.OrderStatus.CANCELLED -> "Cancelado"
            }, style = MaterialTheme.typography.bodySmall)
            order.problemCategory?.let { Text("Incidencia: " + when (it) {
                com.example.chaskirider.domain.orders.OrderProblemReason.LOCAL_CLOSED -> "Local cerrado"
                com.example.chaskirider.domain.orders.OrderProblemReason.ORDER_UNAVAILABLE -> "Pedido no disponible"
                com.example.chaskirider.domain.orders.OrderProblemReason.VEHICLE_PROBLEM -> "Problema con el vehículo"
                com.example.chaskirider.domain.orders.OrderProblemReason.CUSTOMER_UNREACHABLE -> "Cliente no responde"
                com.example.chaskirider.domain.orders.OrderProblemReason.ADDRESS_PROBLEM -> "Problema con la dirección"
                else -> "Otro problema"
            }) }
        }
    }
}

private fun ticketStatus(ticket: SupportTicket) = when {
    ticket.pending -> "Pendiente de envío"
    ticket.status == SupportTicketStatus.CLOSED -> "Cerrado"
    else -> "Abierto"
}
private fun formatTime(time: Long) = if (time <= 0) "Fecha pendiente de confirmación" else
    DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(time))
